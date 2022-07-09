package com.phonygames.pengine.util;

import android.support.annotation.NonNull;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.input.PMouse;
import com.phonygames.pengine.math.PVec3;

import java.awt.image.renderable.RenderContext;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PCharacterCameraController {
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  public static PCharacterCameraController activeCharacterCameraController;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 dir = PVec3.obtain().set(1, 0, 0), smoothDir = PVec3.obtain().set(1, 0, 0);
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 pos = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float rotateSpeed = 2;

  public interface Delegate {
    void getFirstPersonCameraPosition(PVec3 out, PVec3 dir);
  }

  private final @NonNull Delegate delegate;

  public PCharacterCameraController(@NonNull Delegate delegate) {
    this.delegate = delegate;
  }

  public void setActive() {
    activeCharacterCameraController = this;
  }

  public boolean isActive() {
    return activeCharacterCameraController == this;
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
    float yawRotateAmount = (PKeyboard.isDown(Input.Keys.Q) ? -1 : 0) + (PKeyboard.isDown(Input.Keys.E) ? 1 : 0) +
                            (PMouse.isCatched() ? PMouse.frameDx() * .35f : 0);
    float pitchRotateAmount = (PKeyboard.isDown(Input.Keys.F) ? -1 : 0) + (PKeyboard.isDown(Input.Keys.R) ? 1 : 0) +
                              (PMouse.isCatched() ? PMouse.frameDy() * -.35f : 0);
    float currentPitch = MathUtils.asin(dir().y());
    float maxPitch = MathUtils.HALF_PI - .15f;
    float minPitch = -MathUtils.HALF_PI + .15f;
    yawRotateAmount *= rotateSpeed * PEngine.uidt;
    pitchRotateAmount *= rotateSpeed * PEngine.uidt;
    if (currentPitch + pitchRotateAmount > maxPitch) {
      pitchRotateAmount = maxPitch - currentPitch;
    } else if (currentPitch + pitchRotateAmount < minPitch) {
      pitchRotateAmount = minPitch - currentPitch;
    }
    dir().rotate(flatLeft, -pitchRotateAmount);
    dir().rotate(0, -1, 0, yawRotateAmount);
    // Use lerp to smooth the camera movement.
    final float smoothFactor = 90;
    smoothDir().lerp(dir(), Math.min(1, PEngine.uidt * smoothFactor));
    delegate.getFirstPersonCameraPosition(pos(), smoothDir());
    pool.free();
  }
}
