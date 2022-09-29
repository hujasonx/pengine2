package com.phonygames.cybertag.world.grid.gen.helper;

import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileDoor;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.PSortableByScore;
import com.phonygames.pengine.util.collection.PArrayUtils;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PPooledIterable;

import lombok.Builder;

/** Class representing a possible door location in a tile room(s). */
@Builder public class TileRoomPossibleDoor implements PSortableByScore<TileRoomPossibleDoor> {
  /** The cached score. */
  private final PVec1 cachedScore = PVec1.obtain();
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

  /**
   * Finds the possible door locations.
   */
  public static PList<TileRoomPossibleDoor> findPossibleDoorLocations(TileBuilding building) {
    PList<TileRoomPossibleDoor> doors = new PList<>();
    // Loop through all walls and facings in the grid and emit doors when two rooms are adjacent through the wall.
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = building.tileGrid().obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        GridTile tile0 = e.val();
        TileRoom room0 = building.roomAtTilePosition(tile0.x, tile0.y, tile0.z);
        if (room0 == null) {continue;}
        for (int a = 0; a < PFacing.count(); a++) {
          GridTile.EmitOptions.Wall wall0 = tile0.emitOptions.walls[a];
          if (wall0.possibleDoor != null) {
            doors.addIfNotPresent(wall0.possibleDoor, true);
            continue;
          }
          GridTile.EmitOptions.Wall wall1 = wall0.wallOnOtherSideIn(building.tileGrid());
          if (wall1 == null) {continue;}
          PAssert.isNull(wall1.possibleDoor, "Why does one wall have a possible when its neighbor doesnt?");
          GridTile tile1 = wall1.owner;
          TileRoom room1 = building.roomAtTilePosition(tile1.x, tile1.y, tile1.z);
          if (room1 == null || room0 == room1) {continue;}
          // Create a possible door location.
          TileRoomPossibleDoor door = new TileRoomPossibleDoor(room0, room1, building, wall0, wall1);
          wall0.possibleDoor = door;
          wall1.possibleDoor = door;
          doors.add(door);
        }
      }
    }
    return doors;
  }

  /** Actually converts this possible door to an actual door. */
  public void emitToRooms() {
    // Create a list of possible door locations to add to the door.
    PList<TileRoomPossibleDoor> doorsToInclude = new PList<>();
    doorsToInclude.add(this);
    // Calculate the desired door width from the room door parameters.
    int maxDoorSize = Math.min(room0.parameters().doorWidthWeights.length, room1.parameters().doorWidthWeights.length);
    float[] newWeights = new float[maxDoorSize];
    for (int a = 0; a < newWeights.length; a++) {
      newWeights[a] = room0.parameters().doorWidthWeights[a] + room1.parameters().doorWidthWeights[a];
    }
    int desiredDoorSize = PArrayUtils.randomIndexWithWeights(newWeights);
    // Find neighboring possible door locations.
    int searchOffL = 1;
    int searchOffR = 1;
    for (int attempt = 0; attempt < maxDoorSize - 1; attempt++) {
      // Get the left and right possibleDoors and filter them out if they are not valid to include with this door.
      GridTile leftTile0 = room0.tileGrid()
                                .getTileAt(wall0.owner.x + searchOffL * wall0.facing.left().forwardX(), wall0.owner.y,
                                           wall0.owner.z + searchOffL * wall0.facing.left().forwardZ());
      TileRoomPossibleDoor doorOnLeft0 =
          leftTile0 == null ? null : leftTile0.emitOptions.walls[wall0.facing.intValue()].possibleDoor;
      if (doorOnLeft0 != null && (doorOnLeft0.score() <= 0 || this.tracksSameRooms(doorOnLeft0))) {
        doorOnLeft0 = null;
      }
      GridTile rightTile0 = room0.tileGrid()
                                 .getTileAt(wall0.owner.x + searchOffR * wall0.facing.right().forwardX(), wall0.owner.y,
                                            wall0.owner.z + searchOffR * wall0.facing.right().forwardZ());
      TileRoomPossibleDoor doorOnRight0 =
          rightTile0 == null ? null : rightTile0.emitOptions.walls[wall0.facing.intValue()].possibleDoor;
      if (doorOnRight0 != null && (doorOnRight0.score() <= 0 || this.tracksSameRooms(doorOnRight0))) {
        doorOnRight0 = null;
      }
      // Check which door possibleDoor is better to include.
      if (doorOnLeft0 == null) {
        if (doorOnRight0 == null) {
          // No valid door piece to add.
          break;
        }
        // Add the door on the right.
        doorsToInclude.add(doorOnRight0);
      } else {
        if (doorOnRight0 == null) {
          // Add the door on the left
          doorsToInclude.add(doorOnLeft0);
        } else {
          // Choose the better of the two doors.
          doorsToInclude.add(doorOnRight0.score() > doorOnLeft0.score() ? doorOnRight0 : doorOnLeft0);
        }
      }
    }
    // Finally, create the door in the first place.
    TileDoor.TileDoorBuilder doorBuilder = TileDoor.builder();
    doorBuilder.genTaskTracker(room0.building().genTaskTracker());
    doorBuilder.building(tileBuilding);
    TileDoor door = doorBuilder.build();
    // Add the doors to the rooms and buildings and tiles.
    for (int a = 0; a < doorsToInclude.size(); a++) {
      TileRoomPossibleDoor includedDoor = doorsToInclude.get(a);
      includedDoor.wall0.door = door;
      includedDoor.wall1.door = door;
      door.walls().add(includedDoor.wall0);
      door.walls().add(includedDoor.wall1);
    }
    room0.building().doors().add(door);
    room1.building().doors().addIfNotPresent(door, true);
    room0.doors().doors().genUnpooled(room1).add(door);
    room1.doors().doors().genUnpooled(room1).add(door);
    TileRoomDoorHelper.recalcRoomDoorSeparations(room0);
    TileRoomDoorHelper.recalcRoomDoorSeparations(room0);
    // TODO: add door models and block the task tracker.
    //    room0.building().genTaskTracker().addBlocker(door);
  }

  @Override public float score() {
    recalcScore();
    return cachedScore.x();
  }

  /** Returns true if this door connects the same rooms as the other. */
  public boolean tracksSameRooms(TileRoomPossibleDoor other) {
    if (room0 == other.room0 && room1 == other.room1) {
      return true;
    }
    if (room0 == other.room1 && room1 == other.room0) {
      return true;
    }
    return false;
  }

  public boolean equals(Object o) {
    if (o instanceof TileRoomPossibleDoor) {
      TileRoomPossibleDoor t = (TileRoomPossibleDoor) o;
      if (wall0 == t.wall0 && wall1 == t.wall1) {
        return true;
      }
      if (wall0 == t.wall1 && wall1 == t.wall0) {
        return true;
      }
      // We shouldn't need to check anything other than if the walls are equal.
    }
    return false;
  }

  /** Recalculates the score of this door. */
  public float recalcScore() {
    cachedScore.setZero();
    // If either of the walls here can't have a door placed here, return 0.
    if (wall0.doorPlacementScore == 0 || wall1.doorPlacementScore == 0 || wall0.door != null || wall1.door != null) {
      return 0;
    }
    int roomDoorSeparation = room0.doors().doorsBetween().genPooled(room1).valueOf();
    float score = wall0.doorPlacementScore + wall1.doorPlacementScore;
    // Penalize scores based on how many doors are between the two rooms. If it's 1 door, use the max penalty.
    // If it's maxRoomSeparationForPenalty (mrs) the penalty is maxPenalty / mrs.
    float penaltyFromRoomSeparation =
        roomDoorSeparation == 0 ? 0 : (tileBuilding.parameters().maxRoomDoorSeparationPenalty / roomDoorSeparation);
    score -= penaltyFromRoomSeparation;
    // Additional penalty if there is already at least one door connecting these two rooms directly.
    int existingDoorsBetween = room0.doors().doors().has(room1) ? room0.doors().doors().get(room1).size() : 0;
    score -= tileBuilding.parameters().scorePenaltyPerAdditionalDoor * existingDoorsBetween;
    cachedScore.set(score);
    return score;
  }
}
