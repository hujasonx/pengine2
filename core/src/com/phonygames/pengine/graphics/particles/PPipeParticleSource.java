package com.phonygames.pengine.graphics.particles;

import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import java.nio.Buffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PPipeParticleSource implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  // 3 Position, 2 UV, 4 color.
  public static final int MAX_FLOATS = 9 * 1024 * 4;
  public static final int MAX_SHORTS = 3 * 1024 * 4;
  private static final String PARTICLES = "particles";
  private static final PPool<PPipeParticleSource> staticPool = new PPool<PPipeParticleSource>() {
    @Override protected PPipeParticleSource newObject() {
      return new PPipeParticleSource();
    }
  };
  private static int maxCapacity = 1000;
  private static Boolean modelGenStarted = false;
  private final short[] indices = new short[MAX_SHORTS];
  private final PList<PPipeParticle> particles = new PList<>(PPipeParticle.staticPool());
  private final PTexture texture = new PTexture();
  private final float[] vertices = new float[MAX_FLOATS];
  // The index to insert the next particle into the indices buffer at.
  private int currentBufferIndicesIndex = 0;
  // The index to insert the next particle into the vertices buffer at.
  private int currentBufferVerticesIndex = 0;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PPipeParticle.Delegate delegate;
  private PGlNode glNode;
  private PMaterial material;
  private PMesh mesh;
  private PModel model;
  private PModelInstance modelInstance;

  public PPipeParticleSource() {
  }

  public static PPipeParticleSource obtain() {
    PPipeParticleSource ret = staticPool.obtain();
    return ret;
  }

  public void clear() {
    mesh.getIndicesBuffer().position(0);
    mesh.getIndicesBuffer().limit(0);
  }

  public void frameUpdate() {
    int checkIndex = 0;
    while (checkIndex < particles.size()) {
      PPipeParticle particle = particles.get(checkIndex);
      if (!particle.isLive) {
        // Remove dead particles.
        particles.removeIndex(checkIndex);
        continue;
      }
      checkIndex++;
    }
  }

  public void render(PRenderContext renderContext) {
    ensureModelInit();
    if (modelInstance == null) {
      return;
    }
    if (particles.size() == 0) {
      return;
    }
    particles.sort();
    currentBufferVerticesIndex = 0;
    currentBufferIndicesIndex = 0;
    texture.set(particles.get(0).texture().getBackingTexture());
    material.setTexWithUniform(PMaterial.UniformConstants.Sampler2D.u_diffuseTex, texture);
    for (int a = 0; a < particles.size(); a++) {
      PPipeParticle p = particles.get(a);
      p.frameUpdateIfNeeded(delegate);
      if (!setVerticesForParticle(p)) {
        continue;
      }
      currentBufferVerticesIndex += p.verticesFloatCount();
      currentBufferIndicesIndex += p.indicesShortCount();
      System.out.println(a + ", " + p.pos());
    }
    if (currentBufferVerticesIndex == 0 || currentBufferIndicesIndex == 0) {
      return;
    }
    mesh.setVertices(vertices, 0, currentBufferVerticesIndex);
    mesh.setIndices(indices, 0, currentBufferIndicesIndex);
    ((Buffer) mesh.getIndicesBuffer()).position(0);
    ((Buffer) mesh.getIndicesBuffer()).limit(currentBufferIndicesIndex);
    modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
  }

  protected void ensureModelInit() {
    makeModelIfNeeded();
  }

  /** Should be called by render() */
  private boolean setVerticesForParticle(PPipeParticle particle) {
    int floatsPerV = PVertexAttributes.getGLTF_UNSKINNED().getNumFloatsPerVertex();
    return particle.outputVertexAndIndexData(vertices, currentBufferVerticesIndex, currentBufferVerticesIndex / floatsPerV, indices, currentBufferIndicesIndex);
  }

  private void makeModelIfNeeded() {
    if (modelInstance != null) {
      return;
    }
    mesh = new PMesh(false, MAX_FLOATS, MAX_SHORTS, PVertexAttributes.getGLTF_UNSKINNED());
    //    mesh.setIndices(indices);
    ModelBuilder modelBuilder = new ModelBuilder();
    modelBuilder.begin();
    PList<PGlNode> glNodes = new PList<>();
    PModel.Builder builder = new PModel.Builder();
    builder.chainGlNode(glNodes, PARTICLES, mesh, new PMaterial(PARTICLES, null).noModelTransform(true), null,
                        PGltf.Layer.PBR, false, false);
    glNode = glNodes.get(0);
    builder.addNode("particles", null, glNodes, PMat4.IDT);
    modelInstance = PModelInstance.obtain(builder.build());
    material = modelInstance.material(PARTICLES);
  }

  @Override public void reset() {
    texture.reset();
    delegate = null;
    particles.clearAndFreePooled();
  }

  public PPipeParticleSource setOrigin(PVec3 vec3) {
    if (modelInstance != null) {
      modelInstance.worldTransform().setToTranslation(vec3);
    }
    return this;
  }

  public PPipeParticle spawnParticle() {
    PPipeParticle particle = particles.genPooledAndAdd();
    particle.isLive = true;
    return particle;
  }
}
