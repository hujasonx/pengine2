package com.phonygames.cybertag.world.grid.gen.helper;

import static com.phonygames.pengine.util.PFacing.X;
import static com.phonygames.pengine.util.PFacing.Z;
import static com.phonygames.pengine.util.PFacing.mX;
import static com.phonygames.pengine.util.PFacing.mZ;

import android.support.annotation.Nullable;

import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.cybertag.world.grid.gen.TileRoomParameters;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.PSortableByScore;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PPooledIterable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Helper class to generate hallways. */
public class TileBuildingHallwayHelper {
  private static final String TAG = "TileBuildingHallwayHelper";

  /** Adds hallways to the building, generating doors as needed. Places new rooms in the roomsToTrack list. */
  public static void addHallways(TileBuilding building) {
    PList<TileRoom> addedRooms = new PList<>();
    // Add hallway segments until we can't.
    for (int attempt = 0; attempt < 100; attempt++) {
      PLog.i(TAG + "| Attempting to add hallway segment");
      TileRoom addedHallwayRoom = __addHallwayRoomSegment(building);
      if (addedHallwayRoom == null) {
        break;
      }
      addedRooms.addIfNotPresent(addedHallwayRoom, true);
    }
  }

  /**
   * Adds a hallway segment to the building. Can create a new hallway room, join an existing one, or neither. Adds the
   * room to roomsToTrack.
   * <p>
   * Hallway segments additions will affect tile EmitOptions immediately. Walkway data will be used to to generate more
   * segments, and wall data allows for better internal wall connectivity.
   *
   * @return the hallway room if any.
   */
  private static TileRoom __addHallwayRoomSegment(TileBuilding building) {
    PList<HallwaySearchPoint> searchPoints = __possibleHallwayLocations(building);
    PLog.i(TAG + "| __addHallwayRoomSegment found " + searchPoints.size() + " search points");
    HallwaySearchPoint bestSearchPoint = PSortableByScore.highestScorerIn(searchPoints);
    if (bestSearchPoint == null || bestSearchPoint.score() <= 0) {return null;}
    return bestSearchPoint.emitToBuilding();
  }

