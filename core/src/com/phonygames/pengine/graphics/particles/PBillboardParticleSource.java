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
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.PPool;

import java.nio.Buffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PBillboardParticleSource implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  // 4 x 3 Position, 4 x 2 UV, 4 x 4 color. Only one color per billboard.
  public static final int FLOATS_PER_PARTICLE = 12 + 8 + 16;
  private static final String PARTICLES = "particles";
  private static final PPool<PBillboardParticleSource> staticPool = new PPool<PBillboardParticleSource>() {
    @Override protected PBillboardParticleSource newObject() {
      return new PBillboardParticleSource();
    }
  };
  private static int maxCapacity = 1000;
  private final PList<PBillboardParticle> particles = new PList<>(PBillboardParticle.staticPool());
  private final PTexture texture = new PTexture();
  private final float[] vertices;
  // The index to insert the next particle into the vertices buffer at.
  private int currentFloatBufferIndex = 0;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PBillboardParticle.Delegate delegate;
  private PGlNode glNode;
  private PMaterial material;
  private PMesh mesh;
  private PModel model;
  private PModelInstance modelInstance;

  public PBillboardParticleSource() {
    vertices = new float[FLOATS_PER_PARTICLE * maxCapacity];
  }

  public static PBillboardParticleSource obtain() {
    PBillboardParticleSource ret = staticPool.obtain();
    ;
    return ret;
  }

  public void clear() {
    mesh.getIndicesBuffer().position(0);
    mesh.getIndicesBuffer().limit(0);
  }

  public void frameUpdate() {
    int checkIndex = 0;
    while (checkIndex < particles.size()) {
      PBillboardParticle particle = particles.get(checkIndex);
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
    currentFloatBufferIndex = 0;
    texture.set(particles.get(0).texture().getBackingTexture());
    material.setTexWithUniform(PMaterial.UniformConstants.Sampler2D.u_diffuseTex, texture);
    int maxIndex = 0;
    for (int a = 0; a < particles.size(); a++) {
      PBillboardParticle p = particles.get(a);
      p.frameUpdateIfNeeded(delegate);
      if (!setVerticesForParticle(p)) {
        break;
      }
      maxIndex += 6;
    }
    if (currentFloatBufferIndex == 0) {
      return;
    }
    mesh.setVertices(vertices, 0, currentFloatBufferIndex);
    ((Buffer) mesh.getIndicesBuffer()).position(0);
    ((Buffer) mesh.getIndicesBuffer()).limit(maxIndex);
    modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
  }

  protected void ensureModelInit() {
    makeModelIfNeeded();
  }

  /** Should be called by render() */
  private boolean setVerticesForParticle(PBillboardParticle particle) {
    if (currentFloatBufferIndex + FLOATS_PER_PARTICLE >= vertices.length) {
      return false;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      if (particle.faceCamera()) {
        PVec3 cameraLeft =
            pool.vec3(PRenderContext.activeContext().cameraUp()).crs(PRenderContext.activeContext().cameraDir()).nor();
        // The particle xAxis and yAxis are 2d, so x increases towards the right.
        particle.xAxis().set(cameraLeft).scl(-1)
                .rotate(PRenderContext.activeContext().cameraDir(), particle.faceCameraAngle()).nor()
                .scl(particle.faceCameraXScale());
        particle.yAxis().set(particle.xAxis()).crs(PRenderContext.activeContext().cameraDir()).nor()
                .scl(particle.faceCameraYScale());
      }
      float u0 = particle.texture().uvOS().x();
      float v0 = particle.texture().uvOS().y();
      float u1 = particle.texture().uvOS().z() + particle.texture().uvOS().x();
      float v1 = particle.texture().uvOS().w() + particle.texture().uvOS().y();
      // Positions.
      PVec3 pos = pool.vec3(particle.pos).add(particle.xAxis(), -.5f).add(particle.yAxis(), -.5f);
      vertices[currentFloatBufferIndex + 0] = pos.x(); // x0;
      vertices[currentFloatBufferIndex + 1] = pos.y(); // y0;
      vertices[currentFloatBufferIndex + 2] = pos.z(); // z0;
      vertices[currentFloatBufferIndex + 3] = u0; // u0;
      vertices[currentFloatBufferIndex + 4] = v0; // v0;
      vertices[currentFloatBufferIndex + 5] = particle.col0().r(); // c0;
      vertices[currentFloatBufferIndex + 6] = particle.col0().g(); // c0;
      vertices[currentFloatBufferIndex + 7] = particle.col0().b(); // c0;
      vertices[currentFloatBufferIndex + 8] = particle.col0().a(); // c0;
      pos = pool.vec3(particle.pos).add(particle.xAxis(), -.5f).add(particle.yAxis(), .5f);
      vertices[currentFloatBufferIndex + 9] = pos.x(); // x1;
      vertices[currentFloatBufferIndex + 10] = pos.y(); // y1;
      vertices[currentFloatBufferIndex + 11] = pos.z(); // z1;
      vertices[currentFloatBufferIndex + 12] = u0; // u0;
      vertices[currentFloatBufferIndex + 13] = v1; // v1;
      vertices[currentFloatBufferIndex + 14] = particle.col1().r(); // c1;
      vertices[currentFloatBufferIndex + 15] = particle.col1().g(); // c1;
      vertices[currentFloatBufferIndex + 16] = particle.col1().b(); // c1;
      vertices[currentFloatBufferIndex + 17] = particle.col1().a(); // c1;
      pos = pool.vec3(particle.pos).add(particle.xAxis(), .5f).add(particle.yAxis(), .5f);
      vertices[currentFloatBufferIndex + 18] = pos.x(); // x2;
      vertices[currentFloatBufferIndex + 19] = pos.y(); // y2;
      vertices[currentFloatBufferIndex + 20] = pos.z(); // z2;
      vertices[currentFloatBufferIndex + 21] = u1; // u1;
      vertices[currentFloatBufferIndex + 22] = v1; // v1;
      vertices[currentFloatBufferIndex + 23] = particle.col2().r(); // c2;
      vertices[currentFloatBufferIndex + 24] = particle.col2().g(); // c2;
      vertices[currentFloatBufferIndex + 25] = particle.col2().b(); // c2;
      vertices[currentFloatBufferIndex + 26] = particle.col2().a(); // c2;
      pos = pool.vec3(particle.pos).add(particle.xAxis(), .5f).add(particle.yAxis(), -.5f);
      vertices[currentFloatBufferIndex + 27] = pos.x(); // x3;
      vertices[currentFloatBufferIndex + 28] = pos.y(); // y3;
      vertices[currentFloatBufferIndex + 29] = pos.z(); // z3;
      vertices[currentFloatBufferIndex + 30] = u1; // u1;
      vertices[currentFloatBufferIndex + 31] = v0; // v0;
      vertices[currentFloatBufferIndex + 32] = particle.col3().r(); // c3;
      vertices[currentFloatBufferIndex + 33] = particle.col3().g(); // c3;
      vertices[currentFloatBufferIndex + 34] = particle.col3().b(); // c3;
      vertices[currentFloatBufferIndex + 35] = particle.col3().a(); // c3;
      currentFloatBufferIndex += 36;
    }
    return true;
  }

  private void makeModelIfNeeded() {
    if (modelInstance != null) {
      return;
    }
    mesh = new PMesh(false, maxCapacity * 4, maxCapacity * 6, PVertexAttributes.getBILLBOARD_PARTICLE());
    int len = maxCapacity * 6;
    short[] indices = new short[len];
    short j = 0;
    for (int i = 0; i < len; i += 6, j += 4) {
      indices[i + 0] = (short) (j + 0);
      indices[i + 1] = (short) (j + 2);
      indices[i + 2] = (short) (j + 1);
      indices[i + 3] = (short) (j + 2);
      indices[i + 4] = (short) (j + 0);
      indices[i + 5] = (short) (j + 3);
    }
    mesh.setIndices(indices);
    ModelBuilder modelBuilder = new ModelBuilder();
    modelBuilder.begin();
    PList<PGlNode> glNodes = new PList<>();
    PModel.Builder builder = new PModel.Builder();
    builder.chainGlNode(glNodes, PARTICLES, mesh, new PMaterial(PARTICLES, null).noModelTransform(true), null,
                        PGltf.Layer.AlphaBlend, false, true);
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

  public PBillboardParticleSource setOrigin(PVec3 vec3) {
    if (modelInstance != null) {
      modelInstance.worldTransform().setToTranslation(vec3);
    }
    return this;
  }

  public PBillboardParticle spawnParticle() {
    PBillboardParticle particle = particles.genPooledAndAdd();
    particle.isLive = true;
    return particle;
  }
}
