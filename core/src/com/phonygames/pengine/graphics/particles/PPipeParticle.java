package com.phonygames.pengine.graphics.particles;

import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PPipeParticle extends PParticle {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static final PPool<PPipeParticle> staticPool = new PPool<PPipeParticle>() {
    @Override protected PPipeParticle newObject() {
      return new PPipeParticle();
    }
  };

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
  private final PTexture texture = new PTexture();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final float[] userData = new float[16];

  public PPipeParticle setColFrom0() {
    col1.set(col0);
    col2.set(col0);
    col3.set(col0);
    return this;
  }

  private PPipeParticle() {
    reset();
  }

  @Override public void reset() {
    super.reset();
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
    col0.set(PVec4.ONE);
    col1.set(PVec4.ONE);
    col2.set(PVec4.ONE);
    col3.set(PVec4.ONE);
    Arrays.fill(userData,0f);
    texture.reset();
  }
}
