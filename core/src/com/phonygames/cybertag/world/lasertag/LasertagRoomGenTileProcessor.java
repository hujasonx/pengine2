package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PIntMap3d;

import lombok.val;

public class LasertagRoomGenTileProcessor {
  public static void processRoomFloors(LasertagRoomGen roomGen) {
    for (val e : roomGen.tileGens) {
      int x = e.x();
      int y = e.y();
      int z = e.z();
      LasertagTileGen tileGen = e.val();
      try {
        PAssert.isTrue(x == tileGen.x);
        PAssert.isTrue(y == tileGen.y);
        PAssert.isTrue(z == tileGen.z);
      } catch (Exception er) {
        er.printStackTrace();;
      }
      LasertagTile tile = tileGen.tile;
      if (roomAtTileGen(roomGen.tileGens, x, y - 1, z) != roomGen.lasertagRoom) {
        tile.hasFloor = true;
      }
    }
  }
  public static void processRoomCeilings(LasertagRoomGen roomGen) {
    for (val e : roomGen.tileGens) {
      int x = e.x();
      int y = e.y();
      int z = e.z();
      LasertagTileGen tileGen = e.val();
      LasertagTile tile = tileGen.tile;
      if (roomAtTileGen(roomGen.tileGens, x, y + 1, z) != roomGen.lasertagRoom) {
        tile.hasCeiling = true;
      }
    }
  }

  public static LasertagRoom roomAtTileGen(PIntMap3d<LasertagTileGen> tiles, int x, int y, int z) {
    LasertagTileGen tile = tiles.get(x, y, z);
    if (tile == null) {
      return null;
    }
    return tile.tile.room;
  }

  public static void processRoomWalls(LasertagRoomGen roomGen) {
    for (val e : roomGen.tileGens) {
      int x = e.x();
      int y = e.y();
      int z = e.z();
      LasertagTileGen tileGen = e.val();
      LasertagTile tile = tileGen.tile;
      if (!tile.wallX.hasWall && needsWallInDirection(roomGen, tileGen, LasertagTileWall.Facing.X)) {
        emitWallGen(roomGen, tileGen, LasertagTileWall.Facing.X);
      }
      if (!tile.wallZ.hasWall && needsWallInDirection(roomGen, tileGen, LasertagTileWall.Facing.Z)) {
        emitWallGen(roomGen, tileGen, LasertagTileWall.Facing.Z);
      }
      if (!tile.wallMX.hasWall && needsWallInDirection(roomGen, tileGen, LasertagTileWall.Facing.mX)) {
        emitWallGen(roomGen, tileGen, LasertagTileWall.Facing.mX);
      }
      if (!tile.wallMZ.hasWall && needsWallInDirection(roomGen, tileGen, LasertagTileWall.Facing.mZ)) {
        emitWallGen(roomGen, tileGen, LasertagTileWall.Facing.mZ);
      }
    }
  }

  public static boolean needsWallInDirection(LasertagRoomGen roomGen, @Nullable LasertagTileGen tileGen,
                                             LasertagTileWall.Facing facing) {
    if (tileGen == null) {
      return false;
    }
    if (tileGen.tile.room != roomGen.lasertagRoom) {
      return false;
    }
    switch (facing) {
      case X:
        return roomGen.lasertagRoom != roomAtTile(roomGen.buildingGen.tileGens, tileGen.x - 1, tileGen.y, tileGen.z);
      case Z:
        return roomGen.lasertagRoom != roomAtTile(roomGen.buildingGen.tileGens, tileGen.x, tileGen.y, tileGen.z - 1);
      case mX:
        return roomGen.lasertagRoom != roomAtTile(roomGen.buildingGen.tileGens, tileGen.x + 1, tileGen.y, tileGen.z);
      case mZ:
      default:
        return roomGen.lasertagRoom != roomAtTile(roomGen.buildingGen.tileGens, tileGen.x, tileGen.y, tileGen.z + 1);
    }
  }

  public static LasertagTileGen otherTileForWall(LasertagBuildingGen lasertagBuildingGen, @Nullable LasertagTileGen tileGen,
                                             LasertagTileWall.Facing facing) {
    if (tileGen == null) {
      return null;
    }
    switch (facing) {
      case X:
        return lasertagBuildingGen.tileGens.get(tileGen.x - 1, tileGen.y, tileGen.z);
      case Z:
        return lasertagBuildingGen.tileGens.get(tileGen.x, tileGen.y, tileGen.z - 1);
      case mX:
        return lasertagBuildingGen.tileGens.get(tileGen.x + 1, tileGen.y, tileGen.z);
      case mZ:
      default:
        return lasertagBuildingGen.tileGens.get(tileGen.x, tileGen.y, tileGen.z + 1);
    }
  }

  private static void emitWallGen(LasertagRoomGen roomGen, LasertagTileGen tileGen, LasertagTileWall.Facing facing) {
    LasertagRoomWallGen wallGen = createRoomWallGen(roomGen, tileGen, facing);
    roomGen.roomWallGens.add(wallGen);
  }

  private static LasertagRoom roomAtTile(@NonNull PIntMap3d<LasertagTileGen> tilesGens, int x, int y, int z) {
    LasertagTileGen tile = tilesGens.get(x, y, z);
    if (tile == null) {
      return null;
    }
    return tile.tile.room;
  }

  private static LasertagRoomWallGen createRoomWallGen(LasertagRoomGen roomGen, LasertagTileGen tileGen,
                                                       LasertagTileWall.Facing facing) {
    LasertagRoom room = roomGen.lasertagRoom;
    // Find the bottom starting corner of the wall on this plane, and the size.
    int searchX = tileGen.x, searchY = tileGen.y, searchZ = tileGen.z;
    LasertagTileGen searchTileGen = tileGen;
    while (true) {
      LasertagTileGen nextSearchTileGen = roomGen.tileGens.get(searchX, searchY - 1, searchZ);
      // Stop searching if there is no tile in the next spot, or the room isn't the same, or there was already a wall
      // generated at the spot, or there does not need to be a wall generated at that spot.
      if (nextSearchTileGen == null || nextSearchTileGen.tile.wall(facing).hasWall ||
          !needsWallInDirection(roomGen, nextSearchTileGen, facing)) {break;}
      searchTileGen = nextSearchTileGen;
      searchY--;
    }
    searchY = searchTileGen.y;
    int xChangeForAlongWall = facing == LasertagTileWall.Facing.Z ? -1 : (facing == LasertagTileWall.Facing.mZ ? 1 : 0);
    int zChangeForAlongWall = facing == LasertagTileWall.Facing.X ? 1 : (facing == LasertagTileWall.Facing.mX ? -1 : 0);
    while (true) {
      LasertagTileGen nextSearchTileGen =
          roomGen.tileGens.get(searchX - xChangeForAlongWall, searchY, searchZ - zChangeForAlongWall);
      if (nextSearchTileGen == null || nextSearchTileGen.tile.wall(facing).hasWall ||
          !needsWallInDirection(roomGen, nextSearchTileGen, facing)) {break;}
      searchTileGen = nextSearchTileGen;
      searchX -= xChangeForAlongWall;
      searchZ -= zChangeForAlongWall;
    }
    return new LasertagRoomWallGen(roomGen, roomGen.tileGens, searchTileGen, facing);
  }
}
