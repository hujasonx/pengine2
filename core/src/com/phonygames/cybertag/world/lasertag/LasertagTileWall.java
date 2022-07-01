package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class LasertagTileWall {
  public final Facing facing;
  public final LasertagTile tile;
  /** If this tile wall is or is part of a door, this field will hold a reference to that door. */
  public @Nullable
  LasertagDoor door;
  public boolean hasLeftDoorframe, hasRightDoorframe, hasDoorframeTop;
  public boolean isWindow, isSolidWall;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected boolean valid;
  private String wallStyle;

  protected LasertagTileWall(Facing facing, LasertagTile tile) {
    this.facing = facing;
    this.tile = tile;
  }

  public enum Facing {
    X, Z, mX, mZ;
  }
}