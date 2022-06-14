package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureBinder;
import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.val;

public class PRenderContext {
  private static final int DATA_BUFFER_CAPACITY = 512 * 512;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PList<PGlDrawCall>> glDrawCallListPool = new PPool<PList<PGlDrawCall>>() {
    @Override protected PList<PGlDrawCall> newObject() {
      return new PList<>();
    }
  };
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PMap<PShader, PList<PGlDrawCall>>> glDrawCallListMapPool =
      new PPool<PMap<PShader, PList<PGlDrawCall>>>() {
        @Override protected PMap<PShader, PList<PGlDrawCall>> newObject() {
          return new PMap<>(glDrawCallListPool());
        }
      };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static PRenderContext activeContext = null;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final RenderContext backingRenderContext =
      new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PFloat4Texture boneTransformsBuffer = PFloat4Texture.getTemp(DATA_BUFFER_CAPACITY);
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 cameraDir = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec1 cameraFov = PVec1.obtain().set(60);
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 cameraPos = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec2 cameraRange = PVec2.obtain().set(1, 100);
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 cameraUp = PVec3.obtain();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PFloat4Texture> dataBuffers = new PStringMap<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PMap<PShader, PList<PGlDrawCall>>> enqueuedDrawCalls =
      new PStringMap<>(glDrawCallListMapPool());
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final OrthographicCamera orthographicCamera = new OrthographicCamera();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final ArrayMap<String, PhaseHandler> phaseHandlers = new ArrayMap<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<Integer> storedBufferOffsets = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<Integer> storedVecsPerInstance = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 viewProjInvTraTransform = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 viewProjInvTransform = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 viewProjTransform = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int width = 1, height = 1;

  public PRenderContext disableDepthTest() {
    PAssert.isTrue(isActive());
    backingRenderContext().setDepthTest(0);
    return this;
  }

  public boolean isActive() {
    return activeContext() == this;
  }

  public PRenderContext enableDepthTest() {
    PAssert.isTrue(isActive());
    backingRenderContext().setDepthTest(GL20.GL_LESS);
    return this;
  }

  public void end() {
    PAssert.isTrue(isActive());
    backingRenderContext().end();
    clear();
    activeContext = null;
  }

  private void clear() {
    for (val e : dataBuffers()) {
      e.v().freeTemp();
    }
    boneTransformsBuffer().reset();
    dataBuffers().clearRecursive();
    storedVecsPerInstance().clearRecursive();
    storedBufferOffsets().clearRecursive();
    enqueuedDrawCalls().clearRecursive();
  }

  public boolean enqueue(PShaderProvider shaderProvider, PGlDrawCall drawCall, int boneTransformsLookupOffset,
                         int boneTransformsVecsPerInstance) {
    // First, try to generate the shader.
    if (drawCall.shader() == null && drawCall.mesh() != null) {
      if (phaseHandlers().size == 0) {
        PAssert.fail("No shader was set, yet there were no phases for the MapShaderProvider to create a shader with");
      } else {
        for (val e : phaseHandlers()) {
          val layer = e.key;
          val phaseHandler = e.value;
          if (drawCall.layer().equals(layer) && drawCall.mesh().vertexAttributes() != null) {
            drawCall.setShader(shaderProvider.provide(phaseHandler.renderBuffer().fragmentLayout(), layer,
                                                      drawCall.mesh().vertexAttributes(), drawCall.material()));
            break;
          }
        }
      }
    }
    // Next, enqueue the draw call if it has a shader.
    if (drawCall.shader() != null) {
      enqueue(drawCall.shader(), drawCall.layer(), drawCall, boneTransformsLookupOffset, boneTransformsVecsPerInstance);
      return true;
    }
    return false;
  }

