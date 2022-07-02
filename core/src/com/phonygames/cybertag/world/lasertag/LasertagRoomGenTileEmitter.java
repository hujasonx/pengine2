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
      vertexProcessor.setFlatQuad(tile010.y(tile010.y() - .1f), tile110.y(tile110.y() - .1f),
                                  tile111.y(tile111.y() - .1f), tile011.y(tile011.y() - .1f));
      floorTemplate.emit(modelGen, vertexProcessor, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    emitWall(tile.wallX, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, vertexProcessor, pool);
    emitWall(tile.wallZ, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, vertexProcessor, pool);
    emitWall(tile.wallMX, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, vertexProcessor, pool);
    emitWall(tile.wallMZ, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, vertexProcessor, pool);
    tile.tileVColIndexStart = tileVColIndex;
    tileVColIndex += LasertagTile.PER_TILE_VCOL_INDICES;
    pool.finish();
    PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
    return tileVColIndex;
  }

  private static void emitWall(LasertagTileWall wall, PModelGen modelGen, PModelGen.Part basePart,
                               PModelGen.StaticPhysicsPart staticPhysicsPart, int tileVColIndex,
                               PList<PModelGen.Part> alphaBlendParts, PModelGen.Part.VertexProcessor vertexProcessor,
                               PPool.PoolBuffer pool) {
    if (!wall.hasWall) {return;}
    PAssert.isTrue(wall.isValid);
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile110 = pool.vec3(), tile001 =
        pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    wall.tile.getCornersFloorCeiling(tile000, tile001, tile010, tile011, tile100, tile101, tile110, tile111);
    if (wall.isWindow) { //TODO this is test code.
      tile010.y(tile000.y() + .2f);
      tile011.y(tile001.y() + .2f);
      tile111.y(tile101.y() + .2f);
      tile110.y(tile100.y() + .2f);
    }
    switch (wall.facing) {
      case X:
        vertexProcessor.setWall(tile000, tile010.y() - tile000.y(), tile001, tile011.y() - tile001.y());
        break;
      case Z:
        vertexProcessor.setWall(tile100, tile110.y() - tile100.y(), tile000, tile010.y() - tile000.y());
        break;
      case mX:
        vertexProcessor.setWall(tile101, tile111.y() - tile101.y(), tile100, tile110.y() - tile100.y());
        break;
      case mZ:
        vertexProcessor.setWall(tile001, tile011.y() - tile001.y(), tile101, tile111.y() - tile101.y());
        break;
    }
//    if (wall.isSolidWall) {
      MeshTemplate wallTemplate = MeshTemplate.get("model/template/wall/basic.glb");
      wallTemplate.emit(modelGen, vertexProcessor, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
//    }
  }
}
