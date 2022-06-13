package com.phonygames.pengine.lighting;

import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.graphics.model.gen.PUVSphereGen;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import lombok.Getter;

public class PPointLight extends PLight implements Pool.Poolable {
  @Getter
  private static PMesh MESH;
  @Getter
  private final PVec4 attenuation = PVec4.obtain();

  public PPointLight() {
    reset();
  }

  @Override public void reset() {
    getTransform().reset();
    getColor().setZero();
    getAttenuation().set(2, 1, 1, 0.05f);
  }

  public static void assertMeshReady() {
    PAssert.isNotNull(MESH);
  }

  public static void initMesh() {
    new PModelGen() {
      PModelGen.Part basePart;

      @Override protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getPOSITION());
      }

      @Override protected void modelMiddle() {
        PUVSphereGen.getShared().setSetNormals(false);
        PUVSphereGen.getShared().genSphere(20, 20, PVec3.ZERO, 1, basePart);
      }

      @Override protected void modelEnd() {
        MESH = basePart.getMesh();
      }
    }.buildSynchronous();
  }

  @Override public boolean addInstanceData(PFloat4Texture buffer) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 translation = pool.vec3();
    getTransform().getTranslation(translation);
    // Set the transform for the mesh.
    PMat4 transformOutMat = pool.mat4().setToTranslation(translation);
    float scale = attenuationCutoffDistance(getAttenuation()) * 1.1f;
    transformOutMat.scl(scale, scale, scale);
    // 0: Transform.
    buffer.addData(transformOutMat);
    // 4: Position.
    buffer.addData(translation, 1);
    // 5: Color.
    buffer.addData(getColor());
    // 6. Attenuation.
    buffer.addData(getAttenuation());
    pool.finish();
    // Total: 7;
    return true;
  }

  @Override public int vecsPerInstance() {
    return 7;
  }
}
