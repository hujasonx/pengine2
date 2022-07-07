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
      for (int c = 0; c < FACINGS.length; c++) {
        LasertagTileWall.Facing facing = FACINGS[c];
        LasertagTileGen otherTileGen = tileGen.tileGenInRoomWithLocationOffset(-facing.normalX(), 0, -facing.normalZ());
        if (couldGenWalkway(otherTileGen, connectedGroups) != 3) {
          continue;
        } // Only get nodes that can go forward or down.
        ConnectedGroup connectedGroup = connectedGroupForTile(tileGen, connectedGroups);
        if (connectedGroup == null) {continue;}
        ret.add(new WalkwayNode(otherTileGen, facing, connectedGroup));
      }
    }
    return ret;
  }

  /**
   * @param tileGen
   * @param connectedGroups
   * @return 0 if none, 1 if flat, 2 if sloped, 3 if sloped or flat.
   */
  private static int couldGenWalkway(LasertagTileGen tileGen, PList<ConnectedGroup> connectedGroups) {
    int ret = 0;
    if (tileGen == null || tileGen.tile.hasWalkway) {return 0;}
    if (tileGen.tile.hasFloor) {ret = 2;} // Tiles with floors can have sloped walkways.
    else {
      boolean valid = true;
      // Check to make sure the tile below does not have a non-floor walkway.
      for (int a = 0; a < connectedGroups.size; a++) {
        PossibleWalkway lowerWalkway = connectedGroups.get(a).walkways.get(tileGen.x, tileGen.y - 1, tileGen.z);
        if (lowerWalkway != null && !lowerWalkway.isAtFloorLevel()) {
          valid = false;
          break;
        }
      }
      if (valid) {
        ret = 3;
      }
    }
    int wallGensWithDoors = 0;
    for (int a = 0; a < FACINGS.length; a++) {
      LasertagTileWall.Facing facing = FACINGS[a];
      if (tileGen.wallGen(facing).wall.isDoor()) {
        wallGensWithDoors++;
      }
    }
    if (wallGensWithDoors > 0) { // If the tile has a door next to it, we should not block it with a sloped walkway.
      ret &= 0xFD;
    }
    // Next, count how many door tiles are above us. If there are more than two, we should not emit sloped.
    wallGensWithDoors = 0;
    tileGen = tileGen.roomGen.tileGens.get(tileGen.x, tileGen.y + 1, tileGen.z);
    if (tileGen != null) {
      for (int a = 0; a < FACINGS.length; a++) {
        LasertagTileWall.Facing facing = FACINGS[a];
        if (tileGen.wallGen(facing).wall.isDoor()) {
          wallGensWithDoors++;
        }
      }
    }
    if (wallGensWithDoors > 1) {
      ret &= 0xFD;
    }
    return ret;
  }

  private static ConnectedGroup connectedGroupForTile(LasertagTileGen tileGen, PList<ConnectedGroup> connectedGroups) {
    for (int a = 0; a < connectedGroups.size; a++) {
      ConnectedGroup connectedGroup = connectedGroups.get(a);
      if (connectedGroup.tileGens.contains(tileGen, true)) {
        return connectedGroup;
      }
    }
    return null;
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
          ConnectedGroup connectedGroup = new ConnectedGroup();
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
    final PIntMap3d<PossibleWalkway> walkways = new PIntMap3d<>();

    public ConnectedGroup addAll(ConnectedGroup other) {
      tileGens.addAll(other.tileGens);
      walkways.putAll3d(walkways);
      return this;
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
      PAssert.isTrue(connectedGroups.size > 0);
      PossibleWalkwayLayout ret = new PossibleWalkwayLayout(roomGen);
      // Add random connections until there is only one group.
      for (int attempt = 0; attempt < 100; attempt++) {
        if (connectedGroups.size == 1) {
          break;
        }
        PList<WalkwayNode> walkwayNodes = ret.getWalkwayNodes(connectedGroups);
        // Take a random starting tile from the first group.
        WalkwayNode startingNode = walkwayNodes.random();
        if (startingNode == null) {
          break;
        }
        PossibleWalkwayPath possibleWalkwayPath = new PossibleWalkwayPath(startingNode);
        possibleWalkwayPath.gen(connectedGroups);
        System.out.println("Attempt island connect " + attempt);
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
          for (int yOffsetPerHor = 0; yOffsetPerHor >= -1; yOffsetPerHor--) {
            ConnectedGroup connectedGroup = null;
            if (affectedTiles.size == 1) {
              System.out.println("Possible door");
              LasertagTileGen startTile = affectedTiles.get(0);
              WalkwayNode walkwayNode = new WalkwayNode(startTile, facing, null);
              PossibleWalkwayPath possibleWalkwayPath = new PossibleWalkwayPath(walkwayNode);
              possibleWalkwayPath.gen(connectedGroups);
              if (possibleWalkwayPath.isValid()) {
                possibleWalkwayPath.emit(ret, connectedGroups);
                break;
              }
            } else {
              System.out.println("Door too big");
              for (int a = 0; a < affectedTiles.size; a++) {
              }
              break;
            }
          }
        }
      }
      // Again, add random connections until there is only one group. (Process the new groups generated by the floorless
      // door phase.)
      for (int attempt = 0; attempt < 100; attempt++) {
        if (connectedGroups.size == 1) {
          break;
        }
        PList<WalkwayNode> walkwayNodes = ret.getWalkwayNodes(connectedGroups);
        // Take a random starting tile from the first group.
        WalkwayNode startingNode = walkwayNodes.random();
        if (startingNode == null) {
          break;
        }
        PossibleWalkwayPath possibleWalkwayPath = new PossibleWalkwayPath(startingNode);
        possibleWalkwayPath.gen(connectedGroups);
        System.out.println("Attempt island connect " + attempt);
        if (possibleWalkwayPath.isValid()) {
          possibleWalkwayPath.emit(ret, connectedGroups);
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
      }
      try (val it = walkways.obtainIterator()) { // Add flat floor-height walkways.
        while (it.hasNext()) {
          val e = it.next();
          if (e.val().isAtFloorLevel()) {
            tilesToProcess.add(e.val().tileGen);
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
      private PList<SearchStrategy> searchStrategies = new PList<>();
      private ConnectedGroup secondConnectedGroup;

      public PossibleWalkwayPath(WalkwayNode startingNode) {
        this.startingNode = startingNode;
        if (Math.random() < .5) {
          searchStrategies.add(SearchStrategy.Forward);
          searchStrategies.add(SearchStrategy.Down);
        } else {
          searchStrategies.add(SearchStrategy.Down);
          searchStrategies.add(SearchStrategy.Forward);
        }
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
          layout.walkways.put(tileGen.x, tileGen.y, tileGen.z, possibleWalkway);
        }
      }

      boolean isValid() {
        return secondConnectedGroup != null;
      }

      void gen(PList<ConnectedGroup> connectedGroups) {
        LasertagTileWall.Facing curFacing = startingNode.facing;
        secondConnectedGroup = pushForward(connectedGroups, curFacing, 0, startingNode.tileGen, 10, true);
        if (secondConnectedGroup == null) {
          secondConnectedGroup = pushForward(connectedGroups, curFacing, -1, startingNode.tileGen, 10, true);
        }
      }

      /**
       * Pushes the search forward, returning a connected group if one is connected by the search.
       * @return
       */
      private ConnectedGroup pushForward(PList<ConnectedGroup> connectedGroups, LasertagTileWall.Facing facing,
                                         int yChangePerForward, LasertagTileGen startTile, int maxSearchDist,
                                         boolean recurse) {
        if (startTile == null) {return null;}
        ConnectedGroup ret = null;
        PAssert.isFalse(yChangePerForward > 0, "Pushforward can only go down!");
        int curTilesLength = tileGens.size;
        int searchX = startTile.x;
        int searchY = startTile.y;
        int searchZ = startTile.z;
        int searchWalkwayYOffset = 0;
        LasertagRoomGen roomGen = startTile.roomGen;
        for (int searchDist = 0; searchDist < maxSearchDist; searchDist++) {
          searchX = startTile.x + facing.normalX() * searchDist;
          searchY = startTile.y + (int) Math.floor((searchDist + 1f) * yChangePerForward / rampTiles);
          searchZ = startTile.z + facing.normalZ() * searchDist;
          int endRampYI = PNumberUtils.mod((searchDist + 1) * yChangePerForward, rampTiles);
          int startRampYI = endRampYI - yChangePerForward;
          LasertagTileGen searchTileGen = roomGen.tileGens.get(searchX, searchY, searchZ);
          int couldGenWalkwayResult = couldGenWalkway(searchTileGen, connectedGroups);
          if (searchTileGen == null) {
            break;
          }
          if (yChangePerForward == 0 && (couldGenWalkwayResult & 1) == 0) {break;} // 1 means the result can be flat.
          if (yChangePerForward != 0 && (couldGenWalkwayResult & 2) == 0) {break;} // 2 means the result can be sloped.
          tileGens.add(searchTileGen);
          walkwayHeightOffsets.add(startRampYI);
          walkwayHeightOffsets.add(endRampYI);
          facings.add(facing);
          // Check to see if the end tile, facing forward, is a valid end position.
          if (endRampYI == 0 && (ret = checkIsEndLoc(searchX, searchY, searchZ, facing, connectedGroups)) != null) {
            return ret;
          }
          // Recurse based on the following: If going down, recurse when at an integer floor height. If going forward,
          // recurse, always, but stop the cycle.
          if (!recurse) {continue;}
          if (yChangePerForward < 0) {
            // Recurse forward if we are going down and we're at an integer height.
            if (endRampYI == 0) {
              if ((ret = pushForward(connectedGroups, facing, 0,
                                     roomGen.tileGens.get(searchX + facing.normalX(), searchY,
                                                          searchZ + facing.normalZ()), maxSearchDist, true)) != null) {
                return ret;
              }
            }
          } else {
            // Recurse down and sideways if we are going forward.
            if ((ret = pushForward(connectedGroups, facing, -1,
                                   roomGen.tileGens.get(searchX + facing.normalX(), searchY,
                                                        searchZ + facing.normalZ()), maxSearchDist, true)) != null) {
              return ret;
            }
            if ((ret = pushForward(connectedGroups, facing.leftCorner(), -1,
                                   roomGen.tileGens.get(searchX + facing.leftCorner().normalX(), searchY,
                                                        searchZ + facing.leftCorner().normalZ()), maxSearchDist,
                                   true)) != null) {return ret;}
            if ((ret = pushForward(connectedGroups, facing.leftCorner().opposite(), -1,
                                   roomGen.tileGens.get(searchX + facing.leftCorner().opposite().normalX(), searchY,
                                                        searchZ + facing.leftCorner().opposite().normalZ()),
                                   maxSearchDist, true)) != null) {return ret;}
          }
        }
        // Undo any changes if this is a dead end.
        while (tileGens.size > curTilesLength) {
          tileGens.removeLast();
        }
        while (facings.size > curTilesLength) {
          facings.removeLast();
        }
        if (tileGens.size == 0) {
          walkwayHeightOffsets.clear();
        }
        while (walkwayHeightOffsets.size > curTilesLength * 2) {
          walkwayHeightOffsets.removeLast();
        }
        return ret;
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
