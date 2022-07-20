package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelGenTemplate;
import com.phonygames.pengine.graphics.model.PModelGenTemplateOptions;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

public class LasertagRoomGenTileEmitter {
  private static float floorAndWalkwayVerticalOffset = .01f;

  /**
   * Emits tile model data for a room model.
   *
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
    PModelGenTemplateOptions options = PModelGenTemplateOptions.obtainDefault();
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile110 = pool.vec3(), tile001 =
        pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    PVec3 floorUp = pool.vec3(0, floorAndWalkwayVerticalOffset, 0);
    tile.getCornersFloorCeiling(tile000, tile001, tile010, tile011, tile100, tile101, tile110, tile111);
    if (tile.hasFloor) {
      tile000.add(floorUp);
      tile001.add(floorUp);
      tile101.add(floorUp);
      tile100.add(floorUp);
      PModelGenTemplate floorTemplate = PModelGenTemplate.get("model/template/floor/bigsquare.glb");
      options.setFlatQuad(tile000, tile100, tile101, tile001);
      floorTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (tile.hasCeiling) {
      PModelGenTemplate ceilingTemplate = PModelGenTemplate.get("model/template/ceiling/basic.glb");
      options.setFlatQuad(tile010, tile110, tile111, tile011);
      ceilingTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (tile.hasWalkway) {
      tile.getCornersWalkway(tile000, tile100, tile101, tile001);
      tile000.add(floorUp);
      tile001.add(floorUp);
      tile101.add(floorUp);
      tile100.add(floorUp);
      PModelGenTemplate floorTemplate = PModelGenTemplate.get("model/template/floor/bigsquare.glb");
      options.setFlatQuad(tile000, tile100, tile101, tile001);
      floorTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
      PModelGenTemplate ceilingTemplate = PModelGenTemplate.get("model/template/ceiling/basic.glb");
      ceilingTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    emitWall(tile.wallX, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    emitWall(tile.wallZ, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    emitWall(tile.wallMX, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    emitWall(tile.wallMZ, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    tile.tileVColIndexStart = tileVColIndex;
    tileVColIndex += LasertagTile.PER_TILE_VCOL_INDICES;
    pool.free();
    options.free();
    return tileVColIndex;
  }

  private static void emitWall(LasertagTileWall wall, PModelGen modelGen, PModelGen.Part basePart,
                               PModelGen.StaticPhysicsPart staticPhysicsPart, int tileVColIndex,
                               PList<PModelGen.Part> alphaBlendParts, PModelGenTemplateOptions options,
                               PPool.PoolBuffer pool) {
    if (!wall.hasWall) {return;}
    PAssert.isTrue(wall.isValid);
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile110 = pool.vec3(), tile001 =
        pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    wall.tile.getCornersFloorCeiling(tile000, tile001, tile010, tile011, tile100, tile101, tile110, tile111);
    if (wall.isWindow) { //TODO this is test code.
      tile010.y(tile000.y() + 1);
      tile011.y(tile001.y() + 1);
      tile111.y(tile101.y() + 1);
      tile110.y(tile100.y() + 1);
    }
    switch (wall.facing) {
      case X:
        options.setWall(tile000, tile010.y() - tile000.y(), tile001, tile011.y() - tile001.y());
        break;
      case Z:
        options.setWall(tile100, tile110.y() - tile100.y(), tile000, tile010.y() - tile000.y());
        break;
      case mX:
        options.setWall(tile101, tile111.y() - tile101.y(), tile100, tile110.y() - tile100.y());
        break;
      case mZ:
        options.setWall(tile001, tile011.y() - tile001.y(), tile101, tile111.y() - tile101.y());
        break;
    }
    if (wall.isSolidWall) {
      PModelGenTemplate wallTemplate = PModelGenTemplate.get("model/template/wall/basic.glb");
      wallTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.hasDoorframeR) {
      PModelGenTemplate doorframeRTemplate = PModelGenTemplate.get("model/template/doorframe/right/basic.glb");
      doorframeRTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.hasDoorframeL) {
      PModelGenTemplate doorframeLTemplate = PModelGenTemplate.get("model/template/doorframe/left/basic.glb");
      doorframeLTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.hasDoorframeT) {
      PModelGenTemplate doorframeTTemplate = PModelGenTemplate.get("model/template/doorframe/top/basic.glb");
      doorframeTTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
  }
}
