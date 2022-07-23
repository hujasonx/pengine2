package com.phonygames.pengine.graphics.particles;

import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.phonygames.pengine.PEngine;
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

public class PBillboardParticleSource {
  // 4 x 3 Position, 4 x 2 UV, 4 x 4 color. Only one color per billboard.
  public static final int FLOATS_PER_PARTICLE = 12 + 8 + 16;
  private static final String PARTICLES = "particles";
  private static int maxCapacity = 1000;
  private static Boolean modelGenStarted = false;
  private final PList<PBillboardParticle> particles = new PList<>();
  private final PTexture texture = new PTexture();
  private final float[] vertices;
  // The index to insert the next particle into the vertices buffer at.
  private int currentBufferIndex = 0;
  private PGlNode glNode;
  private PMaterial material;
  private PMesh mesh;
  private PModel model;
  private PModelInstance modelInstance;

  public PBillboardParticleSource() {
    vertices = new float[FLOATS_PER_PARTICLE * maxCapacity];
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
      particle.pos().add(particle.vel(), PEngine.dt);
      float newVelMagInVelDir = Math.max(0, particle.vel().len() + PEngine.dt * (particle.accelVelocityDir()));
      particle.vel().nor().scl(newVelMagInVelDir);
      particle.vel().add(particle.accel(), PEngine.dt);
      if (particle.faceCamera()) {
        particle.faceCameraAngle(particle.faceCameraAngle() + particle.angVel() * PEngine.dt);
      } else {
        PVec3 rotateAxis = PVec3.obtain().set(particle.xAxis()).crs(particle.yAxis());
        particle.xAxis().rotate(rotateAxis, particle.angVel() * PEngine.dt);
        particle.yAxis().rotate(rotateAxis, particle.angVel() * PEngine.dt);
        rotateAxis.free();
      }
      float newAngVel = particle.angVel() + particle.angVelAccel() * PEngine.dt;
      if (newAngVel > 0) {
        newAngVel = Math.max(0, newAngVel - particle.angVelDecel() * PEngine.dt);
      } else {
        newAngVel = Math.min(0, newAngVel + particle.angVelDecel() * PEngine.dt);
      }
      particle.angVel(newAngVel);
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
    currentBufferIndex = 0;
    texture.set(particles.get(0).texture().getBackingTexture());
    material.setTexWithUniform(PMaterial.UniformConstants.Sampler2D.u_diffuseTex, texture);
    for (int a = 0; a < particles.size(); a++) {
      PBillboardParticle p = particles.get(a);
      setVerticesForParticle(p);
    }
    if (currentBufferIndex == 0) {
      return;
    }
    mesh.setVertices(vertices, 0, currentBufferIndex);
    ((Buffer) mesh.getIndicesBuffer()).position(0);
    ((Buffer) mesh.getIndicesBuffer()).limit(particles.size() * 6);
    modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
  }

  protected void ensureModelInit() {
    makeModelIfNeeded();
  }

  /** Should be called by render() */
  private void setVerticesForParticle(PBillboardParticle particle) {
    if (currentBufferIndex + FLOATS_PER_PARTICLE >= vertices.length) {
      return;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      if (particle.faceCamera()) {
        PVec3 cameraLeft =
            pool.vec3(PRenderContext.activeContext().cameraUp()).crs(PRenderContext.activeContext().cameraDir()).nor();
        // The particle xAxis and yAxis are 2d, so x goes up towards the right.
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
      vertices[currentBufferIndex + 0] = pos.x(); // x0;
      vertices[currentBufferIndex + 1] = pos.y(); // y0;
      vertices[currentBufferIndex + 2] = pos.z(); // z0;
      vertices[currentBufferIndex + 3] = u0; // u0;
      vertices[currentBufferIndex + 4] = v0; // v0;
      vertices[currentBufferIndex + 5] = particle.col0().r(); // c0;
      vertices[currentBufferIndex + 6] = particle.col0().g(); // c0;
      vertices[currentBufferIndex + 7] = particle.col0().b(); // c0;
      vertices[currentBufferIndex + 8] = particle.col0().a(); // c0;
      pos = pool.vec3(particle.pos).add(particle.xAxis(), -.5f).add(particle.yAxis(), .5f);
      vertices[currentBufferIndex + 9] = pos.x(); // x1;
      vertices[currentBufferIndex + 10] = pos.y(); // y1;
      vertices[currentBufferIndex + 11] = pos.z(); // z1;
      vertices[currentBufferIndex + 12] = u0; // u0;
      vertices[currentBufferIndex + 13] = v1; // v1;
      vertices[currentBufferIndex + 14] = particle.col1().r(); // c1;
      vertices[currentBufferIndex + 15] = particle.col1().g(); // c1;
      vertices[currentBufferIndex + 16] = particle.col1().b(); // c1;
      vertices[currentBufferIndex + 17] = particle.col1().a(); // c1;
      pos = pool.vec3(particle.pos).add(particle.xAxis(), .5f).add(particle.yAxis(), .5f);
      vertices[currentBufferIndex + 18] = pos.x(); // x2;
      vertices[currentBufferIndex + 19] = pos.y(); // y2;
      vertices[currentBufferIndex + 20] = pos.z(); // z2;
      vertices[currentBufferIndex + 21] = u1; // u1;
      vertices[currentBufferIndex + 22] = v1; // v1;
      vertices[currentBufferIndex + 23] = particle.col2().r(); // c2;
      vertices[currentBufferIndex + 24] = particle.col2().g(); // c2;
      vertices[currentBufferIndex + 25] = particle.col2().b(); // c2;
      vertices[currentBufferIndex + 26] = particle.col2().a(); // c2;
      pos = pool.vec3(particle.pos).add(particle.xAxis(), .5f).add(particle.yAxis(), -.5f);
      vertices[currentBufferIndex + 27] = pos.x(); // x3;
      vertices[currentBufferIndex + 28] = pos.y(); // y3;
      vertices[currentBufferIndex + 29] = pos.z(); // z3;
      vertices[currentBufferIndex + 30] = u1; // u1;
      vertices[currentBufferIndex + 31] = v0; // v0;
      vertices[currentBufferIndex + 32] = particle.col3().r(); // c3;
      vertices[currentBufferIndex + 33] = particle.col3().g(); // c3;
      vertices[currentBufferIndex + 34] = particle.col3().b(); // c3;
      vertices[currentBufferIndex + 35] = particle.col3().a(); // c3;
      // Other stuff.
      //    bufferData[8] = particle.getTextureRegion().getU();
      //    bufferData[9] = particle.getTextureRegion().getU2();
      //    bufferData[10] = 1 - particle.getTextureRegion().getV2();
      //    bufferData[11] = 1 - particle.getTextureRegion().getV();
      currentBufferIndex += 36;
    }
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
    modelInstance = new PModelInstance(builder.build());
    material = modelInstance.material(PARTICLES);
  }

  public PBillboardParticleSource setOrigin(PVec3 vec3) {
    if (modelInstance != null) {
      modelInstance.worldTransform().setToTranslation(vec3);
    }
    return this;
  }

  public PBillboardParticle spawnParticle() {
    PBillboardParticle particle = PBillboardParticle.obtain();
    particles.add(particle);
    particle.isLive = true;
    return particle;
  }
}
