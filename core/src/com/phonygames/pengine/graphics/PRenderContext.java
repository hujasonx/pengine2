package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureBinder;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import java.util.Collections;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
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
      public final static String u_renderBufferSize = "u_renderBufferSize";
      public final static String u_tdtuituidt = "u_tdtuituidt";
    }

    public static class Sampler2d {
    }

    public static class Float {
    }

    public static class Mat4 {
      public final static String u_viewProjTransform = "u_viewProjTransform";
      public final static String u_viewProjTransformInvTra = "u_viewProjTransformInvTra";
    }
  }

  private static Pool<DrawCall> drawCallPool = new Pool<DrawCall>() {
    @Override
    protected DrawCall newObject() {
      return new DrawCall();
    }
  };
  private static DrawCall DEFAULT_DRAWCALL = new DrawCall();

  @Getter
  @Setter
  private PhaseHandler[] phaseHandlers;

  @Getter
  private static PRenderContext activeContext = null;
  private final RenderContext backingRenderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));

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

  private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
  private final OrthographicCamera orthographicCamera = new OrthographicCamera();

  @Getter
  private int width = 1, height = 1;

  private PMap<String, PMap<PShader, PList<DrawCall>>> enqueuedDrawCalls = new PMap<String, PMap<PShader, PList<DrawCall>>>() {
    @Override
    protected Object makeNew(String o) {
      return new PMap<PShader, PList<DrawCall>>() {
        @Override
        protected Object makeNew(PShader o) {
          return new PList<DrawCall>();
        }
      };
    }
  };

  public PRenderContext updatePerspectiveCamera() {
    PAssert.isNotNull(PRenderBuffer.getActiveBuffer(), "currentRenderBuffer should be set, as the camera viewport needs to be set.");
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
    viewProjInvTraTransform.set(camera.invProjectionView.val).tra();
    return this;
  }

  public void resetDefaults() {
    DEFAULT_DRAWCALL.applyToContext(this);
  }

  public void start() {
    PAssert.isNull(activeContext);
    activeContext = this;
    DEFAULT_DRAWCALL.applyToContext(this);
    backingRenderContext.begin();
  }

  public void setForRenderBuffer(PRenderBuffer renderBuffer) {
    width = renderBuffer.width();
    height = renderBuffer.height();

    orthographicCamera.setToOrtho(true, width, height);

    updatePerspectiveCamera();
  }

  public void end() {
    PAssert.isTrue(isActive());
    backingRenderContext.end();
    for (String phase : enqueuedDrawCalls.keySet()) {
      clearQueueMap(enqueuedDrawCalls.get(phase));
    }
    activeContext = null;
  }

  public abstract static class PhaseHandler {
    protected final String phase;
    @Getter
    protected final PRenderBuffer renderBuffer;

    public PhaseHandler(String phase, PRenderBuffer renderBuffer) {
      this.phase = phase;
      this.renderBuffer = renderBuffer;
    }

    public abstract void begin();

    public abstract void end();
  }

  public void emit() {
    if (phaseHandlers == null) {
      return;
    }

    for (PhaseHandler phaseHandler : phaseHandlers) {
      if (phaseHandler.renderBuffer != null) {
        phaseHandler.renderBuffer.begin();
      }
      phaseHandler.begin();
      val queueMap = enqueuedDrawCalls.get(phaseHandler.phase);

      if (queueMap != null) {
        for (PShader shader : queueMap.keySet()) {
          shader.start(this);;

          val queue = queueMap.get(shader);
          Collections.sort(queue);
          for (DrawCall drawCall : queue) {
            drawCall.renderGl(this, shader);
          }
          shader.end();
        }
      }

      phaseHandler.end();
      if (phaseHandler.renderBuffer != null) {
        phaseHandler.renderBuffer.end();
      }
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

  public void enqueue(PShader shader, String layer, PGlNode node) {
    enqueuedDrawCalls.getOrMake(layer).getOrMake(shader).add(drawCallPool.obtain().set(node));
  }

  public void enqueue(PRenderContext.PhaseHandler[] phaseHandlers, PShaderProvider shaderProvider, PGlNode node) {
    PAssert.isTrue(PRenderContext.getActiveContext() != null);

    if (node.getDefaultShader() == null) {
      for (int a = 0; a < phaseHandlers.length; a++) {
        if (node.getMaterial().getLayer().equals(phaseHandlers[a].phase)) {
          node.setDefaultShader(shaderProvider.provide(phaseHandlers[a].getRenderBuffer().getFragmentLayout(), node));
        }
      }
    }

    PShader defaultShader = node.getDefaultShader();

    if (defaultShader != null) {
      enqueue(defaultShader, node.getMaterial().getLayer(), node);
    }
  }

  public boolean isActive() {
    return activeContext == this;
  }

  public static class DrawCall implements Pool.Poolable, Comparable<DrawCall> {
    PGlNode glNode;
    final PVec3 worldLoc = new PVec3();
    float distToCamera;
    int dstFactor, srcFactor, dptTest, cullFace;
    boolean enableBlend, depthTest, depthMask;
    int numInstances;

    private void applyToContext(PRenderContext renderContext) {
      renderContext.setBlending(enableBlend, srcFactor, dstFactor);
      renderContext.setDepthTest(dptTest);
      renderContext.setDepthMask(depthMask);
      renderContext.setCullFace(cullFace);
    }

    private void renderGl(PRenderContext renderContext, PShader shader) {
      applyToContext(renderContext);
      glNode.renderGl(shader);
    }

    DrawCall set(PGlNode node) {
      this.glNode = node;
      return this;
    }

    DrawCall() {
      reset();
    }

    @Override
    public void reset() {
      glNode = null;
      worldLoc.reset();
      distToCamera = -1;
      enableBlend = false;
      depthTest = true;
      depthMask = true;
      srcFactor = GL20.GL_SRC_ALPHA;
      dstFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
      dptTest = GL20.GL_LESS;
      cullFace = GL20.GL_BACK;
      numInstances = 1;
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

  public PRenderContext setCullFaceFront() {
    setCullFace(GL20.GL_BACK);
    return this;
  }

  public PRenderContext setCullFaceBack() {
    setCullFace(GL20.GL_BACK);
    return this;
  }

  public PRenderContext setCullFaceBoth() {
    setCullFace(GL20.GL_FRONT_AND_BACK);
    return this;
  }

  public PRenderContext setCullFaceDisabled() {
    setCullFace(-1);
    return this;
  }

  public PRenderContext setCullFace(final int face) {
    PAssert.isTrue(isActive());
    backingRenderContext.setCullFace(face);
    return this;
  }

  public PRenderContext setDepthMask(final boolean depthMask) {
    PAssert.isTrue(isActive());
    backingRenderContext.setDepthMask(depthMask);
    return this;
  }

  public PRenderContext setDepthTest(final int depthFunction) {
    PAssert.isTrue(isActive());
    backingRenderContext.setDepthTest(depthFunction);
    return this;
  }

  public PRenderContext enableDepthTest() {
    PAssert.isTrue(isActive());
    backingRenderContext.setDepthTest(GL20.GL_LESS);
    return this;
  }

  public PRenderContext disableDepthTest() {
    PAssert.isTrue(isActive());
    backingRenderContext.setDepthTest(0);
    return this;
  }

  public PRenderContext setBlending(final boolean enabled, final int sFactor, final int dFactor) {
    PAssert.isTrue(isActive());
    backingRenderContext.setBlending(enabled, sFactor, dFactor);
    return this;
  }

  public TextureBinder getTextureBinder() {
    return backingRenderContext.textureBinder;
  }

}
