package com.phonygames.pengine.graphics;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.GL20;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PDeepCopyable;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PGlDrawCall implements PPool.Poolable, Comparable<PGlDrawCall>, PDeepCopyable<PGlDrawCall> {
  // #pragma mark - PWriteLockable
  // #pragma end - PWriteLockable
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PGlDrawCall DEFAULT = new PGlDrawCall(true);
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PGlDrawCall> staticPool = new PPool<PGlDrawCall>() {
    // #pragma mark - PWriteLockable
    // #pragma end - PWriteLockable
    // #pragma mark - PPool.Poolable
    // #pragma end - PPool.Poolable

    @Override protected PGlDrawCall newObject() {
      return new PGlDrawCall(false);
    }
  };
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMap<String, PInt> dataBufferLookupOffsets = new PMap<String, PInt>(PInt.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMap<String, PInt> dataBufferLookupVecsPerInstance = new PMap<String, PInt>(PInt.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 origin = PVec3.obtain(), bounds = PVec3.obtain();
  @Getter
  @Accessors(fluent = true)
  private final boolean renderingDisabled;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean enableBlend, depthTest, depthMask;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private String layer = "";
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PMaterial material;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PMesh mesh;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int numInstances, dstFactor, srcFactor, dptTest, cullFace, boneTransformsLookupOffset,
      boneTransformsVecsPerInstance;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float occludeRadius = 0;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PShader shader;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean strictDepthOrder = false, neverCull = false;

  private PGlDrawCall(boolean renderingDisabled) {
    reset();
    this.renderingDisabled = renderingDisabled;
  }

  @Override public void reset() {
    PAssert.isFalse(renderingDisabled, "Cannot reset() a renderingDisabled PGlDrawCall");
    dataBufferLookupOffsets().clearRecursive();
    dataBufferLookupVecsPerInstance().clearRecursive();
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
    origin().setZero();
    strictDepthOrder = false;
    neverCull = false;
    occludeRadius = 0;
    shader = null;
    layer = null;
    boneTransformsLookupOffset = 0;
    boneTransformsVecsPerInstance = 0;
  }

  @NonNull public static PGlDrawCall genNewTemplate() {
    PGlDrawCall drawCall = new PGlDrawCall(true);
    return drawCall;
  }

  @NonNull public static PGlDrawCall getTemp(@Nullable PGlDrawCall template) {
    if (template != null) {
      return staticPool().obtain().deepCopyFrom(template);
    }
    return staticPool().obtain();
  }

  @Override public int compareTo(PGlDrawCall other) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
//            PDebugRenderer.line(origin(), 0, 0, origin(), 0, 10, PVec4.ONE, PVec4.ONE, 4, 4);
      //      PVec3 p000 = pool.vec3(origin()).add(-occludeRadius, -occludeRadius, -occludeRadius);
      //      PVec3 p100 = pool.vec3(origin()).add(occludeRadius, -occludeRadius, -occludeRadius);
      //      PVec3 p001 = pool.vec3(origin()).add(-occludeRadius, -occludeRadius, occludeRadius);
      //      PVec3 p101 = pool.vec3(origin()).add(occludeRadius, -occludeRadius, occludeRadius);
      //      PVec3 p010 = pool.vec3(origin()).add(-occludeRadius, occludeRadius, -occludeRadius);
      //      PVec3 p110 = pool.vec3(origin()).add(occludeRadius, occludeRadius, -occludeRadius);
      //      PVec3 p011 = pool.vec3(origin()).add(-occludeRadius, occludeRadius, occludeRadius);
      //      PVec3 p111 = pool.vec3(origin()).add(occludeRadius, occludeRadius, occludeRadius);
      //      PDebugRenderer.line(p000, 0, 0, p100, 0, 10, PColor.RED, PColor.RED, 1, 1);
      //      PDebugRenderer.line(p010, 0, 0, p110, 0, 10, PColor.RED, PColor.RED, 1, 1);
      //      PDebugRenderer.line(p001, 0, 0, p101, 0, 10, PColor.RED, PColor.RED, 1, 1);
      //      PDebugRenderer.line(p011, 0, 0, p111, 0, 10, PColor.RED, PColor.RED, 1, 1);
      //      PDebugRenderer.line(p000, 0, 0, p001, 0, 10, PColor.GREEN, PColor.GREEN, 1, 1);
      //      PDebugRenderer.line(p010, 0, 0, p011, 0, 10, PColor.GREEN, PColor.GREEN, 1, 1);
      //      PDebugRenderer.line(p110, 0, 0, p111, 0, 10, PColor.GREEN, PColor.GREEN, 1, 1);
      //      PDebugRenderer.line(p010, 0, 0, p011, 0, 10, PColor.GREEN, PColor.GREEN, 1, 1);
      //      PDebugRenderer.line(p000, 0, 0, p010, 0, 10, PColor.BLUE, PColor.BLUE, 1, 1);
      //      PDebugRenderer.line(p100, 0, 0, p110, 0, 10, PColor.BLUE, PColor.BLUE, 1, 1);
      //      PDebugRenderer.line(p101, 0, 0, p111, 0, 10, PColor.BLUE, PColor.BLUE, 1, 1);
      //      PDebugRenderer.line(p001, 0, 0, p011, 0, 10, PColor.BLUE, PColor.BLUE, 1, 1);
    }
    return 0;
  }

  @Override public PGlDrawCall deepCopy() {
    return new PGlDrawCall(renderingDisabled).deepCopyFrom(this);
  }

  @Override public PGlDrawCall deepCopyFrom(PGlDrawCall other) {
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
    origin().set(other.origin());
    strictDepthOrder = other.strictDepthOrder;
    neverCull = other.neverCull;
    occludeRadius = other.occludeRadius;
    shader = other.shader;
    layer = other.layer;
    boneTransformsLookupOffset = other.boneTransformsLookupOffset;
    boneTransformsVecsPerInstance = other.boneTransformsVecsPerInstance;
    return this;
  }

  public void glDraw(@NonNull PRenderContext renderContext, @NonNull PShader shader, boolean freeAfterwards) {
    PAssert.isFalse(renderingDisabled, "Cannot glDraw() a renderingDisabled PGlDrawCall");
    prepRenderContext(renderContext);
    if (shader.checkValid() && mesh != null) {
      try (val it = dataBufferLookupOffsets().obtainIterator()) {
        while (it.hasNext()) {
          val e = it.next();
          renderContext.genDataBuffer(e.k()).applyShader(shader, e.k(), dataBufferLookupOffsets().get(e.k()).valueOf(),
                                                         dataBufferLookupVecsPerInstance().get(e.k()).valueOf());
        }
      }
      if (material != null) {
        material.applyUniforms(shader);
      }
      renderContext.boneTransformsBuffer()
                   .applyShader(shader, "boneTransforms", boneTransformsLookupOffset, boneTransformsVecsPerInstance);
      mesh.glRenderInstanced(shader, numInstances);
    }
    if (freeAfterwards) {
      freeTemp();
    }
  }

  public void prepRenderContext(@NonNull PRenderContext renderContext) {
    PAssert.isTrue(renderContext.isActive());
    renderContext.setBlending(enableBlend, srcFactor, dstFactor);
    renderContext.setDepthTest(dptTest);
    renderContext.setDepthMask(depthMask);
    renderContext.setCullFace(cullFace);
  }

  public void freeTemp() {
    PAssert.isFalse(renderingDisabled, "Cannot freeTemp() a renderingDisabled PGlDrawCall");
    staticPool().free(this);
  }

  public PGlDrawCall setCullFace(int cullFace) {
    this.cullFace = cullFace;
    return this;
  }

  public PGlDrawCall setDataBufferInfo(PMap<String, PInt> dataBufferLookupVecsPerInstance,
                                       PMap<String, PInt> dataBufferLookupOffsets, int boneTransformsLookupOffset,
                                       int boneTransformsVecsPerInstance) {
    this.dataBufferLookupVecsPerInstance().clearRecursive();
    this.dataBufferLookupOffsets().clearRecursive();
    PAssert.isFalse(renderingDisabled, "Cannot setDataBufferInfo() a renderingDisabled PGlDrawCall");
    try (val it = dataBufferLookupVecsPerInstance.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        this.dataBufferLookupVecsPerInstance().genPooled(e.k()).set(e.v());
      }
    }
    try (val it = dataBufferLookupOffsets.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        this.dataBufferLookupOffsets().genPooled(e.k()).set(e.v());
      }
    }
    this.boneTransformsLookupOffset = boneTransformsLookupOffset;
    this.boneTransformsVecsPerInstance = boneTransformsVecsPerInstance;
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

  public PGlDrawCall setOrigin(PVec3 origin) {
    origin().set(origin);
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
}
