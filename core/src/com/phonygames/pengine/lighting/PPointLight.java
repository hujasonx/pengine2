package com.phonygames.pengine.lighting;

import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PMeshGen;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.model.gen.PUVSphereGen;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PPointLight extends PLight implements Pool.Poolable {
  @Getter
  @Accessors(fluent = true)
  private static PMesh MESH;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PVec4 attenuation = PVec4.obtain();

  public PPointLight() {
    reset();
  }

  @Override public void reset() {
    transform().reset();
    color().setZero();
    attenuation().set(2, 1, 1, 0.05f);
  }

  public static void assertMeshReady() {
    PAssert.isNotNull(MESH);
  }

  public static void initMesh() {
    new PModelGen() {
      PMeshGen baseMeshGen;

      @Override protected void modelIntro() {
        baseMeshGen = getOrAddOpaqueMesh("base", PVertexAttributes.getPOS());
      }

      @Override protected void modelMiddle() {
        PUVSphereGen.getShared().setSetNormals(false);
        PUVSphereGen.getShared().genSphere(20, 20, PVec3.ZERO, 1, baseMeshGen);
      }

      @Override protected void modelEnd() {
        MESH = baseMeshGen.getMesh();
      }
    }.buildSynchronous();
  }

  @Override public int addInstanceData(PFloat4Texture buffer) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 translation = pool.vec3();
    transform().getTranslation(translation);
    // Set the transform for the mesh.
    PMat4 transformOutMat = pool.mat4().setToTranslation(translation);
    float scale = attenuationCutoffDistance(attenuation()) * 1.1f;
    transformOutMat.scl(scale, scale, scale);
    // 0: Transform.
    buffer.addData(transformOutMat);
    // 4: Position.
    buffer.addData(translation, 1);
    // 5: Color.
    buffer.addData(color());
    // 6. Attenuation.
    buffer.addData(attenuation());
    pool.free();
    // Total: 7;
    return 7;
  }
}
