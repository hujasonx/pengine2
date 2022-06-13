package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PDeepCopyable;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

public class PGlDrawCall implements PPool.Poolable, Comparable<PGlDrawCall>, PDeepCopyable<PGlDrawCall> {
  public static final PGlDrawCall DEFAULT = new PGlDrawCall(true);
  @Getter
  private static final PPool<PGlDrawCall> staticPool = new PPool<PGlDrawCall>() {
    @Override
    protected PGlDrawCall newObject() {
      return new PGlDrawCall(false);
    }
  };
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private final PMap<String, Integer> dataBufferLookupOffsets = new PMap<String, Integer>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private final PMap<String, Integer> dataBufferLookupVecsPerInstance = new PMap<String, Integer>();
  @Getter
  private final boolean renderingDisabled;
  @Getter
  private boolean enableBlend, depthTest, depthMask;
  @Getter
  private String layer = "";
  @Getter
  private PMaterial material;
  @Getter
  private PMesh mesh;
  @Getter
  private int numInstances, dstFactor, srcFactor, dptTest, cullFace;
  @Getter
  @Setter
  private PPool ownerPool;
  @Getter
  private PShader shader;
  @Getter
  private PVec3 worldLoc;
  private PGlDrawCall(boolean renderingDisabled) {
    reset();
    this.renderingDisabled = renderingDisabled;
  }

  public static PGlDrawCall genTemplate() {
    PGlDrawCall drawCall = new PGlDrawCall(true);
    return drawCall;
  }

  public static PGlDrawCall getTemp(PGlDrawCall template) {
    if (template != null) {
      return staticPool.obtain().deepCopyFrom(template);
    }
    return staticPool.obtain();
  }

  @Override
  public int compareTo(PGlDrawCall other) {
    return 0;
  }

  @Override
  public PGlDrawCall deepCopy() {
    return new PGlDrawCall(renderingDisabled).deepCopyFrom(this);
  }

  @Override
  public PGlDrawCall deepCopyFrom(PGlDrawCall other) {
    material = other.material;
    mesh = other.mesh;
    enableBlend = other.enableBlend;
    depthTest = other.depthTest;
    depthMask = other.depthMask;
    srcFactor = other.srcFactor;
    dstFactor = other.dstFactor;
    dptTest = other.dptTest;
    cullFace = other.cullFace;
    numInstances = other.numInstances;
    worldLoc = other.worldLoc;
    shader = other.shader;
    layer = other.layer;
    return this;
  }

  public void freeTemp() {
    PAssert.isFalse(renderingDisabled, "Cannot freeTemp() a renderingDisabled PGlDrawCall");
    staticPool.free(this);
  }

  public void glDraw(@NonNull PRenderContext renderContext, @NonNull PShader shader, boolean freeAfterwards) {
    PAssert.isFalse(renderingDisabled, "Cannot glDraw() a renderingDisabled PGlDrawCall");
    prepRenderContext(renderContext);
    if (shader.checkValid() && mesh != null) {
      for (val e : getDataBufferLookupOffsets()) {
        renderContext.genDataBuffer(e.k())
            .applyShader(shader, e.k(), getDataBufferLookupOffsets().get(e.k()), getDataBufferLookupVecsPerInstance().get(e.k()));
      }

      if (material != null) {
        material.applyUniforms(shader);
      }
      mesh.glRenderInstanced(shader, numInstances);
    }

    if (freeAfterwards) {
      freeTemp();
    }
  }

  public void prepRenderContext(PRenderContext renderContext) {
    PAssert.isTrue(renderContext.isActive());
    renderContext.setBlending(enableBlend, srcFactor, dstFactor);
    renderContext.setDepthTest(dptTest);
    renderContext.setDepthMask(depthMask);
    renderContext.setCullFace(cullFace);
  }

  @Override
  public void reset() {
    PAssert.isFalse(renderingDisabled, "Cannot reset() a renderingDisabled PGlDrawCall");
    getDataBufferLookupOffsets().clear();
    getDataBufferLookupVecsPerInstance().clear();

    material = null;
    mesh = null;
    enableBlend = false;
    depthTest = true;
    depthMask = true;
    srcFactor = GL20.GL_SRC_ALPHA;
    dstFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
    dptTest = GL20.GL_LESS;
    cullFace = GL20.GL_BACK;
    numInstances = 0;
    worldLoc = null;
    shader = null;
    layer = null;
  }

  public PGlDrawCall setCullFace(int cullFace) {
    this.cullFace = cullFace;
    return this;
  }

  public PGlDrawCall setDataBufferInfo(PMap<String, Integer> dataBufferLookupVecsPerInstance,
                                       PMap<String, Integer> dataBufferLookupOffsets) {
    PAssert.isFalse(renderingDisabled, "Cannot setDataBufferInfo() a renderingDisabled PGlDrawCall");
    this.getDataBufferLookupVecsPerInstance().clear();
    this.getDataBufferLookupVecsPerInstance().putAll(dataBufferLookupVecsPerInstance);
    this.getDataBufferLookupOffsets().clear();
    this.getDataBufferLookupOffsets().putAll(dataBufferLookupOffsets);
    return this;
  }

  public PGlDrawCall setDepthMask(boolean depthMask) {
    this.depthMask = depthMask;
    return this;
  }

  public PGlDrawCall setDepthTest(boolean depthTest) {
    this.depthTest = depthTest;
    return this;
  }

  public PGlDrawCall setDptTest(int dptTest) {
    this.dptTest = dptTest;
    return this;
  }

  public PGlDrawCall setDstFactor(int dstFactor) {
    this.dstFactor = dstFactor;
    return this;
  }

  public PGlDrawCall setEnableBlend(boolean enableBlend) {
    this.enableBlend = enableBlend;
    return this;
  }

  public PGlDrawCall setLayer(String layer) {
    this.layer = layer != null ? layer : "";
    return this;
  }

  public PGlDrawCall setMaterial(PMaterial material) {
    this.material = material;
    return this;
  }

  public PGlDrawCall setMesh(PMesh mesh) {
    this.mesh = mesh;
    return this;
  }

  public PGlDrawCall setNumInstances(int numInstances) {
    this.numInstances = numInstances;
    return this;
  }

  public PGlDrawCall setShader(PShader shader) {
    this.shader = shader;
    return this;
  }

  public PGlDrawCall setSrcFactor(int srcFactor) {
    this.srcFactor = srcFactor;
    return this;
  }

  public PGlDrawCall setWorldLoc(PVec3 worldLoc) {
    this.worldLoc = worldLoc;
    return this;
  }
}