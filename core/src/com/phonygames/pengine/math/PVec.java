package com.phonygames.pengine.math;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.val;

public abstract class PVec<V extends PVec> implements Pool.Poolable {

  public abstract int numComponents();

  public float len() {
    return (float) Math.sqrt(len2());
  }

  public abstract float len2();
}
