package com.phonygames.pengine.graphics.particles;

import android.support.annotation.Nullable;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PBillboardParticle extends PParticle {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static final PPool<PBillboardParticle> staticPool = new PPool<PBillboardParticle>() {
    @Override protected PBillboardParticle newObject() {
      return new PBillboardParticle();
    }
  };

  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final PVec3 xAxis = PVec3.obtain(), yAxis = PVec3.obtain();

  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean faceCamera, doubleSided;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float faceCameraAngle, faceCameraXScale, faceCameraYScale, angVel, angVelAccel, angVelDecel;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec4 col0 = PVec4.obtain(), col1 = PVec4.obtain(), col2 = PVec4.obtain(), col3 = PVec4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PTexture texture = new PTexture();

  public PBillboardParticle setColFrom0() {
    col1.set(col0);
    col2.set(col0);
    col3.set(col0);
    return this;
  }

  public boolean frameUpdateIfNeeded(@Nullable Delegate delegate) {
    if (!super.frameUpdateSharedIfNeeded()) { return false;}
    if (faceCamera()) {
      faceCameraAngle(faceCameraAngle() + angVel() * PEngine.dt);
    } else {
      PVec3 rotateAxis = PVec3.obtain().set(xAxis()).crs(yAxis());
      xAxis().rotate(rotateAxis, angVel() * PEngine.dt);
      yAxis().rotate(rotateAxis, angVel() * PEngine.dt);
      rotateAxis.free();
    }
    float newAngVel = angVel() + angVelAccel() * PEngine.dt;
    if (newAngVel > 0) {
      newAngVel = Math.max(0, newAngVel - angVelDecel() * PEngine.dt);
    } else {
      newAngVel = Math.min(0, newAngVel + angVelDecel() * PEngine.dt);
    }
    angVel(newAngVel);
    if (delegate != null) {
      delegate.processBillboardParticle(this);
    }
    return true;
  }

  private PBillboardParticle() {
    reset();
  }

  @Override public void reset() {
    super.reset();
    xAxis.set(PVec3.X);
    yAxis.set(PVec3.Y);
    faceCamera = false;
    doubleSided = false;
    angVel= 0;
    angVelAccel = 0;
    angVelDecel = 0;
    faceCameraAngle = 0;
    faceCameraXScale = 1;
    faceCameraYScale = 1;
    col0.set(PVec4.ONE);
    col1.set(PVec4.ONE);
    col2.set(PVec4.ONE);
    col3.set(PVec4.ONE);
    texture.reset();
  }

  public interface Delegate {
    void processBillboardParticle(PBillboardParticle particle);
  }
}
