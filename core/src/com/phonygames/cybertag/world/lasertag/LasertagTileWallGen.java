package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;

import com.phonygames.pengine.util.PBuilder;

public class LasertagTileWallGen extends PBuilder {
  protected final LasertagTileGen tileGen;
  protected final LasertagTileWall wall;
  protected LasertagRoomWallGen roomWallGen, otherRoomWallGen;
  /** If set, then doors will not spawn that connect directly to this tileGen. Doesn't affect doors created by hallways. */
  protected boolean preventWallDoorSpawns = false;

  protected LasertagTileWallGen(LasertagTileWall.Facing facing, @NonNull LasertagTileGen tileGen) {
    this.tileGen = tileGen;
    wall = new LasertagTileWall(facing, tileGen.tile);
  }

  public LasertagTileWall build() {
    lockBuilder();
    return wall;
  }

  /** If this wallgen is not valid, try to copy from the opposite. */
  public void copySettingsFromOrToOtherWall(boolean defaultUseSolidWall) {
    if (!wall.hasWall) {return;}
    LasertagTileGen otherTileGen = otherRoomWallGen == null ? null :
                                   LasertagRoomGenTileProcessor.otherTileForWall(otherRoomWallGen.roomGen.buildingGen,
                                                                                 tileGen, wall.facing);
    if (!wall.isValid) {
      if (otherTileGen != null) {
        otherTileGen.wallGen(wall.facing.opposite()).copySettingsToOtherWall(defaultUseSolidWall);
        return;
      }
    }
    copySettingsToOtherWall(defaultUseSolidWall);
  }

  /** Copies settings like window, doorframe, etc that need to be shared, from the other wall. */
  public void copySettingsToOtherWall(boolean defaultUseSolidWall) {
    if (!wall.hasWall) {return;}
    if (!wall.isValid) {
      // The wall properties were not already set, so set it from the defaults.
      if (defaultUseSolidWall) {
        wall.isSolidWall = true;
      }
      wall.isValid = true;
    }
    // Now, copy the data.
    if (otherRoomWallGen == null) {return;}
    LasertagTileGen otherTileGen =
        LasertagRoomGenTileProcessor.otherTileForWall(otherRoomWallGen.roomGen.buildingGen, tileGen, wall.facing);
    if (otherTileGen == null) {return;}
    LasertagTileWallGen other = otherTileGen.wallGen(wall.facing.opposite());
    if (!other.wall.hasWall || other.wall.isValid) {return;}
    other.wall.isValid = true;
    other.wall.hasDoorframeR = wall.hasDoorframeL;
    other.wall.hasDoorframeL = wall.hasDoorframeR;
    other.wall.hasDoorframeT = wall.hasDoorframeT;
    other.wall.isSolidWall = wall.isSolidWall;
    other.wall.isWindow = wall.isWindow;
    other.wall.door = wall.door;
  }
}