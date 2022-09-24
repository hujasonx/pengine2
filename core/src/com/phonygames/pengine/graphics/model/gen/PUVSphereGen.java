package com.phonygames.pengine.graphics.model.gen;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.graphics.model.PMeshGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public abstract class PUVSphereGen implements Pool.Poolable {
  @Getter
  private static final PUVSphereGen shared = new PUVSphereGen() {
    @Override
    public void perVertex(PMeshGen meshGen, PVec3 pos, PVec3 nor, float theta, float phi, float rho, float u, float v) {
      // Noop.
    }
  };
  @Getter
  @Setter
  private boolean setNormals;

  public PUVSphereGen() {
    reset();
  }

  @Override public void reset() {
    setNormals = true;
  }

  public void genSphere(int lat, int lon, PVec3 center, float radius, PMeshGen meshGen) {
    val pool = PPool.getBuffer();
    PVec3 n00 = pool.vec3();
    PVec3 n01 = pool.vec3();
    PVec3 n11 = pool.vec3();
    PVec3 n10 = pool.vec3();
    PVec3 posTemp = pool.vec3();
    for (int v = 0; v < lat; v++) {
      for (int h = 0; h < lon; h++) {
        float p0 = (MathUtils.PI * (v + 0) / lat); // Phi.
        float p1 = (MathUtils.PI * (v + 1) / lat); // Phi.
        float t0 = (MathUtils.PI2 * (h + 0) / lon); // Theta.
        float t1 = (MathUtils.PI2 * (h + 1) / lon); // Theta.
        float u0 = ((float) h) / lon;
        float u1 = ((float) (h + 1)) / lon;
        float v0 = 1f - ((float) v) / lat;
        float v1 = 1f - ((float) (v + 1)) / lat;
        n00.sphericalYUpZForward(t0, p0, radius);
        n01.sphericalYUpZForward(t0, p1, radius);
        n11.sphericalYUpZForward(t1, p1, radius);
        n10.sphericalYUpZForward(t1, p0, radius);
        meshGen.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n00));
        if (setNormals) {
          meshGen.set(PVertexAttributes.Attribute.Keys.nor, n00.nor());
        }
        perVertex(meshGen, posTemp, n00, t0, p0, radius, u0, v0);
        meshGen.emitVertex();
        meshGen.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n01));
        if (setNormals) {
          meshGen.set(PVertexAttributes.Attribute.Keys.nor, n01.nor());
        }
        perVertex(meshGen, posTemp, n01, t0, p1, radius, u0, v1);
        meshGen.emitVertex();
        meshGen.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n11));
        if (setNormals) {
          meshGen.set(PVertexAttributes.Attribute.Keys.nor, n11.nor());
        }
        perVertex(meshGen, posTemp, n11, t1, p1, radius, u1, v1);
        meshGen.emitVertex();
        meshGen.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n10));
        if (setNormals) {
          meshGen.set(PVertexAttributes.Attribute.Keys.nor, n10.nor());
        }
        perVertex(meshGen, posTemp, n10, t1, p0, radius, u1, v0);
        meshGen.emitVertex();
        meshGen.quad(true);
      }
    }
    pool.free();
  }

  /** Override this method to emit any custom vertex data per vertex. */
  public abstract void perVertex(PMeshGen meshGen, PVec3 pos, PVec3 nor, float theta, float phi, float rho, float u,
                                 float v);
}
