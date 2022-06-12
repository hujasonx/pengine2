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

import java.util.Collections;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

public class PRenderContext {
  private static final int DATA_BUFFER_CAPACITY = 512 * 512;

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

  @Getter
  private ArrayMap<String, PhaseHandler> phaseHandlers = new ArrayMap<>();

  @Getter
  private static PRenderContext activeContext = null;
  private final RenderContext backingRenderContext =
      new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));

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
  private final PMat4 viewProjInvTransform = new PMat4();
  @Getter
  private final PMat4 viewProjInvTraTransform = new PMat4();

  private final PerspectiveCamera perspectiveCamera = new PerspectiveCamera();
  private final OrthographicCamera orthographicCamera = new OrthographicCamera();

  @Getter
  private int width = 1, height = 1;

  private final ArrayMap<String, PFloat4Texture> dataBuffers = new ArrayMap<>();
  private final ArrayMap<String, Integer> storedVecsPerInstance = new ArrayMap<>();
  private final ArrayMap<String, Integer> storedBufferOffsets = new ArrayMap<>();

  private PMap<String, PMap<PShader, PList<PGlDrawCall>>> enqueuedDrawCalls =
      new PMap<String, PMap<PShader, PList<PGlDrawCall>>>() {
        @Override
        protected Object makeNewVal(String o) {
          return new PMap<PShader, PList<PGlDrawCall>>() {
            @Override
            protected Object makeNewVal(PShader o) {
              return new PList<PGlDrawCall>();
            }
          };
        }
      };

  public PRenderContext updatePerspectiveCamera() {
    PAssert.isNotNull(PRenderBuffer.getActiveBuffer(),
                      "currentRenderBuffer should be set, as the camera viewport needs to be set.");
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

  public PRenderBuffer getBuffer() {
    return PRenderBuffer.getActiveBuffer();
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
    viewProjInvTransform.set(camera.invProjectionView.val);
    viewProjInvTraTransform.set(camera.invProjectionView.val).tra();
    return this;
  }

  public void resetDefaults() {
    PGlDrawCall.DEFAULT.prepRenderContext(this);
  }

  public void start() {
    PAssert.isNull(activeContext);
    activeContext = this;
    resetDefaults();
    backingRenderContext.begin();
  }

  public void setForRenderBuffer(PRenderBuffer renderBuffer) {
    width = renderBuffer.width();
    height = renderBuffer.height();

    orthographicCamera.setToOrtho(true, width, height);

    updatePerspectiveCamera();
  }

  public PFloat4Texture genDataBuffer(String name) {
    if (dataBuffers.containsKey(name)) {
      return dataBuffers.get(name);
    }

    PFloat4Texture dataBuffer = PFloat4Texture.getTemp(DATA_BUFFER_CAPACITY);
    dataBuffers.put(name, dataBuffer);
    storedVecsPerInstance.put(name, 0);
    storedBufferOffsets.put(name, 0);
    return dataBuffer;
  }

  private void clear() {
    for (val e : dataBuffers) {
      e.value.freeTemp();
    }
    dataBuffers.clear();
    storedVecsPerInstance.clear();
    storedBufferOffsets.clear();

    for (val e : enqueuedDrawCalls) {
      e.getValue().clear();
    }

    enqueuedDrawCalls.clear();
  }

  private void snapshotBufferOffsets() {
    for (val e : dataBuffers) {
      storedBufferOffsets.put(e.key, e.value.vecsWritten());
    }
  }

  public void end() {
    PAssert.isTrue(isActive());
    backingRenderContext.end();
    clear();
    activeContext = null;
  }

  public abstract static class PhaseHandler {
    protected final String layer;
    @Getter
    protected final PRenderBuffer renderBuffer;

    public PhaseHandler(@NonNull String layer, PRenderBuffer renderBuffer) {
      this.layer = layer;
      this.renderBuffer = renderBuffer;
    }

    public abstract void begin();

    public abstract void end();
  }

  public void glRenderQueue() {
    for (val e1 : phaseHandlers) {
      String key = e1.key;
      PhaseHandler phaseHandler = e1.value;

      if (phaseHandler.renderBuffer != null) {
        phaseHandler.renderBuffer.begin();
      }
      phaseHandler.begin();
      val queueMap = enqueuedDrawCalls.get(phaseHandler.layer);

      if (queueMap != null) {
        for (val e : queueMap) {
          val shader = e.getKey();
          shader.start(this);

          val queue = queueMap.get(shader);
          Collections.sort(queue);
          for (PGlDrawCall drawCall : queue) {
            drawCall.glDraw(this, shader, true);
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

  public int vecsWrittenToDataBuffer(String name) {
    return dataBuffers.get(name).vecsWritten();
  }

  public int storeDataBufferOffset(String name) {
    return storedBufferOffsets.get(name);
  }

  public PRenderContext setVecsPerInstanceForDataBuffer(String name, int vecs) {
    storedVecsPerInstance.put(name, vecs);
    return this;
  }

  public void enqueue(@NonNull PShader shader,
                      @NonNull String layer,
                      @NonNull PGlDrawCall drawCall) {
    enqueuedDrawCalls.gen(layer).gen(shader)
        .add(drawCall.setDataBufferInfo(storedVecsPerInstance, storedBufferOffsets));
    snapshotBufferOffsets();
  }

  public PRenderContext clearPhaseHandlers() {
    phaseHandlers.clear();
    return this;
  }

  public PRenderContext setPhaseHandlers(PhaseHandler[] phaseHandlers) {
    clearPhaseHandlers();
    for (val p : phaseHandlers) {
      this.phaseHandlers.put(p.layer, p);
    }
    return this;
  }

  public boolean enqueue(PShaderProvider shaderProvider, PGlDrawCall drawCall) {
    // First, try to generate the shader.
    if (drawCall.getShader() == null && drawCall.getMesh() != null) {
      if (phaseHandlers.size == 0) {
        PAssert.fail("No shader was set, yet there were no phases for the MapShaderProvider to create a shader with");
      } else {
        for (val e : phaseHandlers) {
          val layer = e.key;
          val phaseHandler = e.value;
          if (drawCall.getLayer().equals(layer) && drawCall.getMesh().getVertexAttributes() != null) {
            drawCall.setShader(shaderProvider.provide(phaseHandler.getRenderBuffer().getFragmentLayout(),
                                                      layer,
                                                      drawCall.getMesh().getVertexAttributes(),
                                                      drawCall.getMaterial()));
          }
        }
      }
    }

    // Next, enqueue the draw call if it has a shader.
    if (drawCall.getShader() != null) {
      enqueue(drawCall.getShader(), drawCall.getLayer(), drawCall);
      return true;
    }

    return false;
  }

  public boolean isActive() {
    return activeContext == this;
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

  public interface DataBufferEmitter {
    void emitDataBuffersInto(PRenderContext renderContext);
  }
}
