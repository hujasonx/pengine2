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

public class PPbrPipeline implements PPostProcessor.Delegate{
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PRenderBuffer combineBuffer;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PRenderBuffer gBuffer;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PEnvironment environment;
  private PRenderContext.PhaseHandler[] phases;
  private PShader postprocessingcombineshader;
  private PPostProcessor postProcessor;

  public PPbrPipeline() {
    super();
    this.gBuffer =
        new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuseM").addFloatAttachment("normalR")
                                   .addFloatAttachment("emissiveI").addFloatAttachment("alphaBlend")
                                   .addDepthAttachment().build();
    this.combineBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("combine").build();
    this.postprocessingcombineshader =
        combineBuffer.getQuadShader(Gdx.files.local("engine/shader/postprocessingcombine.quad.glsl"));
    this.postProcessor = new PPostProcessor(this);
    setPhases();
  }

  @Override public Texture getTextureForPostProcessing() {
    return combineBuffer.texture();
  }

  @Override public Texture getDepthTextureForPostProcessing() {
    return gBuffer.texture("depth");
  }
  @Override public Texture getNormalRTextureForPostProcessing() {
    return gBuffer.texture("normalR");
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
    }, new PRenderContext.PhaseHandler("Lights", null, true) {
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
    }, new PRenderContext.PhaseHandler("Combine", null, true) {
      @Override public void begin() {
        PGLUtils.clearScreen(0, 0, 0, 1);
        if (environment == null) {
          return;
        }
        Texture lightedTex = environment.getTexture();
        Texture alphaBlendTex = gBuffer.texture("alphaBlend");
        combineBuffer.begin(true);
        postprocessingcombineshader.start(PRenderContext.activeContext());
        postprocessingcombineshader.setWithUniform("u_lightedTex", lightedTex);
        postprocessingcombineshader.setWithUniform("u_alphaBlendTex", alphaBlendTex);
        combineBuffer.renderQuad(postprocessingcombineshader);
        postprocessingcombineshader.end();
        combineBuffer.end();
      }

      @Override public void end() {
      }
    }, postProcessor.getPhaseHandler()};
  }

  public void attach(PRenderContext renderContext) {
    renderContext.setPhaseHandlersTo(phases);
  }

  public Texture getTexture() {
    return postProcessor.getFinalTexture();
  }
}