  /**
   * Returns a list of all valid locations to emit a door; these can be used to connect rooms and hallways. The walls
   * that are returned will be the ones in existing rooms.
   */
  private static PList<HallwaySearchPoint> __possibleHallwayLocations(TileBuilding building) {
    PList<HallwaySearchPoint> out = new PList<>();
    // Loop through all walls and facings in the grid and emit doors when two rooms are adjacent through the wall.
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = building.tileGrid().obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        GridTile tile0 = e.val();
        TileRoom room0 = building.roomAtTilePosition(tile0.x, tile0.y, tile0.z);
        if (room0 == null) {continue;}
        for (int a = 0; a < PFacing.count(); a++) {
          GridTile.EmitOptions.Wall wall0 = tile0.emitOptions.walls[a];
          if (wall0.doorPlacementScore <= 0) {continue;}
          // If the other side of the wall is in the building but not a room, it's a valid location.
          if (building.tilePositionInBuilding(tile0.x + wall0.facing.forwardX(), tile0.y,
                                              tile0.z + wall0.facing.forwardZ()) &&
              building.roomAtTilePosition(tile0.x + wall0.facing.forwardX(), tile0.y,
                                          tile0.z + wall0.facing.forwardZ()) == null) {
            out.add(new HallwaySearchPoint(building, wall0));
          }
        }
      }
    }
    return out;
  }

  /** A hallway segment starting location. This class helps with searching as well as generating hallways. */
  private static class HallwaySearchPoint implements PSortableByScore<HallwaySearchPoint> {
    /** The building that this hallway would be in. */
    private final TileBuilding building;
    /** The searched segments. Will be populated by search() */
    private final PList<SearchedSegment> searchedSegments = new PList<>();
    /** The room that this hallway would spawn from. */
    private final TileRoom startRoom;
    /** The wall that this hallway would spawn from. */
    private final GridTile.EmitOptions.Wall startWall;

    private HallwaySearchPoint(TileBuilding building, GridTile.EmitOptions.Wall startWall) {
      this.building = building;
      this.startWall = startWall;
      this.startRoom = building.roomAtTilePosition(startWall.owner.x, startWall.owner.y, startWall.owner.z);
      PAssert.isNotNull(this.startRoom);
      search();
      PLog.i(TAG + "| Searched hallway point and found " + this.searchedSegments.size() + " possible segments");
    }

    /**
     * Recursively searches with the given existing buffer and potential new item. Should ultimately not modify the
     * tiles list.
     */
    private void __recursiveSearch(PList<HallwayTile> tiles, HallwayTile newTile, PFacing newFacing,
                                   int turnsRemaining) {
      tiles.add(newTile);
      // Check to see if this tile would connect any new rooms, and if so, track the segment.
      PList<GridTile.EmitOptions.Wall> possibleEndpoints = newTile.connectableWalls();
      for (int a = 0; a < possibleEndpoints.size(); a++) {
        GridTile.EmitOptions.Wall wall = possibleEndpoints.get(a);
        GridTile.EmitOptions.Wall otherWall = wall.wallOnOtherSideIn(building.tileGrid());
        if (otherWall == null) {continue;}
        TileRoom otherRoom = building.roomAtTilePosition(otherWall.owner.x, otherWall.owner.y, otherWall.owner.z);
        // The other room can't be null or the start room, and they cant both be hallways.
        if (otherRoom == null || otherRoom == startRoom ||
            (startRoom.parameters().isHallway && otherRoom.parameters().isHallway)) {
          continue;
        }
        // The other wall must accept a door.
        if (otherWall.doorPlacementScore <= 0) {continue;}
        if (otherRoom.parameters().isHallway) {
          // The other wall must have a flat, floored or walkwayed surface if the other room is a hallway.
          GridTile otherTile = otherWall.owner;
          if (otherTile.emitOptions.walkwayModelTemplateID == null &&
              otherTile.emitOptions.floorModelTemplateID == null) {continue;}
          // If the other tile has a walkway, it must be flat!
          if (otherTile.emitOptions.walkwayModelTemplateID != null &&
              !otherTile.emitOptions.walkwayCornerVerticalOffsets.isZero()) {continue;}
        }
        // Track the segment.
        SearchedSegment segment = new SearchedSegment(tiles, otherWall);
        searchedSegments.add(segment);
      }
      float downY = 1f / building.parameters().hallwayTilesPerSlopeUp;
      HallwayTile recurseTile;
      // Recurse.
      int maxLength = building.parameters().hallwayLengthWeights.length - 1; // Start at 1.
      if (tiles.size() + 1 <= maxLength) {
        for (int a = 0; a < PFacing.count(); a++) {
          PFacing recurseFacing = PFacing.get(a);
          // Skip this facing if it's going exactly where we came from.
          if (recurseFacing == newFacing.opposite()) {continue;}
          boolean isTurn = recurseFacing != newFacing;
          // Check to see if we have any turns remaining.
          if (turnsRemaining <= 0 && isTurn) {continue;}
          recurseTile = newTile.extend(recurseFacing, 0);
          if (recurseTile != null) {
            __recursiveSearch(tiles, recurseTile, recurseFacing, turnsRemaining - (isTurn ? 1 : 0));
          }
          recurseTile = newTile.extend(recurseFacing, downY);
          if (recurseTile != null) {
            __recursiveSearch(tiles, recurseTile, recurseFacing, turnsRemaining - (isTurn ? 1 : 0));
          }
        }
      }
      tiles.removeLast();
    }

    /**
     * Emits the best searched segment, either creating a new hallway room or combining it with an existing one.
     *
     * @return the hallway room.
     */
    public TileRoom emitToBuilding() {
      SearchedSegment bestSegment = PSortableByScore.highestScorerIn(searchedSegments);
      if (bestSegment == null) {
        return null;
      }
      return bestSegment.emitToBuilding();
    }

    @Override public float score() {
      if (searchedSegments.isEmpty()) {
        return 0;
      }
      SearchedSegment bestSegment = PSortableByScore.highestScorerIn(searchedSegments);
      if (bestSegment == null) {
        return 0;
      }
      return bestSegment.score();
    }

    /** Performs a search from this search point, adding possible segments to searchedSegments. */
    private void search() {
      PAssert.isTrue(searchedSegments.isEmpty(), "Don't call search more than once!");
      PList<HallwayTile> tileBuffer = new PList<>();
      GridTile nextTile = startWall.tileOnOtherSideIn(building.tileGrid());
      // Don't search if there is already a room on the other side.
      if (building.roomAtTilePosition(nextTile.x, nextTile.y, nextTile.z) != null) {
        return;
      }
      // Search out from this search position.
      HallwayTile firstTile;
      firstTile = new HallwayTile(nextTile, null);
      __recursiveSearch(tileBuffer, firstTile, startWall.facing, building.parameters().hallwayTurnWeights.length);
      // Do a search downwards as well, if it is possible.
      GridTile tileBelow = building.tileAtTilePosition(startWall.owner.x, startWall.owner.y - 1, startWall.owner.z);
      if (tileBelow != null && building.roomAtTilePosition(tileBelow.x, tileBelow.y, tileBelow.z) == null) {
        firstTile = new HallwayTile(tileBelow, nextTile);
        float downY = 1f / building.parameters().hallwayTilesPerSlopeUp;
        firstTile.walkwayHeights.x((startWall.facing == X || startWall.facing == Z) ? 1 : 1 - downY); // 00.
        firstTile.walkwayHeights.y((startWall.facing == mX || startWall.facing == Z) ? 1 : 1 - downY); // 10.
        firstTile.walkwayHeights.z((startWall.facing == mX || startWall.facing == mZ) ? 1 : 1 - downY); // 11.
        firstTile.walkwayHeights.x((startWall.facing == X || startWall.facing == mZ) ? 1 : 1 - downY); // 01.
        __recursiveSearch(tileBuffer, firstTile, startWall.facing, building.parameters().hallwayTurnWeights.length);
      }
    }

    /** Represents a grid tile traversed by the hallway; can be two tiles tall if the hallway is in between Ys. */
    private class HallwayTile {
      /** The bottom gridTile. */
      private final GridTile baseTile;
      /** The top gridTile. Only needed if the hallway is in between Ys. */
      private final GridTile upperTile;
      /** The tile-space walkway offset. */
      private final PVec4 walkwayHeights = PVec4.obtain();
      /** The possible connection points for this tile (inside this tile). */
      @Getter(value = AccessLevel.PRIVATE, lazy = true)
      @Accessors(fluent = true)
      private final PList<GridTile.EmitOptions.Wall> connectableWalls = __genConnectableWalls();

      public HallwayTile(GridTile baseTile, @Nullable GridTile upperTile) {
        this.baseTile = baseTile;
        this.upperTile = upperTile;
      }

      /** Returns the list of possible connecting points for this hallway tile. */
      private PList<GridTile.EmitOptions.Wall> __genConnectableWalls() {
        PList<GridTile.EmitOptions.Wall> walls = new PList<>();
        if (walkwayHeights.x() == 0) { // 00 corner.
          if (walkwayHeights.y() == 0) { // 10 corner.
            walls.add(baseTile.emitOptions.walls[mZ.intValue()]);
          }
          if (walkwayHeights.w() == 0) { // 01 corner.
            walls.add(baseTile.emitOptions.walls[mX.intValue()]);
          }
        }
        if (walkwayHeights.z() == 0) { // 11 corner.
          if (walkwayHeights.y() == 0) { // 10 corner.
            walls.add(baseTile.emitOptions.walls[X.intValue()]);
          }
          if (walkwayHeights.w() == 0) { // 01 corner.
            walls.add(baseTile.emitOptions.walls[Z.intValue()]);
          }
        }
        if (upperTile != null) {
          if (walkwayHeights.x() == 1) { // 00 corner.
            if (walkwayHeights.y() == 1) { // 10 corner.
              walls.add(upperTile.emitOptions.walls[mZ.intValue()]);
            }
            if (walkwayHeights.w() == 1) { // 01 corner.
              walls.add(upperTile.emitOptions.walls[mX.intValue()]);
            }
          }
          if (walkwayHeights.z() == 1) { // 11 corner.
            if (walkwayHeights.y() == 1) { // 10 corner.
              walls.add(upperTile.emitOptions.walls[X.intValue()]);
            }
            if (walkwayHeights.w() == 1) { // 01 corner.
              walls.add(upperTile.emitOptions.walls[Z.intValue()]);
            }
          }
        }
        return walls;
      }

      /**
       * Generates a new hallway tile that connects to this one in the given direction. Returns null if that direction
       * cannot have a hallway generated.
       */
      public @Nullable HallwayTile extend(PFacing direction, float tileSpaceEndYChange) {
        // Check if this hallway tile is flat in the direction, and if so, set startYOffset.
        float startYOffset = 0;
        switch (direction) {
          case X:
            if ((startYOffset = walkwayHeights.y()) != walkwayHeights.z()) { // 10 and 11.
              return null;
            }
            break;
          case Z:
            if ((startYOffset = walkwayHeights.z()) != walkwayHeights.w()) { // 11 and 01.
              return null;
            }
            break;
          case mX:
            if ((startYOffset = walkwayHeights.x()) != walkwayHeights.w()) { // 00 and 01.
              return null;
            }
            break;
          case mZ:
            if ((startYOffset = walkwayHeights.x()) != walkwayHeights.y()) { // 00 and 10.
              return null;
            }
            break;
        }
        int nextX = baseTile.x + direction.forwardX(), nextY = baseTile.y, nextZ = baseTile.z + direction.forwardZ();
        float endYOffset = startYOffset - tileSpaceEndYChange;
        // If we are already close to the bottom and we are going below it, shift the tiles down one.
        if (endYOffset < 0) {
          startYOffset++;
          endYOffset++;
          nextY--;
        }
        // If we are already close to the top and we are going above it, shift the tiles up one.
        if (endYOffset > 1) {
          startYOffset--;
          endYOffset--;
          nextY++;
        }
        PAssert.isTrue(startYOffset <= 1 && startYOffset >= 0);
        PAssert.isTrue(endYOffset <= 1 && endYOffset >= 0);
        GridTile nextTile = null, nextTileUpper = null;
        if (building.roomAtTilePosition(nextX, nextY, nextZ) == null) {
          nextTile = building.tileAtTilePosition(nextX, nextY, nextZ);
        }
        if (nextTile == null) {return null;}
        // Include an upper tile if the tile is in between Ys.
        if (startYOffset > 0 || endYOffset > 0) {
          if (building.roomAtTilePosition(nextX, nextY + 1, nextZ) == null) {
            nextTileUpper = building.tileAtTilePosition(nextX, nextY + 1, nextZ);
          }
          if (nextTileUpper == null) {return null;}
        }
        HallwayTile newTile = new HallwayTile(nextTile, nextTileUpper);
        // Set the walkway heights. 00, 10, 11, 01.
        switch (direction) {
          case X:
            newTile.walkwayHeights.set(startYOffset, endYOffset, endYOffset, startYOffset);
            break;
          case Z:
            newTile.walkwayHeights.set(startYOffset, startYOffset, endYOffset, endYOffset);
            break;
          case mX:
            newTile.walkwayHeights.set(endYOffset, startYOffset, startYOffset, endYOffset);
            break;
          case mZ:
            newTile.walkwayHeights.set(endYOffset, endYOffset, startYOffset, startYOffset);
            break;
        }
        return newTile;
      }

      /** Returns true if this HallwayTile intersects the other. */
      public boolean intersects(HallwayTile other) {
        if (this.baseTile == other.baseTile || this.baseTile == other.upperTile) {return true;}
        if (this.upperTile == null) {return false;}
        return this.upperTile == other.baseTile || this.upperTile == other.upperTile;
      }
    }

    /** Represents an actually potential segment. */
    private class SearchedSegment implements PSortableByScore<SearchedSegment> {
      /** The ending room this segment connects to. */
      private final TileRoom endRoom;
      /**
       * The ending wall this segment connects to - not inside the segment! (owned by the room that this segment would
       * connect with).
       */
      private final GridTile.EmitOptions.Wall endWall;
      /** The starting room this segment connects to. */
      private final TileRoom startRoom;
      /** The tiles in this segment. */
      private final PList<HallwayTile> tiles = new PList<>();

      /**
       * @param tiles
       * @param endWall The end wall that this will connect to - should be inside an existing room.
       */
      public SearchedSegment(PList<HallwayTile> tiles, GridTile.EmitOptions.Wall endWall) {
        this.tiles.addAll(tiles);
        this.startRoom = building.roomAtTilePosition(startWall.owner.x, startWall.owner.y, startWall.owner.z);
        this.endWall = endWall;
        this.endRoom = building.roomAtTilePosition(endWall.owner.x, endWall.owner.y, endWall.owner.z);
        PAssert.isNotNull(endRoom);
        PAssert.isFalse(this.endRoom == this.startRoom);
      }

      /**
       * Emits the searched segment, either creating a new hallway room or combining it with an existing one. Sets the
       * emit options for the walls and creates doors.
       */
      public TileRoom emitToBuilding() {
        PLog.i(TAG + "| EmitToBuilding called for hallway of length " + tiles.size());
        TileRoom hallwayRoom = null;
        if (startRoom.parameters().isHallway) {hallwayRoom = startRoom;} else if (endRoom.parameters().isHallway) {
          hallwayRoom = endRoom;
        } else {
          // Create a new room.
          hallwayRoom = TileRoom.builder().parameters(building.parameters().getHallwayParameters())
                                .genTaskTracker(building.genTaskTracker()).building(building).build();
          building.rooms().add(hallwayRoom);
          hallwayRoom.genTaskTracker().addBlocker(hallwayRoom);
        }
        PAssert.isNotNull(hallwayRoom);
        // Loop through the hallway tiles and process them.
        PList<GridTile.EmitOptions.Wall> wallsToPossiblyEmit = new PList<>();
        for (int a = 0; a < tiles.size(); a++) {
          HallwayTile tile = tiles.get(a);
          hallwayRoom.tileGrid().trackTile(tile.baseTile);
          if (tile.upperTile != null) {
            hallwayRoom.tileGrid().trackTile(tile.upperTile);
          }
          // Create the floor.
          tile.baseTile.emitOptions.walkwayCornerVerticalOffsets.set(tile.walkwayHeights);
          tile.baseTile.emitOptions.walkwayModelTemplateID =
              ((TileRoomParameters.HallwayParameters) hallwayRoom.parameters()).walkwayTemplateForTraversedTile(
                  hallwayRoom, tile.baseTile);
          // Create the ceiling.
          if (tile.upperTile == null) {
            tile.baseTile.emitOptions.ceilingCornerVerticalOffsets.set(tile.walkwayHeights);
            tile.baseTile.emitOptions.ceilingModelTemplateID =
                ((TileRoomParameters.HallwayParameters) hallwayRoom.parameters()).walkwayTemplateForTraversedTile(
                    hallwayRoom, tile.baseTile);
          } else {
            tile.upperTile.emitOptions.ceilingCornerVerticalOffsets.set(tile.walkwayHeights);
            // Since the upper tile is up one, we need to shift the ceiling offset down.
            tile.upperTile.emitOptions.ceilingCornerVerticalOffsets.add(-1, -1, -1, -1);
            tile.upperTile.emitOptions.ceilingModelTemplateID =
                ((TileRoomParameters.HallwayParameters) hallwayRoom.parameters()).ceilingTemplateForTraversedTile(
                    hallwayRoom, tile.baseTile);
          }
          // Mark all connection points as accepting doors.
          PList<GridTile.EmitOptions.Wall> validConnectionPoints = tile.connectableWalls();
          for (int b = 0; b < validConnectionPoints.size(); b++) {
            validConnectionPoints.get(b).doorPlacementScore = 1;
          }
          // Add walls to a list of walls to process.
          for (int b = 0; b < PFacing.count(); b++) {
            // Walls that separate two tiles in this segment should not be generated.
            GridTile.EmitOptions.Wall wall = tile.baseTile.emitOptions.walls[b];
            GridTile otherTile = wall.tileOnOtherSideIn(building.tileGrid());
            // If this is not the first hallway tile, check to see if this wall would separate this tile from the
            // previous.
            if (a == 0 || otherTile == null ||
                (tiles.get(a - 1).baseTile != otherTile && tiles.get(a - 1).upperTile != otherTile)) {
              wallsToPossiblyEmit.add(wall);
            }
            if (tile.upperTile != null) {
              wall = tile.upperTile.emitOptions.walls[b];
              otherTile = wall.tileOnOtherSideIn(building.tileGrid());
              // If this is not the first hallway tile, check to see if this wall would separate this tile from the
              // previous.
              if (a == 0 || otherTile == null ||
                  (tiles.get(a - 1).baseTile != otherTile && tiles.get(a - 1).upperTile != otherTile)) {
                wallsToPossiblyEmit.add(wall);
              }
              wallsToPossiblyEmit.add(wall);
            }
          }
        }
        // Process the walls and emit doors.
        for (int a = 0; a < wallsToPossiblyEmit.size(); a++) {
          GridTile.EmitOptions.Wall wall = wallsToPossiblyEmit.get(a);
          // If this is the starting wall, don't emit a normal wall here. If the starting room is a hallway, delete the
          // other wall. If it is not, create a door.
          boolean shouldEmitNormalWall = true;
          if (wall.wallOnOtherSideIn(building.tileGrid()) == startWall) {
            shouldEmitNormalWall = false;
            if (startRoom.parameters().isHallway) {
              startWall.wallModelTemplateIDs.clear();
            } else {
              // TODO: emit door frame.
              TileRoomPossibleDoor door = new TileRoomPossibleDoor(startRoom, hallwayRoom, building, startWall, wall);
              startWall.possibleDoor = door;
              wall.possibleDoor = door;
              door.emitToRooms();
              PLog.i(TAG + "| Placed door connecting " + startRoom + " and " + hallwayRoom);
            }
          }
          // If this is the ending wall, don't emit a normal wall here. If the ending room is a hallway, delete the
          // other wall. If it is not, create a door.
          if (wall.wallOnOtherSideIn(building.tileGrid()) == endWall) {
            shouldEmitNormalWall = false;
            if (endRoom.parameters().isHallway) {
              endWall.wallModelTemplateIDs.clear();
            } else {
              // TODO: emit door frame.
              TileRoomPossibleDoor door = new TileRoomPossibleDoor(endRoom, hallwayRoom, building, endWall, wall);
              endWall.possibleDoor = door;
              wall.possibleDoor = door;
              door.emitToRooms();
              PLog.i(TAG + "| Placed door connecting " + hallwayRoom + " and " + endRoom);
            }
          }
          if (shouldEmitNormalWall) {
            ((TileRoomParameters.HallwayParameters) hallwayRoom.parameters()).addWallTemplatesFor(hallwayRoom,
                                                                                                  wall.owner, wall);
          }
        }
        TileRoomDoorHelper.recalcRoomDoorSeparations(hallwayRoom);
        return hallwayRoom;
      }

      @Override public float score() {
        /** The rooms cannot have a door between them already. */
        if (endRoom.doors().doors().has(startRoom)) {return 0;}
        float scoreFromLength = building.parameters().hallwayLengthWeights[tiles.size()];
        return scoreFromLength;
      }
    }
  }
}
