package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PArrayUtils;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

public class LasertagRoomGenRoomPlacer {
  // The edgeShiftBoundary is added to the range edges of the location random() call. The location is then clamped
  // back down to the original range. This allows for the rooms to be more clustered towards the edge of the
  // building.
  public static int edgeShiftBoundaryX, edgeShiftBoundaryZ, edgeShiftLowerY;
  public static int minRoomSize, maxRoomSize;
  // Check for overlaps with other rooms.
  public static int minRoomWidthInDimension; // This room will not replace existing room tiles, so
  // make sure there arent any parts of this room that will be smaller than this value in dimension.
  public static float minimumKeptTilesRatio;
  public static float[] roomHeightWeights = null;

  public static PIntAABB getValidAABBForRoomPlacement(LasertagBuildingGen buildingGen) {
    PIntMap3d<LasertagTileGen> tilemap = buildingGen.tileGens;
    return getValidAABBForRoomPlacement(buildingGen.aabb, buildingGen.outsideAabbs, tilemap);
  }

  public static @Nullable PIntAABB getValidAABBForRoomPlacement(PIntAABB aabb, PList<PIntAABB> outsideAabbs,
                                                                PIntMap3d<LasertagTileGen> tileGenMap) {
    PIntAABB ret = new PIntAABB();
    for (int attempt = 0; attempt < 250; attempt++) {
      int roomSizeX = MathUtils.random(minRoomSize, maxRoomSize);
      int roomSizeY = PArrayUtils.randomIndexWithWeights(roomHeightWeights) + 1;
      int roomSizeZ = MathUtils.random(minRoomSize, maxRoomSize);
      int roomX = MathUtils.random(aabb.x0() - edgeShiftBoundaryX, aabb.x1() + edgeShiftBoundaryX - roomSizeX + 1);
      roomX = MathUtils.clamp(roomX, aabb.x0(), aabb.x1() - roomSizeX + 1);
      int roomY = MathUtils.random(aabb.y0() - edgeShiftLowerY, aabb.y1() - roomSizeY + 1);
      roomY = PNumberUtils.clamp(roomY, aabb.y0(), aabb.y1() - roomSizeY + 1);
      int roomZ = MathUtils.random(aabb.z0() - edgeShiftBoundaryZ, aabb.z1() + edgeShiftBoundaryZ - roomSizeZ + 1);
      roomZ = PNumberUtils.clamp(roomZ, aabb.z0(), aabb.z1() - roomSizeZ + 1);
      ret.set(roomX, roomY, roomZ, roomX + roomSizeX - 1, roomY + roomSizeY - 1, roomZ + roomSizeZ - 1);
      for (int b = 0; b < outsideAabbs.size(); b++) { // Check that the tile does not intersect.
        if (outsideAabbs.get(b).intersects(ret)) {continue;}
      }
      if (validateAABBForRoomPlacement(ret, tileGenMap)) {
        return ret;
      }
    }
    return null;
  }

  /**
   * In this sense, "encroachment" refers to the tiles of this room after we account the fact that it cannot overwrite
   * existing tiles in the roomTiles array.
   * @return
   */
  public static boolean validateAABBForRoomPlacement(PIntAABB aabb, PIntMap3d<LasertagTileGen> tileGenMap) {
    int keptTiles = 0;
    int allTiles = 0;
    for (int y = aabb.y0(); y <= aabb.y1(); y++) {
      for (int x = aabb.x0(); x <= aabb.x1(); x++) {
        int consecutiveFreeSpots = 0;
        for (int z = aabb.z0(); z <= aabb.z1(); z++) {
          LasertagTileGen tileAt = tileGenMap.get(x, y, z);
          if (tileAt == null) {return false;} // tileAt should never be null, since the aabb should always be enclosed.
          if (tileAt.tile.room != null) {
            if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minRoomWidthInDimension) {return false;}
            consecutiveFreeSpots = 0;
            allTiles++;
          } else {
            keptTiles++;
            allTiles++;
            consecutiveFreeSpots++;
          }
        }
        if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minRoomWidthInDimension) {return false;}
      }
    }
    for (int y = aabb.y0(); y <= aabb.y1(); y++) {
      for (int z = aabb.z0(); z <= aabb.z1(); z++) {
        int consecutiveFreeSpots = 0;
        for (int x = aabb.x0(); x <= aabb.x1(); x++) {
          LasertagTileGen tileAt = tileGenMap.get(x, y, z);
          if (tileAt == null) {return false;} // tileAt should never be null, since the aabb should always be enclosed.
          if (tileAt.tile.room != null) {
            if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minRoomWidthInDimension) {return false;}
            consecutiveFreeSpots = 0;
          } else {
            consecutiveFreeSpots++;
          }
        }
        if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minRoomWidthInDimension) {return false;}
      }
    }
    if (allTiles == 0 || ((float) keptTiles) / ((float) allTiles) < minimumKeptTilesRatio) {
      return false;
    }
    return true;
  }

  public static void reset() {
    minRoomSize = 4;
    maxRoomSize = 8;
    edgeShiftBoundaryX = 3;
    edgeShiftBoundaryZ = 3;
    edgeShiftLowerY = 1;
    roomHeightWeights = new float[]{.5f, .4f};
    minRoomWidthInDimension = 3;
    minimumKeptTilesRatio = .5f;
  }
}
