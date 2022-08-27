package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagDoorGen.DOOR_HEIGHT_WEIGHTS;
import static com.phonygames.cybertag.world.lasertag.LasertagDoorGen.DOOR_WIDTH_WEIGHTS;
import static com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor.needsWallInDirection;
import static com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor.otherTileForWall;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;

/** Helper class to handle wall-scale wall generation. */
public class LasertagRoomWallGen {
  protected final LasertagTileGen cornerTile;
  protected final LasertagTileWall.Facing facing;
  protected final LasertagRoomGen roomGen;
  protected final PIntMap3d<LasertagTileGen> tileGens;
  protected final PList<Integer> wallHeights = new PList<>();
  protected final int xChangeForAlongWall, zChangeForAlongWall;

  public LasertagRoomWallGen(LasertagRoomGen roomGen, PIntMap3d<LasertagTileGen> tileGens, LasertagTileGen cornerTile,
                             LasertagTileWall.Facing facing) {
    this.roomGen = roomGen;
    this.tileGens = tileGens;
    this.facing = facing;
    this.cornerTile = cornerTile;
    xChangeForAlongWall = facing == LasertagTileWall.Facing.Z ? -1 : (facing == LasertagTileWall.Facing.mZ ? 1 : 0);
    zChangeForAlongWall = facing == LasertagTileWall.Facing.X ? 1 : (facing == LasertagTileWall.Facing.mX ? -1 : 0);
    genWallHeightData();
    genPossibleDoors();
  }

  private void genWallHeightData() {
    LasertagTileGen searchTileGenBase = null, searchTileGenTop = null;
    int searchTileBaseX = cornerTile.x;
    int searchTileBaseY = cornerTile.y;
    int searchTileBaseZ = cornerTile.z;
    LasertagTileGen nextTileGenBase = cornerTile, nextTileGenTop;
    while (nextTileGenBase != null) {
      searchTileGenBase = nextTileGenBase;
      nextTileGenTop = searchTileGenBase;
      int searchTileYOffset = 0;
      while (needsWallInDirection(roomGen, nextTileGenTop, facing)) {
        searchTileGenTop = nextTileGenTop;
        // Mark the wall as a window if it is on the edge of the building.
        // If the wall on the edge of the building, make it a window.
        if (roomGen.buildingGen.tileGens.get(searchTileGenTop.x - facing.normalX(), searchTileGenTop.y, searchTileGenTop.z - facing.normalZ()) == null) {
          searchTileGenTop.tile.wall(facing).isWindow = true;
        }
        // Mark all the walls for the visited tiles as valid, since we own them now.
        searchTileGenTop.tile.wall(facing).hasWall = true;
        searchTileGenTop.wallGen(facing).roomWallGen = this;
        LasertagTileGen otherTileGen = otherTileForWall(roomGen.buildingGen, searchTileGenTop, facing);
        if (otherTileGen != null) {
          otherTileGen.wallGen(facing.opposite()).otherRoomWallGen = this;
        }
        searchTileYOffset++;
        nextTileGenTop = tileGens.get(searchTileBaseX, searchTileBaseY + searchTileYOffset, searchTileBaseZ);
      }
      if (searchTileYOffset == 0) {break;}
      wallHeights.add(searchTileYOffset);
      searchTileBaseX += xChangeForAlongWall;
      searchTileBaseZ += zChangeForAlongWall;
      nextTileGenBase = tileGens.get(searchTileBaseX, searchTileBaseY, searchTileBaseZ);
    }
    PAssert.isTrue(wallHeights.size() > 0);
  }

  private void genPossibleDoors() {
    for (int startX = 0; startX < wallHeights.size(); startX++) {
      for (int startY = 0; startY < wallHeights.get(startX); startY++) {
        LasertagTileGen testCornerTile =
            tileGens.get(cornerTile.x + (startX) * xChangeForAlongWall, cornerTile.y + startY,
                         cornerTile.z + (startX) * zChangeForAlongWall);
        PAssert.isNotNull(testCornerTile);
        LasertagTileGen testCornerTileOther = otherTileForWall(roomGen.buildingGen, testCornerTile, facing);
        if (testCornerTileOther == null || testCornerTileOther.roomGen == null) {
          continue;
        } // Can't make doors if there is no tile to connect to.
        // Try all the possible door sizes.
        for (int w = 1; w <= DOOR_WIDTH_WEIGHTS.length; w++) {
          for (int h = 1; h <= Math.min(w, DOOR_HEIGHT_WEIGHTS.length);
               h++) { // Doors can't be higher than they are wide.
            // Determine if this is a valid door placement.
            LasertagRoom otherRoom =
                testCornerTileOther.roomGen == null ? null : testCornerTileOther.roomGen.lasertagRoom;
            if (otherRoom == null) {
              break;
            }
            boolean couldBeValid = false;
            for (int testX = startX; testX < startX + w; testX++) {
              for (int testY = startY; testY < startY + h; testY++) {
                LasertagTileGen lookTile =
                    tileGens.get(cornerTile.x + (testX) * xChangeForAlongWall, cornerTile.y + testY,
                                 cornerTile.z + (testX) * zChangeForAlongWall);
                LasertagTileGen otherTile = otherTileForWall(roomGen.buildingGen, lookTile, facing);
                if (otherTile == null || lookTile.wallGen(facing).preventWallDoorSpawns ||
                    otherTile.wallGen(facing.opposite()).preventWallDoorSpawns) {
                  couldBeValid = false;
                  break;
                }
                if (!needsWallInDirection(roomGen, lookTile, facing) || otherTile.roomGen == null ||
                    otherRoom != otherTile.roomGen.lasertagRoom) {
                  couldBeValid = false;
                  break;
                } else {
                  couldBeValid = true;
                }
              }
              if (!couldBeValid) {
                break;
              }
            }
            if (couldBeValid) {
              LasertagDoorGen.PossibleWallDoor possibleWallDoor =
                  new LasertagDoorGen.PossibleWallDoor(this, startX, startY, w, h, roomGen, testCornerTileOther.roomGen);
              roomGen.buildingGen.possibleDoors.add(possibleWallDoor);
            }
          }
        }
      }
    }
  }
}

