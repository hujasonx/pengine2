package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PCopyable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

public class PGlDrawCall implements Pool.Poolable, Comparable<PGlDrawCall>, PCopyable<PGlDrawCall> {
  @Getter
  private final boolean isTemplate;
  public static final PGlDrawCall DEFAULT = new PGlDrawCall(true);

  private static Pool<PGlDrawCall> staticPool = new Pool<PGlDrawCall>() {
    @Override
    protected PGlDrawCall newObject() {
      return new PGlDrawCall(false);
    }
  };

  public static PGlDrawCall getTemp(PGlDrawCall template) {
    if (template != null) {
      return staticPool.obtain().copyFrom(template);
    }
    return staticPool.obtain();
  }

  private final ArrayMap<String, Integer> dataBufferLookupVecsPerInstance;
  private final ArrayMap<String, Integer> dataBufferLookupOffsets;
  @Getter
  private PShader shader;
  @Getter
  private PMaterial material;
  @Getter
  private PMesh mesh;
  @Getter
  private int numInstances, dstFactor, srcFactor, dptTest, cullFace;
  @Getter
  private boolean enableBlend, depthTest, depthMask;
  @Getter
  private PVec3 worldLoc;
  @Getter
  private String layer = "";

  public static PGlDrawCall genTemplate() {
    PGlDrawCall drawCall = new PGlDrawCall(true);
    return drawCall;
  }

  private PGlDrawCall(boolean isTemplate) {
    reset();
    this.isTemplate = isTemplate;
    if (isTemplate) {
      dataBufferLookupVecsPerInstance = null;
      dataBufferLookupOffsets = null;
    } else {
      dataBufferLookupVecsPerInstance = new ArrayMap<>();
      dataBufferLookupOffsets = new ArrayMap<>();
    }
  }

  public PGlDrawCall setShader(PShader shader) {
    this.shader = shader;
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

  public PGlDrawCall setDstFactor(int dstFactor) {
    this.dstFactor = dstFactor;
    return this;
  }

  public PGlDrawCall setSrcFactor(int srcFactor) {
    this.srcFactor = srcFactor;
    return this;
  }

  public PGlDrawCall setDptTest(int dptTest) {
    this.dptTest = dptTest;
    return this;
  }

  public PGlDrawCall setCullFace(int cullFace) {
    this.cullFace = cullFace;
    return this;
  }

  public PGlDrawCall setEnableBlend(boolean enableBlend) {
    this.enableBlend = enableBlend;
    return this;
  }

  public PGlDrawCall setDepthTest(boolean depthTest) {
    this.depthTest = depthTest;
    return this;
  }

  public PGlDrawCall setDepthMask(boolean depthMask) {
    this.depthMask = depthMask;
    return this;
  }

  public PGlDrawCall setWorldLoc(PVec3 worldLoc) {
    this.worldLoc = worldLoc;
    return this;
  }

  public PGlDrawCall setLayer(String layer) {
    this.layer = layer != null ? layer : "";
    return this;
  }

  @Override
  public PGlDrawCall copyFrom(PGlDrawCall other) {
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

  @Override
  public void reset() {
    PAssert.isFalse(isTemplate, "Cannot reset() a template PGlDrawCall");
    if (dataBufferLookupVecsPerInstance != null) {
      dataBufferLookupVecsPerInstance.clear();
    }

    if (dataBufferLookupOffsets != null) {
      dataBufferLookupOffsets.clear();
    }

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

  public void prepRenderContext(PRenderContext renderContext) {
    PAssert.isTrue(renderContext.isActive());
    renderContext.setBlending(enableBlend, srcFactor, dstFactor);
    renderContext.setDepthTest(dptTest);
    renderContext.setDepthMask(depthMask);
    renderContext.setCullFace(cullFace);
  }

  public void glDraw(@NonNull PRenderContext renderContext, @NonNull PShader shader, boolean freeAfterwards) {
    PAssert.isFalse(isTemplate, "Cannot glDraw() a template PGlDrawCall");
    prepRenderContext(renderContext);
    if (shader.checkValid() && mesh != null) {
      for (val e : dataBufferLookupOffsets) {
        renderContext.genDataBuffer(e.key)
            .applyShader(shader, e.key, dataBufferLookupOffsets.get(e.key), dataBufferLookupVecsPerInstance.get(e.key));
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

  public void freeTemp() {
    PAssert.isFalse(isTemplate, "Cannot freeTemp() a template PGlDrawCall");
    staticPool.free(this);
  }

  @Override
  public int compareTo(PGlDrawCall other) {
    return 0;
  }

  public PGlDrawCall setDataBufferInfo(ArrayMap<String, Integer> dataBufferLookupVecsPerInstance,
                                       ArrayMap<String, Integer> dataBufferLookupOffsets) {
    PAssert.isFalse(isTemplate, "Cannot setDataBufferInfo() a template PGlDrawCall");
    this.dataBufferLookupVecsPerInstance.clear();
    this.dataBufferLookupVecsPerInstance.putAll(dataBufferLookupVecsPerInstance);
    this.dataBufferLookupOffsets.clear();
    this.dataBufferLookupOffsets.putAll(dataBufferLookupOffsets);
    return this;
  }
}
