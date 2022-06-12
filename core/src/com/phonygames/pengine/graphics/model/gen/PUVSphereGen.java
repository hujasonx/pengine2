package com.phonygames.pengine.graphics.model.gen;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PVec3;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public abstract class PUVSphereGen implements Pool.Poolable {
  @Getter
  @Setter
  private boolean setNormals;

  public PUVSphereGen() {
    reset();
  }

  @Getter
  private static final PUVSphereGen shared = new PUVSphereGen() {
    @Override
    public void perVertex(PVec3 pos, PVec3 nor, float theta, float phi, float rho, float u, float v) {
      // Noop.
    }
  };

  public void genSphere(int lat, int lon, PVec3 center, float radius, PModelGen.Part part) {
    val vec3s = PVec3.tempBuffer();
    PVec3 n00 = vec3s.obtain();
    PVec3 n01 = vec3s.obtain();
    PVec3 n11 = vec3s.obtain();
    PVec3 n10 = vec3s.obtain();
    PVec3 posTemp = vec3s.obtain();
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
        part.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n00));
        if (setNormals) {
          part.set(PVertexAttributes.Attribute.Keys.nor, n00.nor());
        }
        perVertex(posTemp, n00, t0, p0, radius, u0, v0);
        part.emitVertex();
        part.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n01));
        if (setNormals) {
          part.set(PVertexAttributes.Attribute.Keys.nor, n01.nor());
        }
        perVertex(posTemp, n01, t0, p1, radius, u0, v1);
        part.emitVertex();
        part.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n11));
        if (setNormals) {
          part.set(PVertexAttributes.Attribute.Keys.nor, n11.nor());
        }
        perVertex(posTemp, n11, t1, p1, radius, u1, v1);
        part.emitVertex();
        part.set(PVertexAttributes.Attribute.Keys.pos, posTemp.set(center).add(n10));
        if (setNormals) {
          part.set(PVertexAttributes.Attribute.Keys.nor, n10.nor());
        }
        perVertex(posTemp, n10, t1, p0, radius, u1, v0);
        part.emitVertex();
        part.quad(true);
      }
    }
    vec3s.finish();


  }

  @Override
  public void reset() {
    setNormals = true;
  }

  public abstract void perVertex(PVec3 pos, PVec3 nor, float theta, float phi, float rho, float u, float v);
}
