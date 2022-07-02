package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;
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
  public boolean hasDoorframeL, hasDoorframeR, hasDoorframeT;
  public boolean isWindow, isSolidWall;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected boolean hasWall, isValid;
  private String wallStyle;

  protected LasertagTileWall(@NonNull Facing facing, @NonNull LasertagTile tile) {
    this.facing = facing;
    this.tile = tile;
  }

  public enum Facing {
    X, Z, mX, mZ;

    public Facing opposite() {
      switch (this) {
        case X:
          return mX;
        case Z:
          return mZ;
        case mX:
          return X;
        case mZ:
        default:
          return Z;
      }
    }
  }
}