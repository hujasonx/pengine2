package com.phonygames.pengine.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;

public class PFlyingCameraController {
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float speed = 4, shiftSpeedFactor = 2, ySpeedFactor = 0;

  public void update() {

  }
}
