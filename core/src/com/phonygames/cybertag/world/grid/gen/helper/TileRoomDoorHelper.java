package com.phonygames.cybertag.world.grid.gen.helper;

import android.support.annotation.NonNull;

import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileDoor;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PPooledIterable;

/** Helper class for managing doors in a building while it is being generated and laid out. */
public class TileRoomDoorHelper {
  /**
   * Recalculates the room door separations. Run this after adding a door to a room's doors().doors() map.
   *
   * @return true if anything was changed.
   */
  private static boolean __recalcRoomDoorSeparations(TileRoom starterRoom, @NonNull TileRoom checkRoom,
                                                     int curRoomSeparation) {
    boolean modified = false;
    // We are recursing. First, update the room separation with curRoomSeparation.
    int prevStoreSepInStarter = starterRoom.doors().doorsBetween().genPooled(checkRoom).valueOf();
    int prevStoreSepInCheck = checkRoom.doors().doorsBetween().genPooled(starterRoom).valueOf();
    if (prevStoreSepInStarter > curRoomSeparation) {
      starterRoom.doors().doorsBetween().genPooled(checkRoom).set(curRoomSeparation);
      modified = true;
    }
    if (prevStoreSepInCheck > curRoomSeparation) {
      checkRoom.doors().doorsBetween().genPooled(starterRoom).set(curRoomSeparation);
      modified = true;
    }
    if (!modified) {
      // If we didn't modify anything, then theres no need to propagate anything either, so return early.
      return false;
    }
    try (PPooledIterable.PPoolableIterator<PMap.Entry<TileRoom, PList<TileDoor>>> it = checkRoom.doors().doors()
                                                                                                .obtainIterator()) {
      while (it.hasNext()) {
        PMap.Entry<TileRoom, PList<TileDoor>> e = it.next();
        TileRoom otherRoom = e.k();
        __recalcRoomDoorSeparations(starterRoom, otherRoom, 1 + curRoomSeparation);
      }
    }
    return true;
  }

  /**
   * Recalculates the room door separations for the room.
   *
   * @return true if anything was changed.
   */
  public static boolean recalcRoomDoorSeparations(@NonNull TileRoom room) {
    boolean modified = false;
    try (PPooledIterable.PPoolableIterator<PMap.Entry<TileRoom, PList<TileDoor>>> it = room.doors().doors()
                                                                                           .obtainIterator()) {
      while (it.hasNext()) {
        PMap.Entry<TileRoom, PList<TileDoor>> e = it.next();
        TileRoom otherRoom = e.k();
        boolean added = __recalcRoomDoorSeparations(room, otherRoom, 1);
        modified |= added;
      }
      return modified;
    }
  }

  /**
   * Recalcs the room separations for all rooms in the building. Simply calls recalcRoomDoorSeparations on all rooms.
   *
   * @return true if anything was changed.
   */
  public static boolean recalcRoomDoorSeparations(TileBuilding building) {
    boolean modified = false;
    for (int a = 0; a < building.rooms().size(); a++) {
      boolean added = recalcRoomDoorSeparations(building.rooms().get(a));
      modified |= added;
    }
    return modified;
  }
}
