package com.phonygames.pengine.math;

import com.badlogic.gdx.utils.Pool;

public abstract class PVec implements Pool.Poolable {
  public abstract int numComponents();

  public float len() {
    return (float)Math.sqrt(len2());
  }

  public abstract float len2();


}
