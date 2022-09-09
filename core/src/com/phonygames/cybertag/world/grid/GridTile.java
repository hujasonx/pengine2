package com.phonygames.cybertag.world.grid;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder
public class GridTile {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int x, y, z;


}
