package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.cybertag.world.ColorDataEmitter;
import com.phonygames.cybertag.world.lasertag.emit.LasertagRoomTemplateSelector;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PSet;

import lombok.val;

public class LasertagRoomGen extends PBuilder {
  protected final LasertagBuildingGen buildingGen;
  protected final PMap<LasertagRoomGen, PVec1> connectedRoomConnectionSizes = new PMap<>(PVec1.getStaticPool());
  protected final PSet<LasertagRoomGen> directlyConnectedRooms = new PSet<>();
  protected final PSet<LasertagRoomGen> horizontallyAdjacentRooms = new PSet<>();
  protected final LasertagRoom lasertagRoom;
  protected final PList<LasertagRoomWallGen> roomWallGens = new PList<>();
  protected final PIntMap3d<LasertagTileGen> tileGens = new PIntMap3d<>();
  protected final PSet<LasertagRoomGen> verticallyAdjacentRooms = new PSet<>();
  protected LasertagRoomTemplateSelector templateSelector;

  public LasertagRoomGen(@NonNull LasertagBuildingGen buildingGen, PIntAABB aabb) {
    this.buildingGen = buildingGen;
    lasertagRoom = new LasertagRoom(buildingGen.building.id + ":room" + buildingGen.roomGens.size());
    buildingGen.roomGens.add(this);
    lasertagRoom.building = buildingGen.building;
    for (int x = aabb.x0(); x <= aabb.x1(); x++) {
      for (int y = aabb.y0(); y <= aabb.y1(); y++) {
        for (int z = aabb.z0(); z <= aabb.z1(); z++) {
          LasertagTileGen tileGen = buildingGen.tileGens.get(x, y, z);
          markTileWithSelf(tileGen);
        }
      }
    }
  }

  private void markTileWithSelf(@Nullable LasertagTileGen tileGen) {
    if (tileGen == null || tileGen.tile.room != null) {return;}
    tileGen.tile.room = lasertagRoom;
    tileGen.roomGen = this;
    int x = tileGen.x;
    int y = tileGen.y;
    int z = tileGen.z;
    lasertagRoom.tiles().put(x, y, z, tileGen.tile);
    tileGens.put(x, y, z, tileGen);
    markTileGenRoomAsAdjacent(buildingGen.tileGens.get(x, y + 1, z), false);
    markTileGenRoomAsAdjacent(buildingGen.tileGens.get(x, y - 1, z), false);
    markTileGenRoomAsAdjacent(buildingGen.tileGens.get(x + 1, y + 1, z), true);
    markTileGenRoomAsAdjacent(buildingGen.tileGens.get(x - 1, y + 1, z), true);
    markTileGenRoomAsAdjacent(buildingGen.tileGens.get(x, y + 1, z + 1), true);
    markTileGenRoomAsAdjacent(buildingGen.tileGens.get(x, y + 1, z - 1), true);
  }

  private void markTileGenRoomAsAdjacent(@Nullable LasertagTileGen otherTileGen, boolean horizontal) {
    if (otherTileGen == null || otherTileGen.roomGen == null || otherTileGen.roomGen == this) {return;}
    (horizontal ? otherTileGen.roomGen.horizontallyAdjacentRooms : otherTileGen.roomGen.verticallyAdjacentRooms).add(
        this);
    (horizontal ? horizontallyAdjacentRooms : verticallyAdjacentRooms).add(otherTileGen.roomGen);
  }

  public LasertagRoomGen(@NonNull LasertagBuildingGen buildingGen, PList<LasertagTileGen> tileGens) {
    this.buildingGen = buildingGen;
    lasertagRoom = new LasertagRoom(buildingGen.building.id + ":room" + buildingGen.roomGens.size());
    buildingGen.roomGens.add(this);
    lasertagRoom.building = buildingGen.building;
    for (int a = 0; a < tileGens.size(); a++) {
      markTileWithSelf(tileGens.get(a));
    }
  }

  public LasertagRoom build() {
    lockBuilder();
    buildModelInstance();
    return lasertagRoom;
  }

  private void buildModelInstance() {
    if (templateSelector == null) {
      createTemplateSelectorDefault();
    }
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
        try (val it = tileGens.obtainIterator()) {
          while (it.hasNext()) {
            val e = it.next();
            tileVColIndex = LasertagRoomGenTileEmitter.emit(e.val(), this, basePart, staticPhysicsPart, tileVColIndex,
                                                            alphaBlendParts);
          }
        }
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        PModel.Builder builder = new PModel.Builder();
        builder.chainGlNode(glNodes, basePart.name(), basePart.getMesh(),
                            new PMaterial(basePart.name(), null).useVColIndex(true), null, PGltf.Layer.PBR, true,
                            false);
        //        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null).useVColIndex(true), null, PGltf
        //        .Layer.PBR,
        //                    true, false);
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode(basePart.name(), null, glNodes, PMat4.IDT);
        for (int a = 0; a < alphaBlendParts.size(); a++) {
          Part part = alphaBlendParts.get(a);
          glNodes.clear();
          builder.chainGlNode(glNodes, part.name(), part.getMesh(), new PMaterial(part.name(), null).useVColIndex(true), null, PGltf.Layer.AlphaBlend,
                      true, true);
          builder.addNode(part.name(), null, glNodes, PMat4.IDT);
        }
        lasertagRoom.modelInstance = new PModelInstance(builder.build());
        lasertagRoom.initialized = true;
        // Create the color data emitter buffer.
        int numVCols = lasertagRoom.numBaseVCols;
        try (val it = lasertagRoom.tiles().obtainIterator()) {
          while (it.hasNext()) {
            val e = it.next();
            numVCols += LasertagTile.PER_TILE_VCOL_INDICES;
          }
        }
        lasertagRoom.colorDataEmitter = new ColorDataEmitter(numVCols);
        lasertagRoom.modelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
      }
    });
  }

  private void createTemplateSelectorDefault() {
    this.templateSelector = new LasertagRoomTemplateSelector(this);
  }

  // Use actually placed door data, which should update directlyConnectedRooms.
  public void recalcRoomDistancesScores() {
    PAssert.failNotImplemented("recalcRoomDistancesScores");
    connectedRoomConnectionSizes.clear();
    try (val it = directlyConnectedRooms.obtainIterator()) {
      while (it.hasNext()) {
        val roomGen = it.next();
        connectedRoomConnectionSizes.genPooled(roomGen).x(1);
      }
    }
  }
}
