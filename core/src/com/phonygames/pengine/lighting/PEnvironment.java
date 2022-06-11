package com.phonygames.pengine.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
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

  public static class StringConstants {
    public static final String lightBuffer = "lightBuffer";
  }

  @Getter
  @Setter
  private PShader pointLightShader;

  private String fragmentLayoutString = "";

  private final PFloat4Texture depthTestLightsFloatArrayBuffer;
  private final PFloat4Texture noDepthTestLightsFloatArrayBuffer;

  public PEnvironment() {
    depthTestLightsFloatArrayBuffer = PFloat4Texture.get(256 * 256, true);
    noDepthTestLightsFloatArrayBuffer = PFloat4Texture.get(256 * 256, true);
  }

  private final PSet<PPointLight> pointLights = new PSet<>();


  public void renderLights(PRenderContext renderContext, Texture diffuseMTex, Texture emissiveRTex, Texture normalITex) {
    if (!fragmentLayoutString.equals(PRenderBuffer.getActiveBuffer().getFragmentLayout())) {
      if (fragmentLayoutString.length() > 0) {
        PLog.w("Making new shader, as fragment layout changed [\n" + fragmentLayoutString + "\n -> \n" + PRenderBuffer.getActiveBuffer().getFragmentLayout() + "]");
      }

      fragmentLayoutString = PRenderBuffer.getActiveBuffer().getFragmentLayout();

      // Generate new shaders.
      pointLightShader = new PShader("// POINTLIGHT\n", fragmentLayoutString, PPointLight.getMESH().getVertexAttributes(), Gdx.files.local("engine/shader/light/light.vert.glsl"),
                                     Gdx.files.local("engine/shader/light/pointlight.frag.glsl"));
    }
    renderContext.setBlending(true, GL20.GL_ONE, GL20.GL_ONE);

    depthTestLightsFloatArrayBuffer.reset();
    noDepthTestLightsFloatArrayBuffer.reset();
    int useDepthTestBuffer = 0;
    int useNoDepthTestBuffer = 0;
    int vecsPerInstance = 0;
    for (val pointLight : pointLights) {
      vecsPerInstance = pointLight.vecsPerInstance();
      if (pointLight.shouldUseDepthTestOffAndFrontFaceCull(renderContext.getCameraPos())) {
        useNoDepthTestBuffer++;
        pointLight.addInstanceData(noDepthTestLightsFloatArrayBuffer);
      } else {
        useDepthTestBuffer++;
        pointLight.addInstanceData(depthTestLightsFloatArrayBuffer);
      }
    }

    pointLightShader.start(renderContext);
    setLightUniforms(pointLightShader,diffuseMTex, emissiveRTex, normalITex);

    if (useDepthTestBuffer > 0) {
      renderContext.setCullFaceBack();
      renderContext.setDepthTest(GL20.GL_LESS);
      depthTestLightsFloatArrayBuffer.dataTransferFinished();
      depthTestLightsFloatArrayBuffer.setUniforms(pointLightShader, StringConstants.lightBuffer, vecsPerInstance);
      PPointLight.getMESH().glRenderInstanced(pointLightShader, useDepthTestBuffer);
    }

    if (useNoDepthTestBuffer > 0) {
      renderContext.setCullFaceFront();
      renderContext.disableDepthTest();
      noDepthTestLightsFloatArrayBuffer.dataTransferFinished();
      noDepthTestLightsFloatArrayBuffer.setUniforms(pointLightShader, StringConstants.lightBuffer, vecsPerInstance);
      PPointLight.getMESH().glRenderInstanced(pointLightShader, useNoDepthTestBuffer);
    }

    pointLightShader.end();
    renderContext.resetDefaults();
  }

  private void setLightUniforms(PShader shader, Texture diffuseMTex, Texture emissiveRTex, Texture normalITex) {
    shader.set(UniformConstants.Sampler2D.u_diffuseMTex, diffuseMTex);
    shader.set(UniformConstants.Sampler2D.u_emissiveRTex, emissiveRTex);
    shader.set(UniformConstants.Sampler2D.u_normalITex, normalITex);
  }

  public void addLight(PLight light) {
    pointLights.add((PPointLight) light);
  }
}
