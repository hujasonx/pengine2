package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

public class LasertagRoomGenTileEmitter {
  /**
   * Emits tile model data for a room model.
   * @param tile
   * @param modelGen
   * @param basePart
   * @param staticPhysicsPart
   * @param tileVColIndex
   * @param alphaBlendParts
   * @return The new tileVColIndex.
   */
  public static int emit(LasertagTile tile, PModelGen modelGen, PModelGen.Part basePart,
                         PModelGen.StaticPhysicsPart staticPhysicsPart, int tileVColIndex,
                         PList<PModelGen.Part> alphaBlendParts) {
    LasertagRoom lasertagRoom = tile.room;
    PAssert.isNotNull(lasertagRoom, "Tiles without owner rooms are not supported yet!");
    PModelGen.Part.VertexProcessor vertexProcessor = PModelGen.Part.VertexProcessor.staticPool().obtain();
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile110 = pool.vec3(), tile001 =
        pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    tile.getCornersFloorCeiling(tile000, tile001, tile010, tile011, tile100, tile101, tile110, tile111);
    if (tile.hasFloor) {
      MeshTemplate floorTemplate = MeshTemplate.get("model/template/floor/basic.glb");
      vertexProcessor.setFlatQuad(tile000, tile100, tile101, tile001);
      floorTemplate.emit(modelGen, vertexProcessor, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (tile.hasCeiling) {
      MeshTemplate floorTemplate = MeshTemplate.get("model/template/floor/basic.glb");
      vertexProcessor.setFlatQuad(tile010, tile110, tile111, tile011);
      floorTemplate.emit(modelGen, vertexProcessor, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (tile.wallX.valid) {
      vertexProcessor.setWall(tile000, tile010.y() - tile000.y(), tile001, tile011.y() - tile001.y());
      MeshTemplate wallTemplate = MeshTemplate.get("model/template/wall/basic.glb");
      wallTemplate.emit(modelGen, vertexProcessor, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    tile.tileVColIndexStart = tileVColIndex;
    tileVColIndex += LasertagTile.PER_TILE_VCOL_INDICES;
    pool.finish();
    PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
    return tileVColIndex;
  }
}
