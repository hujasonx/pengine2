package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.lighting.PEnvironment;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PPbrPipeline {
  @Getter
  protected final PRenderBuffer gBuffer;
  @Getter
  protected final PRenderBuffer lightedBuffer;
  @Getter
  protected final PRenderBuffer finalBuffer;
  @Getter @Setter
  protected PEnvironment environment;

  private PRenderContext.PhaseHandler[] phases;

  private PShader lightingShader;

  public PPbrPipeline() {
    super();
    this.gBuffer =
        new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuseM").addFloatAttachment("emissiveR").addFloatAttachment("normalI").addDepthAttachment().build();
    this.lightedBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("lighted").build();
    this.finalBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("final").build();

    lightingShader = lightedBuffer.getQuadShader(Gdx.files.local("engine/postprocessing/lighting.frag.glsl"));
    setPhases();
  }

  public Texture getTexture() {
    return finalBuffer.getTexture();
  }

  public void attach(PRenderContext renderContext) {
    renderContext.setPhaseHandlers(phases);
  }

  protected void setPhases() {
    phases = new PRenderContext.PhaseHandler[]{
        new PRenderContext.PhaseHandler("PBR", gBuffer) {
          @Override
          public void begin() {
            PGLUtils.clearScreen(0, 0, 1, 1);
          }

          @Override
          public void end() {

          }
        },
        new PRenderContext.PhaseHandler("Lights", lightedBuffer) {
          @Override
          public void begin() {
            PGLUtils.clearScreen(0, 0, 0, 1);
            if (environment == null) {
              return;
            }
//
//            lightingShader.start();
//            lightingShader.set("u_diffuseMTex", gBuffer.getTexture("diffuseM"));
//            lightingShader.set("u_emissiveRTex", gBuffer.getTexture("emissiveR"));
//            lightingShader.set("u_normalITex", gBuffer.getTexture("normalI"));
//            lightedBuffer.renderQuad(lightingShader);
//            lightingShader.end();
            Texture diffuseMTex = gBuffer.getTexture("diffuseM");
            Texture emissiveRTex = gBuffer.getTexture("emissiveR");
            Texture normalITex = gBuffer.getTexture("normalI");
            environment.renderLights(PRenderContext.getActiveContext(), diffuseMTex, emissiveRTex, normalITex);
          }

          @Override
          public void end() {

          }
        }
    };
  }
}
