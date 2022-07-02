package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;

import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.phonygames.cybertag.world.ColorDataEmitter;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

import lombok.val;

public class LasertagRoomGen extends PBuilder {
  protected final LasertagBuildingGen buildingGen;
  protected final LasertagRoom lasertagRoom;
  private final PIntAABB roomAABB;
  protected final PIntMap3d<LasertagTileGen> tileGens = new PIntMap3d<>();
  protected final PList<LasertagRoomWallGen> roomWallGens = new PList<>();
  protected final ObjectFloatMap<LasertagRoomGen> connectedRoomsDistances = new ObjectFloatMap<>();
  protected final PList<LasertagRoomGen> directlyConnectedRooms = new PList<>();

  public LasertagRoomGen(@NonNull LasertagBuildingGen buildingGen, PIntAABB aabb) {
    this.buildingGen = buildingGen;
    lasertagRoom = new LasertagRoom(buildingGen.building.id + ":room" + buildingGen.roomGens.size);
    buildingGen.roomGens.add(this);
    lasertagRoom.building = buildingGen.building;
    this.roomAABB = aabb;
    for (int x = roomAABB.x0(); x <= roomAABB.x1(); x++) {
      for (int y = roomAABB.y0(); y <= roomAABB.y1(); y++) {
        for (int z = roomAABB.z0(); z <= roomAABB.z1(); z++) {
          LasertagTileGen tileGen = buildingGen.tilesBuilders.genUnpooled(x, y, z);
          if (tileGen.tile.room == null) { // Don't overwrite existing room data.
            tileGen.tile.room = lasertagRoom;
            tileGen.roomGen = this;
            lasertagRoom.tiles().put(x, y, z, tileGen.tile);
            tileGens.put(x, y, z, tileGen);
          }
        }
      }
    }
  }

  public LasertagRoom build() {
    lockBuilder();
    buildModelInstance();
    return lasertagRoom;
  }

  private void buildModelInstance() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      PList<Part> alphaBlendParts = new PList<>();
      Part basePart;
      StaticPhysicsPart staticPhysicsPart;
      int tileVColIndex = lasertagRoom.numBaseVCols;

      @Override protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getGLTF_UNSKINNED());
        staticPhysicsPart = addStaticPhysicsPart("staticPhysics");
      }

      @Override protected void modelMiddle() {
        for (val e : lasertagRoom.tiles()) {
          tileVColIndex = LasertagRoomGenTileEmitter.emit(e.val(), this, basePart, staticPhysicsPart, tileVColIndex,
                                                          alphaBlendParts);
        }
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        PModel.Builder builder = new PModel.Builder();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null).useVColIndex(true), null, PGltf.Layer.PBR,
                    true);
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode(basePart.name(), null, glNodes, PMat4.IDT);
        for (Part part : alphaBlendParts) {
          glNodes.clear();
          chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null).useVColIndex(true), null,
                      PGltf.Layer.AlphaBlend, true);
          builder.addNode(part.name(), null, glNodes, PMat4.IDT);
        }
        lasertagRoom.modelInstance = new PModelInstance(builder.build());
        lasertagRoom.initialized = true;
        // Create the color data emitter buffer.
        int numVCols = lasertagRoom.numBaseVCols;
        for (val e : lasertagRoom.tiles()) {
          numVCols += LasertagTile.PER_TILE_VCOL_INDICES;
        }
        lasertagRoom.colorDataEmitter = new ColorDataEmitter(numVCols);
        lasertagRoom.modelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
      }
    });
  }
}
