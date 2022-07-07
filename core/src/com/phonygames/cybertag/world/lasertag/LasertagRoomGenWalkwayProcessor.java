package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagTileWall.FACINGS;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.Duple;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPooledIterable;
import com.phonygames.pengine.util.Tuple3;

import lombok.val;

public class LasertagRoomGenWalkwayProcessor {
  public static final int rampTiles = 2; // The number of tiles hor. it takes to move up or down one tile vertically.

  /**
   * Gets a list of all possible walkway nodes, where a node is an attachment point to an island or walkway.
   * Assumes that the input tiles are indeed valid tile with floors (or a floor walkway)!
   * @return
   */
  public static PList<WalkwayNode> getWalkwayNodes(PList<LasertagTileGen> tilesToProcess,
                                                   PList<ConnectedGroup> connectedGroups) {
    PList<WalkwayNode> ret = new PList<>();
    for (int a = 0; a < tilesToProcess.size; a++) {
      LasertagTileGen tileGen = tilesToProcess.get(a);
      ConnectedGroup connectedGroup = connectedGroupForTile(tileGen, connectedGroups);
      for (int c = 0; c < FACINGS.length; c++) {
        LasertagTileWall.Facing facing = FACINGS[c];
        LasertagTileGen otherTileGen = tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 0, facing.normalZ());
        // Do some basic prefiltering.
        if (otherTileGen == null || otherTileGen.tile.hasFloor ||
            (!couldGenWalkway(otherTileGen, facing, false, 0, connectedGroups) &&
             !couldGenWalkway(otherTileGen.tileGenInRoomWithLocationOffset(0, -1, 0), facing, true, rampTiles - 1,
                              connectedGroups))) {
          continue;
        }
        if (connectedGroup == null) {continue;}
        ret.add(new WalkwayNode(otherTileGen, facing, connectedGroup));
      }
    }
    return ret;
  }

  private static ConnectedGroup connectedGroupForTile(LasertagTileGen tileGen, PList<ConnectedGroup> connectedGroups) {
    for (int a = 0; a < connectedGroups.size; a++) {
      ConnectedGroup connectedGroup = connectedGroups.get(a);
      if (connectedGroup.tileGens.contains(tileGen, true)) {
        return connectedGroup;
      }
      if (connectedGroup.walkways.get(tileGen.x, tileGen.y, tileGen.z) != null) {
        return connectedGroup;
      }
    }
    return null;
  }

  /**
   *
   */
  private static boolean couldGenWalkway(LasertagTileGen tileGen, LasertagTileWall.Facing facing, boolean sloped,
                                         int endHeightOffset, PList<ConnectedGroup> connectedGroups) {
    if (tileGen == null || tileGen.tile.hasWalkway || (tileGen.tile.hasFloor && !sloped && endHeightOffset == 0)) {
      return false;
    }
    // Check to make sure the tiles below and above do not conflicting walkways.
    for (int a = 0; a < connectedGroups.size; a++) {
      PossibleWalkway lowerWalkway = connectedGroups.get(a).walkways.get(tileGen.x, tileGen.y - 1, tileGen.z);
      if (lowerWalkway != null && !lowerWalkway.isAtFloorLevel()) {return false;}
      PossibleWalkway higherWalkway = connectedGroups.get(a).walkways.get(tileGen.x, tileGen.y + 1, tileGen.z);
      if (higherWalkway != null && sloped) {return false;}
    }
    if (sloped) {
      // Count the number of doors on the tile and the tile above.
      int wallGensWithDoors = 0;
      int wallGensWithDoorsAbove = 0;
      LasertagTileGen aboveTile = tileGen.tileGenInRoomWithLocationOffset(0, 1, 0);
      LasertagTileGen aboveForwardTile = tileGen.tileGenInRoomWithLocationOffset(facing.normalX(), 1, facing.normalZ());
      // Check to make sure the tile above and above-forward is owned by this room.
      if (aboveTile == null || aboveForwardTile == null) {return false;}
      for (int a = 0; a < FACINGS.length; a++) {
        LasertagTileWall.Facing f = FACINGS[a];
        if (tileGen.wallGen(f).wall.isDoor()) {
          wallGensWithDoors++;
        }
        if (aboveTile.wallGen(f).wall.isDoor()) {
          wallGensWithDoorsAbove++;
          // Slopes must be compatible with all doors in the above tile.
          if (f == facing && endHeightOffset != rampTiles - 1) {return false;}
        }
        // Slopes must be not lead players into a tile beneath a door (since youd hit ur head.)
        if (aboveForwardTile.wallGen(f).wall.isDoor()) {return false;}
        // Check to make sure there isn't a walkway that would try to dump players at this spot.
        for (int b = 0; b < connectedGroups.size; b++) {
          PossibleWalkway neighborWalkway =
              connectedGroups.get(b).walkways.get(tileGen.x + f.normalX(), tileGen.y, tileGen.z + f.normalZ());
          if (neighborWalkway != null && neighborWalkway.slopedWalkwayAtTileWouldBlock(tileGen)) {return false;}
        }
      }
      // Check to make sure the slope won't block any doors.
      if (wallGensWithDoors != 0) {return false;}
      // If the above tile has two doors, don't allow sloped walkways.
      if (wallGensWithDoorsAbove > 1) {return false;}
    } else {
      return endHeightOffset == 0;
    }
    return true;
  }

  public static void processRoomWalkways(LasertagBuildingGen buildingGen) {
    for (int a = 0; a < buildingGen.roomGens.size; a++) {
      processRoomWalkways(buildingGen.roomGens.get(a));
    }
  }

  // Doors should already be applied for walls.
  public static void processRoomWalkways(LasertagRoomGen roomGen) {
    PList<ConnectedGroup> connectedGroups = new PList<>();
    PIntAABB roomBoundsAABB = roomGen.tileGens.keyBounds(new PIntAABB());
    PList<PList<LasertagTileGen>> tileGensPerY = new PList<>();
    PList<PList<LasertagTileGen>> tileGensWithFloorsPerY = new PList<>();
    PList<PList<PList<LasertagTileGen>>> tileGensIslandsWithFloorsPerY = new PList<>();
    PMap<LasertagDoorGen, Duple<PList<LasertagTileGen>, LasertagTileWall.Facing>> floorlessDoors =
        new PMap<LasertagDoorGen, Duple<PList<LasertagTileGen>, LasertagTileWall.Facing>>() {
          @Override public Duple<PList<LasertagTileGen>, LasertagTileWall.Facing> newUnpooled(LasertagDoorGen doorGen) {
            return new Duple<>(new PList<>(), null);
          }
        };
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
    // Process doors and determine if they have any tiles not on the floor.
    for (int a = 0; a < roomGen.buildingGen.doorGens.size; a++) {
      LasertagDoorGen doorGen = roomGen.buildingGen.doorGens.get(a);
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
        int xChangeForAlongWall = doorGen.door.facing == LasertagTileWall.Facing.Z ? -1 :
                                  (doorGen.door.facing == LasertagTileWall.Facing.mZ ? 1 : 0);
        int zChangeForAlongWall = doorGen.door.facing == LasertagTileWall.Facing.X ? 1 :
                                  (doorGen.door.facing == LasertagTileWall.Facing.mX ? -1 : 0);
        for (int testX = 0; testX < doorGen.door.w; testX++) { // We only need to check the bottom layer.
          LasertagTileGen lookTile =
              roomGen.tileGens.get(tileX + testX * xChangeForAlongWall, tileY, tileZ + testX * zChangeForAlongWall);
          if (lookTile.tile.hasFloor) {
            continue;
          }
          Duple<PList<LasertagTileGen>, LasertagTileWall.Facing> floorlessDoor = floorlessDoors.genUnpooled(doorGen);
          floorlessDoor.getKey().add(lookTile);
          floorlessDoor.setValue(lookFacing);
        }
      }
    }
    // Find the islands.
    PList<LasertagTileGen> islandTilesSearchBuffer = new PList<>();
    for (int yI = 0; yI < tileGensWithFloorsPerY.size; yI++) {
      PList<LasertagTileGen> unassignedTiles = new PList<>();
      unassignedTiles.addAll(tileGensWithFloorsPerY.get(yI));
      while (!unassignedTiles.isEmpty()) {
        PList<LasertagTileGen> islandTilesBuffer = new PList<>();
        PList<LasertagTileWallGen> islandTilesWallsBuffer = new PList<>();
        islandTilesSearchBuffer.add(unassignedTiles.removeLast());
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
          ConnectedGroup connectedGroup = new ConnectedGroup(roomGen);
          connectedGroups.add(connectedGroup);
          connectedGroup.tileGens.addAll(islandTilesBuffer);
        }
      }
    }
    if (floorlessDoors.size() != 0) {
      System.out.println("Floorless doors: " + floorlessDoors.size());
    }
    PossibleWalkwayLayout rampOnlyLayout =
        PossibleWalkwayLayout.getOnlyRampLayout(roomGen, connectedGroups, floorlessDoors);
    if (rampOnlyLayout != null) {
      rampOnlyLayout.applyToRoom();
    }
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

  // Represents a set of connected tiles and doors.
  static class ConnectedGroup {
    final PList<LasertagTileGen> tileGens = new PList<>();
    private final LasertagRoomGen roomGen;
    final PIntMap3d<PossibleWalkway> walkways = new PIntMap3d<PossibleWalkway>() {
      @Override public PossibleWalkway newUnpooled(int x, int y, int z) {
        PossibleWalkway ret = new PossibleWalkway();
        ret.tileGen = roomGen.tileGens.get(x, y, z);
        return ret;
      }
    };

    public ConnectedGroup(LasertagRoomGen roomGen) {
      this.roomGen = roomGen;
    }

    public ConnectedGroup addAll(ConnectedGroup other) {
      tileGens.addAll(other.tileGens);
      walkways.putAll3d(walkways);
      return this;
    }

    /**
     * Only processes flat walkways.
     * @param other
     * @return
     */
    public boolean isAdjacentTo(ConnectedGroup other) {
      for (int a = 0; a < tileGens.size; a++) {
        LasertagTileGen tileGen = tileGens.get(a);
        if (other.tileGens.contains(tileGen.tileGenInRoomWithLocationOffset(1, 0, 0), true)) {return true;}
        if (other.walkways.get(tileGen.x + 1, tileGen.y, tileGen.z) != null) {return true;}
        if (other.tileGens.contains(tileGen.tileGenInRoomWithLocationOffset(-1, 0, 0), true)) {return true;}
        if (other.walkways.get(tileGen.x - 1, tileGen.y, tileGen.z) != null) {return true;}
        if (other.tileGens.contains(tileGen.tileGenInRoomWithLocationOffset(0, 0, 1), true)) {return true;}
        if (other.walkways.get(tileGen.x, tileGen.y, tileGen.z + 1) != null) {return true;}
        if (other.tileGens.contains(tileGen.tileGenInRoomWithLocationOffset(0, 0, -1), true)) {return true;}
        if (other.walkways.get(tileGen.x, tileGen.y, tileGen.z - 1) != null) {return true;}
      }
      try (val it = walkways.obtainIterator()) {
        while (it.hasNext()) {
          val e = it.next();
          PossibleWalkway walkway = e.val();
          if (other.tileGens.contains(roomGen.tileGens.get(e.x() + 1, e.y(), e.z()), true)) {return true;}
          if (other.walkways.get(e.x() + 1, e.y(), e.z()) != null) {return true;}
          if (other.tileGens.contains(roomGen.tileGens.get(e.x() - 1, e.y(), e.z()), true)) {return true;}
          if (other.walkways.get(e.x() - 1, e.y(), e.z()) != null) {return true;}
          if (other.tileGens.contains(roomGen.tileGens.get(e.x(), e.y(), e.z() + 1), true)) {return true;}
          if (other.walkways.get(e.x(), e.y(), e.z() + 1) != null) {return true;}
          if (other.tileGens.contains(roomGen.tileGens.get(e.x(), e.y(), e.z() - 1), true)) {return true;}
          if (other.walkways.get(e.x(), e.y(), e.z() - 1) != null) {return true;}
        }
      }
      return false;
    }
  }

  private static class PossibleWalkway {
    // Whole numbers representing the numerator of the fraction of the height of the corner, with the denominator
    // equal to rampTiles.
    private int height00, height10, height11, height01;
    private LasertagTileGen tileGen;

    private void applyToRoom(LasertagRoomGen roomGen) {
      PAssert.isNotNull(tileGen);
      tileGen.tile.hasWalkway = true;
      tileGen.tile.walkwayTile00OffsetY = ((float) height00) / rampTiles;
      tileGen.tile.walkwayTile10OffsetY = ((float) height10) / rampTiles;
      tileGen.tile.walkwayTile11OffsetY = ((float) height11) / rampTiles;
      tileGen.tile.walkwayTile01OffsetY = ((float) height01) / rampTiles;
    }

    public boolean isAtFloorLevel() {
      return height00 == 0 && height01 == 0 && height11 == 0 && height10 == 0;
    }

    public boolean slopedWalkwayAtTileWouldBlock(LasertagTileGen otherTileGen) {
      PAssert.isTrue(tileGen.y == otherTileGen.y);
      if (otherTileGen.x == tileGen.x && otherTileGen.z == tileGen.z - 1) {
        return height00 == 0 && height10 == 0;
      }
      if (otherTileGen.x == tileGen.x - 1 && otherTileGen.z == tileGen.z) {
        return height00 == 0 && height01 == 0;
      }
      if (otherTileGen.x == tileGen.x && otherTileGen.z == tileGen.z + 1) {
        return height01 == 0 && height11 == 0;
      }
      if (otherTileGen.x == tileGen.x + 1 && otherTileGen.z == tileGen.z) {
        return height10 == 0 && height11 == 0;
      }
      PAssert.fail("Tiles were not adjacent.");
      return false;
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
                                                           PList<ConnectedGroup> connectedGroups,
                                                           PMap<LasertagDoorGen, Duple<PList<LasertagTileGen>,
                                                               LasertagTileWall.Facing>> floorlessDoors) {
      System.out.println("Roomgen layout get");
      PAssert.isTrue(connectedGroups.size > 0);
      PossibleWalkwayLayout ret = new PossibleWalkwayLayout(roomGen);
      PList<Tuple3<PossibleWalkwayPath.RecursionStep, Boolean, Boolean>> downForwardRecurseOrder =
          new PList<>(); // A list of tuples: Step, down, enabled. This should only contain down forward.
      downForwardRecurseOrder.add(new Tuple3<>(PossibleWalkwayPath.RecursionStep.Forward, true, true));
      // Process large doors.
      try (
          PPooledIterable.PPoolableIterator<PMap.Entry<LasertagDoorGen, Duple<PList<LasertagTileGen>,
              LasertagTileWall.Facing>>> it = floorlessDoors.obtainIterator()) {
        while (it.hasNext()) {
          val e = it.next();
          LasertagDoorGen doorGen = e.k();
          PList<LasertagTileGen> affectedTiles = e.v().getKey();
          LasertagTileWall.Facing facing = e.v().getValue();
          if (affectedTiles.size <= 1) {continue;}
          boolean tryLargeRamp = Math.random() < 0, emittedRamp = false;
          if (tryLargeRamp) { // Try creating a downward ramp.
            PList<PossibleWalkwayPath> slopeWalkwayPaths = new PList<>();
            for (int a = 0; a < affectedTiles.size; a++) {
              WalkwayNode walkwayNode = new WalkwayNode(affectedTiles.get(a), facing, null);
              PossibleWalkwayPath downPath = new PossibleWalkwayPath(walkwayNode);
              downPath.gen(connectedGroups, downForwardRecurseOrder);
              if (downPath.isValid()) {
                slopeWalkwayPaths.add(downPath);
              }
            }
            if (slopeWalkwayPaths.size == affectedTiles.size) {
              emittedRamp = true;
              System.out.println("\tEmitting large ramp");
              for (int a = 0; a < slopeWalkwayPaths.size; a++) {
                slopeWalkwayPaths.get(a).emit(ret, connectedGroups);
                System.out.println("\t\t" + slopeWalkwayPaths.get(a).tileGens.size);
              }
            }
          }
          if (!emittedRamp) {
            System.out.println("\tDid not emit large ramp; placing walkways");
            // Place walkways at the foot of the door.
            ConnectedGroup doorGroup = new ConnectedGroup(roomGen);
            for (int a = 0; a < affectedTiles.size; a++) {
              LasertagTileGen tileGen = affectedTiles.get(a);
              doorGroup.walkways.genUnpooled(tileGen.x, tileGen.y, tileGen.z);
              System.out.println("\t\t" + tileGen.x + "," + tileGen.y + "," + tileGen.z);
            }
            for (int a = 0; a < connectedGroups.size; a++) {
              if (doorGroup.isAdjacentTo(connectedGroups.get(a))) {
                connectedGroups.get(a).addAll(doorGroup);
                doorGroup = null;
                System.out.println("\t\tJoining group int connectedgroup " + a);
                break;
              }
            }
            if (doorGroup != null) {
              connectedGroups.add(doorGroup);
            }
          }
        }
      }
      System.out.println("\tconnectedNodes: " + connectedGroups.size);
      // Add random connections until there is only one group.
      for (int attempt = 0; attempt < 1000; attempt++) {
        if (connectedGroups.size == 1) {
          if (attempt > 0) {
            System.out.println("\tStopping island connections since there is only 1 group remaining.");
          }
          break;
        }
        PList<WalkwayNode> walkwayNodes = ret.getWalkwayNodes(connectedGroups);
        for (int a = 0; a < walkwayNodes.size; a++) {
          LasertagTileGen tileGen = walkwayNodes.get(a).tileGen;
          tileGen.wallGen(walkwayNodes.get(a).facing).wall.hasWall = true;
          tileGen.wallGen(walkwayNodes.get(a).facing).wall.isWindow = true;
          tileGen.wallGen(walkwayNodes.get(a).facing).wall.isValid = true;
          ;
          tileGen.wallGen(walkwayNodes.get(a).facing).wall.isSolidWall = true;
        }
        if (walkwayNodes.size == 0) {
          System.out.println("\tStopping island connections since there were no nodes");
          break;
        }
        // Take a random starting tile from the first group.
        WalkwayNode startingNode = walkwayNodes.get(attempt % walkwayNodes.size);
        PAssert.isNotNull(startingNode);
        PossibleWalkwayPath possibleWalkwayPath = new PossibleWalkwayPath(startingNode);
        System.out.println("\tAttempt island connect " + attempt + "; connectedNodes: " + connectedGroups.size);
        if (attempt == 500) {
          System.out.println("\tThis seems to not be going well huh");
        }
        possibleWalkwayPath.gen(connectedGroups);
        if (possibleWalkwayPath.isValid()) {
          possibleWalkwayPath.emit(ret, connectedGroups);
        }
      }
      // Add ramps from floorless doors.
      try (
          PPooledIterable.PPoolableIterator<PMap.Entry<LasertagDoorGen, Duple<PList<LasertagTileGen>,
              LasertagTileWall.Facing>>> it = floorlessDoors.obtainIterator()) {
        while (it.hasNext()) {
          val e = it.next();
          LasertagDoorGen doorGen = e.k();
          PList<LasertagTileGen> affectedTiles = e.v().getKey();
          LasertagTileWall.Facing facing = e.v().getValue();
          if (affectedTiles.size == 1) {
            System.out.println("\tTrying to create a walkway from a 1-tile door.");
            LasertagTileGen startTile = affectedTiles.get(0);
            WalkwayNode walkwayNode = new WalkwayNode(startTile, facing, null);
            PossibleWalkwayPath possibleWalkwayPath = new PossibleWalkwayPath(walkwayNode);
            possibleWalkwayPath.gen(connectedGroups);
            if (possibleWalkwayPath.isValid()) {
              possibleWalkwayPath.emit(ret, connectedGroups);
            }
          }
        }
      }
      // The layout should get the walkways from the connected groups.
      for (int a = 0; a < connectedGroups.size; a++) {
        try (val it = connectedGroups.get(a).walkways.obtainIterator()) {
          while (it.hasNext()) {
            val e = it.next();
            ret.walkways.put(e.x(), e.y(), e.z(), e.val());
          }
        }
      }
      return ret;
    }

    /**
     * Gets a list of all possible walkway nodes, where a node is an attachment point to an island or walkway.
     * @return
     */
    public PList<WalkwayNode> getWalkwayNodes(PList<ConnectedGroup> connectedGroups) {
      PList<LasertagTileGen> tilesToProcess = new PList<>();
      for (int a = 0; a < connectedGroups.size; a++) {
        tilesToProcess.addAll(connectedGroups.get(a).tileGens);
        try (val it = connectedGroups.get(a).walkways.obtainIterator()) { // Add flat floor-height walkways.
          while (it.hasNext()) {
            val e = it.next();
            if (e.val().isAtFloorLevel()) {
              tilesToProcess.add(e.val().tileGen);
            }
          }
        }
      }
      return LasertagRoomGenWalkwayProcessor.getWalkwayNodes(tilesToProcess, connectedGroups);
    }

    private void applyToRoom() {
      try (val it = walkways.obtainIterator()) {
        while (it.hasNext()) {
          val e = it.next();
          e.val().applyToRoom(roomGen);
        }
      }
    }

    private static class PossibleWalkwayPath {
      final PList<LasertagTileWall.Facing> facings = new PList<>();
      final WalkwayNode startingNode;
      final PList<LasertagTileGen> tileGens = new PList<>();
      final PList<Integer> walkwayHeightOffsets = new PList<>(); // Length should be 2xtileGens.length.
      PList<Tuple3<RecursionStep, Boolean, Boolean>> defaultRecurseOrder;
      private ConnectedGroup secondConnectedGroup;

      public PossibleWalkwayPath(WalkwayNode startingNode) {
        this.startingNode = startingNode;
        defaultRecurseOrder = new PList<>(); // A list of tuples: Step, down, enabled.
        defaultRecurseOrder.add(new Tuple3<>(RecursionStep.Forward, false, true));
        defaultRecurseOrder.add(new Tuple3<>(RecursionStep.Forward, true, true));
        defaultRecurseOrder.add(new Tuple3<>(RecursionStep.Left, false, true));
        defaultRecurseOrder.add(new Tuple3<>(RecursionStep.Left, true, true));
        defaultRecurseOrder.add(new Tuple3<>(RecursionStep.Right, false, true));
        defaultRecurseOrder.add(new Tuple3<>(RecursionStep.Right, true, true));
        defaultRecurseOrder.shuffle();
      }

      /**
       * Checks if this position could connect to another connected group, and if so, returns that group.
       * @param x
       * @param y
       * @param z
       * @param facing
       * @return
       */
      private ConnectedGroup checkIsEndLoc(int x, int y, int z, LasertagTileWall.Facing facing,
                                           PList<ConnectedGroup> connectedGroups) {
        x += facing.normalX();
        z += facing.normalZ();
        for (int a = 0; a < connectedGroups.size; a++) {
          ConnectedGroup group = connectedGroups.get(a);
          if (group == startingNode.connectedGroup) { // Don't stop this walkway if it is simply connected to ourselves.
            continue;
          }
          if (group.walkways.get(x, y, z) != null) { // Check if there is a valid (floorLevel) walkway.
            if (group.walkways.get(x, y, z).isAtFloorLevel()) {
              return group;
            }
          }
          for (int b = 0; b < group.tileGens.size; b++) { // Check if there is a valid tile.
            LasertagTileGen tileGen = group.tileGens.get(b);
            if (tileGen.x == x && tileGen.y == y && tileGen.z == z && tileGen != null &&
                (tileGen.tile.hasFloor || tileGen.tile.hasFloorWalkway())) {
              return group;
            }
          }
        }
        return null;
      }

      /**
       * Outputs the walkways into the layout's walkway map, and merges the connected group.
       * @param layout
       * @param connectedGroups
       */
      void emit(PossibleWalkwayLayout layout, PList<ConnectedGroup> connectedGroups) {
        System.out.println("Emitting");
        PAssert.isTrue(isValid());
        ConnectedGroup group = startingNode.connectedGroup != null ? startingNode.connectedGroup : secondConnectedGroup;
        if (startingNode.connectedGroup != null) {
          startingNode.connectedGroup.addAll(secondConnectedGroup);
          connectedGroups.removeValue(secondConnectedGroup, true);
        }
        for (int a = 0; a < tileGens.size; a++) {
          LasertagTileGen tileGen = tileGens.get(a);
          PossibleWalkway possibleWalkway = new PossibleWalkway();
          possibleWalkway.tileGen = tileGen;
          LasertagTileWall.Facing facing = facings.get(a);
          switch (facing) {
            case X:
              possibleWalkway.height00 = walkwayHeightOffsets.get(a * 2 + 0);
              possibleWalkway.height10 = walkwayHeightOffsets.get(a * 2 + 1);
              possibleWalkway.height11 = walkwayHeightOffsets.get(a * 2 + 1);
              possibleWalkway.height01 = walkwayHeightOffsets.get(a * 2 + 0);
              break;
            case Z:
              possibleWalkway.height00 = walkwayHeightOffsets.get(a * 2 + 0);
              possibleWalkway.height10 = walkwayHeightOffsets.get(a * 2 + 0);
              possibleWalkway.height11 = walkwayHeightOffsets.get(a * 2 + 1);
              possibleWalkway.height01 = walkwayHeightOffsets.get(a * 2 + 1);
              break;
            case mX:
              possibleWalkway.height00 = walkwayHeightOffsets.get(a * 2 + 1);
              possibleWalkway.height10 = walkwayHeightOffsets.get(a * 2 + 0);
              possibleWalkway.height11 = walkwayHeightOffsets.get(a * 2 + 0);
              possibleWalkway.height01 = walkwayHeightOffsets.get(a * 2 + 1);
              break;
            case mZ:
              possibleWalkway.height00 = walkwayHeightOffsets.get(a * 2 + 1);
              possibleWalkway.height10 = walkwayHeightOffsets.get(a * 2 + 1);
              possibleWalkway.height11 = walkwayHeightOffsets.get(a * 2 + 0);
              possibleWalkway.height01 = walkwayHeightOffsets.get(a * 2 + 0);
              break;
          }
          group.walkways.put(tileGen.x, tileGen.y, tileGen.z, possibleWalkway);
        }
      }

      boolean isValid() {
        return secondConnectedGroup != null;
      }

      void gen(PList<ConnectedGroup> connectedGroups) {
        gen(connectedGroups, defaultRecurseOrder);
      }

      void gen(PList<ConnectedGroup> connectedGroups, PList<Tuple3<RecursionStep, Boolean, Boolean>> recurseOrder) {
        LasertagTileWall.Facing curFacing = startingNode.facing;
        boolean forwardDownFirst = false;
        boolean hasStraightForward = false;
        boolean hasDownForward = false;
        for (int a = recurseOrder.size - 1; a >= 0; a--) {
          if (recurseOrder.get(a).c() && recurseOrder.get(a).a() == RecursionStep.Forward) {
            forwardDownFirst = recurseOrder.get(a).b();
            if (!forwardDownFirst) {
              hasStraightForward = true;
            } else {
              hasDownForward = true;
            }
          }
        }
        if (forwardDownFirst) {
          secondConnectedGroup = pushForward(connectedGroups, curFacing, -1,
                                             startingNode.tileGen.tileGenInRoomWithLocationOffset(0, -1, 0), 10,
                                             recurseOrder);
        }
        if (hasStraightForward && secondConnectedGroup == null) {
          secondConnectedGroup = pushForward(connectedGroups, curFacing, 0, startingNode.tileGen, 10, recurseOrder);
        }
        if (!forwardDownFirst && hasDownForward && secondConnectedGroup == null) {
          secondConnectedGroup = pushForward(connectedGroups, curFacing, -1,
                                             startingNode.tileGen.tileGenInRoomWithLocationOffset(0, -1, 0), 10,
                                             recurseOrder);
        }
      }

      private void printSearchPath() {
        if (walkwayHeightOffsets.isEmpty()) {return;}
        for (int a = 0; a < tileGens.size; a++) {
          LasertagTileGen t = tileGens.get(a);
          System.out.print(walkwayHeightOffsets.get(a * 2 + 0) + ":" + t.x + "," + t.y + "," + t.z + ":" +
                           walkwayHeightOffsets.get(a * 2 + 1) + "  ");
        }
        System.out.println();
      }

      /**
       * Pushes the search forward by one tile, returning a connected group if one is connected by the search.
       * recurseMask: 0 is no recursion, 1 is flat, 2 is sloped, 3 is both flat and sloped.
       * @return
       */
      private ConnectedGroup pushForward(PList<ConnectedGroup> connectedGroups, LasertagTileWall.Facing facing,
                                         int yChangePerForward, LasertagTileGen tileGen, int maxSearchDist,
                                         PList<Tuple3<RecursionStep, Boolean, Boolean>> recurseOrder) {
        if (tileGen == null || maxSearchDist <= tileGens.size) {
          return null;
        }
        ConnectedGroup ret = null;
        PAssert.isFalse(yChangePerForward > 0, "Pushforward can only go down!");
        int searchX = tileGen.x;
        int searchY = tileGen.y;
        int searchZ = tileGen.z;
        LasertagRoomGen roomGen = tileGen.roomGen;
        int endRampYI =
            PNumberUtils.mod((walkwayHeightOffsets.isEmpty() ? 0 : walkwayHeightOffsets.peek()) + yChangePerForward,
                             rampTiles);
        int startRampYI = endRampYI - yChangePerForward;
        boolean couldGenWalkwayResult =
            couldGenWalkway(tileGen, facing, yChangePerForward != 0, endRampYI, connectedGroups);
        if (!couldGenWalkwayResult) {
          System.out.println("Stop search due to invalid position: " + searchX + "," + searchY + "," + searchZ);
          couldGenWalkwayResult = couldGenWalkway(tileGen, facing, yChangePerForward != 0, endRampYI, connectedGroups);
          return null;
        }
        tileGens.add(tileGen);
        walkwayHeightOffsets.add(startRampYI);
        walkwayHeightOffsets.add(endRampYI);
        facings.add(facing);
        // Check to see if the end tile, facing forward, is a valid end position.
        if (endRampYI == 0 && (ret = checkIsEndLoc(searchX, searchY, searchZ, facing, connectedGroups)) != null) {
          System.out.print("Successful search ");
          printSearchPath();
          return ret;
        }
        // Check to see if the end tile, facing the left, is a valid position.
        if (endRampYI == 0 && startRampYI == 0 &&
            (ret = checkIsEndLoc(searchX, searchY, searchZ, facing.leftCorner(), connectedGroups)) != null) {
          System.out.print("Successful search ");
          printSearchPath();
          return ret;
        }
        // Check to see if the end tile, facing the right, is a valid position.
        if (endRampYI == 0 && startRampYI == 0 &&
            (ret = checkIsEndLoc(searchX, searchY, searchZ, facing.leftCorner().opposite(), connectedGroups)) != null) {
          System.out.print("Successful search ");
          printSearchPath();
          return ret;
        }
        // Recurse using the steps in the recurseorder param.
        for (int a = 0; a < recurseOrder.size; a++) {
          // Skip this step if it is disabled.
          if (!recurseOrder.get(a).c()) {continue;}
          LasertagTileWall.Facing nextFacing;
          LasertagTileGen nextTileGen;
          boolean nextGoingDown = recurseOrder.get(a).b();
          int nextYChangePerForward = nextGoingDown ? -1 : 0;
          int nextTileGenY = (nextGoingDown && endRampYI == 0) ? tileGen.y - 1 : tileGen.y;
          boolean isValid = false;
          RecursionStep recursionStep = recurseOrder.get(a).a();
          switch (recursionStep) {
            case Forward:
            default:
              nextFacing = facing;
              isValid = true;
              break;
            case Left:
              nextFacing = facing.leftCorner();
              isValid = endRampYI == 0 && startRampYI == 0;
              break;
            case Right:
              nextFacing = facing.leftCorner().opposite();
              isValid = endRampYI == 0 && startRampYI == 0;
              break;
          }
          nextTileGen = tileGen.roomGen.tileGens.get(tileGen.x + nextFacing.normalX(), nextTileGenY,
                                                     tileGen.z + nextFacing.normalZ());
          if (isValid) {
            if (recursionStep.disableAfterUsing()) {
              for (int c = 0; c < recurseOrder.size; c++) {
                if (recurseOrder.get(c).a().disableAfterUsing()) {
                  recurseOrder.get(c).c(false);
                }
              }
            }
            if ((ret = pushForward(connectedGroups, nextFacing, nextYChangePerForward, nextTileGen, maxSearchDist - 1,
                                   recurseOrder)) != null) {return ret;}
            if (recursionStep.disableAfterUsing()) {
              for (int c = 0; c < recurseOrder.size; c++) {
                if (recurseOrder.get(c).a().disableAfterUsing()) {
                  recurseOrder.get(c).c(true);
                }
              }
            }
          }
        }
        // Undo adding the buffer data.
        tileGens.removeLast();
        walkwayHeightOffsets.removeLast();
        walkwayHeightOffsets.removeLast();
        facings.removeLast();
        return null;
      }

      enum RecursionStep {
        Forward, Left, Right;

        boolean disableAfterUsing() {return this != Forward;}
      }

      enum SearchStrategy {
        Forward, Down // Never search up - instead, rely on downward search from above.
      }
    }
  }

  // A node is a tile and an edge, where that edge connects to a door, walkway, or island.
  static class WalkwayNode {
    final ConnectedGroup connectedGroup;
    final LasertagTileWall.Facing facing;
    final LasertagTileGen tileGen;

    private WalkwayNode(LasertagTileGen tileGen, LasertagTileWall.Facing facing, ConnectedGroup connectedGroup) {
      this.tileGen = tileGen;
      this.facing = facing;
      this.connectedGroup = connectedGroup;
    }
  }
}
