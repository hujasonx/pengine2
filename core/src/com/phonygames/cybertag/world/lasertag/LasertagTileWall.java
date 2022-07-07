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

  public boolean isDoor() {
    return door != null;
  }

  public static final Facing[] FACINGS = new Facing[] {Facing.X, Facing.Z, Facing.mX, Facing.mZ};

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

    /**
     *
     * @return The facing that adjoins this facing if looking at a wall corner from the inside, and the caller is on
     * the left.
     */
    public Facing leftCorner() {
      switch (this) {
        case X:
          return Z;
        case Z:
          return mX;
        case mX:
          return mZ;
        case mZ:
        default:
          return X;
      }
    }

    public int normalX() {
      switch (this) {
        case X:
          return 1;
        case Z:
          return 0;
        case mX:
          return -1;
        case mZ:
        default:
          return 0;
      }
    }

    public int normalZ() {
      switch (this) {
        case X:
          return 0;
        case Z:
          return 1;
        case mX:
          return 0;
        case mZ:
        default:
          return -1;
      }
    }
  }
}