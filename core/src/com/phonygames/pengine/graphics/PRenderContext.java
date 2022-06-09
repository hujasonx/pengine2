package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import java.util.Collections;

import lombok.Getter;
import lombok.val;

public class PRenderContext {
  public static class UniformConstants {
    public static class Vec4 {
      public final static String u_tdtuituidt = "u_tdtuituidt";
    }

    public static class Sampler2d {
    }

    public static class Vec2 {
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
  private final PMat4 viewProjTransform = new PMat4();
  @Getter
  private final PMat4 viewProjInvTraTransform = new PMat4();

  private final OrthographicCamera camera = new OrthographicCamera();
  private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera();

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

  public PRenderContext setPerspectiveCamera() {
    cameraPos.out(camera.position);
    cameraDir.out(camera.direction);
    cameraUp.out(camera.up);
    camera.update(true);
    viewProjTransform.set(camera.combined.val);
    viewProjInvTraTransform.set(camera.invProjectionView.val);
    return this;
  }

  public void start() {
    PAssert.isNull(activeContext);
    activeContext = this;
  }

  public void end() {
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
    if (shader.isUseAlphaBlend()) {
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
