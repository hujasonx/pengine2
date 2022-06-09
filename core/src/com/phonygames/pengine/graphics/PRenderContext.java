package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import java.util.Collections;

import lombok.Getter;
import lombok.val;

public class PRenderContext {
  public static class UniformConstants {

    public static class Vec2 {
    }

    public static class Vec3 {
      public final static String u_cameraPos = "u_cameraPos";
      public final static String u_cameraDir = "u_cameraDir";
      public final static String u_cameraUp = "u_cameraUp";
    }
    public static class Vec4 {
      public final static String u_tdtuituidt = "u_tdtuituidt";
    }

    public static class Sampler2d {
    }

    public static class Float {
    }

    public static class Mat4 {
      public final static String u_viewProjTransform = "u_viewProjTransform";
      public final static String u_viewProjInvTraTransform = "u_viewProjInvTraTransform";
    }
  }

  private static Pool<DrawCall> drawCallPool = new Pool<DrawCall>() {
    @Override
    protected DrawCall newObject() {
      return new DrawCall();
    }
  };

  @Getter
  private static PRenderContext activeContext = null;

  @Getter
  private final PVec3 cameraPos = new PVec3();
  @Getter
  private final PVec3 cameraDir = new PVec3();
  @Getter
  private final PVec3 cameraUp = new PVec3();
  @Getter
  private final PVec2 cameraRange = new PVec2().set(1, 100);
  @Getter
  private final PVec1 cameraFov = new PVec1().set(60);
  @Getter
  private final PMat4 viewProjTransform = new PMat4();
  @Getter
  private final PMat4 viewProjInvTraTransform = new PMat4();

  @Getter
  private PRenderBuffer currentRenderBuffer;

  private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
  private final OrthographicCamera orthographicCamera = new OrthographicCamera();

  @Getter
  private int width = 1, height = 1;

  private PMap<PShader, PList<DrawCall>> enqueuedDrawCalls = new PMap() {
    @Override
    protected Object makeNew(Object o) {
      return new PList<DrawCall>();
    }
  };

  private PMap<PShader, PList<DrawCall>> enqueuedAlphaBlendDrawCalls = new PMap() {
    @Override
    protected Object makeNew(Object o) {
      return new PList<DrawCall>();
    }
  };

  public PRenderContext updatePerspectiveCamera() {
    PAssert.isNotNull(currentRenderBuffer, "currentRenderBuffer should be set, as the camera viewport needs to be set.");
    return putIntoBackingCamera(perspectiveCamera).setFromBackingCamera(perspectiveCamera, true);
  }

  public PRenderContext putIntoBackingCamera(PerspectiveCamera camera) {
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    cameraPos.putInto(camera.position);
    cameraDir.putInto(camera.direction);
    camera.direction.nor();
    cameraUp.putInto(camera.up);
    camera.normalizeUp();
    camera.near = cameraRange.x();
    camera.far = cameraRange.y();
    camera.fieldOfView = cameraFov.x();

    camera.update(true);
    return this;
  }

  public PRenderContext setFromBackingCamera(PerspectiveCamera camera) {
    return setFromBackingCamera(camera, false);
  }

  public PRenderContext setFromBackingCamera(PerspectiveCamera camera, boolean stableSize) {
    if (!stableSize) {
      width = (int) camera.viewportWidth;
      height = (int) camera.viewportHeight;
    }
    cameraPos.set(camera.position);
    cameraDir.set(camera.direction);
    cameraUp.set(camera.up);
    cameraFov.set(camera.fieldOfView);
    cameraRange.set(camera.near, camera.far);
    viewProjTransform.set(camera.combined.val);
    viewProjInvTraTransform.set(camera.invProjectionView.val);
    return this;
  }

  public void start() {
    PAssert.isNull(activeContext);
    activeContext = this;
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
  }

  public void setRenderBuffer(PRenderBuffer renderBuffer) {
    this.currentRenderBuffer = renderBuffer;
    width = renderBuffer.width();
    height = renderBuffer.height();

    orthographicCamera.setToOrtho(true, width, height);

    updatePerspectiveCamera();
  }

  public void end() {
    currentRenderBuffer = null;
    clearQueueMap(enqueuedDrawCalls);
    clearQueueMap(enqueuedAlphaBlendDrawCalls);
    PAssert.isTrue(activeContext == this);
    activeContext = null;
  }

  public void emit() {
    emit(enqueuedDrawCalls);
  }

  private void emit(PMap<PShader, PList<DrawCall>> queueMap) {
    for (PShader shader : queueMap.keySet()) {
      shader.start();
      shader.set(UniformConstants.Vec4.u_tdtuituidt, PEngine.t, PEngine.dt, PEngine.uit, PEngine.uidt);
      shader.set(UniformConstants.Mat4.u_viewProjTransform, viewProjTransform);
      shader.set(UniformConstants.Mat4.u_viewProjInvTraTransform, viewProjInvTraTransform);
      shader.set(UniformConstants.Vec3.u_cameraPos, cameraPos);
      shader.set(UniformConstants.Vec3.u_cameraDir, cameraDir);
      shader.set(UniformConstants.Vec3.u_cameraUp, cameraUp);

      val queue = queueMap.get(shader);
      Collections.sort(queue);
      for (DrawCall drawCall : queue) {
        drawCall.renderGl(shader);
      }
      shader.end();
    }
  }

  private void clearQueueMap(PMap<PShader, PList<DrawCall>> queueMap) {
    for (PShader shader : queueMap.keySet()) {
      val queue = queueMap.get(shader);
      for (DrawCall drawCall : queue) {
        drawCallPool.free(drawCall);
      }
      queue.clear();
    }
  }

  public void enqueue(PShader shader, PGlNode node) {
    if (node.isUseAlphaBlend()) {
      enqueuedAlphaBlendDrawCalls.getOrMake(shader).add(drawCallPool.obtain().set(node));
    } else {
      enqueuedDrawCalls.getOrMake(shader).add(drawCallPool.obtain().set(node));
    }
  }

  public boolean isActive() {
    return activeContext == this;
  }

  private static class DrawCall implements Pool.Poolable, Comparable<DrawCall> {
    PGlNode glNode;
    final PVec3 worldLoc = new PVec3();
    float distToCamera;

    private void renderGl(PShader shader) {
      glNode.renderGl(shader);
    }

    DrawCall set(PGlNode node) {
      this.glNode = node;
      return this;
    }

    @Override
    public void reset() {
      glNode = null;
      distToCamera = -1;
    }

    @Override
    public int compareTo(DrawCall other) {
      if (distToCamera < other.distToCamera) {
        return 1;
      }
      if (distToCamera > other.distToCamera) {
        return -1;
      }
      return 0;
    }
  }
}
