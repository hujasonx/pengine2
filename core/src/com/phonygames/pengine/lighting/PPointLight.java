package com.phonygames.pengine.lighting;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.graphics.model.gen.PUVSphereGen;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;

import lombok.Getter;

public class PPointLight extends PLight {
  @Getter
  private static PMesh MESH;

  public PPointLight() {
    super();
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
        PUVSphereGen.getShared().genSphere(10, 10, PVec3.ZERO, 1, basePart);
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
  public void addInstanceData(PFloat4Texture buffer) {
    buffer.addData(transform);

  }

  @Override
  public int vecsPerInstance() {
    return 4;
  }
}
