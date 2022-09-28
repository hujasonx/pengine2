package com.phonygames.cybertag.world.grid.gen.helper;

import android.support.annotation.Nullable;

import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileDoor;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.cybertag.world.grid.gen.TileRoomParameters;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.PSortableByScore;
import com.phonygames.pengine.util.collection.PArrayUtils;
import com.phonygames.pengine.util.collection.PFloatList;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PPooledIterable;

import lombok.Builder;

/** Helper class for adding hallways and doors to a tile building. */
public class TileBuildingHallwayAndDoorPlacer {
  public static final String TAG = "TileBuildingHallwayAndDoorPlacer";
  /** Temp variable for storing the possible door widths. */
  private static final PFloatList __tmpDoorWidthWidths = PFloatList.obtain();

  /** Makes the possible door origin a reality. */
  private static void __addDoor(TileBuilding building, PossibleDoorOrigin doorOrigin,
                                PList<PossibleDoorOrigin> doorLocations) {
    // Determine the width this door should be.
    int maxSize = Math.max(doorOrigin.room0.parameters().doorWidthWeights.length,
                           doorOrigin.room1.parameters().doorWidthWeights.length);
    __tmpDoorWidthWidths.clear();
    for (int a = 0; a < maxSize; a++) {
      __tmpDoorWidthWidths.set(a, doorOrigin.room0.parameters().doorWidthWeights[a] +
                                  doorOrigin.room1.parameters().doorWidthWeights[a]);
    }
    int resultingDoorWidth = PArrayUtils.randomIndexWithWeights(__tmpDoorWidthWidths) + 1;
    // Create the door.
    TileDoor.TileDoorBuilder doorBuilder = TileDoor.builder();
    doorBuilder.building(building);
    doorBuilder.genTaskTracker(building.genTaskTracker());
    TileDoor door = doorBuilder.build();
    door.rooms().add(doorOrigin.room0);
    door.rooms().add(doorOrigin.room1);
    door.walls().add(doorOrigin.wall0);
    door.walls().add(doorOrigin.wall1);
    doorOrigin.wall0.door = door;
    doorOrigin.wall1.door = door;
    int actualDoorWidth = 1, lookupLeftOffset = 1, lookupRightOffset = 1;
    while (actualDoorWidth < resultingDoorWidth) {
      // Attempt to find neighboring tiles that can be doors, up until the resultingDoorWidth or there are no free wall
      // tiles that can be made into doors.
      PossibleDoorOrigin leftOrigin = null, rightOrigin = null, addedOrigin;
      GridTile leftGridTile = building.tileAtTilePosition(
          doorOrigin.wall0.owner.x + doorOrigin.wall0.facing.left().forwardX() * lookupLeftOffset,
          doorOrigin.wall0.owner.y,
          doorOrigin.wall0.owner.z + doorOrigin.wall0.facing.left().forwardZ() * lookupLeftOffset);
      GridTile rightGridTile = building.tileAtTilePosition(
          doorOrigin.wall0.owner.x + doorOrigin.wall0.facing.right().forwardX() * lookupRightOffset,
          doorOrigin.wall0.owner.y,
          doorOrigin.wall0.owner.z + doorOrigin.wall0.facing.right().forwardZ() * lookupRightOffset);
      if (leftGridTile != null) {
        leftOrigin = leftGridTile.emitOptions.walls[doorOrigin.wall0.facing.intValue()].__possibleDoorOrigin;
      }
      if (rightGridTile != null) {
        rightOrigin = rightGridTile.emitOptions.walls[doorOrigin.wall0.facing.intValue()].__possibleDoorOrigin;
      }
      // Figure out which origin can be emitted, if any.
      if (leftOrigin != null) {
        if (rightOrigin != null) {
          addedOrigin = leftOrigin.score() < rightOrigin.score() ? rightOrigin : leftOrigin;
        } else {
          addedOrigin = leftOrigin;
        }
      } else {
        addedOrigin = rightOrigin;
      }
      if (addedOrigin == null || addedOrigin.score() <= 0) {
        // No more adjacent valid door locations.
        break;
      }
      // Add the origin to the door.
      door.walls().add(addedOrigin.wall0);
      addedOrigin.wall0.door = door;
      door.walls().add(addedOrigin.wall1);
      addedOrigin.wall1.door = door;
      // Shift the offsets.
      if (addedOrigin == leftOrigin) {
        lookupLeftOffset++;
      } else {
        lookupRightOffset++;
      }
      actualDoorWidth++;
    }
    PLog.i(TAG + "| added door of size " + actualDoorWidth + " with origin " + doorOrigin.wall0.owner.x + ", " +
           doorOrigin.wall0.owner.y + ", " + doorOrigin.wall0.owner.z);
    // Adds the directly connected rooms, if necessary.
    building.doors().add(door);
    doorOrigin.room0.doors().add(door);
    doorOrigin.room1.doors().add(door);
    if (!doorOrigin.room0.directlyConnectedRooms().has(doorOrigin.room1, true)) {
      doorOrigin.room0.directlyConnectedRooms().add(doorOrigin.room1);
    }
    if (!doorOrigin.room1.directlyConnectedRooms().has(doorOrigin.room0, true)) {
      doorOrigin.room1.directlyConnectedRooms().add(doorOrigin.room0);
    }
    // Recalculate door separations.
    __recalcRoomDoorSeparations(doorOrigin.room0, null, 0);
    __recalcRoomDoorSeparations(doorOrigin.room1, null, 0);
    // Finally, recalculate door origin scores.
    for (int a = 0; a < doorLocations.size(); a++) {
      PossibleDoorOrigin possibleDoorOrigin = doorLocations.get(a);
      possibleDoorOrigin.recalcScore();
    }
  }

