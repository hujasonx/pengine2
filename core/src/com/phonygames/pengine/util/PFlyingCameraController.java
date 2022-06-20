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
  private float speed = 3, shiftSpeedFactor = 3, ySpeedFactor = 0, rotateSpeed = 1;

  public PFlyingCameraController(@NonNull PRenderContext renderContext) {
    this.renderContext = renderContext;
  }

  public void frameUpdate() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    float rawSpeed = speed * (PKeyboard.isDown(Input.Keys.SHIFT_LEFT) ? shiftSpeedFactor : 1) * PEngine.uidt;
    PVec3 rawDir = pool.vec3().setZero();
    PVec3 cameraFlatLeft = pool.vec3().set(renderContext.cameraDir().z(), 0, -renderContext.cameraDir().x()).nor();
    PVec3 cameraFlatDir = pool.vec3().set(renderContext.cameraDir().x(), 0, renderContext.cameraDir().z()).nor();
    PVec3 cameraPos = pool.vec3().set(renderContext.cameraPos());
    boolean move = false;
    if (PKeyboard.isDown(Input.Keys.W)) {
      rawDir.z(1);
      move = true;
    }
    if (PKeyboard.isDown(Input.Keys.S)) {
      rawDir.z(-1);
      move = true;
    }
    if (PKeyboard.isDown(Input.Keys.A)) {
      rawDir.x(1);
      move = true;
    }
    if (PKeyboard.isDown(Input.Keys.D)) {
      rawDir.x(-1);
      move = true;
    }
    if (move) {
      rawDir.nor().scl(rawSpeed);
      cameraPos.add(cameraFlatDir, rawDir.z()).add(cameraFlatLeft, rawDir.x());
    }
    cameraPos.add(0f, PKeyboard.isDown(Input.Keys.SPACE) ? rawSpeed :
                      (PKeyboard.isDown(Input.Keys.CONTROL_LEFT) ? -rawSpeed : 0), 0f);
    float yawRotateAmount = (PKeyboard.isDown(Input.Keys.Q) ? -1 : 0) + (PKeyboard.isDown(Input.Keys.E) ? 1 : 0) +
                            (PMouse.isCatched() ? PMouse.frameDx() * .35f : 0);
    float pitchRotateAmount = (PKeyboard.isDown(Input.Keys.F) ? -1 : 0) + (PKeyboard.isDown(Input.Keys.R) ? 1 : 0) +
                              (PMouse.isCatched() ? PMouse.frameDy() * -.35f : 0);
    float currentPitch = MathUtils.asin(renderContext.cameraDir().y());
    float maxPitch = MathUtils.HALF_PI - .15f;
    float minPitch = -MathUtils.HALF_PI + .15f;
    yawRotateAmount *= rotateSpeed * PEngine.uidt;
    pitchRotateAmount *= rotateSpeed * PEngine.uidt;
    if (currentPitch + pitchRotateAmount > maxPitch) {
      pitchRotateAmount = maxPitch - currentPitch;
    } else if (currentPitch + pitchRotateAmount < minPitch) {
      pitchRotateAmount = minPitch - currentPitch;
    }
    renderContext.cameraDir().rotate(cameraFlatLeft, -pitchRotateAmount);
    renderContext.cameraDir().rotate(0, -1, 0, yawRotateAmount);
    renderContext.cameraPos().set(cameraPos);
    renderContext.cameraUp().set(0, 1, 0);
    pool.finish();
  }
}
