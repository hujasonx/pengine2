package com.phonygames.pengine.graphics.particles;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PSortableByScore;

import java.util.Arrays;

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
  private final PVec3 vel = PVec3.obtain(), accel = PVec3.obtain();
  protected boolean isLive = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected float radius, lifeT;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float accelVelocityDir;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int userInt;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final float[] userData = new float[16];
  /** Particle frameupdate should be called automatically when needed by the render functions, since we want to make
   * sure particles that were just added are still processed.
   */
  private int lastFrameUpdateFrame = -1;

  /** Returns true if we needed to frameupdate, and marks the particle as updated for this frame. */
  public boolean prepFrameUpdate() {
    if (lastFrameUpdateFrame == PEngine.frameCount) {
      return false;
    }
    lastFrameUpdateFrame = PEngine.frameCount;
    return true;
  }


  protected PParticle() {}

  protected boolean frameUpdateSharedIfNeeded() {
    if (!isLive) {return false;}
    if (!prepFrameUpdate()) {return false;}
    pos().add(vel(), PEngine.dt);
    float newVelMagInVelDir = Math.max(0, vel().len() + PEngine.dt * (accelVelocityDir()));
    vel().nor().scl(newVelMagInVelDir);
    vel().add(accel(), PEngine.dt);
    lifeT += PEngine.dt;
    return true;
  }

  public void kill() {
    PAssert.isTrue(isLive);
    isLive = false;
  }

  @Override public void reset() {
    radius = 0;
    pos.setZero();
    isLive = false;
    userInt = 0;
    lifeT = 0;
    accelVelocityDir = 0;
    vel.setZero();
    accel.setZero();
    Arrays.fill(userData, 0f);
  }

  @Override public float score() {
    PVec3 tempPosDiff = PVec3.obtain().set(pos).sub(PRenderContext.activeContext().cameraPos());
    float ret = -tempPosDiff.dot(PRenderContext.activeContext().cameraDir());
    tempPosDiff.free();
    return ret;
  }
}

