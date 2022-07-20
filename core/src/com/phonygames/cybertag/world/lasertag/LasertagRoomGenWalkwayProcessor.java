package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagTileWall.FACINGS;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.util.PImplementsEquals;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PSortableByScore;

public class LasertagRoomGenWalkwayProcessor {
  private static PList<ProcessorDoorGen> getProcessorDoorGens(LasertagRoomGen roomGen) {
    PList<ProcessorDoorGen> doorGens = new PList<>();
    for (int a = 0; a < roomGen.buildingGen.doorGens.size(); a++) {
      ProcessorDoorGen doorGen = new ProcessorDoorGen(roomGen.buildingGen.doorGens.get(a), roomGen);
      if (doorGen.isValid) {
        doorGens.add(doorGen);
      }
    }
    return doorGens;
  }

  public static void processRoomWalkways(LasertagBuildingGen buildingGen) {
    for (int a = 0; a < buildingGen.roomGens.size(); a++) {
      processRoomWalkways(buildingGen.roomGens.get(a));
    }
  }

  // Doors should already be applied for walls.
  public static void processRoomWalkways(LasertagRoomGen roomGen) {
    // Hallways shouldn't have walkways.
    if (roomGen.lasertagRoom.isHallway) {return;}
    Context context = new Context(roomGen);
    if (context.fullyJoined()) {
      return;
    }
    while (!context.fullyJoined()) {
      if (!context.attemptGenWalkway()) {break;}
    }
    context.emitWalkways();
  }

