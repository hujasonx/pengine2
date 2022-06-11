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

import lombok.Getter;

public class PPointLight extends PLight implements Pool.Poolable {
  @Getter
  private static PMesh MESH;

  private final PVec3 tempVec3 = new PVec3();
  private final PVec4 tempVec4 = new PVec4();
  private final PMat4 tempMat4 = new PMat4();
  @Getter
  private final PVec4 attenuation = new PVec4();

  public PPointLight() {
    reset();
  }

  public static void initMesh() {
    new PModelGen() {
      PModelGen.Part basePart;

      @Override
      protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getPOSITION());
      }

      @Override
      protected void modelMiddle() {
        PUVSphereGen.getShared().setSetNormals(false);
        PUVSphereGen.getShared().genSphere(20, 20, PVec3.ZERO, 1, basePart);
      }

      @Override
      protected void modelEnd() {
        MESH = basePart.getMesh();
      }
    }.buildSynchronous();
  }

  public static void assertMeshReady() {
    PAssert.isNotNull(MESH);
  }

  @Override
  public boolean addInstanceData(PFloat4Texture buffer) {
    transform.getTranslation(tempVec3);

    // Set the transform for the mesh.
    tempMat4.setToTranslation(tempVec3);
    float scale = attenuationCutoffDistance(attenuation) * 1.1f;
    tempMat4.scl(scale, scale, scale);

    // 0: Transform.
    buffer.addData(tempMat4);

    // 4: Position.
    buffer.addData(tempVec3, 1);

    // 5: Color.
    buffer.addData(color);

    // 6. Attenuation.
    buffer.addData(attenuation);

    // Total: 7;
    return true;
  }

  @Override
  public int vecsPerInstance() {
    return 7;
  }


  @Override
  public void reset() {
    transform.reset();
    color.setZero();
    attenuation.set(2, 1, 1, 0.05f);
  }
}
