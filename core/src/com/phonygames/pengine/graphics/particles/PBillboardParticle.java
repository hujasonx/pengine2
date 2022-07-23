package com.phonygames.pengine.graphics.particles;

import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PBillboardParticle extends PParticle {
  private static final PPool<PBillboardParticle> staticPool = new PPool<PBillboardParticle>() {
    @Override protected PBillboardParticle newObject() {
      return new PBillboardParticle();
    }
  };

  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final PVec3 xAxis = PVec3.obtain(), yAxis = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 normalAxis = PVec3.obtain();

  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean faceCamera, doubleSided;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float faceCameraAngle, faceCameraXScale, faceCameraYScale, angVel, angVelAccel, angVelDecel, accelVelocityDir;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 vel = PVec3.obtain(), accel = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec4 col0 = PVec4.obtain(), col1 = PVec4.obtain(), col2 = PVec4.obtain(), col3 = PVec4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec4 uvOS = PVec4.obtain();


  private PBillboardParticle() {}

  public static PBillboardParticle obtain() {
    PBillboardParticle ret = staticPool.obtain();
    return ret;
  }

  @Override public void reset() {
    super.reset();
    xAxis.set(PVec3.X);
    yAxis.set(PVec3.Y);
    normalAxis.set(PVec3.Z);
    faceCamera = false;
    doubleSided = false;
    angVel= 0;
    angVelAccel = 0;
    angVelDecel = 0;
    accelVelocityDir = 0;
    faceCameraAngle = 0;
    faceCameraXScale = 1;
    faceCameraYScale = 1;
    vel.setZero();
    accel.setZero();
    col0.setZero();
    col1.setZero();
    col2.setZero();
    col3.setZero();
    uvOS.setZero();
  }
}
