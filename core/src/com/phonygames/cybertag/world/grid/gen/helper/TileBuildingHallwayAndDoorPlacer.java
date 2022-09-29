package com.phonygames.cybertag.world.grid.gen.helper;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.util.PSortableByScore;
import com.phonygames.pengine.util.collection.PList;

/** Helper class for adding hallways and doors to a tile building. */
public class TileBuildingHallwayAndDoorPlacer {
  public static final String TAG = "TileBuildingHallwayAndDoorPlacer";

  /**
   * Adds hallways and places doors to this building. Room generation should succeed this. Any added rooms are added to
   * roomsStillGenerating.
   */
  public static void addHallwaysAndPlaceDoors(TileBuilding building) {
    TileRoomDoorHelper.recalcRoomDoorSeparations(building);
    // First, place emit possible door locations (setting the underlying tile.EmitOptions).
    for (int a = 0; a < building.rooms().size(); a++) {
      TileRoom room = building.rooms().get(a);
      room.parameters().emitPossibleDoorLocations(room);
    }
    PList<TileRoomPossibleDoor> possibleDoors = TileRoomPossibleDoor.findPossibleDoorLocations(building);
    __addPossibleDoorsLoop(possibleDoors);
    // Then, add hallways. This step will automatically create more doors.
    TileBuildingHallwayHelper.addHallways(building);
  }
  /**
   * Adds doors from the possible doors list until no doors are worth adding or the limit is reached.
   */
  private static void __addPossibleDoorsLoop(PList<TileRoomPossibleDoor> possibleDoors) {
    for (int attempt = 0; attempt < 1000; attempt ++) {
      // Finds the best possible door, then emits it.
      TileRoomPossibleDoor bestDoor = PSortableByScore.highestScorerIn(possibleDoors);
      if (bestDoor != null && bestDoor.score() > 0) {
        // Emit the door.
        bestDoor.emitToRooms();
      } else {
        break;
      }
    }
  }
}