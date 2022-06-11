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
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PSet;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PEnvironment {
  public static class UniformConstants {
    private static final int NUM_DIRECTIONAL_LIGHTS = 4;

    public static class Sampler2D {
      public static final String u_depthTex = "u_depthTex";
      public static final String u_diffuseMTex = "u_diffuseMTex";
      public static final String u_normalRTex = "u_normalRTex";
      public static final String u_emissiveITex = "u_emissiveITex";
    }

    public static class Mat4 {
      public static final String u_cameraViewProInv = "u_cameraViewProInv";
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

  private final PVec3 ambientLightCol = new PVec3();
  private final PVec3 directionalLightDir[] = new PVec3[UniformConstants.NUM_DIRECTIONAL_LIGHTS];
  private final PVec3 directionalLightCol[] = new PVec3[UniformConstants.NUM_DIRECTIONAL_LIGHTS];

  public PEnvironment() {
    lightsFloatBuffer = PFloat4Texture.get(256 * 256, true);
    for (int a = 0; a < UniformConstants.NUM_DIRECTIONAL_LIGHTS; a++) {
      directionalLightDir[a] = new PVec3();
      directionalLightCol[a] = new PVec3();
    }
  }

  private final PSet<PPointLight> pointLights = new PSet<>();


  public void renderLights(PRenderContext renderContext, Texture depthTex, Texture diffuseMTex, Texture normalRTex, Texture emissiveITex) {
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
    renderContext.setDepthTest(0);

    // Ambient and directional lights.
    ambientAndDirectionalLightShader.start(renderContext);
    ambientAndDirectionalLightShader.set("u_ambientLightCol", ambientLightCol);
    for (int a = 0; a < UniformConstants.NUM_DIRECTIONAL_LIGHTS; a++) {
      ambientAndDirectionalLightShader.set("u_directionalLightCol" + a, directionalLightCol[a]);
      ambientAndDirectionalLightShader.set("u_directionalLightDir" + a, directionalLightDir[a]);
    }
    setLightUniforms(ambientAndDirectionalLightShader, depthTex, diffuseMTex, normalRTex, emissiveITex, renderContext.getViewProjInvTransform());
    PRenderBuffer.getActiveBuffer().renderQuad(ambientAndDirectionalLightShader);
    ambientAndDirectionalLightShader.end();

    // Point lights.
    lightsFloatBuffer.reset();
    int numLights = 0;
    int vecsPerInstance = 0;
    for (val pointLight : pointLights) {
      vecsPerInstance = pointLight.vecsPerInstance();
      boolean shouldRender = pointLight.addInstanceData(lightsFloatBuffer);
      numLights += shouldRender ? 1 : 0;
    }

    if (numLights > 0) {
      pointLightShader.start(renderContext);
      setLightUniforms(pointLightShader, depthTex, diffuseMTex, normalRTex, emissiveITex, renderContext.getViewProjInvTransform());
      lightsFloatBuffer.dataTransferFinished();
      lightsFloatBuffer.setUniforms(pointLightShader, "u_lightBufferTex", vecsPerInstance);
      PPointLight.getMESH().glRenderInstanced(pointLightShader, numLights);
      pointLightShader.end();
    }

    renderContext.resetDefaults();
  }

  public PEnvironment setAmbientLightCol(float r, float g, float b) {
    this.ambientLightCol.set(r, g, b);
    return this;
  }

  public PEnvironment setDirectionalLightDir(int index, float x, float y, float z) {
    directionalLightDir[index].set(x, y, z).nor();
    return this;
  }

  public PEnvironment setDirectionalLightColor(int index, float r, float g, float b) {
    directionalLightCol[index].set(r, g, b);
    return this;
  }

  private void setLightUniforms(PShader shader, Texture depthTex, Texture diffuseMTex, Texture normalRTex, Texture emissiveITex, PMat4 viewProjInv) {
    shader.set(UniformConstants.Sampler2D.u_depthTex, depthTex);
    shader.set(UniformConstants.Sampler2D.u_diffuseMTex, diffuseMTex);
    shader.set(UniformConstants.Sampler2D.u_normalRTex, normalRTex);
    shader.set(UniformConstants.Sampler2D.u_emissiveITex, emissiveITex);
    shader.set(UniformConstants.Mat4.u_cameraViewProInv, viewProjInv);
  }

  public void addLight(PLight light) {
    pointLights.add((PPointLight) light);
  }
}
