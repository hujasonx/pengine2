package com.phonygames.pengine.lighting;

import com.badlogic.gdx.graphics.Color;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PMath;
import com.phonygames.pengine.math.PVec4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public abstract class PLight {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PVec4 color = PVec4.obtain();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 transform = PMat4.obtain();

  public static float attenuationCutoffDistance(PVec4 attenuation) {
    // cutoff is attenuation.w
    // cutoff = 1 / (ax^2 + bx + c)
    // ax^2 + bx + c = 1 / cutoff
    // ax^2 + bx + c - 1 / cutoff = 0
    // x is where the distance from the light source results in an attenuation equal to the cutoff.
    return PMath.quadraticFormulaPositive(attenuation.x(), attenuation.y(), attenuation.z() - 1f / attenuation.w());
  }

  public static void initMeshes() {
    PPointLight.initMesh();
  }

  public abstract int addInstanceData(PFloat4Texture buffer);

  public PLight setColor(PVec4 col) {
    color().set(col);
    return this;
  }

  public PLight setColor(Color col) {
    color().set(col.r, col.g, col.b, col.a);
    return this;
  }

  public PLight setColor(float r, float g, float b, float a) {
    color().set(r, g, b, a);
    return this;
  }

  public static class UniformConstants {
    public static class Vec3 {
      public static String u_lightPos = "u_lightPos";
    }

    public static class Vec4 {
      public static String u_lightColor = "u_lightColor";
    }
  }
}
