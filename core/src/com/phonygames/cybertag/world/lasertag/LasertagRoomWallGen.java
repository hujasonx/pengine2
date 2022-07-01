package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor.needsWallInDirection;

import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

/** Helper class to handle wall-scale wall generation. */
public class LasertagRoomWallGen {
  private final LasertagTileGen cornerTile;
  private final LasertagTileWall.Facing facing;
  private final LasertagRoomGen roomGen;
  private final PIntMap3d<LasertagTileGen> tileGens;
  private final PList<Integer> wallHeights = new PList<>();
  private final int xChangeForAlongWall, zChangeForAlongWall;

  public LasertagRoomWallGen(LasertagRoomGen roomGen, PIntMap3d<LasertagTileGen> tileGens, LasertagTileGen cornerTile,
                             LasertagTileWall.Facing facing) {
    this.roomGen = roomGen;
    this.tileGens = tileGens;
    this.facing = facing;
    this.cornerTile = cornerTile;
    xChangeForAlongWall = facing == LasertagTileWall.Facing.Z ? -1 : (facing == LasertagTileWall.Facing.mZ ? 1 : 0);
    zChangeForAlongWall = facing == LasertagTileWall.Facing.X ? 1 : (facing == LasertagTileWall.Facing.mX ? -1 : 0);
    genWallHeightData();
  }

  private void genWallHeightData() {
    LasertagTileGen searchTileGenBase = null, searchTileGenTop = null;
    int searchTileBaseX = cornerTile.x;
    int searchTileBaseY = cornerTile.y;
    int searchTileBaseZ = cornerTile.z;
    LasertagTileGen nextTileGenBase = cornerTile, nextTileGenTop;
    while (nextTileGenBase != null) {
      searchTileGenBase = nextTileGenBase;
      nextTileGenTop = searchTileGenBase;
      int searchTileYOffset = 0;
      while (nextTileGenTop != null && needsWallInDirection(roomGen, nextTileGenTop, facing)) {
        searchTileGenTop = nextTileGenTop;
        searchTileGenTop.tile.wall(facing).valid = true;
        // Mark all the walls for the visited tiles as valid, since we own them now.
        nextTileGenTop = tileGens.get(searchTileBaseX, searchTileBaseY + searchTileYOffset, searchTileBaseZ);
        searchTileYOffset++;
      }
      if (searchTileYOffset == 0) {break;}
      wallHeights.add(searchTileYOffset);
      searchTileBaseX += xChangeForAlongWall;
      searchTileBaseZ += zChangeForAlongWall;
      nextTileGenBase = tileGens.get(searchTileBaseX, searchTileBaseY, searchTileBaseZ);
    }
  }
}
