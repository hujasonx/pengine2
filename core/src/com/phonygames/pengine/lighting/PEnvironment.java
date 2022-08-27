package com.phonygames.pengine.lighting;

import static com.phonygames.pengine.graphics.PPostProcessor.totalTexScale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.collection.PSet;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PEnvironment {
  private final PVec3 ambientLightCol = PVec3.obtain();
  private final PVec3 directionalLightCol[] = new PVec3[UniformConstants.NUM_DIRECTIONAL_LIGHTS];
  private final PVec3 directionalLightDir[] = new PVec3[UniformConstants.NUM_DIRECTIONAL_LIGHTS];
  private final PFloat4Texture lightsFloatBuffer;
  private final PSet<PPointLight> pointLights = new PSet<>();
  @Getter
  @Setter
  private PShader ambientAndDirectionalLightShader, pointLightShader, ssaoShader;
  private String fragmentLayoutString = "";
  private PRenderBuffer ssaoBuffer, lightedBuffer;
  private float[] ssaoNoise = getRandomNoiseVectorsForSSAO(4);
  private float ssaoTexScale = .25f, lightedTexScale = 1;
  private float[] ssaoVecs = getRandomVectorsForSSAO(10);

  public PEnvironment() {
    lightsFloatBuffer = PFloat4Texture.get(256 * 256, true);
    for (int a = 0; a < UniformConstants.NUM_DIRECTIONAL_LIGHTS; a++) {
      UniformConstants.Vec4.u_directionalLightCol[a] = "u_directionalLightCol" + a;
      UniformConstants.Vec4.u_directionalLightDir[a] = "u_directionalLightDir" + a;
      directionalLightDir[a] = PVec3.obtain();
      directionalLightCol[a] = PVec3.obtain();
    }
    this.ssaoBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * ssaoTexScale).addFloatAttachment("ssao").build();
    this.ssaoShader = ssaoBuffer.getQuadShader(Gdx.files.local("engine/shader/light/ssao.quad.glsl"));
    this.lightedBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * lightedTexScale).addFloatAttachment("lighted")
                                   .build();
    this.ambientAndDirectionalLightShader =
        lightedBuffer.getQuadShader(Gdx.files.local("engine/shader/light/ambient_and_directional_light.quad.glsl"));
    this.pointLightShader = new PShader("", lightedBuffer.fragmentLayout(), PPointLight.MESH().vertexAttributes(),
                                        Gdx.files.local("engine/shader/light/light.vert.glsl"),
                                        Gdx.files.local("engine/shader/light/pointlight.frag.glsl"), null);
  }

  private static float[] getRandomNoiseVectorsForSSAO(int size) {
    Vector3[] vecs = new Vector3[size];
    for (int a = 0; a < size; a++) {
      vecs[a] = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), 0);
    }
    float[] ret = new float[size * 3];
    for (int a = 0; a < size; a++) {
      ret[a * 3 + 0] = vecs[a].x;
      ret[a * 3 + 1] = vecs[a].y;
      ret[a * 3 + 2] = vecs[a].z;
    }
    return ret;
  }

  private static float[] getRandomVectorsForSSAO(int size) {
    Vector3[] vecs = new Vector3[size];
    for (int a = 0; a < size; a++) {
      vecs[a] = new Vector3(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(.25f, 1f)).nor().scl(
          MathUtils.random(.2f, 1f));
      //      float scl = ((float)a) / ((float)size);
      //      scl = PMx.lerp(0.1f, 1.0f, scl * scl);
      //      vecs[a].scl(scl);
    }
    float[] ret = new float[size * 3];
    for (int a = 0; a < size; a++) {
      ret[a * 3 + 0] = vecs[a].x;
      ret[a * 3 + 1] = vecs[a].y;
      ret[a * 3 + 2] = vecs[a].z;
    }
    return ret;
  }

  public void addLight(PLight light) {
    pointLights.add((PPointLight) light);
  }

  public Texture getTexture() {
    return lightedBuffer.texture();
  }

  public void renderLights(PRenderContext renderContext, Texture depthTex, Texture diffuseMTex, Texture normalRTex,
                           Texture emissiveITex) {
    ssaoPass(normalRTex, depthTex, renderContext);
    lightedBuffer.begin();
    PGLUtils.clearScreen(0, 0, 0, 1);
    // Lights should be added to each other, and should not fill the depth buffer.
    renderContext.setBlending(true, GL20.GL_ONE, GL20.GL_ONE, GL20.GL_ONE, GL20.GL_ONE);
    renderContext.setDepthMask(false);
    renderContext.setDepthTest(0);
    // Ambient and directional lights.
    ambientAndDirectionalLightShader.start(renderContext);
    ambientAndDirectionalLightShader.set("u_ambientLightCol", ambientLightCol);
    ssaoBuffer.texture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    ambientAndDirectionalLightShader.setWithUniform("u_ssaoTex", ssaoBuffer.texture());
    for (int a = 0; a < UniformConstants.NUM_DIRECTIONAL_LIGHTS; a++) {
      ambientAndDirectionalLightShader.set(UniformConstants.Vec4.u_directionalLightCol[a], directionalLightCol[a]);
      ambientAndDirectionalLightShader.set(UniformConstants.Vec4.u_directionalLightDir[a], directionalLightDir[a]);
    }
    setLightUniforms(ambientAndDirectionalLightShader, depthTex, diffuseMTex, normalRTex, emissiveITex, renderContext);
    PRenderBuffer.activeBuffer().renderQuad(ambientAndDirectionalLightShader);
    ambientAndDirectionalLightShader.end();
    // Point lights.
    lightsFloatBuffer.reset();
    renderContext.setCullFaceFront(); // The rest of the models should have cull face front.
    int numLights = 0;
    int vecsPerInstance = 0;
    try (val it = pointLights.obtainIterator()) {
      while (it.hasNext()) {
        val pointLight = it.next();
        int vecsPut = pointLight.addInstanceData(lightsFloatBuffer);
        if (vecsPut > 0) {
          numLights++;
          vecsPerInstance = vecsPut;
        }
      }
    }
    if (numLights > 0) {
      pointLightShader.start(renderContext);
      setLightUniforms(pointLightShader, depthTex, diffuseMTex, normalRTex, emissiveITex, renderContext);
      lightsFloatBuffer.applyShader(pointLightShader, "lightBuffer", 0, vecsPerInstance);
      PPointLight.MESH().glRenderInstanced(pointLightShader, numLights);
      pointLightShader.end();
    }
    renderContext.resetDefaults();
    lightedBuffer.end();
  }

  private void ssaoPass(Texture normalRTex, Texture depthTex, PRenderContext renderContext) {
    renderContext.resetDefaults();
    ssaoBuffer.begin();
    ssaoShader.start(PRenderContext.activeContext());
    ssaoShader.set("u_ssaoRadius", .5f);
    ssaoShader.set("u_ssaoBias", .01f);
    ssaoShader.set("u_ssaoMagnitude", 1.1f);
    ssaoShader.set("u_ssaoContrast", 1.5f);
    setLightUniforms(ssaoShader, depthTex, null, normalRTex, null, renderContext);
    ssaoShader.set("u_noise", ssaoNoise, 3);
    ssaoShader.set("u_samples", ssaoVecs, 3);
    ssaoBuffer.renderQuad(ssaoShader);
    ssaoShader.end();
    ssaoBuffer.end();
    ssaoBuffer.blurSelf(1, 1);
  }

  private void setLightUniforms(PShader shader, Texture depthTex, Texture diffuseMTex, Texture normalRTex,
                                Texture emissiveITex, PRenderContext renderContext) {
    shader.setWithUniform(UniformConstants.Sampler2D.u_depthTex, depthTex);
    shader.setWithUniform(UniformConstants.Sampler2D.u_diffuseMTex, diffuseMTex);
    shader.setWithUniform(UniformConstants.Sampler2D.u_normalRTex, normalRTex);
    shader.setWithUniform(UniformConstants.Sampler2D.u_emissiveITex, emissiveITex);
    shader.set(UniformConstants.Mat4.u_cameraViewPro, renderContext.viewProjTransform());
    shader.set(UniformConstants.Mat4.u_cameraViewProInv, renderContext.viewProjInvTransform());
    shader.set(UniformConstants.Vec3.u_cameraPos, renderContext.cameraPos());
    shader.set(UniformConstants.Vec3.u_cameraDir, renderContext.cameraDir());
  }

  public PEnvironment setAmbientLightCol(float r, float g, float b) {
    this.ambientLightCol.set(r, g, b);
    return this;
  }

  public PEnvironment setDirectionalLightColor(int index, float r, float g, float b) {
    directionalLightCol[index].set(r, g, b);
    return this;
  }

  public PEnvironment setDirectionalLightDir(int index, float x, float y, float z) {
    directionalLightDir[index].set(x, y, z).nor();
    return this;
  }

  public static class StringConstants {
    public static final String lightBuffer = "lightBuffer";
  }

  public static class UniformConstants {
    private static final int NUM_DIRECTIONAL_LIGHTS = 4;

    public static class Mat4 {
      public static final String u_cameraViewPro = "u_cameraViewPro";
      public static final String u_cameraViewProInv = "u_cameraViewProInv";
    }

    public static class Sampler2D {
      public static final String u_depthTex = "u_depthTex";
      public static final String u_diffuseMTex = "u_diffuseMTex";
      public static final String u_emissiveITex = "u_emissiveITex";
      public static final String u_normalRTex = "u_normalRTex";
    }

    public static class Vec3 {
      public static final String u_cameraDir = "u_cameraDir";
      public static final String u_cameraPos = "u_cameraPos";
    }

    public static class Vec4 {
      private static final String[] u_directionalLightCol = new String[NUM_DIRECTIONAL_LIGHTS];
      private static final String[] u_directionalLightDir = new String[NUM_DIRECTIONAL_LIGHTS];
    }
  }
}
