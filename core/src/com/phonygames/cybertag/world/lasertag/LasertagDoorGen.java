package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.util.PBuilder;

public class LasertagDoorGen extends PBuilder {
  protected final LasertagDoor door;

  protected LasertagDoorGen() {
    door = new LasertagDoor();
  }

  public LasertagDoor build() {
    lockBuilder();
    return door;
  }
}
