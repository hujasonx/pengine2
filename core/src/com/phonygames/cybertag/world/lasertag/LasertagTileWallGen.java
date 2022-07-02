package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.util.PBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class LasertagTileWallGen extends PBuilder {
  protected final LasertagTileWall wall;
  protected LasertagRoomWallGen roomWallGen;

  public LasertagTileWall build() {
    lockBuilder();
    return wall;
  }

  protected LasertagTileWallGen(LasertagTileWall.Facing facing, @NonNull LasertagTileGen tilegen) {
    wall = new LasertagTileWall(facing, tilegen.tile);
  }
}