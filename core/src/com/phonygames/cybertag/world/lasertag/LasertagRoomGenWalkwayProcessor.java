package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagTileWall.FACINGS;

import android.support.annotation.Nullable;

import com.phonygames.pengine.util.PList;

public class LasertagRoomGenWalkwayProcessor {
  public static final int rampTiles = 2; // The number of tiles hor. it takes to move up or down one tile vertically.

  private static PList<ProcessorDoorGen> getProcessorDoorGens(LasertagRoomGen roomGen) {
    PList<ProcessorDoorGen> doorGens = new PList<>();
    for (int a = 0; a < roomGen.buildingGen.doorGens.size; a++) {
      ProcessorDoorGen doorGen = new ProcessorDoorGen(roomGen.buildingGen.doorGens.get(a), roomGen);
      if (doorGen.isValid) {
        doorGens.add(doorGen);
      }
    }
    return doorGens;
  }

  public static void processRoomWalkways(LasertagBuildingGen buildingGen) {
    for (int a = 0; a < buildingGen.roomGens.size; a++) {
      processRoomWalkways(buildingGen.roomGens.get(a));
    }
  }

  // Doors should already be applied for walls.
  public static void processRoomWalkways(LasertagRoomGen roomGen) {
    Context context = new Context(roomGen);
  }

