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
   * @param tileGen
   * @param modelGen
   * @param basePart
   * @param staticPhysicsPart
   * @param tileVColIndex
   * @param alphaBlendParts
   * @return The new tileVColIndex.
   */
  public static int emit(LasertagTileGen tileGen, PModelGen modelGen, PModelGen.Part basePart,
                         PModelGen.StaticPhysicsPart staticPhysicsPart, int tileVColIndex,
                         PList<PModelGen.Part> alphaBlendParts) {
    LasertagTile tile = tileGen.tile;
    tile.tileVColIndexStart = tileVColIndex;
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
      PModelGenTemplate floorTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.floorTemplate(tileGen));
      options.setFlatQuad(tile000, tile100, tile101, tile001);
      floorTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (tile.hasCeiling) {
      PModelGenTemplate ceilingTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.ceilingTemplate(tileGen));
      options.setFlatQuad(tile010, tile110, tile111, tile011);
      ceilingTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (tile.hasWalkway) {
      tile.getCornersWalkway(tile000, tile100, tile101, tile001);
      tile000.add(floorUp);
      tile001.add(floorUp);
      tile101.add(floorUp);
      tile100.add(floorUp);
      PModelGenTemplate floorTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.walkwayTemplateFloor(tileGen));
      options.setFlatQuad(tile000, tile100, tile101, tile001);
      floorTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
      PModelGenTemplate ceilingTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.walkwayTemplateCeiling(tileGen));
      ceilingTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    emitWall(tileGen, tile.wallX, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    emitWall(tileGen, tile.wallZ, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    emitWall(tileGen, tile.wallMX, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    emitWall(tileGen, tile.wallMZ, modelGen, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts, options, pool);
    tileVColIndex += LasertagTile.PER_TILE_VCOL_INDICES;
    pool.free();
    options.free();
    return tileVColIndex;
  }

  private static void emitWall(LasertagTileGen tileGen, LasertagTileWall wall, PModelGen modelGen, PModelGen.Part basePart,
                               PModelGen.StaticPhysicsPart staticPhysicsPart, int tileVColIndex,
                               PList<PModelGen.Part> alphaBlendParts, PModelGenTemplateOptions options,
                               PPool.PoolBuffer pool) {
    if (!wall.hasWall) {return;}
    PAssert.isTrue(wall.isValid);
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile110 = pool.vec3(), tile001 =
        pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    wall.tile.getCornersFloorCeiling(tile000, tile001, tile010, tile011, tile100, tile101, tile110, tile111);
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
    String wallExtraTemplateName = tileGen.roomGen.templateSelector.wallTemplateExtras(tileGen);
    if (wallExtraTemplateName != null) {
      PModelGenTemplate extraTemplate = PModelGenTemplate.get(wallExtraTemplateName);
      extraTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.isWindow) {
      if (tileGen.x == 0 && tileGen.z == 0 && wall.facing == LasertagTileWall.Facing.X && tileGen.roomGen.buildingGen.building.id.equals("building0")) {
        System.out.println(tileGen.tile.tileVColIndexStart);
      }
      PModelGenTemplate windowTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.windowTemplate(tileGen));
      windowTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    } else if (wall.isSolidWall) {
      PModelGenTemplate wallTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.wallTemplate(tileGen));
      wallTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.hasDoorframeR) {
      PModelGenTemplate doorframeRTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.doorFrameRTemplate(tileGen));
      doorframeRTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.hasDoorframeL) {
      PModelGenTemplate doorframeLTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.doorFrameLTemplate(tileGen));
      doorframeLTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
    if (wall.hasDoorframeT) {
      PModelGenTemplate doorframeTTemplate = PModelGenTemplate.get(tileGen.roomGen.templateSelector.doorFrameTTemplate(tileGen));
      doorframeTTemplate.emit(modelGen, options, basePart, staticPhysicsPart, tileVColIndex, alphaBlendParts);
    }
  }
}
