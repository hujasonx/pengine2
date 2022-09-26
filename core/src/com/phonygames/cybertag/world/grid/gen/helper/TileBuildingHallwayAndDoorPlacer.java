package com.phonygames.cybertag.world.grid.gen.helper;

import android.support.annotation.Nullable;

import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileDoor;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.PSortableByScore;
import com.phonygames.pengine.util.collection.PArrayUtils;
import com.phonygames.pengine.util.collection.PFloatList;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;
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
    // TODO: build the door.
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
      PossibleDoorOrigin leftOrigin = null, rightOrigin = null, addedOrigin = null;
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

  /** Goes through all the walls and generates possible door origins. */
  private static PList<PossibleDoorOrigin> __initialPossibleDoors(TileBuilding building) {
    PList<PossibleDoorOrigin> ret = new PList<>();
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = building.tileGrid().obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        GridTile tile0 = e.val();
        TileRoom room0 = building.roomAtTilePosition(tile0.x, tile0.y, tile0.z);
        if (room0 == null) {continue;}
        for (int a = 0; a < PFacing.count(); a++) {
          GridTile.EmitOptions.Wall wall0 = tile0.emitOptions.walls[a];
          // Tile0's wall at this facing must support a door and not already have one.
          if (wall0.doorPlacementScore <= 0 || wall0.door != null || wall0.__possibleDoorOrigin != null) {continue;}
          GridTile tile1 = building.tileAtTilePosition(tile0.x + wall0.facing.forwardX(), tile0.y,
                                                       tile0.z + wall0.facing.forwardZ());
          if (tile1 == null) {continue;}
          TileRoom room1 = building.roomAtTilePosition(tile1.x, tile1.y, tile1.z);
          if (room1 == null || room1 == room0) {continue;}
          // The rooms on each side of this wall are different and exist at this point.
          GridTile.EmitOptions.Wall wall1 = tile1.emitOptions.walls[PFacing.get(a).opposite().intValue()];
          // Tile1's wall on the corresponding side to wall0 must be able to have a door and not have one.
          if (wall1.doorPlacementScore <= 0 || wall1.door != null || wall1.__possibleDoorOrigin != null) {continue;}
          // Add a possibleDoorOrigin.
          PossibleDoorOrigin added =
              PossibleDoorOrigin.builder().wall0(wall0).wall1(wall1).room0(room0).room1(room1).tileBuilding(building)
                                .build();
          wall0.__possibleDoorOrigin = added;
          wall1.__possibleDoorOrigin = added;
          added.recalcScore();
          ret.add(added);
        }
      }
    }
    return ret;
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

  /** Adds hallways and places doors to this building. Room generation should succeed this. */
  public static void addHallwaysAndPlaceDoors(TileBuilding building) {
    // First, place adjacent doors.
    for (int a = 0; a < building.rooms().size(); a++) {
      TileRoom room = building.rooms().get(a);
      room.parameters().emitPossibleDoorLocations(room);
    }
    PList<PossibleDoorOrigin> doorLocations = __initialPossibleDoors(building);
    __placeAdjacentDoors(building, doorLocations);
    // Now, generate hallways.
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
      if (wall0.doorPlacementScore == 0 || wall1.doorPlacementScore == 0 || wasEmitted) {
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
      // If it's maxRoomSeparationForPenalty (mrs) the penalty is maxPenalty / mrs.
      __score -= tileBuilding.parameters().doorScorePenaltyForAlreadyDirectlyConnectedRooms / roomDoorSeparation;
    }

    @Override public float score() {
      return __score;
    }
  }
}
