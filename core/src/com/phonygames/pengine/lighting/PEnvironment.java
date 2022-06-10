package com.phonygames.pengine.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.util.PSet;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PEnvironment {
  public static class UniformConstants {
    public static class Sampler2D {
      public static final String u_diffuseMTex = "u_diffuseMTex";
      public static final String u_emissiveRTex = "u_emissiveRTex";
      public static final String u_normalITex = "u_normalITex";
    }
  }

  @Getter
  @Setter
  private PShader pointLightShader;

  private String fragmentLayoutString = "";

  public PEnvironment() {
  }

  private final PSet<PPointLight> pointLights = new PSet<>();

  public void renderLights(PRenderContext renderContext, Texture diffuseMTex, Texture emissiveRTex, Texture normalITex) {
    if (!fragmentLayoutString.equals(PRenderBuffer.getActiveBuffer().getFragmentLayout())) {
      if (fragmentLayoutString.length() > 0) {
        PLog.w("Making new shader, as fragment layout changed [\n" + fragmentLayoutString + "\n -> \n" + PRenderBuffer.getActiveBuffer().getFragmentLayout() + "]");
      }

      fragmentLayoutString = PRenderBuffer.getActiveBuffer().getFragmentLayout();

      // Generate new shaders.
      pointLightShader = new PShader("// POINTLIGHT", fragmentLayoutString, PPointLight.getMESH().getVertexAttributes(), Gdx.files.local("engine/shader/light/light.vert.glsl"),
                                     Gdx.files.local("engine/shader/light/pointlight.frag.glsl"));
    }

    pointLightShader.start();
    renderContext.applyUniforms(pointLightShader);
    setLightUniforms(pointLightShader, diffuseMTex, emissiveRTex, normalITex);
    for (val pointLight : pointLights) {
      pointLightShader.set(PGlNode.UniformConstants.Mat4.u_worldTransform, pointLight.getTransform());
    }
    pointLightShader.end();
  }

  private void setLightUniforms(PShader shader, Texture diffuseMTex, Texture emissiveRTex, Texture normalITex) {
    shader.set(UniformConstants.Sampler2D.u_diffuseMTex, diffuseMTex);
    shader.set(UniformConstants.Sampler2D.u_emissiveRTex, emissiveRTex);
    shader.set(UniformConstants.Sampler2D.u_normalITex, normalITex);
  }
}