  /**
   * Separates the given tiles into floor tile islands.
   * @param tiles
   * @param @optional blockedTiles tiles that will not be included in island generation.
   * @return
   */
  private static PList<FloorTileIsland> separateFloorTilesIntoIslands(PList<LasertagTileGen> tiles,
                                                                      @Nullable PList<LasertagTileGen> blockedTiles) {
    PList<FloorTileIsland> ret = new PList();
    PList<LasertagTileGen> unGroupedTileGens = new PList<>();
    unGroupedTileGens.addAll(tiles);
    while (!unGroupedTileGens.isEmpty()) {
      LasertagTileGen islandStartTile = unGroupedTileGens.removeLast();
      FloorTileIsland island = new FloorTileIsland(islandStartTile.y);
      PList<LasertagTileGen> islandSearchBuffer = new PList<>();
      islandSearchBuffer.add(islandStartTile);
      while (!islandSearchBuffer.isEmpty()) {
        LasertagTileGen searchBufferTile = islandSearchBuffer.removeLast();
        if (searchBufferTile == null) {continue;}
        if (!searchBufferTile.tile.hasFloor) {continue;}
        if (blockedTiles != null && blockedTiles.contains(searchBufferTile, true)) {continue;}
        // Remove the search tile from the ungrouped tiles. If this returns true, that means the tile was originally
        // in the ungrouped tilegens buffer, which means we can add it to the island.
        if (unGroupedTileGens.removeValue(searchBufferTile, true)) {
          island.tiles.add(searchBufferTile);
          // Add neighbors to the search buffer.
          islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(1, 0, 0));
          islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(-1, 0, 0));
          islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(0, 0, 1));
          islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(0, 0, -1));
        }
      }
      if (!island.tiles.isEmpty()) {
        ret.add(island);
      }
    }
    return ret;
  }

  private static class ConnectedGroup {
    final PList<FloorTileIsland> floorTileIslands = new PList<>();
    final PList<Walkway> walkways = new PList<>();

    ConnectedGroup addAllFrom(ConnectedGroup other) {
      for (int a = 0; a < other.floorTileIslands.size; a++) {
        FloorTileIsland island = other.floorTileIslands.get(a);
        if (!floorTileIslands.contains(island, true)) {
          floorTileIslands.add(island);
        }
      }
      for (int a = 0; a < other.walkways.size; a++) {
        Walkway walkway = other.walkways.get(a);
        // Check to make sure there isnt already a walkway at this tile.
        boolean canAdd = true;
        for (int b = 0; b < walkways.size; b++) {
          if (walkways.get(b).tileGen == walkway.tileGen) {
            canAdd = false;
            break;
          }
        }
        if (canAdd) {
          walkways.add(walkway);
        }
      }
      return this;
    }

    /**
     * Returns whether or not these groups are adjacent and really should be one.
     * This works by checking flat walkways and floor tile islands only.
     * @param other
     * @return
     */
    boolean flatWalkwaysAndFloorTileIslandsCanJoinInto(ConnectedGroup other) {
      // Check to see that at least one walkway or island tile has a neighbor relation.
      for (int a = 0; a < floorTileIslands.size; a++) {
        FloorTileIsland island = floorTileIslands.get(a);
        for (int b = 0; b < island.tiles.size; b++) {
          LasertagTileGen tileGen = island.tiles.get(b);
          for (int f = 0; f < FACINGS.length; f++) {
            LasertagTileWall.Facing facing = FACINGS[f];
            if (other.hasFloorOrFlatWalkwayAt(
                tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ()))) {return true;}
          }
        }
      }
      for (int a = 0; a < walkways.size; a++) {
        if (walkways.get(a).sloped) {continue;}
        LasertagTileGen tileGen = walkways.get(a).tileGen;
        for (int f = 0; f < FACINGS.length; f++) {
          LasertagTileWall.Facing facing = FACINGS[f];
          if (other.hasFloorOrFlatWalkwayAt(
              tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ()))) {return true;}
        }
      }
      return false;
    }

    /**
     * Returns whether or not
     * @param tileGen
     * @return
     */
    boolean hasFloorOrFlatWalkwayAt(@Nullable LasertagTileGen tileGen) {
      if (tileGen == null) {return false;}
      for (int a = 0; a < floorTileIslands.size; a++) {
        if (floorTileIslands.get(a).tiles.contains(tileGen, true)) {return true;}
      }
      for (int a = 0; a < walkways.size; a++) {
        Walkway walkway = walkways.get(a);
        if (!walkway.sloped && walkway.tileGen == tileGen) {return true;}
      }
      return false;
    }
  }

  private static class Context {
    final PList<ConnectedGroup> connectedGroups = new PList<>();
    final PList<FloorTileIsland> floorTileIslands;
    /**
     * List of doorgens with one floorless tile. These will be used to spawn walkway nodes.
     */
    final PList<ProcessorDoorGen> oneTileFloorlessProcessorDoorGens = new PList<>();
    final PList<ProcessorDoorGen> processorDoorGens;
    final LasertagRoomGen roomGen;

    public Context(LasertagRoomGen roomGen) {
      this.roomGen = roomGen;
      this.processorDoorGens = getProcessorDoorGens(roomGen);
      this.floorTileIslands = separateFloorTilesIntoIslands(roomGen.tileGens.genValuesList(), null);
      // Generate connected groups.
      for (int a = 0; a < floorTileIslands.size; a++) {
        FloorTileIsland island = floorTileIslands.get(a);
        ConnectedGroup connectedGroup = new ConnectedGroup();
        connectedGroups.add(connectedGroup);
        connectedGroup.floorTileIslands.add(island);
      }
      for (int a = 0; a < processorDoorGens.size; a++) {
        ProcessorDoorGen doorGen = processorDoorGens.get(a);
        // Door gens that have no floorless tiles don't need to be added to a connected group.
        if (doorGen.floorlessTilesGens.size == 0) {continue;}
        // Door gens that have one floorless tile will be used to spawn walkway nodes.
        if (doorGen.floorlessTilesGens.size == 1) {
          oneTileFloorlessProcessorDoorGens.add(doorGen);
          continue;
        }
        // If the door has 2 or more floorless tiles, generate a connected group for it with walkways placed underneath.
        ConnectedGroup connectedGroup = new ConnectedGroup();
        connectedGroups.add(connectedGroup);
        for (int b = 0; b < doorGen.floorlessTilesGens.size; b++) {
          LasertagTileGen tileGen = doorGen.floorlessTilesGens.get(b);
          connectedGroup.walkways.add(new Walkway(tileGen, 0));
        }
      }
      prepassJoinConnectedGroups();
    }

    private void prepassJoinConnectedGroups() {
      PList<ConnectedGroup> connectedGroupsToProcess = new PList<>();
      connectedGroupsToProcess.addAll(connectedGroups);
      while (!connectedGroupsToProcess.isEmpty()) {
        ConnectedGroup connectedGroup = connectedGroupsToProcess.removeLast();
        // If the connectedGroups list does not contain this group, it might have already been merged.
        if (!connectedGroups.contains(connectedGroup, true)) {continue;}
        for (int a = 0; a < connectedGroups.size; a++) {
          ConnectedGroup otherGroup = connectedGroups.get(a);
          if (otherGroup == connectedGroup) {continue;}
          // Merge groups if they can be merged.
          if (connectedGroup.flatWalkwaysAndFloorTileIslandsCanJoinInto(otherGroup)) {
            connectedGroup.addAllFrom(otherGroup);
            connectedGroups.removeIndex(a);
            connectedGroupsToProcess.add(connectedGroup);
            break;
          }
        }
      }
    }
  }

  private static class FloorTileIsland {
    final PList<LasertagTileGen> tiles = new PList<>();
    final int y;

    FloorTileIsland(int y) {
      this.y = y;
    }
  }

  private static class ProcessorDoorGen {
    public final PList<LasertagTileGen> affectedTileGens = new PList<>();
    public final PList<LasertagTileGen> floorlessTilesGens = new PList<>();
    final LasertagDoorGen doorGen;
    final LasertagTileWall.Facing facing;
    final boolean isValid;

    ProcessorDoorGen(LasertagDoorGen lasertagDoorGen, LasertagRoomGen roomGen) {
      this.doorGen = lasertagDoorGen;
      // Process doors and determine if they have any tiles not on the floor.
      if (doorGen.ownerRoomGen == roomGen || doorGen.otherRoomGen == roomGen) {
        int tileX = doorGen.door.tileX;
        int tileY = doorGen.door.tileY;
        int tileZ = doorGen.door.tileZ;
        LasertagTileWall.Facing lookFacing = doorGen.door.facing;
        if (doorGen.otherRoomGen == roomGen) {
          // We are on the other side of this door.
          tileX -= doorGen.door.facing.normalX();
          tileZ -= doorGen.door.facing.normalZ();
          lookFacing = lookFacing.opposite();
        }
        this.facing = lookFacing;
        int xChangeForAlongWall = doorGen.door.facing == LasertagTileWall.Facing.Z ? -1 :
                                  (doorGen.door.facing == LasertagTileWall.Facing.mZ ? 1 : 0);
        int zChangeForAlongWall = doorGen.door.facing == LasertagTileWall.Facing.X ? 1 :
                                  (doorGen.door.facing == LasertagTileWall.Facing.mX ? -1 : 0);
        for (int testX = 0; testX < doorGen.door.w; testX++) { // We only need to check the bottom layer.
          LasertagTileGen lookTile =
              roomGen.tileGens.get(tileX + testX * xChangeForAlongWall, tileY, tileZ + testX * zChangeForAlongWall);
          affectedTileGens.add(lookTile);
          if (!lookTile.tile.hasFloor) {
            floorlessTilesGens.add(lookTile);
          }
        }
        isValid = true;
      } else {
        isValid = false;
        facing = LasertagTileWall.Facing.X;
      }
    }
  }

  private static class Walkway {
    final int bottomYOffset;
    final LasertagTileWall.Facing facing;
    final boolean sloped;
    final LasertagTileGen tileGen;

    public Walkway(LasertagTileGen tileGen, int bottomYOffset, boolean sloped, LasertagTileWall.Facing facing) {
      this.tileGen = tileGen;
      this.bottomYOffset = bottomYOffset;
      this.sloped = sloped;
      this.facing = facing;
    }

    public Walkway(LasertagTileGen tileGen, int bottomYOffset) {
      this.tileGen = tileGen;
      this.bottomYOffset = bottomYOffset;
      this.sloped = false;
      this.facing = LasertagTileWall.Facing.X;
    }
  }
}
