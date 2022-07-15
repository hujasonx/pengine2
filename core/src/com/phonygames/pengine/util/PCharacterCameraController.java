package com.phonygames.pengine.util;

import android.support.annotation.NonNull;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.input.PMouse;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PCharacterCameraController {
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  public static PCharacterCameraController activeCharacterCameraController;
  private final @NonNull
  Delegate delegate;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 dir = PVec3.obtain().set(1, 0, 0), smoothDir = PVec3.obtain().set(1, 0, 0);
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 pos = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransform = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float maxPitch = MathUtils.HALF_PI - .15f, minPitch = -MathUtils.HALF_PI + .15f;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float rotateSpeed = 2;
  private float currentPitch = 0, currentYaw = 0, goalPitch, goalYaw;
  private final PSODynamics.PSODynamics2 pitchYawSpring = PSODynamics.obtain2();

  public PCharacterCameraController(@NonNull Delegate delegate) {
    this.delegate = delegate;
    pitchYawSpring.setDynamicsParams(24, 1, 0);
  }

  public void applyToRenderContext(PRenderContext renderContext) {
    renderContext.cameraDir().set(smoothDir());
    renderContext.cameraPos().set(pos());
    renderContext.cameraUp().set(0, 1, 0);
  }

  public void frameUpdate() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 wasdFactor = pool.vec3().setZero();
    PVec3 flatLeft = pool.vec3().set(dir().z(), 0, -dir().x()).nor();
    PVec3 flatDir = pool.vec3().set(dir().x(), 0, dir().z()).nor();
    float yawRotateAmount = (PMouse.isCatched() ? PMouse.frameDx() * .35f : 0);
    float pitchRotateAmount = (PMouse.isCatched() ? PMouse.frameDy() * -.35f : 0);
    yawRotateAmount *= rotateSpeed * PEngine.uidt;
    pitchRotateAmount *= rotateSpeed * PEngine.uidt;
    if (goalPitch + pitchRotateAmount > maxPitch) {
      pitchRotateAmount = maxPitch - goalPitch;
    } else if (goalPitch + pitchRotateAmount < minPitch) {
      pitchRotateAmount = minPitch - goalPitch;
    }
    if (PKeyboard.isFrameJustDown(Input.Keys.Z)) {
      yawRotateAmount += 1;
    }
    goalPitch += pitchRotateAmount;
    goalYaw += yawRotateAmount;
    pitchYawSpring.setGoal(goalPitch, goalYaw);
    pitchYawSpring.frameUpdate();
    smoothDir().setToSpherical(pitchYawSpring.pos().y(), pitchYawSpring.pos().x(),1);
    delegate.getFirstPersonCameraPosition(pos(), smoothDir());
    PVec3 smoothLeft = pool.vec3().set(smoothDir()).crs(0, -1, 0).nor();
    PVec3 smoothUp = pool.vec3().set(smoothDir()).crs(smoothLeft).nor();
    worldTransform().set(smoothLeft, smoothUp, smoothDir(), pos());
    pool.free();
  }

  public boolean isActive() {
    return activeCharacterCameraController == this;
  }

  public void setActive() {
    activeCharacterCameraController = this;
  }

  public interface Delegate {
    void getFirstPersonCameraPosition(PVec3 out, PVec3 dir);
  }
}