  /**
   * Goes through all the walls and generates possible door origins. Adds to the given list. Will not regenerate if
   * origin already exists.
   */
  private static PList<PossibleDoorOrigin> __initialPossibleDoors(@Nullable PList<PossibleDoorOrigin> out,
                                                                  TileBuilding building) {
    if (out == null) {out = new PList<>();}
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = building.tileGrid().obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        GridTile tile0 = e.val();
        TileRoom room0 = building.roomAtTilePosition(tile0.x, tile0.y, tile0.z);
        if (room0 == null) {continue;}
        for (int a = 0; a < PFacing.count(); a++) {
          GridTile.EmitOptions.Wall wall0 = tile0.emitOptions.walls[a];
          // Skip if the existing origin if it exists. This origin will be the shared for both wall1 and wall0.
          if (wall0.__possibleDoorOrigin != null) {
            continue;
          }
          // Tile0's wall at this facing must support a door and not already have one.
          if (wall0.doorPlacementScore <= 0 || wall0.door != null) {continue;}
          GridTile tile1 = building.tileAtTilePosition(tile0.x + wall0.facing.forwardX(), tile0.y,
                                                       tile0.z + wall0.facing.forwardZ());
          if (tile1 == null) {continue;}
          TileRoom room1 = building.roomAtTilePosition(tile1.x, tile1.y, tile1.z);
          if (room1 == null || room1 == room0) {continue;}
          // The rooms on each side of this wall are different and exist at this point.
          GridTile.EmitOptions.Wall wall1 = tile1.emitOptions.walls[PFacing.get(a).opposite().intValue()];
          // Tile1's wall on the corresponding side to wall0 must be able to have a door and not have one.
          if (wall1.doorPlacementScore <= 0 || wall1.door != null) {continue;}
          // Add a possibleDoorOrigin.
          PossibleDoorOrigin added =
              PossibleDoorOrigin.builder().wall0(wall0).wall1(wall1).room0(room0).room1(room1).tileBuilding(building)
                                .build();
          wall0.__possibleDoorOrigin = added;
          wall1.__possibleDoorOrigin = added;
          added.recalcScore();
          out.add(added);
        }
      }
    }
    return out;
  }

  /** Places doors that connect adjacent rooms. This should be run before hallways are generated. */
  private static void __placeAdjacentDoors(TileBuilding building, PList<PossibleDoorOrigin> doorLocations) {
    // Limit ourselves to 5000 doors, which should be plenty.
    for (int attempt = 0; attempt < 5000; attempt++) {
      doorLocations.sort();
      if (doorLocations.peek().score() <= 0) {
        // Even the best scoring door shouldn't be emitted, so stop emitting.
        break;
      }
      PossibleDoorOrigin bestOrigin = doorLocations.peek();
      __addDoor(building, bestOrigin, doorLocations);
    }
  }

  /**
   * Recalculates the room door separations. if checkRoom is null, starts the recursion, ignoring curRoomSeparation.
   * return true if anything was changed.
   */
  private static boolean __recalcRoomDoorSeparations(TileRoom starterRoom, @Nullable TileRoom checkRoom,
                                                     int curRoomSeparation) {
    boolean modified = false;
    if (checkRoom == null) {
      for (int a = 0; a < starterRoom.directlyConnectedRooms().size(); a++) {
        TileRoom otherRoom = starterRoom.directlyConnectedRooms().get(a);
        boolean added = __recalcRoomDoorSeparations(starterRoom, otherRoom, 1);
        modified |= added;
      }
      return modified;
    }
    // We are recursing. First, update the room separation with curRoomSeparation.
    int prevStoreSepInStarter = starterRoom.doorsBetween().genPooled(checkRoom).valueOf();
    int prevStoreSepInCheck = checkRoom.doorsBetween().genPooled(starterRoom).valueOf();
    if (prevStoreSepInStarter > curRoomSeparation) {
      starterRoom.doorsBetween().genPooled(checkRoom).set(curRoomSeparation);
      modified = true;
    }
    if (prevStoreSepInCheck > curRoomSeparation) {
      checkRoom.doorsBetween().genPooled(starterRoom).set(curRoomSeparation);
      modified = true;
    }
    if (!modified) {
      // If we didn't modify anything, then theres no need to propagate anything either, so return early.
      return false;
    }
    // Recurse.
    for (int a = 0; a < checkRoom.directlyConnectedRooms().size(); a++) {
      TileRoom otherRoom = checkRoom.directlyConnectedRooms().get(a);
      __recalcRoomDoorSeparations(starterRoom, otherRoom, 1 + curRoomSeparation);
    }
    return true;
  }

  /**
   * Adds hallways and places doors to this building. Room generation should succeed this. Any added rooms are added to
   * roomsStillGenerating.
   */
  public static void addHallwaysAndPlaceDoors(TileBuilding building, PList<TileRoom> roomsStillGenerating) {
    // First, place adjacent doors.
    for (int a = 0; a < building.rooms().size(); a++) {
      TileRoom room = building.rooms().get(a);
      room.parameters().emitPossibleDoorLocations(room);
    }
    PList<PossibleDoorOrigin> doorLocations = __initialPossibleDoors(null, building);
    __placeAdjacentDoors(building, doorLocations);
    // Now, generate hallways.
    HallwayGenerator hallwayGenerator = new HallwayGenerator(building);
    roomsStillGenerating.addAll(hallwayGenerator.genHallwayRooms());
  }

  /** Helper class for adding hallways. */
  @Builder private static class HallwayGenerator {
    /** The building this hallway generator is for. */
    private final TileBuilding building;
    /** The hallway rooms that are being processed. */
    private final PList<HallwayRoomToEmit> hallwayRooms = new PList<>();
    /** A map of tiles to the hallway rooms their are affected by. */
    private final PMap<GridTile, HallwayRoomToEmit> hallwayRoomsToEmit = new PMap<>();
    /**
     * The room door separations. This will be continuously updated as hallways segments are emitted.
     */
    private final PMap<Room, PMap<Room, PInt>> updatedRoomDoorSeparations = new PMap<>();

    /** Generates hallway rooms and adds them to the building, returning them in a list. */
    public PList<TileRoom> genHallwayRooms() {
      __initialize();
      hallwayRooms.clear();
      // Generate hallway rooms to edit.
      for (int attempt = 0; attempt < 20; attempt++) {
        HallwayRoomToEmit addedHallwayRoom = __genHallwaySegment(building);
        if (addedHallwayRoom == null) {
          break;
        }
        hallwayRooms.addIfNotPresent(addedHallwayRoom, true);
      }
      // Generate actual rooms for the hallway rooms.
      PList<TileRoom> ret = new PList<>();
      for (int a = 0; a < hallwayRooms.size(); a++) {
        ret.add(hallwayRooms.get(a).emitRoom());
      }
      return ret;
    }

    /** Call this after building. This initializes the updatedRoomDoorSeparations map. */
    public void __initialize() {
      for (int a = 0; a < building.rooms().size(); a++) {
        TileRoom tileRoom = building.rooms().get(a);
        Room room = new Room(this, null, tileRoom);
        updatedRoomDoorSeparations.genUnpooled(room);
      }
    }

    /** Attempts to add a hallway segment to the building, returning the hallway room it added (or extended) or null. */
    private @Nullable HallwayRoomToEmit __genHallwaySegment(TileBuilding building) {
      // First, find the possible hallway start points. These should be walls of existing tiles.
      PList<GridTile.EmitOptions.Wall> validDoorLocations = new PList<>();
      // Add all valid doors in non-hallway rooms.
      for (int a = 0; a < building.rooms().size(); a++) {
        TileRoom nonHallwayRoom = building.rooms().get(a);
        try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = nonHallwayRoom.tileGrid()
                                                                                             .obtainIterator()) {
          while (it.hasNext()) {
            PIntMap3d.Entry<GridTile> e = it.next();
            GridTile tile = e.val();
            for (int b = 0; b < PFacing.count(); b++) {
              GridTile.EmitOptions.Wall wall = tile.emitOptions.walls[b];
              if (wall.doorPlacementScore > 0) {
                validDoorLocations.addIfNotPresent(wall, true);
              }
            }
          }
        }
      }
      // Add all valid connection points in hallway rooms.
      for (int a = 0; a < hallwayRooms.size(); a++) {
        validDoorLocations.addAllIfNotPresent(hallwayRooms.get(a).possibleConnectionPointWalls, true);
      }
      // Loop through the possible door locations and search.
      PList<HallwayRoomToEmit.PossibleHallwaySegment> possibleSegments = new PList<>();
      for (int a = 0; a < validDoorLocations.size(); a++) {
        HallwayRoomToEmit.HallwaySegmentSearcher.searchFrom(this, validDoorLocations.get(a), possibleSegments);
      }
      // Now, generate the best segment.
      possibleSegments.sort();
      if (possibleSegments.size() == 0 || possibleSegments.peek().score() <= 0) {
        return null;
      }
      HallwayRoomToEmit.PossibleHallwaySegment bestSegment = possibleSegments.peek();
      HallwayRoomToEmit emittedRoom = null;
      if (bestSegment.startRoom.hallwayRoomToEmit != null) {
        // Combine the best segment into the start room hallway.
        emittedRoom = bestSegment.startRoom.hallwayRoomToEmit;
      } else if (bestSegment.endRoom.hallwayRoomToEmit != null) {
        // Combine the best segment into the end room hallway.
        emittedRoom = bestSegment.endRoom.hallwayRoomToEmit;
      } else {
        // Create a new hallway room.
        HallwayRoomToEmit.HallwayRoomToEmitBuilder hallBuilder = HallwayRoomToEmit.builder();
        hallBuilder.building(building);
        hallBuilder.hallwayGenerator(this);
        hallBuilder.hallwayParameters(building.parameters().getHallwayParameters());
        emittedRoom = hallBuilder.build();
        hallwayRooms.add(emittedRoom);
      }
      if (emittedRoom != null) {
        emittedRoom.addHallwaySegment(bestSegment);
      }
      return emittedRoom;
    }

    /** A hallway room that is still being generated. */
    @Builder private static class HallwayRoomToEmit {
      /**
       * Temp list of walls that should be considered to connect to adjacent tiles. This is set every time a new tile is
       * added to this hallway. The owner tiles should be the ones inside this hallway room.
       */
      private final PList<GridTile.EmitOptions.Wall> __potentialWallsThatCouldBeConnectedToForLastAddedTile =
          new PList<>();
      /** All affected tiles, including upper tiles for slopes. */
      private final PList<GridTile> affectedTiles = new PList<>();
      /** The building this is for. */
      private final TileBuilding building;
      /** The non-hallway rooms that this hallway connects to. */
      private final PList<TileRoom> connectedNonHallwayRooms = new PList<>();
      /** The owning hallway generator. */
      private final HallwayGenerator hallwayGenerator;
      /** The parameters that will be used for this hallway room. */
      private final TileRoomParameters.HallwayParameters hallwayParameters;
      /** The list of interior walls for this room. */
      private final PList<GridTile.EmitOptions.Wall> internalWalls = new PList<>();
      /** The possible connection point walls. These can be used to connect with hallway segments */
      private final PList<GridTile.EmitOptions.Wall> possibleConnectionPointWalls = new PList<>();
      /**
       * A map of traversed tiles -> The corner offsets in grid space as vec4s. If the hallway is vertically shifted,
       * this will only include the lowest tile in a vertical column.
       */
      private final PMap<GridTile, PVec4> traversedTileCornerOffsets = new PMap(PVec4.getStaticPool());
      /**
       * A map of traversed tiles -> Facing. This is used to place walls within the hallway room..
       */
      private final PMap<GridTile, PFacing> traversedTileFacings = new PMap(PVec4.getStaticPool());
      /**
       * The walls that could have doors generated connecting each room. All rooms in this list should have a door
       * generated! The walls should be owned by the tiles in the rooms.
       */
      private final PMap<TileRoom, PList<GridTile.EmitOptions.Wall>> wallsThatCanHaveDoorsForRoom = new PMap<>();

      /**
       * Adds the hallway segment, updates the wallsThatCanHaveDoorsForRoom map, and updates possible connection point
       * walls.
       */
      private HallwayRoomToEmit addHallwaySegment(PossibleHallwaySegment segment) {
        PAssert.isFalse(segment.length() == 0);
        int prevTileY = segment.traversedTiles.peek().y;
        for (int a = 0; a < segment.length(); a++) {
          GridTile traversedTile = segment.traversedTiles.get(a);
          float vOffset0 = segment.verticalOffsets.get(a + 0);
          float vOffset1 = segment.verticalOffsets.get(a + 1);
          // If this is a downward slope going into a new y level, the starting y offset should be 1.
          if (traversedTile.y < prevTileY) {
            vOffset0 += 1;
            PAssert.isTrue(vOffset0 == 1);
          }
          PFacing facing = segment.facings.get(a);
          float v00 = (facing == PFacing.X || facing == PFacing.Z) ? vOffset0 : vOffset1;
          float v01 = (facing == PFacing.X || facing == PFacing.mZ) ? vOffset0 : vOffset1;
          float v11 = (facing == PFacing.mX || facing == PFacing.mZ) ? vOffset0 : vOffset1;
          float v10 = (facing == PFacing.mX || facing == PFacing.Z) ? vOffset0 : vOffset1;
          __addHallwayTile(traversedTile, v00, v10, v11, v01, facing);
          // If the segment has a start non-hallway room, track the possible door walls for it.
          if (a == 0 && segment.startRoom.tileRoom != null) {
            PAssert.isFalse(wallsThatCanHaveDoorsForRoom.has(segment.startRoom.tileRoom),
                            "Attempting to connect a room that was already connected to this hallway!");
            PList<GridTile.EmitOptions.Wall> wallsThatCanActuallyHaveDoorsForStartRoom = new PList<>();
            for (int b = 0; b < __potentialWallsThatCouldBeConnectedToForLastAddedTile.size(); b++) {
              GridTile.EmitOptions.Wall potentialWallDoorToStartRoom =
                  __potentialWallsThatCouldBeConnectedToForLastAddedTile.get(b);
              GridTile otherTile = potentialWallDoorToStartRoom.tileOnOtherSideIn(building.tileGrid());
              if (otherTile != null &&
                  segment.startRoom.tileRoom == building.roomAtTilePosition(otherTile.x, otherTile.y, otherTile.z)) {
                // Track the wall for tile in the start room.
                wallsThatCanActuallyHaveDoorsForStartRoom.add(
                    otherTile.emitOptions.walls[potentialWallDoorToStartRoom.facing.opposite().intValue()]);
              }
            }
            wallsThatCanHaveDoorsForRoom.put(segment.endRoom.tileRoom, wallsThatCanActuallyHaveDoorsForStartRoom);
          } else if (a == segment.length() - 1 && segment.endRoom.tileRoom != null) {
            // If the segment has an end non-hallway room, track the possible door walls for it.
            PAssert.isFalse(wallsThatCanHaveDoorsForRoom.has(segment.endRoom.tileRoom),
                            "Attempting to connect a room that was already connected to this hallway!");
            PList<GridTile.EmitOptions.Wall> wallsThatCanActuallyHaveDoorsForEndRoom = new PList<>();
            for (int b = 0; b < __potentialWallsThatCouldBeConnectedToForLastAddedTile.size(); b++) {
              GridTile.EmitOptions.Wall potentialWallDoorToStartRoom =
                  __potentialWallsThatCouldBeConnectedToForLastAddedTile.get(b);
              GridTile otherTile = potentialWallDoorToStartRoom.tileOnOtherSideIn(building.tileGrid());
              if (otherTile != null &&
                  segment.endRoom.tileRoom == building.roomAtTilePosition(otherTile.x, otherTile.y, otherTile.z)) {
                // Track the wall for tile in the end room.
                wallsThatCanActuallyHaveDoorsForEndRoom.add(
                    otherTile.emitOptions.walls[potentialWallDoorToStartRoom.facing.opposite().intValue()]);
              }
            }
            wallsThatCanHaveDoorsForRoom.put(segment.endRoom.tileRoom, wallsThatCanActuallyHaveDoorsForEndRoom);
          }
          prevTileY = traversedTile.y;
        }
        return this;
      }

      /**
       * Adds the tile to the hallway room. Updates connection points and internal walls.
       */
      private HallwayRoomToEmit __addHallwayTile(GridTile tile, float v00, float v10, float v11, float v01,
                                                 PFacing facing) {
        // Emit only to one tile if this hallway tile is flat and at the bottom.
        boolean onlyOneTile = v00 == 0 && v10 == 0 && v11 == 0 && v01 == 0;
        GridTile tileAbove = onlyOneTile ? null : building.tileAtTilePosition(tile.x, tile.y + 1, tile.z);
        if (!onlyOneTile && tileAbove == null) {
          PAssert.fail("Unable to emit hallway tile (maybe you are at the top of the building?");
        }
        // Track the tile.
        PAssert.isFalse(affectedTiles.has(tile, true), "Tile was already being used!");
        traversedTileCornerOffsets.genPooled(tile).set(v00, v10, v11, v01);
        traversedTileFacings.put(tile, facing);
        affectedTiles.add(tile);
        hallwayGenerator.hallwayRoomsToEmit.put(tile, this);
        if (tileAbove != null) {
          PAssert.isFalse(affectedTiles.has(tileAbove, true), "Tile above was already being used!");
          affectedTiles.add(tileAbove);
          hallwayGenerator.hallwayRoomsToEmit.put(tileAbove, this);
        }
        // Add new possible connection points.
        __potentialWallsThatCouldBeConnectedToForLastAddedTile.clear();
        if (v00 == 0) {
          if (v10 == 0) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(tile.emitOptions.walls[PFacing.mZ.intValue()]);
          }
          if (v01 == 0) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(tile.emitOptions.walls[PFacing.mX.intValue()]);
          }
        } else if (v00 == 1) {
          PAssert.isNotNull(tileAbove);
          if (v10 == 1) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(
                tileAbove.emitOptions.walls[PFacing.mZ.intValue()]);
          }
          if (v01 == 1) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(
                tileAbove.emitOptions.walls[PFacing.mX.intValue()]);
          }
        }
        if (v11 == 0) {
          if (v10 == 0) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(tile.emitOptions.walls[PFacing.X.intValue()]);
          }
          if (v01 == 0) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(tile.emitOptions.walls[PFacing.Z.intValue()]);
          }
        } else if (v11 == 1) {
          PAssert.isNotNull(tileAbove);
          if (v10 == 1) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(
                tileAbove.emitOptions.walls[PFacing.X.intValue()]);
          }
          if (v01 == 1) {
            __potentialWallsThatCouldBeConnectedToForLastAddedTile.add(
                tileAbove.emitOptions.walls[PFacing.Z.intValue()]);
          }
        }
        possibleConnectionPointWalls.addAll(__potentialWallsThatCouldBeConnectedToForLastAddedTile);
        // Adds internal walls. This allows multiple segments to travel next to each other without opening walls.
        for (int a = 0; a < PFacing.count(); a++) {
          GridTile.EmitOptions.Wall wall = tile.emitOptions.walls[a];
          if (wall.facing == facing) {
            // However, if the facing is the facing that this tile is, then instead remove internal walls from the tile
            // that this connects from.
            GridTile.EmitOptions.Wall otherWall = wall.wallOnOtherSideIn(building.tileGrid());
            PAssert.isNotNull(otherWall);
            internalWalls.removeValue(otherWall, true);
            if (tileAbove != null) {
              GridTile.EmitOptions.Wall otherWallAbove =
                  tileAbove.emitOptions.walls[a].wallOnOtherSideIn(building.tileGrid());
              if (otherWallAbove != null) {
                internalWalls.removeValue(otherWallAbove, true);
              }
            }
          } else {
            // Add internal walls on all other sides.
            internalWalls.addIfNotPresent(wall, true);
            if (tileAbove != null) {
              internalWalls.addIfNotPresent(tileAbove.emitOptions.walls[a], true);
            }
          }
        }
        return this;
      }

      /**
       * Converts this to an actual room and emits doors associated with it. Sets the tile parameters for this room.
       * Finally, adds the room to the building.
       */
      public TileRoom emitRoom() {
        TileRoom room = TileRoom.builder().parameters(hallwayParameters).genTaskTracker(building.genTaskTracker())
                                .building(building).build();
        room.genTaskTracker().addBlocker(room);
        building.rooms().add(room);
        for (int a = 0; a < affectedTiles.size(); a++) {
          GridTile tile = affectedTiles.get(a);
          room.tileGrid().trackTile(tile);
          // Continue if this is not a traversed tile. (It's above one though)
          if (!traversedTileCornerOffsets.has(tile)) {
            continue;
          }
          // Emit the walkway.
          PVec4 cornerOffsets = traversedTileCornerOffsets.get(tile);
          cornerOffsets.emit(tile.emitOptions.walkwayCornerVerticalOffsets);
          tile.emitOptions.walkwayModelTemplateID = hallwayParameters.walkwayTemplateForTraversedTile(room, tile);
        }
        // TODO: emit doors.
        // Emit internal walls.
        for (int a = 0; a < internalWalls.size(); a++) {
          GridTile.EmitOptions.Wall wall = internalWalls.get(a);
          hallwayParameters.addWallTemplatesFor(room, wall.owner, wall);
        }
        return room;
      }

      /** Helper class to search hallway segments. */
      private static class HallwaySegmentSearcher {
        /** Searches possible segments starting from the given wall and adds them to the list. */
        public static void searchFrom(HallwayGenerator hallwayGenerator, GridTile.EmitOptions.Wall wall,
                                      PList<PossibleHallwaySegment> possibleSegments) {
          TileRoom nonHallwayStartRoom =
              hallwayGenerator.building.roomAtTilePosition(wall.owner.x, wall.owner.y, wall.owner.z);
          HallwayRoomToEmit hallwayStartRoom = hallwayGenerator.hallwayRoomsToEmit.get(wall.owner);
          Room startRoom = new Room(hallwayGenerator, hallwayStartRoom, nonHallwayStartRoom);
          int maxSearchDepth = hallwayStartRoom.building.parameters().hallwayLengthWeights.length;
          int maxHorizontalTurns = hallwayStartRoom.building.parameters().hallwayTurnWeights.length;
          PList<GridTile> tileBuffer = new PList<>();
          PList<PFacing> facingBuffer = new PList<>();
          PFloatList heightOffsetBuffer = PFloatList.obtain();
          // Try searching on the same level.
          heightOffsetBuffer.add(0);
          __searchRecursive(hallwayGenerator, wall, startRoom, tileBuffer, facingBuffer, heightOffsetBuffer,
                            possibleSegments, maxSearchDepth, maxHorizontalTurns,
                            wall.tileOnOtherSideIn(hallwayGenerator.building.tileGrid()), 0, wall.facing);
          // Try searching downwards.
          heightOffsetBuffer.clear();
          heightOffsetBuffer.add(1);
          GridTile tileBelowOnOtherSide = hallwayGenerator.building.tileGrid()
                                                                   .getTileAt(wall.owner.x + wall.facing.forwardX(),
                                                                              wall.owner.y - 1,
                                                                              wall.owner.z + wall.facing.forwardZ());
          __searchRecursive(hallwayGenerator, wall, startRoom, tileBuffer, facingBuffer, heightOffsetBuffer,
                            possibleSegments, maxSearchDepth, maxHorizontalTurns, tileBelowOnOtherSide,
                            1f - 1f / hallwayGenerator.building.parameters().hallwayTilesPerSlopeUp, wall.facing);
        }


        /** Recursively searches for possible hallway segments. */
        private static void __searchRecursive(HallwayGenerator hallwayGenerator, GridTile.EmitOptions.Wall startWall,
                                              Room startRoom, PList<GridTile> tileBuffer, PList<PFacing> facingBuffer,
                                              PFloatList heightOffsetBuffer,
                                              PList<PossibleHallwaySegment> possibleSegments, int searchDepthRemaining,
                                              int horizontalTurnsRemaining, GridTile tileToTry, float heightOffsetToTry,
                                              PFacing facingToTry) {
          searchDepthRemaining--;
          if (searchDepthRemaining == 0) {
            return;
          }
          if ( tileToTry == null) {
            return;
          }
          // Whether or not the tile piece is downwards sloping.
          int gridSpaceStartY = (tileBuffer.isEmpty() ? startWall.owner.y : tileBuffer.peek().y)
          boolean wentDown = (tileBuffer.isEmpty() ? tileToTry.y < startWall.owner.y : tileToTry.y < tileBuffer.peek().y);
          if (!wentDown) {
            // We still have to check that the height offsets didnt decrease.
          }
          // Add the tile to the buffer. */
          tileBuffer.add(tileToTry);
          facingBuffer.add(facingToTry);
          heightOffsetBuffer.add(heightOffsetToTry);



          // Check to see if this is a valid segment.
          for (int a = 0; a < PFacing.count(); a++) {
            if ()

          }
          // Undo adding the tile to the buffer.
          tileBuffer.removeLast();
          facingBuffer.removeLast();
          heightOffsetBuffer.del(heightOffsetBuffer.size() - 1);
        }
      }

      /**
       * A sequence of tiles and walkway offsets that represent a possible hallway segment. Every time a segment is
       * emitted, these become obsolete.
       */
      @Builder private static class PossibleHallwaySegment implements PSortableByScore<PossibleHallwaySegment> {
        /** The building that this will be added to. */
        private final TileBuilding building;
        /** The ending room. */
        private final Room endRoom;
        /** The ending wall, on an existing tile in the building. */
        private final GridTile.EmitOptions.Wall endWall;
        /**
         * The facings that the tiles were generated from.
         */
        private final PList<PFacing> facings = new PList<>();
        /** The hallway generator that owns this. */
        private final HallwayGenerator hallwayGenerator;
        /** The possible connection point walls. */
        private final PList<GridTile.EmitOptions.Wall> possibleConnectionPointWalls = new PList<>();
        /** The start room. */
        private final Room startRoom;
        /** The starting wall, on an existing tile in the building, or in a HallwayToEmit. */
        private final GridTile.EmitOptions.Wall startWall;
        /**
         * The tiles that this hallway traverses. If the hallway is vertically shifted, this will only include the
         * lowest tile in a vertical column.
         */
        private final PList<GridTile> traversedTiles = new PList<>();
        /**
         * The walkway vertical offsets of this segment. The length of this list will be 1 + length(); the last value
         * should always be 0.
         */
        private final PFloatList verticalOffsets = PFloatList.obtain();

        /** If the score is zero, don't emit this hallway. */
        @Override public float score() {
          if (length() > building.parameters().hallwayLengthWeights.length) {
            PAssert.warn("Hallway too long");
            return 0;
          }
          float lengthScore = building.parameters().hallwayLengthWeights[length()];
          if (lengthScore <= 0) {
            // Invalid length.
            return 0;
          }
          // Count how many times this segment turns.
          int turns = 0;
          {
            PFacing curFacing = facings.get(0);
            for (int a = 1; a < facings.size(); a++) {
              if (facings.get(a) != curFacing) {
                turns++;
                curFacing = facings.get(a);
              }
            }
          }
          if (turns > building.parameters().hallwayTurnWeights.length) {
            PAssert.warn("Hallway has too many turns: " + turns);
            return 0;
          }
          float turnsScore = building.parameters().hallwayTurnWeights[turns];
          if (turnsScore == 0) {
            // Invalid number of turns.
            return 0;
          }
          return lengthScore + turnsScore; // TODO: other score modifiers.
          //          float penaltyFromRoomConnections = 0;
          //          // Figure out how many connected doors adding this segment would cut out for all rooms in the
          //          building.
          //          if (startRoom == null && endRoom == null) {
          //            PAssert.isNotNull(startHallwayToEdit);
          //            PAssert.isNotNull(endHallwayToEdit);
          //          } else if (startRoom == null) {
          //            PAssert.isNotNull(startHallwayToEdit);
          //          } else if (endRoom == null) {
          //            PAssert.isNotNull(endHallwayToEdit);
          //          } else {
          //            // A hallway segment that directly connects two actual rooms.
          //            int doorsBetweenRooms = startRoom.doorsBetween().genUnpooled(endRoom).valueOf();
          //            if (doorsBetweenRooms != 0 && doorsBetweenRooms > building.parameters()
          //            .minConnectivityForHallways) {
          //              // This hallway segment isnt valid.
          //              return 0;
          //            }
          //            penaltyFromRoomConnections =
          //          }
          //          return lengthScore - penaltyFromRoomConnections;
        }

        /** The length of the possible hallway. */
        public int length() {
          return traversedTiles.size();
        }
      }
    }

    /** Class that stores either a tileroom or a hallwayRoomToEmit. */
    @Builder private static class Room {
      /** The owning hallway generator. */
      private final HallwayGenerator hallwayGenerator;
      /** The hallwayRoomToEmit. If this is set, tileRoom must be null. */
      private final @Nullable
      HallwayRoomToEmit hallwayRoomToEmit;
      /** The tileRoom. If this is set, hallwayRoomToEmit must be null. */
      private final @Nullable
      TileRoom tileRoom;

      public boolean equals(Object o) {
        if (o instanceof Room) {
          return is((Room) o);
        }
        return false;
      }

      /** Return true if this room is equal to the given Room. */
      public boolean is(Room room) {
        return room.hallwayGenerator == hallwayGenerator && room.tileRoom == tileRoom &&
               room.hallwayRoomToEmit == hallwayRoomToEmit;
      }

      /** Return true if this room is equal to the given hallway room to emit. */
      public boolean is(HallwayRoomToEmit room) {
        return hallwayRoomToEmit == room;
      }

      /** Return true if this room is equal to the given tileRoom. */
      public boolean is(TileRoom room) {
        return tileRoom == room;
      }
    }
  }

  /** Class that stores a possible door origin. (a wall, with both sides.) */
  @Builder public static class PossibleDoorOrigin implements PSortableByScore<PossibleDoorOrigin> {
    /** The room that wall0 is in. */
    private final TileRoom room0;
    /** The room that wall1 is in. */
    private final TileRoom room1;
    /** The TileBuilding that this door would belong to. */
    private final TileBuilding tileBuilding;
    /** One of the walls that this door origin would occupy. */
    private final GridTile.EmitOptions.Wall wall0;
    /** The other one of the walls that this door origin would occupy. */
    private final GridTile.EmitOptions.Wall wall1;
    /** The score of this door origin. If it is positive, this door should be emitted. */
    private float __score;
    /** Whether or not a door was emitted at this location. */
    private boolean wasEmitted;
    /** Whether or not a door was emitted at this location as the origin. */
    private boolean wasEmittedAsOrigin;

    /** Recalculates the score of this door. */
    private void recalcScore() {
      if (wall0.doorPlacementScore == 0 || wall1.doorPlacementScore == 0 || wasEmitted ||
          room1.directlyConnectedRooms().has(room0, true)) {
        __score = 0;
        return;
      }
      __score = wall0.doorPlacementScore + wall1.doorPlacementScore;
      int roomDoorSeparation = room0.doorsBetween().genPooled(room1).valueOf();
      if (roomDoorSeparation == 0 ||
          roomDoorSeparation > tileBuilding.parameters().maxRoomDoorSeparationForPenalty) { // The rooms are
        // not connected to each other at all, or are too far apart.
        return;
      }
      // Penalize scores based on how many doors are between the two rooms. If it's 1 door, use the max penalty.
      // If it's maxRoomSeparationForPenalty (mrs) the penalty is maxPenalty / mrs. However, 1 will never occur since
      // directly connected rooms can't have multiple doors connecting them.
      __score -= tileBuilding.parameters().doorScorePenaltyForAlreadyDirectlyConnectedRooms / roomDoorSeparation;
    }

    @Override public float score() {
      return __score;
    }
  }
}
