package com.phonygames.pengine.util;

import android.support.annotation.NonNull;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.input.PMouse;
import com.phonygames.pengine.math.PVec3;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PFlyingCameraController {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 dir = PVec3.obtain(), pos = PVec3.obtain();
  private final PRenderContext renderContext;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float speed = 4, shiftSpeedFactor = 5, ySpeedFactor = 0, rotateSpeed = 2;

  public PFlyingCameraController(@NonNull PRenderContext renderContext) {
    this.renderContext = renderContext;
    dir().set(renderContext.cameraDir());
    pos().set(renderContext.cameraPos());
  }

  public void frameUpdate() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    float rawSpeed = speed * (PKeyboard.isDown(Input.Keys.SHIFT_LEFT) ? shiftSpeedFactor : 1) * PEngine.uidt;
    PVec3 wasdFactor = pool.vec3().setZero();
    PVec3 flatLeft = pool.vec3().set(dir().z(), 0, -dir().x()).nor();
    PVec3 flatDir = pool.vec3().set(dir().x(), 0, dir().z()).nor();
    PVec3 cameraPos = pool.vec3().set(pos());
    boolean move = false;
    if (PKeyboard.isDown(Input.Keys.W)) {
      wasdFactor.z(1);
      move = true;
    }
    if (PKeyboard.isDown(Input.Keys.S)) {
      wasdFactor.z(-1);
      move = true;
    }
    if (PKeyboard.isDown(Input.Keys.A)) {
      wasdFactor.x(1);
      move = true;
    }
    if (PKeyboard.isDown(Input.Keys.D)) {
      wasdFactor.x(-1);
      move = true;
    }
    if (move) {
      wasdFactor.nor().scl(rawSpeed);
      cameraPos.add(flatDir, wasdFactor.z()).add(flatLeft, wasdFactor.x());
    }
    cameraPos.add(0f, PKeyboard.isDown(Input.Keys.SPACE) ? rawSpeed :
                      (PKeyboard.isDown(Input.Keys.CONTROL_LEFT) ? -rawSpeed : 0), 0f);
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
    pos().set(cameraPos);
    // Use lerp to smooth the camera movement.
    final float smoothFactor = 30;
    renderContext.cameraPos().lerp(pos(), Math.min(1, PEngine.uidt * smoothFactor));
    renderContext.cameraDir().lerp(dir(), Math.min(1, PEngine.uidt * smoothFactor));
    renderContext.cameraUp().set(0, 1, 0);
    pool.finish();
  }
}
