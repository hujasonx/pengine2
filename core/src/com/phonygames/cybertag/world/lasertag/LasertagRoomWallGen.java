package com.phonygames.cybertag.world.lasertag;

import static com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor.needsWallInDirection;
import static com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor.otherTileForWall;

import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

/** Helper class to handle wall-scale wall generation. */
public class LasertagRoomWallGen {
  public static final float[] DOOR_HEIGHT_WEIGHTS = new float[]{1, 1.5f, .3f};
  public static final float[] DOOR_WIDTH_WEIGHTS = new float[]{1, 1.5f, .3f};
  private final LasertagTileGen cornerTile;
  private final LasertagTileWall.Facing facing;
  private final LasertagRoomGen roomGen;
  private final PIntMap3d<LasertagTileGen> tileGens;
  private final PList<Integer> wallHeights = new PList<>();
  private final int xChangeForAlongWall, zChangeForAlongWall;

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
      while (nextTileGenTop != null && needsWallInDirection(roomGen, nextTileGenTop, facing)) {
        searchTileGenTop = nextTileGenTop;
        // Mark all the walls for the visited tiles as valid, since we own them now.
        searchTileGenTop.tile.wall(facing).valid = true;
        searchTileGenTop.wallGen(facing).roomWallGen = this;
        searchTileYOffset++;
        nextTileGenTop = tileGens.get(searchTileBaseX, searchTileBaseY + searchTileYOffset, searchTileBaseZ);
      }
      if (searchTileYOffset == 0) {break;}
      wallHeights.add(searchTileYOffset);
      searchTileBaseX += xChangeForAlongWall;
      searchTileBaseZ += zChangeForAlongWall;
      nextTileGenBase = tileGens.get(searchTileBaseX, searchTileBaseY, searchTileBaseZ);
    }
  }

  private void genPossibleDoors() {
    for (int startX = 0; startX < wallHeights.size; startX++) {
      for (int startY = 0; startY < wallHeights.get(startX); startY++) {
        // Try all the possible door sizes.
        for (int w = 1; w <= DOOR_WIDTH_WEIGHTS.length; w++) {
          for (int h = 1; h <= DOOR_HEIGHT_WEIGHTS.length; h++) {
            // Determine if this is a valid door placement.
            LasertagTileGen otherTileGen = otherTileForWall(roomGen.buildingGen,
                                                            tileGens.get(cornerTile.x + startX * xChangeForAlongWall,
                                                                         cornerTile.y + startY,
                                                                         cornerTile.z + startX * zChangeForAlongWall),
                                                            facing);
            LasertagRoomGen otherRoomGen = otherTileGen == null ? null : otherTileGen.roomGen;
            if (otherRoomGen == null) {
              break;
            }
            boolean couldBeValid = false;
            for (int testX = startX; testX < startX + w; testX++) {
              for (int testY = startY; testY < startY + h; testY++) {
                LasertagTileGen lookTile =
                    tileGens.get(cornerTile.x + (testX) * xChangeForAlongWall, cornerTile.y + testY,
                                 cornerTile.z + (testX) * zChangeForAlongWall);
                if (lookTile == null || !needsWallInDirection(roomGen, lookTile, facing) || otherRoomGen != otherTileForWall(
                    roomGen.buildingGen, lookTile, facing).roomGen) {
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
              PossibleDoor possibleDoor = new PossibleDoor(this, startX, startY, w, h, roomGen, otherRoomGen);
              roomGen.buildingGen.possibleDoors.add(possibleDoor);
            }
          }
        }
      }
    }
  }

  public static class PossibleDoor implements Comparable<PossibleDoor> {
    private final LasertagRoomGen ownerRoomGen, otherRoomGen;
    private final LasertagRoomWallGen ownerWall;
    private final int x, y, w, h;

    private PossibleDoor(LasertagRoomWallGen wallGen, int x, int y, int w, int h, LasertagRoomGen ownerRoomGen,
                         LasertagRoomGen otherRoomGen) {
      this.ownerWall = wallGen;
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.ownerRoomGen = ownerRoomGen;
      this.otherRoomGen = otherRoomGen;
    }

    /** Finalizes this possible door as an actual door. */
    public LasertagDoorGen toDoorGen() {
      LasertagDoorGen ret =  new LasertagDoorGen();
      ret.door.w = w;
      ret.door.h = h;
      ret.door.tileX = ownerWall.xChangeForAlongWall * x + ownerWall.cornerTile.x;
      ret.door.tileY = y + ownerWall.cornerTile.y;
      ret.door.tileZ = ownerWall.zChangeForAlongWall * x + ownerWall.cornerTile.z;
      ret.door.facing = ownerWall.facing;
      // Fill out the wall data.
      for (int testX = 0; testX < w; testX++) {
        for (int testY = 0; testY < h; testY++) {
          LasertagTileGen lookTile =
              ownerRoomGen.buildingGen.tilesBuilders.get(ret.door.tileX + testX * ownerWall.xChangeForAlongWall,
                                                         ret.door.tileY + testY,
                                                         ret.door.tileZ + testX * ownerWall.zChangeForAlongWall);
          lookTile.wallGen(ownerWall.facing).wall.hasDoorframeL = testX == 0;
          lookTile.wallGen(ownerWall.facing).wall.hasDoorframeR = testX == w - 1;
          lookTile.wallGen(ownerWall.facing).wall.hasDoorframeT = testY == h - 1;
        }
      }
      ownerRoomGen.buildingGen.doorGens.add(ret);
      return ret;
    }

    @Override
    public int compareTo(PossibleDoor other) {
      float score = score();
      float otherScore = other.score();
      if (score > otherScore) {return 1;}
      if (score < otherScore) {return -1;}
      return 0;
    }

    public float score() {
      float scoreFromRoomConnections = 0;
      return DOOR_HEIGHT_WEIGHTS[h - 1] + DOOR_WIDTH_WEIGHTS[w - 1] + scoreFromRoomConnections;
    }
  }
}

