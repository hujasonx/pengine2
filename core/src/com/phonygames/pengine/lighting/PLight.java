package com.phonygames.pengine.lighting;

import com.badlogic.gdx.graphics.Color;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec4;

import lombok.Getter;
import lombok.Setter;

public class PLight {
  public static class UniformConstants {
    public static class Vec4 {
      public static String u_lightColor = "u_lightColor";
    }


  }

  public static void initMeshes() {
    PPointLight.initMesh();
  }

  @Getter
  protected final PVec4 color = new PVec4();
  @Getter
  protected final PMat4 transform = new PMat4();

  protected void applyUniforms(PShader shader) {
    shader.set(UniformConstants.Vec4.u_lightColor, color);
  }

  public PLight setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
    return this;
  }

  public PLight setColor(PVec4 col) {
    color.set(col);
    return this;
  }

  public PLight setColor(Color col) {
    color.set(col.r, col.g, col.b, col.a);
    return this;
  }
}
