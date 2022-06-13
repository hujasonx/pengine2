package com.phonygames.pengine.lighting;

import com.badlogic.gdx.graphics.Color;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PMath;
import com.phonygames.pengine.math.PVec4;

import lombok.Getter;

public abstract class PLight {
  @Getter(lazy = true)
  private final PVec4 color = PVec4.obtain();
  @Getter(lazy = true)
  private final PMat4 transform = PMat4.obtain();

  public static float attenuationCutoffDistance(PVec4 attenuation) {
    // cutoff is attenuation.w
    // cutoff = 1 / (ax^2 + bx + c)
    // ax^2 + bx + c = 1 / cutoffC
    // ax^2 + bx + c - 1 / cutoff = 0
    return PMath.quadraticFormulaPositive(attenuation.x(), attenuation.y(), attenuation.z() - 1 / attenuation.w());
  }

  public static void initMeshes() {
    PPointLight.initMesh();
  }

  public abstract boolean addInstanceData(PFloat4Texture buffer);

  public PLight setColor(PVec4 col) {
    getColor().set(col);
    return this;
  }

  public PLight setColor(Color col) {
    getColor().set(col.r, col.g, col.b, col.a);
    return this;
  }

  public PLight setColor(float r, float g, float b, float a) {
    getColor().set(r, g, b, a);
    return this;
  }

  public abstract int vecsPerInstance();

  public static class UniformConstants {
    public static class Vec3 {
      public static String u_lightPos = "u_lightPos";
    }

    public static class Vec4 {
      public static String u_lightColor = "u_lightColor";
    }
  }
}
