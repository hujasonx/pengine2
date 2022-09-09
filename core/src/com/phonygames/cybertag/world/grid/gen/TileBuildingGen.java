package com.phonygames.cybertag.world.grid.gen;

import android.support.annotation.Nullable;

import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.math.aabb.PIntAABB;

/** Helper class for generating tile buildings. */
public class TileBuildingGen {
  /**
   * Run this when finished adding rooms to the building.
   *
   * Adds hallways and places doors.
   */
  public static void onFinishedAddingRooms(TileBuilding building) {
    //
  }

  /**
   * Creates a new room with the given generation parameters.
   *
   * @return the added room, if one was able to be generated.
   */
  public static @Nullable TileRoom addRoom(TileBuilding building, TileRoomParameters parameters) {
    // It may take many attempts to find a room configuration that is valid.
    int attemptsLeft = 10;
    TileRoom room = null;
    do {

      attemptsLeft--;

    } while (room == null && attemptsLeft > 0);
    return room;
  }

  /** Returns if the bounds are valid given the building and parameters. */
  private static boolean validatePotentialRoomLocation(PIntAABB bounds, TileBuilding building, TileRoomParameters parameters) {
    PIntAABB buildingBounds = building.tileBounds();
    // Check that the building bounds are not violated.
    if (!buildingBounds.fullyContains( bounds)) { return false; }
    // Check that the minimum room thickness is not violated.
    for (int y = bounds.y0(); y <= bounds.y1(); y++) {
      // Check in the z axis.
      for (int x = bounds.x0(); x <= bounds.x1(); x++) {
        int consecutiveTilesInRoom = 0;
        for (int z = bounds.z0(); z <= bounds.z1(); z++) {
          // Tile is valid if it does
        }
      }
    }
//    parameters.minimumRoomThickness
    return true;
  }
}
