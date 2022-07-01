package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.val;

public class LasertagRoomGen extends PBuilder {
  private final LasertagBuildingGen buildingGen;
  private final LasertagRoom lasertagRoom;
  private final PIntAABB roomAABB;

  public LasertagRoomGen(LasertagBuildingGen buildingGen, PIntAABB aabb) {
    lasertagRoom = new LasertagRoom(buildingGen.building.id + ":room" + buildingGen.roomGens.size);
    buildingGen.roomGens.add(this);
    lasertagRoom.building = buildingGen.building;
    this.buildingGen = buildingGen;
    this.roomAABB = aabb;
    for (int x = roomAABB.x0(); x <= roomAABB.x1(); x++) {
      for (int y = roomAABB.y0(); y <= roomAABB.y1(); y++) {
        for (int z = roomAABB.z0(); z <= roomAABB.z1(); z++) {
          LasertagTileGen tileGen = buildingGen.tilesBuilders.genUnpooled(x, y, z);
          if (tileGen.tile.room == null) { // Don't overwrite existing room data.
            tileGen.tile.room = lasertagRoom;
            lasertagRoom.tiles().put(x, y, z, tileGen.tile);
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

      @Override protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getGLTF_UNSKINNED());
      }

      @Override protected void modelMiddle() {
        PModelGen.Part.VertexProcessor vertexProcessor = PModelGen.Part.VertexProcessor.staticPool().obtain();
        PPool.PoolBuffer pool = PPool.getBuffer();
        MeshTemplate meshTemplate = MeshTemplate.get("model/template/floor/basic.glb");
        PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile110 = pool.vec3(), tile001 =
            pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
        for (val e : lasertagRoom.tiles().iterator3d()) {
          int tileX = e.x();
          int tileY = e.y();
          int tileZ = e.z();
          LasertagTile tile = e.val();
          tile.getCornersFloorCeiling(tile000, tile001, tile010, tile011, tile100, tile101, tile110, tile111);
          vertexProcessor.setFlatQuad(tile000, tile100, tile101, tile001);
          meshTemplate.emit(this, vertexProcessor, basePart, staticPhysicsPart, 0, alphaBlendParts);
        }
        pool.finish();
        PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        PModel.Builder builder = new PModel.Builder();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null).useVColIndex(true), null, PGltf.Layer.PBR,
                    true);
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode(lasertagRoom.id, null, glNodes, PMat4.IDT);
        for (Part part : alphaBlendParts) {
          glNodes.clear();
          chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null).useVColIndex(true), null,
                      PGltf.Layer.AlphaBlend, true);
          builder.addNode(part.name(), null, glNodes, PMat4.IDT);
        }
        lasertagRoom.modelInstance = new PModelInstance(builder.build());
        lasertagRoom.initialized = true;
      }
    });
  }
}
