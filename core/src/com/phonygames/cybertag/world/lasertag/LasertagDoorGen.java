package com.phonygames.cybertag.world.lasertag;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PSortableByScore;

public class LasertagDoorGen extends PBuilder {
  protected final LasertagDoor door;
  protected final LasertagRoomGen ownerRoomGen, otherRoomGen;
  public static final float[] DOOR_HEIGHT_WEIGHTS = new float[]{1, 1.5f, .3f};
  public static final float[] DOOR_WIDTH_WEIGHTS = new float[]{1, .8f, .3f};

  protected LasertagDoorGen(LasertagRoomGen ownerRoomGen, LasertagRoomGen otherRoomGen) {
    door = new LasertagDoor();
    this.otherRoomGen = otherRoomGen;
    this.ownerRoomGen = ownerRoomGen;
    ownerRoomGen.directlyConnectedRooms.add(otherRoomGen);
    otherRoomGen.directlyConnectedRooms.add(ownerRoomGen);
  }

  public LasertagDoor build() {
    lockBuilder();
    return door;
  }

  public static class PossibleDoor implements PSortableByScore<PossibleDoor> {
    private final LasertagRoomGen ownerRoomGen, otherRoomGen;
    private final LasertagRoomWallGen ownerWall;
    private final int x, y, w, h;
    private final float stableRandom = MathUtils.random();

    PossibleDoor(LasertagRoomWallGen wallGen, int x, int y, int w, int h, LasertagRoomGen ownerRoomGen,
                 LasertagRoomGen otherRoomGen) {
      this.ownerWall = wallGen;
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.ownerRoomGen = ownerRoomGen;
      this.otherRoomGen = otherRoomGen;
    }

    /** Checks to see if the tiles that would be affected can still have this door emitted to them. */
    public boolean checkStillValid() {
      int tileX = ownerWall.xChangeForAlongWall * x + ownerWall.cornerTile.x;
      int tileY = y + ownerWall.cornerTile.y;
      int tileZ = ownerWall.zChangeForAlongWall * x + ownerWall.cornerTile.z;
      for (int testX = 0; testX < w; testX++) {
        for (int testY = 0; testY < h; testY++) {
          LasertagTileGen lookTile =
              ownerRoomGen.buildingGen.tileGens.get(tileX + testX * ownerWall.xChangeForAlongWall,
                                                    tileY + testY,
                                                    tileZ + testX * ownerWall.zChangeForAlongWall);
          LasertagTileWallGen wallGen = lookTile.wallGen(ownerWall.facing);
          if (wallGen.wall.isValid) { // If the wall is valid, that means the wall has been set.
            return false;
          }
        }
      }

      return true;
    }

    @Override public float score() {
      float scoreFromRoomConnections = 0;
      // If the rooms that will be connected are already connected, reduce the score.
      PVec1 connectionScore = ownerRoomGen.connectedRoomConnectionSizes.get(otherRoomGen);
      if (connectionScore != null) {
        scoreFromRoomConnections = -connectionScore.x() * 113.8f;
      }
      return DOOR_HEIGHT_WEIGHTS[h - 1] + DOOR_WIDTH_WEIGHTS[w - 1] + scoreFromRoomConnections + 5 * stableRandom;
    }

    /** Finalizes this possible door as an actual door. */
    public LasertagDoorGen toDoorGen() {
      LasertagDoorGen ret = new LasertagDoorGen(ownerRoomGen, otherRoomGen);
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
              ownerRoomGen.buildingGen.tileGens.get(ret.door.tileX + testX * ownerWall.xChangeForAlongWall,
                                                    ret.door.tileY + testY,
                                                    ret.door.tileZ + testX * ownerWall.zChangeForAlongWall);
          LasertagTileWallGen wallGen = lookTile.wallGen(ownerWall.facing);
          wallGen.wall.hasDoorframeR = testX == 0;
          wallGen.wall.hasDoorframeL = testX == w - 1;
          wallGen.wall.hasDoorframeT = testY == h - 1;
          wallGen.wall.isValid = true;
          wallGen.wall.door = ret.door;
        }
      }

      ownerRoomGen.buildingGen.doorGens.add(ret);
      ownerRoomGen.connectedRoomConnectionSizes.genPooled(otherRoomGen).add(w); // Add the width to the connected room
      // sizes, mostly to reduce the likelihood of another door spawning connecting the two rooms.
      otherRoomGen.connectedRoomConnectionSizes.genPooled(ownerRoomGen).add(w);
      return ret;
    }
  }
}
