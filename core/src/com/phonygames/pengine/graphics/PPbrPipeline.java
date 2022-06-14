package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.gl.PGLUtils;
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

  public PPbrPipeline() {
    super();
    this.gBuffer =
        new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuseM").addFloatAttachment("normalR")
                                   .addFloatAttachment("emissiveI").addDepthAttachment().build();
    this.lightedBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("lighted").build();
    this.finalBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("final").build();
    setPhases();
  }

  protected void setPhases() {
    phases = new PRenderContext.PhaseHandler[]{new PRenderContext.PhaseHandler("PBR", gBuffer) {
      @Override public void begin() {
        PGLUtils.clearScreen(0, 0, 0, 1);
      }

      @Override public void end() {
      }
    }, new PRenderContext.PhaseHandler("Lights", lightedBuffer) {
      @Override public void begin() {
        PGLUtils.clearScreen(0, 0, 0, 1);
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
    }};
  }

  public void attach(PRenderContext renderContext) {
    renderContext.setPhaseHandlersTo(phases);
  }

  public Texture getTexture() {
    return finalBuffer.texture();
  }
}