  /**
   * Separates the given tiles into floor tile islands.
   *
   * @param tiles
   * @param @optional blockedTiles tiles that will not be included in island generation.
   * @return
   */
  public static PList<FloorTileIsland> separateFloorTilesIntoIslands(PList<LasertagTileGen> tiles,
                                                                      @Nullable PList<LasertagTileGen> blockedTiles) {
    PList<FloorTileIsland> ret = new PList();
    PList<LasertagTileGen> unGroupedTileGens = new PList<>();
    unGroupedTileGens.addAll(tiles);
    while (!unGroupedTileGens.isEmpty()) {
      LasertagTileGen islandStartTile = unGroupedTileGens.peek();
      FloorTileIsland island = new FloorTileIsland(islandStartTile.y);
      PList<LasertagTileGen> islandSearchBuffer = new PList<>();
      islandSearchBuffer.add(islandStartTile);
      while (!islandSearchBuffer.isEmpty()) {
        LasertagTileGen searchBufferTile = islandSearchBuffer.removeLast();
        boolean wasUngrouped = unGroupedTileGens.removeValue(searchBufferTile, true);
        if (!wasUngrouped) {continue;}
        if (searchBufferTile == null) {continue;}
        if (!searchBufferTile.tile.hasFloor) {continue;}
        if (blockedTiles != null && blockedTiles.has(searchBufferTile, true)) {continue;}
        island.tiles.add(searchBufferTile);
        // Add neighbors to the search buffer.
        islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(1, 0, 0));
        islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(-1, 0, 0));
        islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(0, 0, 1));
        islandSearchBuffer.add(searchBufferTile.tileGenInRoomWithLocationOffset(0, 0, -1));
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
      PAssert.isTrue(this != other);
      for (int a = 0; a < other.floorTileIslands.size(); a++) {
        FloorTileIsland island = other.floorTileIslands.get(a);
        if (!floorTileIslands.has(island, true)) {
          floorTileIslands.add(island);
        }
      }
      for (int a = 0; a < other.walkways.size(); a++) {
        Walkway walkway = other.walkways.get(a);
        // Check to make sure there isnt already a walkway at this tile.
        for (int b = 0; b < walkways.size(); b++) {
          if (walkways.get(b).tileGen == walkway.tileGen && !walkway.equalsT(walkways.get(b))) {
            PAssert.fail("Competing walkways at tile " + walkway.tileGen);
          }
        }
        walkways.add(walkway);
      }
      return this;
    }

    /**
     * Returns whether or not these groups are adjacent and really should be one. This works by checking flat walkways
     * and floor tile islands only.
     *
     * @param other
     * @return
     */
    boolean flatWalkwaysAndFloorTileIslandsCanJoinInto(ConnectedGroup other) {
      // Check to see that at least one walkway or island tile has a neighbor relation.
      for (int a = 0; a < floorTileIslands.size(); a++) {
        FloorTileIsland island = floorTileIslands.get(a);
        for (int b = 0; b < island.tiles.size(); b++) {
          LasertagTileGen tileGen = island.tiles.get(b);
          for (int f = 0; f < FACINGS.length; f++) {
            LasertagTileWall.Facing facing = FACINGS[f];
            if (other.hasFloorOrFlatWalkwayAt(
                tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ()), 0)) {return true;}
          }
        }
      }
      for (int a = 0; a < walkways.size(); a++) {
        if (walkways.get(a).sloped) {continue;}
        LasertagTileGen tileGen = walkways.get(a).tileGen;
        for (int f = 0; f < FACINGS.length; f++) {
          LasertagTileWall.Facing facing = FACINGS[f];
          if (other.hasFloorOrFlatWalkwayAt(
              tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ()),
              walkways.get(a).endOffsetY)) {return true;}
        }
      }
      return false;
    }

    /**
     * Returns whether or not
     *
     * @param tileGen
     * @return
     */
    boolean hasFloorOrFlatWalkwayAt(@Nullable LasertagTileGen tileGen, int offsetY) {
      boolean couldBeTrue = false;
      boolean couldBeIslandFloorTile = false;
      if (tileGen == null) {return false;}
      if (offsetY == 0) {
        for (int a = 0; a < floorTileIslands.size(); a++) {
          if (floorTileIslands.get(a).tiles.has(tileGen, true)) {
            couldBeTrue = true;
            couldBeIslandFloorTile = true;
          }
        }
      }
      for (int a = 0; a < walkways.size(); a++) {
        Walkway walkway = walkways.get(a);
        if (!walkway.sloped && walkway.tileGen == tileGen && walkway.endOffsetY == offsetY) {couldBeTrue = true;}
        // There is a walkway blocking this tile.
        if (walkway.tileGen == tileGen.tileGenInRoomWithLocationOffset(0, 1, 0) && walkway.endOffsetY < offsetY + 1) {
          return false;
        }
        // Can't connect to a floor tile with a walkway.
        if (couldBeIslandFloorTile && tileGen == walkway.tileGen) {
          return false;
        }
      }
      return couldBeTrue;
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
    final int rampTiles = 2; // The number of tiles hor. it takes to move up or down one tile vertically.
    final LasertagRoomGen roomGen;

    public Context(LasertagRoomGen roomGen) {
      this.roomGen = roomGen;
      this.processorDoorGens = getProcessorDoorGens(roomGen);
      this.floorTileIslands = separateFloorTilesIntoIslands(roomGen.tileGens.genValuesList(), null);
      // Generate connected groups.
      for (int a = 0; a < floorTileIslands.size(); a++) {
        FloorTileIsland island = floorTileIslands.get(a);
        ConnectedGroup connectedGroup = new ConnectedGroup();
        connectedGroups.add(connectedGroup);
        connectedGroup.floorTileIslands.add(island);
        PAssert.isTrue(separateFloorTilesIntoIslands(island.tiles, null).size() == 1);
      }
      System.out.println("ROOM " + roomGen.lasertagRoom.id);
      for (int a = 0; a < processorDoorGens.size(); a++) {
        ProcessorDoorGen doorGen = processorDoorGens.get(a);
        System.out.println("\tDOOR " + doorGen.doorGen.door.w + "x" + doorGen.doorGen.door.h);
        // Door gens that have no floorless tiles don't need to be added to a connected group.
        if (doorGen.floorlessTilesGens.size() == 0) {continue;}
        // Door gens that have one floorless tile will be used to spawn walkway nodes.
        if (doorGen.floorlessTilesGens.size() == 1) {
          oneTileFloorlessProcessorDoorGens.add(doorGen);
          System.out.println(
              "\t\tOneTileFloorless: " + doorGen.doorGen.door.tileX + ", " + doorGen.doorGen.door.tileY + ", " +
              doorGen.doorGen.door.tileZ);
          continue;
        }
        // If the door has 2 or more floorless tiles, generate a connected group for it with walkways placed
        // underneath.
        ConnectedGroup connectedGroup = new ConnectedGroup();
        connectedGroups.add(connectedGroup);
        for (int b = 0; b < doorGen.floorlessTilesGens.size(); b++) {
          LasertagTileGen tileGen = doorGen.floorlessTilesGens.get(b);
          connectedGroup.walkways.add(new Walkway(this, tileGen, 0));
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
        if (!connectedGroups.has(connectedGroup, true)) {continue;}
        for (int a = 0; a < connectedGroups.size(); a++) {
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

    /**
     * @return whether or not a walkway was generated.
     */
    private boolean attemptGenWalkway() {
      PList<WalkwayStartNode> startNodes = genWalkwayStartNodes();
      PList<PossibleWalkwayPath> possibleWalkwayPaths = new PList<>();
      for (int a = 0; a < startNodes.size(); a++) {
        WalkwayStartNode walkwayStartNode = startNodes.get(a);
        walkwayStartNode.search();
        possibleWalkwayPaths.addAll(walkwayStartNode.possibleWalkwayPaths);
      }
      possibleWalkwayPaths.sort();
      while (possibleWalkwayPaths.size() > 0) {
        if (possibleWalkwayPaths.removeLast().applyToContext()) {return true;}
      }
      if (connectedGroups.size() > 2 || !oneTileFloorlessProcessorDoorGens.isEmpty()) {
        PAssert.warn("COULD NOT GENERATE WALKWAY!");
      }
      return false;
    }

    private PList<WalkwayStartNode> genWalkwayStartNodes() {
      PList<WalkwayStartNode> ret = new PList<>();
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.walkways.size(); b++) {
          Walkway walkway = connectedGroup.walkways.get(b);
          if (walkway.sloped) {continue;}
          for (int f = 0; f < FACINGS.length; f++) {
            LasertagTileWall.Facing facing = FACINGS[f];
            LasertagTileGen checkTileGen =
                walkway.tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ());
            // Sloped walkway allowance is a subset of flat allowance, so no need to also check sloped.
            if (couldGenWalkway(checkTileGen, false, walkway.endOffsetY, LasertagTileWall.Facing.X)) {
              ret.add(new WalkwayStartNode(this, checkTileGen, walkway.endOffsetY, connectedGroup, facing));
            }
          }
        }
        for (int b = 0; b < connectedGroup.floorTileIslands.size(); b++) {
          FloorTileIsland island = connectedGroup.floorTileIslands.get(b);
          for (int c = 0; c < island.tiles.size(); c++) {
            LasertagTileGen tileGen = island.tiles.get(c);
            for (int f = 0; f < FACINGS.length; f++) {
              LasertagTileWall.Facing facing = FACINGS[f];
              LasertagTileGen checkTileGen =
                  tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ());
              // Sloped walkway allowance is a subset of flat-floor allowance, so no need to also check sloped.
              if (couldGenWalkway(checkTileGen, false, 0, LasertagTileWall.Facing.X)) {
                ret.add(new WalkwayStartNode(this, checkTileGen, 0, connectedGroup, facing));
              }
            }
          }
        }
      }
      for (int a = 0; a < oneTileFloorlessProcessorDoorGens.size(); a++) {
        ProcessorDoorGen doorGen = oneTileFloorlessProcessorDoorGens.get(a);
        ret.add(new WalkwayStartNode(this, doorGen.floorlessTilesGens.get(0), 0, doorGen, doorGen.facing));
      }
      return ret;
    }

    private boolean couldGenWalkway(@Nullable LasertagTileGen tileGen, boolean sloped, int endOffsetY,
                                    @NonNull LasertagTileWall.Facing facing) {
      if (tileGen == null) {return false;}
      boolean isFloorWalkway = !sloped && endOffsetY == 0;
      // Cannot place a floor walkway on a floor tile.
      if (isFloorTile(tileGen) && isFloorWalkway) {return false;}
      // Cant place a walkway where the already is a walkway.
      if (walkwayAt(tileGen) != null) {return false;}
      LasertagTileGen aboveTileGen = tileGen.tileGenInRoomWithLocationOffset(0, 1, 0);
      for (int f = 0; f < FACINGS.length; f++) {
        LasertagTileWall.Facing testFacing = FACINGS[f];
        // Non-floor walkways cannot be generated in front of doors, and floor walkways can only be generated at the
        // door's bottom.
        if (tileGen.wallGen(testFacing).wall.isDoor()) {
          LasertagDoor door = tileGen.wallGen(testFacing).wall.door;
          LasertagTileGen tileGenBelow = tileGen.tileGenInRoomWithLocationOffset(0, -1, 0);
          if (!isFloorWalkway || (tileGenBelow != null && tileGenBelow.wallGen(facing).wall.isDoor() &&
                                  tileGenBelow.wallGen(facing).wall.door == door)) {
            return false;
          }
        }
      }
      if (!isFloorWalkway) {
        if (aboveTileGen == null) { // If this room doesn't own the tile above, we cannot emit a nonfloor walkway.
          return false;
        }
        // If there is a walkway above this tile, we cannot emit a nonfloor walkway here.
        if (walkwayAt(tileGen.tileGenInRoomWithLocationOffset(0, 1, 0)) != null) {return false;}
        for (int f = 0; f < FACINGS.length; f++) {
          LasertagTileWall.Facing testFacing = FACINGS[f];
          if (aboveTileGen.wallGen(testFacing).wall.isDoor()) {
            // If there is a door above, the only way this is valid is if this walkway is coming from it.
            if (endOffsetY == rampTiles - 1 && sloped && facing == testFacing) {continue;}
            return false;
          }
        }
      }
      Walkway walkwayBelow = walkwayAt(tileGen.tileGenInRoomWithLocationOffset(0, -1, 0));
      // If there is a walkway below, it must be a floor walkway.
      if (walkwayBelow != null && (walkwayBelow.sloped || walkwayBelow.endOffsetY != 0)) {return false;}
      return true;
    }

    private boolean isFloorTile(@Nullable LasertagTileGen tileGen) {
      if (tileGen == null) {return false;}
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.floorTileIslands.size(); b++) {
          FloorTileIsland island = connectedGroup.floorTileIslands.get(b);
          if (island.tiles.has(tileGen, true)) {return true;}
        }
      }
      return false;
    }

    private Walkway walkwayAt(@Nullable LasertagTileGen tileGen) {
      if (tileGen == null) {return null;}
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.walkways.size(); b++) {
          Walkway walkway = connectedGroup.walkways.get(b);
          if (walkway.tileGen == tileGen) {return walkway;}
        }
      }
      return null;
    }

    private void emitWalkways() {
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.walkways.size(); b++) {
          Walkway walkway = connectedGroup.walkways.get(b);
          walkway.emitToTile();
        }
      }
    }

    public boolean fullyJoined() {
      return connectedGroups.size() == 1 && oneTileFloorlessProcessorDoorGens.size() == 0;
    }
  }

  public static class FloorTileIsland {
    public final PList<LasertagTileGen> tiles = new PList<>();
    public final int y;

    FloorTileIsland(int y) {
      this.y = y;
    }
  }

  private static class PossibleWalkwayPath implements PSortableByScore<PossibleWalkwayPath> {
    final PList<ConnectedGroup> allConnectedGroups = new PList<>();
    final PList<ProcessorDoorGen> allOneTileDoorGens = new PList<>();
    final WalkwayStartNode walkwayStartNode;
    final PList<Walkway> walkways = new PList<>();

    public PossibleWalkwayPath(WalkwayStartNode startNode, PList<Walkway> walkways) {
      this.walkwayStartNode = startNode;
      Context context = startNode.context;
      if (startNode.oneTileDoorGen != null) {
        allOneTileDoorGens.add(startNode.oneTileDoorGen);
      }
      if (startNode.connectedGroup != null) {
        allConnectedGroups.add(startNode.connectedGroup);
      }
      for (int a = 0; a < walkways.size(); a++) {
        Walkway walkway = walkways.get(a);
        this.walkways.add(walkway);
        for (int b = 0; b < context.connectedGroups.size(); b++) {
          ConnectedGroup connectedGroup = context.connectedGroups.get(b);
          if (connectedGroup == walkwayStartNode.connectedGroup) {continue;}
          if (walkway.connectsWithConnectedGroup(connectedGroup) && !allConnectedGroups.has(connectedGroup, true)) {
            allConnectedGroups.add(connectedGroup);
          }
        }
        for (int b = 0; b < context.oneTileFloorlessProcessorDoorGens.size(); b++) {
          ProcessorDoorGen oneTileDoorGen = context.oneTileFloorlessProcessorDoorGens.get(b);
          if (walkway.flatConnectsWithOneTileFloorlessDoorgen(oneTileDoorGen) &&
              !allOneTileDoorGens.has(oneTileDoorGen, true)) {
            allOneTileDoorGens.add(oneTileDoorGen);
          }
        }
      }
    }

    /**
     * @return Whether or not the path was applied.
     */
    public boolean applyToContext() {
      Context context = walkwayStartNode.context;
      // The start connected group will be null for floorless doors.
      System.out.println("\tEmit walkway len: " + walkways.size());
      ConnectedGroup firstGroupSeen = null;
      for (int a = 0; a < allConnectedGroups.size(); a++) {
        ConnectedGroup otherConnectedGroup = allConnectedGroups.get(a);
        if (firstGroupSeen != null) {
          if (context.connectedGroups.removeValue(otherConnectedGroup, true)) {
            firstGroupSeen.addAllFrom(otherConnectedGroup);
            System.out.println("\t\tCombine connected group");
          }
        } else {
          firstGroupSeen = allConnectedGroups.get(a);
        }
      }
      for (int a = 0; a < allOneTileDoorGens.size(); a++) {
        ProcessorDoorGen doorGen = allOneTileDoorGens.get(a);
        System.out.println(
            "\t\tconnects floorlessdoorgen " + doorGen.doorGen.door.tileX + ", " + doorGen.doorGen.door.tileY + ", " +
            doorGen.doorGen.door.tileZ);
        context.oneTileFloorlessProcessorDoorGens.removeValue(doorGen, true);
      }
      firstGroupSeen.walkways.addAll(walkways);
      return true;
    }

    @Override public float score() {
      int score = 0;
      int scorePerConnection = 10;
      score += allOneTileDoorGens.size();
      score += allConnectedGroups.size();
      score *= scorePerConnection;
      score -= walkways.size();
      return score;
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

  private static class Walkway implements PImplementsEquals<Walkway> {
    final Context context;
    final int endOffsetY;
    final LasertagTileWall.Facing facing;
    final boolean sloped;
    final LasertagTileGen tileGen;

    public Walkway(Context context, @Nullable LasertagTileGen tileGen, int endOffsetY, boolean sloped,
                   LasertagTileWall.Facing facing) {
      this.context = context;
      while (endOffsetY < 0) {
        if (tileGen == null) {
          break;
        }
        endOffsetY += context.rampTiles;
        tileGen = tileGen.tileGenInRoomWithLocationOffset(0, -1, 0);
      }
      this.tileGen = tileGen;
      this.endOffsetY = PNumberUtils.mod(endOffsetY, context.rampTiles);
      this.sloped = sloped;
      this.facing = facing;
    }

    public Walkway(Context context, LasertagTileGen tileGen, int endOffsetY) {
      this.context = context;
      this.tileGen = tileGen;
      this.endOffsetY = endOffsetY;
      this.sloped = false;
      this.facing = LasertagTileWall.Facing.X;
    }

    public boolean connectsWithConnectedGroup(ConnectedGroup connectedGroup) {
      for (int f = 0; f < FACINGS.length; f++) {
        LasertagTileWall.Facing facing = FACINGS[f];
        if (!sloped || facing == this.facing) {
          if (connectedGroup.hasFloorOrFlatWalkwayAt(
              tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ()), endOffsetY)) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean couldBePlaced() {
      if (tileGen == null) {return false;}
      return context.couldGenWalkway(tileGen, sloped, endOffsetY, facing);
    }

    private void emitToTile() {
      tileGen.tile.hasWalkway = true;
      tileGen.tile.walkwayTile00OffsetY = ((float) endOffsetY) / context.rampTiles;
      tileGen.tile.walkwayTile10OffsetY = ((float) endOffsetY) / context.rampTiles;
      tileGen.tile.walkwayTile11OffsetY = ((float) endOffsetY) / context.rampTiles;
      tileGen.tile.walkwayTile01OffsetY = ((float) endOffsetY) / context.rampTiles;
      if (sloped) {
        switch (facing) {
          case mX:
            tileGen.tile.walkwayTile10OffsetY += 1f / context.rampTiles;
            tileGen.tile.walkwayTile11OffsetY += 1f / context.rampTiles;
            break;
          case mZ:
            tileGen.tile.walkwayTile01OffsetY += 1f / context.rampTiles;
            tileGen.tile.walkwayTile11OffsetY += 1f / context.rampTiles;
            break;
          case X:
            tileGen.tile.walkwayTile00OffsetY += 1f / context.rampTiles;
            tileGen.tile.walkwayTile01OffsetY += 1f / context.rampTiles;
            break;
          case Z:
            tileGen.tile.walkwayTile00OffsetY += 1f / context.rampTiles;
            tileGen.tile.walkwayTile10OffsetY += 1f / context.rampTiles;
            break;
        }
      }
    }

    @Override public boolean equalsT(Walkway other) {
      if (tileGen != other.tileGen) {return false;}
      if (endOffsetY != other.endOffsetY) {return false;}
      if (sloped != other.sloped) {return false;}
      if (facing != other.facing) {return false;}
      return true;
    }

    public boolean flatConnectsWithOneTileFloorlessDoorgen(ProcessorDoorGen doorGen) {
      PAssert.isTrue(doorGen.floorlessTilesGens.size() == 1);
      return !sloped && endOffsetY == 0 && tileGen == doorGen.floorlessTilesGens.get(0);
    }

    public String toString() {
      if (sloped) {
        return "{" + tileGen.x + "," + tileGen.y + "," + tileGen.z + "F: " + facing + " [" +
               (endOffsetY + (sloped ? 1 : 0)) + "," + endOffsetY + "]}";
      } else {
        return "{" + tileGen.x + "," + tileGen.y + "," + tileGen.z + " FLAT}";
      }
    }
  }

  private static class WalkwayStartNode {
    private static final PList<LasertagTileGen> tempTileGenList = new PList<>();
    final ConnectedGroup connectedGroup;
    final Context context;
    final LasertagTileWall.Facing facing;
    final int maxSearchDepth = 10;
    final ProcessorDoorGen oneTileDoorGen;
    final PList<PossibleWalkwayPath> possibleWalkwayPaths = new PList<>();
    final int startOffsetY;
    final LasertagTileGen tileGen;

    public WalkwayStartNode(Context context, LasertagTileGen tileGen, int startOffsetY, ConnectedGroup connectedGroup,
                            LasertagTileWall.Facing facing) {
      this.context = context;
      this.tileGen = tileGen;
      this.facing = facing;
      this.startOffsetY = startOffsetY;
      this.connectedGroup = connectedGroup;
      this.oneTileDoorGen = null;
    }

    public WalkwayStartNode(Context context, LasertagTileGen tileGen, int startOffsetY, ProcessorDoorGen oneTileDoorGen,
                            LasertagTileWall.Facing facing) {
      this.context = context;
      this.tileGen = tileGen;
      this.facing = facing;
      this.startOffsetY = startOffsetY;
      this.connectedGroup = null;
      this.oneTileDoorGen = oneTileDoorGen;
    }

    void __searchRecursive(PList<Walkway> currentWalkwayBuffer, Walkway tryAdding, int turnsLeft) {
      // Check for walkway validity.
      if (!tryAdding.couldBePlaced()) {return;}
      // Can't add a walkway where there already is one, or where there would be one on top (if sloped).
      for (int a = 0; a < currentWalkwayBuffer.size(); a++) {
        Walkway walkway = currentWalkwayBuffer.get(a);
        if (walkway.tileGen == tryAdding.tileGen) {return;}
        if (tryAdding.sloped && walkway.tileGen == tryAdding.tileGen.tileGenInRoomWithLocationOffset(0, 1, 0)) {
          return;
        }
      }
      // Add the walkway.
      currentWalkwayBuffer.add(tryAdding);
      // Check to see if adding this tile would connect any connecting groups, and if so, emit.
      for (int a = 0; a < context.connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = context.connectedGroups.get(a);
        if (connectedGroup == this.connectedGroup) {continue;}
        if (tryAdding.connectsWithConnectedGroup(connectedGroup)) {
          emitPossibleWalkwayPath(currentWalkwayBuffer);
          break;
        }
      }
      // Recurse.
      if (currentWalkwayBuffer.size() < maxSearchDepth) {
        for (int a = 0; a < FACINGS.length; a++) {
          LasertagTileWall.Facing facing = FACINGS[a];
          LasertagTileGen nextTileGen =
              tryAdding.tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ());
          // Add downward ramps in all directions if this tile is flat, or forward if not.
          if (!tryAdding.sloped || facing == tryAdding.facing) {
            if (facing != tryAdding.facing && turnsLeft <= 0) {continue;}
            int nextTurnLeft = tryAdding.facing == facing ? turnsLeft : turnsLeft - 1;
            __searchRecursive(currentWalkwayBuffer,
                              new Walkway(context, nextTileGen, tryAdding.endOffsetY - 1, true, facing), nextTurnLeft);
            __searchRecursive(currentWalkwayBuffer,
                              new Walkway(context, nextTileGen, tryAdding.endOffsetY, false, facing), nextTurnLeft);
          }
        }
      }
      // Undo the walkway adding.
      currentWalkwayBuffer.removeLast();
    }

    void emitPossibleWalkwayPath(PList<Walkway> walkways) {
      tempTileGenList.clear();
      for (int a = 0; a < walkways.size(); a++) {
        tempTileGenList.add(walkways.get(a).tileGen);
      }
      // Make sure no connected group's tile islands would be divided into two.
      for (int a = 0; a < context.connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = context.connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.floorTileIslands.size(); b++) {
          FloorTileIsland island = connectedGroup.floorTileIslands.get(b);
          if (separateFloorTilesIntoIslands(island.tiles, tempTileGenList).size() > 1) {
            return;
          }
        }
      }
      tempTileGenList.clear();
      PossibleWalkwayPath possibleWalkwayPath = new PossibleWalkwayPath(this, walkways);
      possibleWalkwayPaths.add(possibleWalkwayPath);
    }

    void search() {
      PList<Walkway> currentWalkwayBuffer = new PList<>();
      __searchRecursive(currentWalkwayBuffer, new Walkway(context, tileGen, startOffsetY, false, facing), 3);
      __searchRecursive(currentWalkwayBuffer, new Walkway(context, tileGen, startOffsetY - 1, true, facing), 3);
    }
  }
}

