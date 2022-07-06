package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

import lombok.val;

public class LasertagRoomGenWalkwayProcessor {
  public static final int rampTiles = 3; // The number of tiles hor. it takes to move up or down one tile vertically.

  public static void processRoomWalkways(LasertagBuildingGen buildingGen) {
    for (int a = 0; a < buildingGen.roomGens.size; a++) {
      processRoomWalkways(buildingGen.roomGens.get(a));
    }
  }

  // Doors should already be applied for walls.
  public static void processRoomWalkways(LasertagRoomGen roomGen) {
    PList<PossibleWalkwayLayout.ConnectedGroup> connectedGroups = new PList<>();
    PIntAABB roomBoundsAABB = roomGen.tileGens.keyBounds(new PIntAABB());
    PList<PList<LasertagTileGen>> tileGensPerY = new PList<>();
    PList<PList<LasertagTileGen>> tileGensWithFloorsPerY = new PList<>();
    PList<PList<PList<LasertagTileGen>>> tileGensIslandsWithFloorsPerY = new PList<>();
    try (val it = roomGen.tileGens.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        int x = e.x();
        int y = e.y();
        int z = e.z();
        LasertagTileGen tileGen = e.val();
        int yIndex = y - roomBoundsAABB.y0();
        while (tileGensPerY.size <= yIndex) {
          tileGensPerY.add(new PList<>());
        }
        while (tileGensWithFloorsPerY.size <= yIndex) {
          tileGensWithFloorsPerY.add(new PList<>());
        }
        while (tileGensIslandsWithFloorsPerY.size <= yIndex) {
          tileGensIslandsWithFloorsPerY.add(new PList<>());
        }
        tileGensPerY.get(yIndex).add(tileGen);
        if (tileGen.tile.hasFloor) {
          tileGensWithFloorsPerY.get(yIndex).add(tileGen);
        }
      }
    }
    // Generated connectedGroups for doors that are not on a floor.
    for (int a = 0; a < roomGen.buildingGen.doorGens.size; a++) {
      LasertagDoorGen doorGen = roomGen.buildingGen.doorGens.get(a);
      PossibleWalkwayLayout.ConnectedGroup doorGroup = null;
      if (doorGen.ownerRoomGen == roomGen || doorGen.otherRoomGen == roomGen) {
        int tileX = doorGen.door.tileX;
        int tileY = doorGen.door.tileY;
        int tileZ = doorGen.door.tileZ;
        int xChangeForAlongWall = doorGen.door.facing == LasertagTileWall.Facing.Z ? -1 :
                                  (doorGen.door.facing == LasertagTileWall.Facing.mZ ? 1 : 0);
        int zChangeForAlongWall = doorGen.door.facing == LasertagTileWall.Facing.X ? 1 :
                                  (doorGen.door.facing == LasertagTileWall.Facing.mX ? -1 : 0);
        for (int testX = 0; testX < doorGen.door.w; testX++) { // We only need to check the bottom layer.
          LasertagTileGen lookTile =
              doorGen.ownerRoomGen.buildingGen.tileGens.get(tileX + testX * xChangeForAlongWall, tileY,
                                                            tileZ + testX * zChangeForAlongWall);
          LasertagTileGen otherTile =
              LasertagRoomGenTileProcessor.otherTileForWall(roomGen.buildingGen, lookTile, doorGen.door.facing);
          LasertagTileWall.Facing lookFacing = doorGen.door.facing;
          if (otherTile.roomGen == roomGen) {
            lookTile = otherTile;
            lookFacing = lookFacing.opposite();
          }
          if (lookTile.tile.hasFloor) {
            continue;
          }
          if (doorGroup == null) {
            doorGroup = new PossibleWalkwayLayout.ConnectedGroup();
            connectedGroups.add(doorGroup);
          }
          doorGroup.doorWalls.add(lookTile.wallGen(lookFacing));
        }
      }
    }
    // Find the islands.
    PList<LasertagTileGen> islandTilesSearchBuffer = new PList<>();
    for (int yI = 0; yI < tileGensWithFloorsPerY.size; yI++) {
      PList<LasertagTileGen> unassignedTiles = new PList<>();
      unassignedTiles.addAll(tileGensWithFloorsPerY.get(yI));
      while (!unassignedTiles.isEmpty()) {
        LasertagTileGen tile = unassignedTiles.removeLast();
        PList<LasertagTileGen> islandTilesBuffer = new PList<>();
        PList<LasertagTileWallGen> islandTilesWallsBuffer = new PList<>();
        islandTilesSearchBuffer.add(tile);
        while (!islandTilesSearchBuffer.isEmpty()) {
          LasertagTileGen islandTile = islandTilesSearchBuffer.removeLast();
          islandTilesBuffer.add(islandTile);
          if (islandTile.wallMX.wall.isDoor()) {
            islandTilesWallsBuffer.add(islandTile.wallMX);
          }
          if (islandTile.wallMZ.wall.isDoor()) {
            islandTilesWallsBuffer.add(islandTile.wallMZ);
          }
          if (islandTile.wallX.wall.isDoor()) {
            islandTilesWallsBuffer.add(islandTile.wallX);
          }
          if (islandTile.wallZ.wall.isDoor()) {
            islandTilesWallsBuffer.add(islandTile.wallZ);
          }
          addIslandNeighborTilesForSearchTile(unassignedTiles, islandTilesSearchBuffer, islandTile);
        }
        if (!islandTilesBuffer.isEmpty()) {
          tileGensIslandsWithFloorsPerY.get(yI).add(islandTilesBuffer);
          PossibleWalkwayLayout.ConnectedGroup connectedGroup = new PossibleWalkwayLayout.ConnectedGroup();
          connectedGroups.add(connectedGroup);
          connectedGroup.connectedIslandTiles = islandTilesBuffer;
        }
      }
    }
    PossibleWalkwayLayout rampOnlyLayout =
        PossibleWalkwayLayout.getOnlyRampLayout(roomGen, tileGensPerY, tileGensWithFloorsPerY,
                                                tileGensIslandsWithFloorsPerY, connectedGroups);
    rampOnlyLayout.applyToRoom();
  }

  private static void addIslandNeighborTilesForSearchTile(PList<LasertagTileGen> unassignedTiles,
                                                          PList<LasertagTileGen> islandTilesSearchBuffer,
                                                          LasertagTileGen tile) {
    LasertagTileGen tileX = tile.roomGen.tileGens.get(tile.x + 1, tile.y, tile.z);
    if (tileX != null && unassignedTiles.removeValue(tileX, true)) {
      islandTilesSearchBuffer.add(tileX);
    }
    LasertagTileGen tileMX = tile.roomGen.tileGens.get(tile.x - 1, tile.y, tile.z);
    if (tileMX != null && unassignedTiles.removeValue(tileMX, true)) {
      islandTilesSearchBuffer.add(tileMX);
    }
    LasertagTileGen tileZ = tile.roomGen.tileGens.get(tile.x, tile.y, tile.z + 1);
    if (tileZ != null && unassignedTiles.removeValue(tileZ, true)) {
      islandTilesSearchBuffer.add(tileZ);
    }
    LasertagTileGen tileMZ = tile.roomGen.tileGens.get(tile.x, tile.y, tile.z - 1);
    if (tileMZ != null && unassignedTiles.removeValue(tileMZ, true)) {
      islandTilesSearchBuffer.add(tileMZ);
    }
  }

  private static class PossibleWalkwayLayout {
    private final LasertagRoomGen roomGen;
    PIntMap3d<PossibleWalkway> walkways = new PIntMap3d<>();
    private boolean isValid = false;

    private PossibleWalkwayLayout(LasertagRoomGen roomGen) {
      this.roomGen = roomGen;
    }

    private static PossibleWalkwayLayout getOnlyRampLayout(LasertagRoomGen roomGen,
                                                           PList<PList<LasertagTileGen>> tileGensPerY,
                                                           PList<PList<LasertagTileGen>> tileGensWithFloorsPerY,
                                                           PList<PList<PList<LasertagTileGen>>> tileGensIslandsWithFloorsPerY,
                                                           PList<PossibleWalkwayLayout.ConnectedGroup> connectedGroups) {
      PAssert.isTrue(connectedGroups.size > 0);
      PossibleWalkwayLayout.ConnectedGroup firstGroup = connectedGroups.get(0);
      PossibleWalkwayLayout ret = new PossibleWalkwayLayout(roomGen);
      // Add random connections until there is only one group.
      PList<LasertagTileGen> visitedTilesStack = new PList<>();
      for (int attempt = 0; attempt < 100; attempt ++) {
        // Take a random starting tile from the first group.
      }
      while (connectedGroups.size > 1) {
        ConnectedGroup group = connectedGroups.removeLast();
      }
      return ret;
    }

    private static @Nullable ConnectedGroup groupForTileGen(PList<ConnectedGroup> connectedGroups,
                                                            LasertagTileGen tileGen) {
      if (tileGen == null) {return null;}
      for (int a = 0; a < connectedGroups.size; a++) {
        if (connectedGroups.get(a).connectedIslandTiles.contains(tileGen, true)) {
          return connectedGroups.get(a);
        }
      }
      return null;
    }

    private static @Nullable ConnectedGroup groupForWallGen(PList<ConnectedGroup> connectedGroups,
                                                            LasertagTileWallGen wallGen) {
      if (wallGen == null) {return null;}
      for (int a = 0; a < connectedGroups.size; a++) {
        if (connectedGroups.get(a).doorWalls.contains(wallGen, true)) {
          return connectedGroups.get(a);
        }
      }
      return null;
    }

    private void applyToRoom() {
    }

    // Represents a set of connected tiles and doors.
    static class ConnectedGroup {
      PList<LasertagTileGen> connectedIslandTiles;
      PList<LasertagTileWallGen> doorWalls;

      public ConnectedGroup addAll(ConnectedGroup other) {
        connectedIslandTiles.addAll(other.connectedIslandTiles);
        doorWalls.addAll(other.doorWalls);
        return this;
      }
    }

    // A node is a tile and an edge, where a walkway could be constructed at that edge of the tile.
    static class WalkwayNode {
      final LasertagTileGen tileGen;
      final LasertagTileWall.Facing facing;
      private WalkwayNode(LasertagTileGen tileGen, LasertagTileWall.Facing facing) {
        this.tileGen = tileGen;
        this.facing = facing;
      }

      boolean canGoUp, canGoFlat, canGoDown;
    }

    private static class PossibleWalkway {
      private int height00, height10, height11, height01; // Whole numbers representing the numerator of the fraction

      // of the height of the corner, with the denominator equal to rampTiles.
      public boolean isAtFloorLevel() {
        return height00 == 0 && height01 == 0 && height11 == 0 && height10 == 0;
      }
    }
  }
}
