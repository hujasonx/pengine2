package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagTileWall.FACINGS;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.util.PImplementsEquals;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PSortableByScore;

import lombok.val;

public class LasertagHallwayProcessor {
  public static void processHallways(LasertagBuildingGen buildingGen) {
    Context context = new Context(buildingGen);
    if (context.fullyJoined()) {
      PLog.i("rooms already fully joined");
      return;
    }
    while (!context.fullyJoined()) {
      if (!context.attemptGenHallway()) {
        PLog.w("Unable to connect all groups in building " + buildingGen.building.id + "; " +
               context.connectedGroups.size() + " groups left");
        break;
      }
    }
    context.emitHallways();
  }

  private static PList<ConnectedGroup> separateRoomsIntoConnectedGroups(PList<LasertagRoomGen> rooms) {
    PList<ConnectedGroup> ret = new PList();
    PList<LasertagRoomGen> unGroupedRooms = new PList<>();
    unGroupedRooms.addAll(rooms);
    while (!unGroupedRooms.isEmpty()) {
      LasertagRoomGen startRoom = unGroupedRooms.peek();
      ConnectedGroup group = new ConnectedGroup();
      PList<LasertagRoomGen> roomSearchBuffer = new PList<>();
      roomSearchBuffer.add(startRoom);
      while (!roomSearchBuffer.isEmpty()) {
        LasertagRoomGen searchBufferRoom = roomSearchBuffer.removeLast();
        boolean wasUngrouped = unGroupedRooms.removeValue(searchBufferRoom, true);
        if (!wasUngrouped) {continue;}
        if (searchBufferRoom == null) {continue;}
        group.roomGens.add(searchBufferRoom);
        // Add neighbors to the search buffer.
        try (val it = searchBufferRoom.directlyConnectedRooms.obtainIterator()) {
          while (it.hasNext()) {
            roomSearchBuffer.add(it.next());
          }
        }
      }
      if (!group.roomGens.isEmpty()) {
        ret.add(group);
      }
    }
    return ret;
  }

  private static class ConnectedGroup {
    final PList<Hallway> hallways = new PList<>();
    final PList<LasertagRoomGen> roomGens = new PList<>();

    ConnectedGroup addAllFrom(ConnectedGroup other) {
      PAssert.isTrue(this != other);
      for (int a = 0; a < other.roomGens.size(); a++) {
        LasertagRoomGen roomGen = other.roomGens.get(a);
        if (!roomGens.has(roomGen, true)) {
          roomGens.add(roomGen);
        }
      }
      for (int a = 0; a < other.hallways.size(); a++) {
        Hallway hallway = other.hallways.get(a);
        // Check to make sure there isnt already a walkway at this tile.
        for (int b = 0; b < hallways.size(); b++) {
          if (hallways.get(b).tileGen == hallway.tileGen && !hallway.equalsT(hallways.get(b))) {
            PAssert.fail("Competing walkways at tile " + hallway.tileGen);
          }
        }
        hallways.add(hallway);
      }
      return this;
    }

    /**
     * Returns whether or not
     *
     * @param tileGen
     * @return
     */
    boolean hasRoomOrFlatHallwayAt(@Nullable LasertagTileGen tileGen, int offsetY) {
      if (tileGen == null) {return false;}
      if (offsetY == 0) {
        for (int a = 0; a < roomGens.size(); a++) {
          if (roomGens.get(a).tileGens.get(tileGen.x, tileGen.y, tileGen.z) == tileGen) {
            return true;
          }
        }
      }
      for (int a = 0; a < hallways.size(); a++) {
        Hallway hallway = hallways.get(a);
        if (!hallway.sloped && hallway.tileGen == tileGen && hallway.endOffsetY == offsetY) {
          return true;
        }
      }
      return false;
    }
  }

  private static class Context {
    final LasertagBuildingGen buildingGen;
    final PList<ConnectedGroup> connectedGroups = new PList<>();
    final PList<ProcessorDoorGen> processorDoorGens = new PList<>();
    final int rampTiles = 2; // The number of tiles hor. it takes to move up or down one tile vertically.

