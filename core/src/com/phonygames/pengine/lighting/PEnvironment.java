package com.phonygames.pengine.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.util.PSet;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PEnvironment {
  public static class UniformConstants {
    public static class Sampler2D {
      public static final String u_diffuseMTex = "u_diffuseMTex";
      public static final String u_normalRTex = "u_normalRTex";
      public static final String u_emissiveITex = "u_emissiveITex";
    }
  }

  public static class StringConstants {
    public static final String lightBuffer = "lightBuffer";
  }

  @Getter
  @Setter
  private PShader ambientAndDirectionalLightShader, pointLightShader;

  private String fragmentLayoutString = "";

  private final PFloat4Texture lightsFloatBuffer;

  public PEnvironment() {
    lightsFloatBuffer = PFloat4Texture.get(256 * 256, true);
  }

  private final PSet<PPointLight> pointLights = new PSet<>();


  public void renderLights(PRenderContext renderContext, Texture diffuseMTex, Texture normalRTex, Texture emissiveITex) {
    if (!fragmentLayoutString.equals(PRenderBuffer.getActiveBuffer().getFragmentLayout())) {
      if (fragmentLayoutString.length() > 0) {
        PLog.w("Making new shader, as fragment layout changed [\n" + fragmentLayoutString + "\n -> \n" + PRenderBuffer.getActiveBuffer().getFragmentLayout() + "]");
      }

      fragmentLayoutString = PRenderBuffer.getActiveBuffer().getFragmentLayout();

      // Generate new shaders.
      ambientAndDirectionalLightShader = PRenderBuffer.getActiveBuffer().getQuadShader(Gdx.files.local("engine/shader/light/ambient_and_directional_light.quad.glsl"));
      pointLightShader = new PShader("",
                                     fragmentLayoutString,
                                     PPointLight.getMESH().getVertexAttributes(), Gdx.files.local("engine/shader/light/light.vert.glsl"),
                                     Gdx.files.local("engine/shader/light/pointlight.frag.glsl"));
    }

    PGLUtils.clearScreen(0, 0, 0, 1);

    // Lights should be added to each other, and should not fill the depth buffer.
    renderContext.setBlending(true, GL20.GL_ONE, GL20.GL_ONE);
    renderContext.setDepthMask(false);

    // Ambient and directional lights.
    ambientAndDirectionalLightShader.start(renderContext);
    setLightUniforms(ambientAndDirectionalLightShader, diffuseMTex, normalRTex, emissiveITex);
    PRenderBuffer.getActiveBuffer().renderQuad(ambientAndDirectionalLightShader);
    ambientAndDirectionalLightShader.end();

    // Point lights.
    lightsFloatBuffer.reset();
    int numLights = 0;
    int vecsPerInstance = 0;
    for (val pointLight : pointLights) {
      vecsPerInstance = pointLight.vecsPerInstance();
      numLights += pointLight.addInstanceData(lightsFloatBuffer) ? 1 : 0;
    }

    pointLightShader.start(renderContext);
    setLightUniforms(pointLightShader, diffuseMTex, normalRTex, emissiveITex);

    if (numLights > 0) {
      lightsFloatBuffer.dataTransferFinished();
      lightsFloatBuffer.setUniforms(pointLightShader, StringConstants.lightBuffer, vecsPerInstance);
      PPointLight.getMESH().glRenderInstanced(pointLightShader, numLights);
    }

    pointLightShader.end();
    renderContext.resetDefaults();
  }

  private void setLightUniforms(PShader shader, Texture diffuseMTex, Texture normalRTex, Texture emissiveITex) {
    shader.set(UniformConstants.Sampler2D.u_diffuseMTex, diffuseMTex);
    shader.set(UniformConstants.Sampler2D.u_normalRTex, normalRTex);
    shader.set(UniformConstants.Sampler2D.u_emissiveITex, emissiveITex);
  }

  public void addLight(PLight light) {
    pointLights.add((PPointLight) light);
  }
}