  /**
   * @param shader
   * @param layer
   * @param drawCall
   */
  public void enqueue(@NonNull PShader shader, @NonNull String layer, @NonNull PGlDrawCall drawCall,
                      int boneTransformsLookupOffset, int boneTransformsVecsPerInstance) {
    // Calculate the stored vecs per instance using the buffer offsets and the buffer fill amounts. This assumes that
    // you enqueue a draw call immediately after writing stuff to buffers.
    for (val e : storedBufferOffsets()) {
      val buffer = genDataBuffer(e.k());
      int vecsWrittenToThisBuffer = buffer.vecsWritten() - storedBufferOffsets().get(e.k());
      int vecsWrittenPerInstance = vecsWrittenToThisBuffer / Math.max(1, drawCall.numInstances());
      storedVecsPerInstance().put(e.k(), vecsWrittenPerInstance);
    }
    enqueuedDrawCalls().genPooled(layer).genPooled(shader).add(
        drawCall.setDataBufferInfo(storedVecsPerInstance(), storedBufferOffsets(), boneTransformsLookupOffset,
                                   boneTransformsVecsPerInstance));
    snapshotBufferOffsets();
  }

  /**
   * Returns (either an existing or by creating a new) data buffer with the given name.
   * @param name
   * @return
   */
  public PFloat4Texture genDataBuffer(String name) {
    if (dataBuffers().has(name)) {
      return dataBuffers().get(name);
    }
    PFloat4Texture dataBuffer = PFloat4Texture.getTemp(DATA_BUFFER_CAPACITY);
    dataBuffers().put(name, dataBuffer);
    storedVecsPerInstance().put(name, 0);
    storedBufferOffsets().put(name, 0);
    return dataBuffer;
  }

  /**
   * Stores the buffer sizes. Call this before filling the buffers and rendering with stuff!
   */
  private void snapshotBufferOffsets() {
    for (val e : dataBuffers()) {
      storedBufferOffsets().put(e.k(), e.v().vecsWritten());
    }
  }

  /**
   * Returns the active PRenderBuffer.
   * @return
   */
  public PRenderBuffer getBuffer() {
    return PRenderBuffer.activeBuffer();
  }

  public TextureBinder getTextureBinder() {
    return backingRenderContext().textureBinder;
  }

  /**
   * Render all drawcalls in the queue by using the phaseHandlers.
   */
  public void glRenderQueue() {
    for (val e1 : phaseHandlers()) {
      String key = e1.key;
      PhaseHandler phaseHandler = e1.value;
      if (phaseHandler.renderBuffer() != null) {
        phaseHandler.renderBuffer().begin();
      }
      phaseHandler.begin();
      val queueMap = enqueuedDrawCalls().get(phaseHandler.layer());
      if (queueMap != null) {
        for (val e : queueMap) {
          val shader = e.k();
          shader.start(this);
          val queue = queueMap.get(shader);
          queue.sort();
          for (PGlDrawCall drawCall : queue) {
            drawCall.glDraw(this, shader, true);
          }
          shader.end();
        }
      }
      phaseHandler.end();
      if (phaseHandler.renderBuffer() != null) {
        phaseHandler.renderBuffer().end();
      }
    }
  }

  public PRenderContext setBlending(final boolean enabled, final int sFactor, final int dFactor) {
    PAssert.isTrue(isActive());
    backingRenderContext().setBlending(enabled, sFactor, dFactor);
    return this;
  }

  public PRenderContext setCullFaceBack() {
    setCullFace(GL20.GL_BACK);
    return this;
  }

