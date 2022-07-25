package com.phonygames.pengine.graphics.particles;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.PVec;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PSortableByScore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public abstract class PParticle implements PPool.Poolable, PSortableByScore<PParticle> {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PVec3 pos = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected float radius, lifeT;
  protected boolean isLive = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int userInt;

  protected PParticle() {}

  @Override public void reset() {
    radius = 0;
    pos.setZero();
    isLive = false;
    userInt = 0;
    lifeT = 0;
  }

  public void kill() {
    PAssert.isTrue(isLive);
    isLive = false;
  }

  @Override public float score() {
    PVec3 tempPosDiff = PVec3.obtain().set(pos).sub( PRenderContext.activeContext().cameraPos());
    float ret = -tempPosDiff.dot(PRenderContext.activeContext().cameraDir());
    tempPosDiff.free();
    return ret;
  }
}

