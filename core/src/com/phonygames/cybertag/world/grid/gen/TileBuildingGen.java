package com.phonygames.cybertag.world.grid.gen;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileGrid;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.collection.PArrayUtils;

/** Helper class for generating tile buildings. */
public class TileBuildingGen {
  private static final String TAG = "TileBuildingGen";

  /**
   * Creates a new room with the given generation parameters.
   *
   * @return the added room, if one was able to be generated.
   */
  public static @Nullable TileRoom addRoom(TileBuilding building, TileRoomParameters parameters) {
    PLog.i(TAG + "| addRoom() called.");
    // It may take many attempts to find a room configuration that is valid.
    int attemptsLeft = 10;
    TileRoom room = null;
    do {
      PLog.i(TAG + "| addRoom() attempt; " + attemptsLeft + " attempts remaining");
      attemptsLeft--;
      PIntAABB roomBounds = generatePotentialRoomBounds(building, parameters);
      TileGrid roomTiles = validatePotentialRoomLocation(roomBounds, building, parameters);
      if (roomTiles == null) {continue;}
      room = TileRoom.builder().parameters(parameters).genTaskTracker(building.genTaskTracker()).building(building).build();
      room.tileGrid().trackAll(roomTiles);
    } while (room == null && attemptsLeft > 0);
    // If the room will be generated, add it as a blocker.
    if (room != null) {
      building.rooms().add(room);
      room.genTaskTracker().addBlocker(room);
    }
    return room;
  }

  /** Generates a potential room position and size with the given parameters. */
  private static PIntAABB generatePotentialRoomBounds(TileBuilding building, TileRoomParameters parameters) {
    PIntAABB aabb = PIntAABB.getStaticPool().obtain();
    int roomSizeX = MathUtils.random(parameters.minimumEdgeSize, parameters.maximumEdgeSize);
    int roomSizeY = PArrayUtils.randomIndexWithWeights(parameters.heightWeights) + 1;
    int roomSizeZ = MathUtils.random(parameters.minimumEdgeSize, parameters.maximumEdgeSize);
    // MathUtils.random(int, int) has an inclusive end parameter, so we add 1.
    int baseX = MathUtils.random(building.tileBounds().x0(),
                                 Math.max(building.tileBounds().x0(), building.tileBounds().x1() - roomSizeX + 1));
    int baseY = MathUtils.random(building.tileBounds().y0(),
                                 Math.max(building.tileBounds().y0(), building.tileBounds().y1() - roomSizeY + 1));
    int baseZ = MathUtils.random(building.tileBounds().z0(),
                                 Math.max(building.tileBounds().z0(), building.tileBounds().z1() - roomSizeZ + 1));
    aabb.set(baseX, baseY, baseZ, baseX + roomSizeX - 1, baseY + roomSizeY - 1, baseZ + roomSizeZ - 1);
    PLog.i(TAG + "| Generated potential room bounds: " + aabb);
    return aabb;
  }

  /** Checks if the bounds and parameters represent a valid room. If so, returns the tilegrid with the tiles. */
  private static @Nullable TileGrid validatePotentialRoomLocation(PIntAABB bounds, TileBuilding building,
                                                        TileRoomParameters parameters) {
    PLog.i(TAG + "| Validating potential room location");
    PIntAABB buildingBounds = building.tileBounds();
    TileGrid roomTiles = new TileGrid();
    // Check that the building bounds are not violated.
    if (!buildingBounds.fullyContains(bounds)) {
      PLog.i(TAG + "| potential room location not fully contained in building");
      return null;
    }
    // Check that the minimum room thickness is not violated.
    for (int y = bounds.y0(); y <= bounds.y1(); y++) {
      // Check in the z axis.
      for (int x = bounds.x0(); x <= bounds.x1(); x++) {
        int consecutiveTilesInRoom = 0;
        for (int z = bounds.z0(); z <= bounds.z1(); z++) {
          boolean tileFree = building.tilePositionInBuilding(x, y, z) && building.roomAtTilePosition(x, y, z) == null;
          if (tileFree) {
            // If the tile is free, add the tile to the grid and count it.
            roomTiles.trackTile(building.tileAtTilePosition(x, y, z));
            consecutiveTilesInRoom ++;
          } else {
            if (consecutiveTilesInRoom > 0 && consecutiveTilesInRoom < parameters.minimumRoomThickness) {
              PLog.i(TAG + "| Room thickness in z axis violated!");
              return null;
            }
          }
        }
      }
    }
    //    parameters.minimumRoomThickness
    PLog.i(TAG + "| Valid room location!");
    return roomTiles;
  }

  /**
   * Run this when finished setting bounds to the building.
   * <p>
   * Creates all the tiles.
   */
  public static void onFinishedSettingBuildingBounds(TileBuilding building) {
    for (int x = building.tileBounds().x0(); x <= building.tileBounds().x1(); x++) {
      for (int y = building.tileBounds().y0(); y <= building.tileBounds().y1(); y++) {
        for (int z = building.tileBounds().z0(); z <= building.tileBounds().z1(); z++) {
          if (building.tilePositionInBuilding(x, y, z)) {
            GridTile tile = GridTile.builder().x(x).y(y).z(z).build();
            building.tileGrid().trackTile(tile);
          }
        }
      }
    }
  }

  /**
   * Run this when finished adding rooms to the building.
   * <p>
   * Adds hallways and places doors.
   */
  public static void onFinishedAddingRooms(TileBuilding building) {
    //
    // Finally, notify the rooms that they should continue processing.
    for (int a = 0; a < building.rooms().size(); a++) {
      TileRoom room = building.rooms().get(a);
      TileRoomGen.onNeighborsAndDoorsReady(room);
    }
    building.unblockTaskTracker();
  }
}