  public PRenderContext setCullFace(final int face) {
    PAssert.isTrue(isActive());
    backingRenderContext().setCullFace(face);
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

  public PRenderContext setCullFaceFront() {
    setCullFace(GL20.GL_BACK);
    return this;
  }

  public PRenderContext setDepthMask(final boolean depthMask) {
    PAssert.isTrue(isActive());
    backingRenderContext().setDepthMask(depthMask);
    return this;
  }

  public PRenderContext setDepthTest(final int depthFunction) {
    PAssert.isTrue(isActive());
    backingRenderContext().setDepthTest(depthFunction);
    return this;
  }

  public void setForRenderBuffer(PRenderBuffer renderBuffer) {
    width = renderBuffer.width();
    height = renderBuffer.height();
    orthographicCamera().setToOrtho(true, width, height);
    updatePerspectiveCamera();
  }

  public PRenderContext updatePerspectiveCamera() {
    PAssert.isNotNull(PRenderBuffer.activeBuffer(),
                      "currentRenderBuffer should be set, as the camera viewport needs to be set.");
    return putIntoBackingCamera(perspectiveCamera()).setFromBackingCamera(perspectiveCamera(), true);
  }

  public PRenderContext setFromBackingCamera(PerspectiveCamera camera, boolean stableSize) {
    if (!stableSize) {
      width = (int) camera.viewportWidth;
      height = (int) camera.viewportHeight;
    }
    cameraPos().set(camera.position);
    cameraDir().set(camera.direction);
    cameraUp().set(camera.up);
    cameraFov().set(camera.fieldOfView);
    cameraRange().set(camera.near, camera.far);
    viewProjTransform().set(camera.combined.val);
    viewProjInvTransform().set(camera.invProjectionView.val);
    viewProjInvTraTransform().set(camera.invProjectionView.val).tra();
    return this;
  }

  public PRenderContext putIntoBackingCamera(PerspectiveCamera camera) {
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    cameraPos().putInto(camera.position);
    cameraDir().putInto(camera.direction);
    camera.direction.nor();
    cameraUp().putInto(camera.up);
    camera.normalizeUp();
    camera.near = cameraRange().x();
    camera.far = cameraRange().y();
    camera.fieldOfView = cameraFov().x();
    camera.update(true);
    return this;
  }

  public PRenderContext setFromBackingCamera(PerspectiveCamera camera) {
    return setFromBackingCamera(camera, false);
  }

  public PRenderContext setPhaseHandlersTo(PhaseHandler[] phaseHandlers) {
    clearPhaseHandlers();
    for (val p : phaseHandlers) {
      this.phaseHandlers().put(p.layer(), p);
    }
    return this;
  }

  public PRenderContext clearPhaseHandlers() {
    phaseHandlers().clear();
    return this;
  }

  public void start() {
    PAssert.isNull(activeContext);
    activeContext = this;
    resetDefaults();
    backingRenderContext().begin();
  }

  public void resetDefaults() {
    PGlDrawCall.DEFAULT().prepRenderContext(this);
  }

  public int storeDataBufferOffset(String name) {
    return storedBufferOffsets().get(name);
  }

  public int vecsWrittenToDataBuffer(String name) {
    return dataBuffers().get(name).vecsWritten();
  }

  public interface DataBufferEmitter {
    void emitDataBuffersInto(PRenderContext renderContext);
  }

  public abstract static class PhaseHandler {
    @Getter(value = AccessLevel.PROTECTED)
    @Accessors(fluent = true)
    protected final String layer;
    @Getter(value = AccessLevel.MODULE)
    @Accessors(fluent = true)
    protected final PRenderBuffer renderBuffer;

    public PhaseHandler(@NonNull String layer, PRenderBuffer renderBuffer) {
      this.layer = layer;
      this.renderBuffer = renderBuffer;
    }

    public abstract void begin();
    public abstract void end();
  }

  public static class UniformConstants {
    public static class Float {}

    public static class Mat4 {
      public final static String u_viewProjTransform = "u_viewProjTransform";
      public final static String u_viewProjTransformInvTra = "u_viewProjTransformInvTra";
    }

    public static class Sampler2d {}

    public static class Vec2 {}

    public static class Vec3 {
      public final static String u_cameraDir = "u_cameraDir";
      public final static String u_cameraPos = "u_cameraPos";
      public final static String u_cameraUp = "u_cameraUp";
    }

    public static class Vec4 {
      public final static String u_renderBufferSize = "u_renderBufferSize";
      public final static String u_tdtuituidt = "u_tdtuituidt";
    }
  }
}
