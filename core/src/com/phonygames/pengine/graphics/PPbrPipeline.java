package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.lighting.PEnvironment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PPbrPipeline {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PRenderBuffer finalBuffer;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PRenderBuffer gBuffer;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PRenderBuffer lightedBuffer;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PEnvironment environment;
  private PRenderContext.PhaseHandler[] phases;
  private PShader postprocessingcombineshader;

  public PPbrPipeline() {
    super();
    this.gBuffer =
        new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuseM").addFloatAttachment("normalR")
                                   .addFloatAttachment("emissiveI").addFloatAttachment("alphaBlend")
                                   .addDepthAttachment().build();
    this.lightedBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("lighted").build();
    this.finalBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("final").build();
    this.postprocessingcombineshader =
        finalBuffer.getQuadShader(Gdx.files.local("engine/shader/postprocessingcombine.quad.glsl"));
    setPhases();
  }

  protected void setPhases() {
    phases = new PRenderContext.PhaseHandler[]{new PRenderContext.PhaseHandler(PGltf.Layer.PBR, gBuffer, true) {
      @Override public void begin() {
        PGLUtils.clearScreen(0, 0, 0, 0);
      }

      @Override public void end() {
      }
    }, new PRenderContext.PhaseHandler(PGltf.Layer.AlphaBlend, gBuffer, false) {
      @Override public void begin() {
        Gdx.gl.glBlendEquationSeparate(GL20.GL_FUNC_ADD, GL20.GL_FUNC_ADD);
      }

      @Override public void end() {
      }
    }, new PRenderContext.PhaseHandler("Lights", lightedBuffer, true) {
      @Override public void begin() {
        PGLUtils.clearScreen(0, 0, 0, 0);
        if (environment == null) {
          return;
        }
        Texture depthTex = gBuffer.texture("depth");
        Texture diffuseMTex = gBuffer.texture("diffuseM");
        Texture normalRTex = gBuffer.texture("normalR");
        Texture emissiveITex = gBuffer.texture("emissiveI");
        // TODO: Lock the camera of the context, to prevent bugs.
        environment.renderLights(PRenderContext.activeContext(), depthTex, diffuseMTex, normalRTex, emissiveITex);
        // TODO: unlock
      }

      @Override public void end() {
      }
    }, new PRenderContext.PhaseHandler("PostProcess", null, true) {
      @Override public void begin() {
        PGLUtils.clearScreen(0, 0, 0, 1);
        if (environment == null) {
          return;
        }
        Texture lightedTex = lightedBuffer.texture("lighted");
        Texture alphaBlendTex = gBuffer.texture("alphaBlend");
        finalBuffer.begin(true);
        postprocessingcombineshader.start(PRenderContext.activeContext());
        postprocessingcombineshader.setWithUniform("u_lightedTex", lightedTex);
        postprocessingcombineshader.setWithUniform("u_alphaBlendTex", alphaBlendTex);
        finalBuffer.renderQuad(postprocessingcombineshader);
        postprocessingcombineshader.end();
        finalBuffer.end();
      }

      @Override public void end() {
      }
    }};
  }

  public void attach(PRenderContext renderContext) {
    renderContext.setPhaseHandlersTo(phases);
  }

  public Texture getTexture() {
    return finalBuffer.texture();
  }
}