    public Context(LasertagBuildingGen buildingGen) {
      this.buildingGen = buildingGen;
      connectedGroups.addAll(separateRoomsIntoConnectedGroups(buildingGen.roomGens));
    }

    /**
     * @return whether or not a hallway was generated.
     */
    private boolean attemptGenHallway() {
      PLog.i("Attempting hallway gen");
      PList<HallwayStartNode> startNodes = genHallwayStartNodes();
      PList<PossibleHallwayPath> possibleHallwayPaths = new PList<>();
      for (int a = 0; a < startNodes.size(); a++) {
        HallwayStartNode hallwayStartNode = startNodes.get(a);
        hallwayStartNode.search();
        possibleHallwayPaths.addAll(hallwayStartNode.possibleHallwayPaths);
      }
      possibleHallwayPaths.sort();
      PLog.i("Found " + possibleHallwayPaths.size() + " possible paths");
      while (possibleHallwayPaths.size() > 0) {
        if (possibleHallwayPaths.removeLast().applyToContext()) {return true;}
      }
      PLog.i("No valid path emitted");
      return false;
    }

    private PList<HallwayStartNode> genHallwayStartNodes() {
      PList<HallwayStartNode> ret = new PList<>();
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.roomGens.size(); b++) {
          // Loop through all tiles in all rooms, emitting if they are valid node points.
          LasertagRoomGen roomGen = connectedGroup.roomGens.get(b);
          PList<LasertagTileGen> tilesInRoom = roomGen.tileGens.genValuesList();
          for (int c = 0; c < tilesInRoom.size(); c++) {
            LasertagTileGen tileGen = tilesInRoom.get(a);
            if (getRoomGen(neighBor(tileGen,0, -1, 0)) == tileGen.roomGen) { continue; }
            for (int f = 0; f < FACINGS.length; f++) {
              LasertagTileWall.Facing facing = FACINGS[f];
              LasertagTileGen neighbor = neighBor(tileGen, facing.normalX(), 0, facing.normalZ());
              if (neighbor != null && neighbor.roomGen == null) {
                ret.add(new HallwayStartNode(this, neighbor, 0, connectedGroup, facing));
              }
            }
          }
        }
        for (int b = 0; b < connectedGroup.hallways.size(); b++) {
          Hallway hallway = connectedGroup.hallways.get(b);
          if (hallway.sloped) {continue;}
          for (int f = 0; f < FACINGS.length; f++) {
            LasertagTileWall.Facing facing = FACINGS[f];
            LasertagTileGen neighbor = neighBor(hallway.tileGen, facing.normalX(), 0, facing.normalZ());
            if (neighbor != null && neighbor.roomGen == null) {
              ret.add(new HallwayStartNode(this, neighbor, hallway.endOffsetY, connectedGroup, facing));
            }
          }
        }
      }
      return ret;
    }

    public LasertagTileGen neighBor(LasertagTileGen tileGen, int xOffset, int yOffset, int zOffset) {
      if (tileGen == null) {return null;}
      return buildingGen.tileGens.get(tileGen.x + xOffset, tileGen.y + yOffset, tileGen.z + zOffset);
    }

    private boolean couldGenHallway(@Nullable LasertagTileGen tileGen, boolean sloped, int endOffsetY,
                                    @NonNull LasertagTileWall.Facing facing) {
      if (tileGen == null) {return false;}
      // Cant place a hallway where the already is a hallway.
      if (hallwayAt(tileGen) != null) {return false;}
      return true;
    }

    private Hallway hallwayAt(@Nullable LasertagTileGen tileGen) {
      if (tileGen == null) {return null;}
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.hallways.size(); b++) {
          Hallway hallway = connectedGroup.hallways.get(b);
          if (hallway.tileGen == tileGen) {return hallway;}
          if (hallway.tileGenAbove == tileGen) {return hallway;}
        }
      }
      return null;
    }

    private void emitHallways() {
      PList<Hallway> hallways = new PList<>();
      for (int a = 0; a < connectedGroups.size(); a++) {
        ConnectedGroup connectedGroup = connectedGroups.get(a);
        for (int b = 0; b < connectedGroup.hallways.size(); b++) {
          Hallway hallway = connectedGroup.hallways.get(b);
          hallways.add(hallway);
        }
      }
      // Generate rooms.
      PList<PList<Hallway>> groupedHallways = combineHallwaysIntoAdjacentGroups(hallways);
      for (int a = 0; a < groupedHallways.size(); a++) {
        PList<LasertagTileGen> roomTileGens = new PList<>();
        for (int b = 0; b < groupedHallways.get(a).size(); b++) {
          Hallway hallway = groupedHallways.get(a).get(b);
          roomTileGens.add(hallway.tileGen);
          if (hallway.tileGenAbove != null) {roomTileGens.add(hallway.tileGenAbove);}
        }
        LasertagRoomGen roomGen = new LasertagRoomGen(buildingGen, roomTileGens);
      }
      // Output doors.
      //      for (int a = 0; a < processorDoorGens.size(); a++) {
      //        LasertagDoorGen.PossibleDoor possibleDoor = null;
      //        ProcessorDoorGen doorGen = processorDoorGens.get(a);
      //        if (doorGen.tileGen.x + 1 == doorGen.otherTileGen.x) {
      //          possibleDoor = new LasertagDoorGen.PossibleDoor(doorGen.tileGen.wallGen(LasertagTileWall.Facing.mX),)
      //        }
      //        PAssert.isNotNull(possibleDoor, "Somehow, this doorgen was invalid.");
      //        buildingGen.possibleDoors.add(possibleDoor);
      //      }
    }

    private PList<PList<Hallway>> combineHallwaysIntoAdjacentGroups(PList<Hallway> hallways) {
      PList<PList<Hallway>> ret = new PList<>();
      PList<Hallway> ungroupedHallways = new PList<>();
      ungroupedHallways.addAll(hallways);
      while (!ungroupedHallways.isEmpty()) {
        Hallway searchStartHallway = ungroupedHallways.peek();
        PList<Hallway> hallwaySearchBuffer = new PList<>();
        PList<Hallway> connectedHallways = new PList<>();
        hallwaySearchBuffer.add(searchStartHallway);
        while (!hallwaySearchBuffer.isEmpty()) {
          Hallway searchBufferHallway = hallwaySearchBuffer.removeLast();
          // If removing the value returns false, then the hallways was already grouped.
          if (!ungroupedHallways.removeValue(searchBufferHallway, true)) {continue;}
          connectedHallways.add(searchBufferHallway);
          // Add the neighbors.
          for (int a = 0; a < hallways.size(); a++) {
            if (hallways.get(a).connectsWithHallwayFlatOrSloped(searchBufferHallway)) {
              hallwaySearchBuffer.add(searchStartHallway);
            }
          }
        }
        if (connectedHallways.size() > 0) {
          ret.add(connectedHallways);
        }
      }
      return ret;
    }

    public boolean fullyJoined() {
      return connectedGroups.size() == 1;
    }

    private LasertagRoomGen getRoomGen(@Nullable LasertagTileGen tileGen) {
      if (tileGen == null) {return null;}
      return tileGen.roomGen;
    }

    public ProcessorDoorGen processorDoorGenAt(LasertagTileGen tileGen) {
      for (int a = 0; a < processorDoorGens.size(); a++) {
        if (processorDoorGens.get(a).tileGen == tileGen || processorDoorGens.get(a).otherTileGen == tileGen) {
          return processorDoorGens.get(a);
        }
      }
      return null;
    }
  }

  /**
   * A hallway will affect up to two tiles vertically.
   */
  private static class Hallway implements PImplementsEquals<Hallway> {
    final Context context;
    final int endOffsetY;
    final LasertagTileWall.Facing facing;
    final boolean sloped;
    final LasertagTileGen tileGen;
    LasertagTileGen tileGenAbove;

    public Hallway(Context context, @Nullable LasertagTileGen tileGen, int endOffsetY, boolean sloped,
                   LasertagTileWall.Facing facing) {
      this.context = context;
      LasertagTileGen tileGenAbove = null;
      while (endOffsetY < 0) {
        if (tileGen == null) {
          break;
        }
        endOffsetY += context.rampTiles;
        tileGen = context.neighBor(tileGen, 0, -1, 0);
      }
      this.tileGen = tileGen;
      this.tileGenAbove = (sloped || endOffsetY != 0) ? context.neighBor(tileGen, 0, 1, 0) : null;
      this.endOffsetY = PNumberUtils.mod(endOffsetY, context.rampTiles);
      this.sloped = sloped;
      this.facing = facing;
    }

    public boolean connectsWithConnectedGroup(ConnectedGroup connectedGroup) {
      for (int f = 0; f < FACINGS.length; f++) {
        LasertagTileWall.Facing facing = FACINGS[f];
        if (!sloped || facing == this.facing) {
          if (connectedGroup.hasRoomOrFlatHallwayAt(context.neighBor(tileGen, facing.normalX(), 0, facing.normalZ()),
                                                    endOffsetY)) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean connectsWithHallwayFlatOrSloped(Hallway other) {
      if (other == null) {return false;}
      // We should always be on the same level or above than other, just to simplify things.
      if (other.tileGen.y > tileGen.y) {
        return other.connectsWithHallwayFlatOrSloped(this);
      }
      for (int f = 0; f < FACINGS.length; f++) {
        LasertagTileWall.Facing facing = FACINGS[f];
        if (other.tileGen != context.neighBor(tileGen, facing.normalX(), 0, facing.normalZ())) {continue;}
        if (!sloped || facing == this.facing) {
          if (other.isFlatOnEdge(facing.opposite(), tileGen.y, endOffsetY)) {
            return true;
          }
        }
        if (sloped && facing == this.facing.opposite()) {
          if (other.isFlatOnEdge(facing.opposite(), tileGen.y, endOffsetY + 1)) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean couldBePlaced() {
      if (tileGen == null) {return false;}
      return context.couldGenHallway(tileGen, sloped, endOffsetY, facing);
    }

    private void emitToTileAndTileAbove() {
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

    @Override public boolean equalsT(Hallway other) {
      if (tileGen != other.tileGen) {return false;}
      if (tileGenAbove != other.tileGenAbove) {return false;}
      if (endOffsetY != other.endOffsetY) {return false;}
      if (sloped != other.sloped) {return false;}
      if (facing != other.facing) {return false;}
      return true;
    }

    public boolean isFlatOnEdge(LasertagTileWall.Facing facing, int gridY, int offsetY) {
      int totalCheckOffsetY = gridY * context.rampTiles + offsetY;
      int totalThisEndOffsetY = tileGen.y * context.rampTiles + endOffsetY;
      if (!sloped && totalCheckOffsetY == totalThisEndOffsetY) {return false;}
      if (sloped && totalCheckOffsetY == totalThisEndOffsetY) {return facing == this.facing;}
      if (sloped && totalCheckOffsetY == totalThisEndOffsetY + 1) {return facing == this.facing.opposite();}
      return false;
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

  private static class HallwayStartNode {
    final ConnectedGroup connectedGroup;
    final Context context;
    final LasertagTileWall.Facing facing;
    final int maxSearchDepth = 6;
    final PList<PossibleHallwayPath> possibleHallwayPaths = new PList<>();
    final int startOffsetY;
    final LasertagTileGen tileGen;

    public HallwayStartNode(Context context, LasertagTileGen tileGen, int startOffsetY, ConnectedGroup connectedGroup,
                            LasertagTileWall.Facing facing) {
      this.context = context;
      this.tileGen = tileGen;
      this.facing = facing;
      this.startOffsetY = startOffsetY;
      this.connectedGroup = connectedGroup;
    }

    void __searchRecursive(PList<Hallway> currentHallwayBuffer, Hallway tryAdding, int turnsLeft) {
      // Check for walkway validity.
      if (!tryAdding.couldBePlaced()) {return;}
      // Can't add a walkway where there already is one, or where there would be one on top (if sloped).
      for (int a = 0; a < currentHallwayBuffer.size(); a++) {
        Hallway hallway = currentHallwayBuffer.get(a);
        if (hallway.tileGen == tryAdding.tileGen) {return;}
        if (tryAdding.sloped && hallway.tileGen == context.neighBor(tileGen, 0, 1, 0)) {
          return;
        }
      }
      // Add the walkway.
      currentHallwayBuffer.add(tryAdding);
//      System.out.println("Current buffer : " + currentHallwayBuffer.deepToString());
      // Check to see if adding this tile would connect any connecting groups, and if so, emit.
      if (currentHallwayBuffer.size() >= 4) { // Check for minimum length.
        for (int a = 0; a < context.connectedGroups.size(); a++) {
          ConnectedGroup connectedGroup = context.connectedGroups.get(a);
          if (connectedGroup == this.connectedGroup) {continue;}
          if (tryAdding.connectsWithConnectedGroup(connectedGroup)) {
//            System.out.println("\t\t Valid path!");
            emitPossibleHallwayPath(currentHallwayBuffer);
            break;
          }
        }
      }
      // Recurse.
      if (currentHallwayBuffer.size() < maxSearchDepth) {
        for (int a = 0; a < FACINGS.length; a++) {
          LasertagTileWall.Facing facing = FACINGS[a];
          LasertagTileGen nextTileGen = context.neighBor(tryAdding.tileGen, facing.normalX(), 0, facing.normalZ());
          // Add downward ramps in all directions if this tile is flat, or forward if not.
          if (!tryAdding.sloped || facing == tryAdding.facing) {
            if (facing != tryAdding.facing && turnsLeft <= 0) {continue;}
            int nextTurnLeft = tryAdding.facing == facing ? turnsLeft : turnsLeft - 1;
            __searchRecursive(currentHallwayBuffer,
                              new Hallway(context, nextTileGen, tryAdding.endOffsetY - 1, true, facing), nextTurnLeft);
            __searchRecursive(currentHallwayBuffer,
                              new Hallway(context, nextTileGen, tryAdding.endOffsetY, false, facing), nextTurnLeft);
          }
        }
      }
      // Undo the walkway adding.
      currentHallwayBuffer.removeLast();
    }

    void emitPossibleHallwayPath(PList<Hallway> hallways) {
      PossibleHallwayPath possibleHallwayPath = new PossibleHallwayPath(this, hallways);
      possibleHallwayPaths.add(possibleHallwayPath);
    }

    void search() {
      PList<Hallway> currentWalkwayBuffer = new PList<>();
      __searchRecursive(currentWalkwayBuffer, new Hallway(context, tileGen, startOffsetY, false, facing), 3);
      __searchRecursive(currentWalkwayBuffer, new Hallway(context, tileGen, startOffsetY - 1, true, facing), 3);
    }
  }

  private static class PossibleHallwayPath implements PSortableByScore<PossibleHallwayPath> {
    final PList<ConnectedGroup> allConnectedGroups = new PList<>();
    final HallwayStartNode hallwayStartNode;
    final PList<Hallway> hallways = new PList<>();

    public PossibleHallwayPath(HallwayStartNode startNode, PList<Hallway> hallways) {
      this.hallwayStartNode = startNode;
      Context context = startNode.context;
      if (startNode.connectedGroup != null) {
        allConnectedGroups.add(startNode.connectedGroup);
      }
      for (int a = 0; a < hallways.size(); a++) {
        Hallway hallway = hallways.get(a);
        this.hallways.add(hallway);
        for (int b = 0; b < context.connectedGroups.size(); b++) {
          ConnectedGroup connectedGroup = context.connectedGroups.get(b);
          if (connectedGroup == hallwayStartNode.connectedGroup) {continue;}
          if (hallway.connectsWithConnectedGroup(connectedGroup) && !allConnectedGroups.has(connectedGroup, true)) {
            allConnectedGroups.add(connectedGroup);
          }
        }
      }
    }

    /**
     * @return Whether or not the path was applied.
     */
    public boolean applyToContext() {
      Context context = hallwayStartNode.context;
      // The start connected group will be null for floorless doors.
      System.out.println("\tEmit hallway len: " + hallways.size());
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
      firstGroupSeen.hallways.addAll(hallways);
      // Prevent door spawns except where we want.
      for (int a = 0; a < hallways.size(); a++) {
        Hallway hallway = hallways.get(a);
        for (int f = 0; f < FACINGS.length; f++) {
          LasertagTileWall.Facing facing = FACINGS[f];
          boolean couldGenDoorOnTile = false;
          if (hallway.isFlatOnEdge(facing, hallway.tileGen.y, 0)) {
            couldGenDoorOnTile = true;
          }
          if (couldGenDoorOnTile) {
            couldGenDoorOnTile = ((a == 0 && facing == hallway.facing) || a == hallways.size() - 1);
          }
          if (!couldGenDoorOnTile) {
            // Use facing.opposite for wallGens.
            hallway.tileGen.wallGen(facing.opposite()).preventDoorSpawns = true;
          }
          // TileGenAbove can have a doorGen if we are descending from it.
          if (hallway.tileGenAbove != null) {
            boolean couldGenDoorOnTileAbove = false;
            if (a == 0 && facing == hallway.facing.opposite()) {
              if (hallway.isFlatOnEdge(facing, hallway.tileGenAbove.y, 0)) {
                couldGenDoorOnTileAbove = true;
              }
            }
            if (!couldGenDoorOnTileAbove) {
              // Use facing.opposite for wallGens.
              hallway.tileGenAbove.wallGen(facing.opposite()).preventDoorSpawns = true;
            }
          }
        }
      }
      //      // Add a doorgen at the start and end locations.
      //      Hallway firstHallway = hallways.get(0);
      //      // Add a door if the first hallways is sloped, using the tile above.
      //      if (firstHallway.sloped && firstHallway.endOffsetY == context.rampTiles - 1) {
      //        if (context.processorDoorGenAt(firstHallway.tileGenAbove) == null) {
      //          LasertagTileGen neighbor =
      //              context.neighBor(firstHallway.tileGenAbove, firstHallway.facing.opposite().normalX(), 0,
      //                               firstHallway.facing.opposite().normalZ());
      //          if (neighbor.roomGen != null) {
      //            ProcessorDoorGen doorGen = new ProcessorDoorGen(firstHallway.tileGenAbove, neighbor);
      //            context.processorDoorGens.add(doorGen);
      //          }
      //        }
      //      } else if (!firstHallway.sloped && firstHallway.endOffsetY == 0) {
      //        // If the first hallway is not sloped and is flat, only connect it via the front still, since that's
      //        how it is
      //        // generated.
      //        if (context.processorDoorGenAt(firstHallway.tileGen) == null) {
      //          LasertagTileGen neighbor = context.neighBor(firstHallway.tileGen, firstHallway.facing.opposite()
      //          .normalX(), 0,
      //                                                      firstHallway.facing.opposite().normalZ());
      //          if (neighbor.roomGen != null) {
      //            ProcessorDoorGen doorGen = new ProcessorDoorGen(firstHallway.tileGen, neighbor);
      //            context.processorDoorGens.add(doorGen);
      //          }
      //        }
      //      }
      //      Hallway lastHallway = hallways.peek();
      //      // For the last hallway, find one valid facing to place a door and the place it.
      //      for (int f = 0; f < FACINGS.length; f++) {
      //        LasertagTileWall.Facing facing = FACINGS[f];
      //        if (facing == lastHallway.facing.opposite()) {continue;}
      //        if (!lastHallway.isFlatOnEdge(facing, 0)) {continue;}
      //        if (context.processorDoorGenAt(lastHallway.tileGen) != null) {continue;}
      //        LasertagTileGen neighbor = context.neighBor(lastHallway.tileGen, lastHallway.facing.opposite()
      //        .normalX(), 0,
      //                                                    lastHallway.facing.opposite().normalZ());
      //        if (neighbor.roomGen != null) {
      //          ProcessorDoorGen doorGen = new ProcessorDoorGen(lastHallway.tileGen, neighbor);
      //          context.processorDoorGens.add(doorGen);
      //        }
      //      }
      return true;
    }

    @Override public float score() {
      int score = 0;
      int scorePerConnection = 10;
      score += allConnectedGroups.size();
      score *= scorePerConnection;
      score -= hallways.size();
      return score;
    }
  }

  /**
   * Hallways and doors should only be 1x1 tile wide for now.
   */
  private static class ProcessorDoorGen {
    LasertagTileGen tileGen, otherTileGen;

    ProcessorDoorGen(LasertagTileGen tileGen, LasertagTileGen otherTileGen) {
      this.tileGen = tileGen;
      this.otherTileGen = otherTileGen;
    }
  }
}