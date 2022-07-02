package com.phonygames.cybertag.world.lasertag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class LasertagDoor {
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  protected LasertagTileWall.Facing facing;
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  protected int w, h, tileX, tileY, tileZ;
}
