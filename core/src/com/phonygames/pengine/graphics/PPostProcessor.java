package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.shader.PShader;

public class PPostProcessor {
  public static float totalTexScale = 1;
  private final PShader bloomShader, outlineShader, finalShader, fxaaShader;
  private final Delegate delegate;
  private PRenderBuffer bloomBuffer, ssaoBuffer, fxaaBuffer, outlineBuffer;
  private float bloomScale = 1, bloomThreshold = .9f;
  private float bloomTexScale = .25f;
  private float fxaaTexScale = 1;
  private float outlineTexScale = 1;
  private float ssaoTexScale = .2f;

  public PPostProcessor(Delegate delegate) {
    this.delegate = delegate;
    this.bloomBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * bloomTexScale).addFloatAttachment("bloom").build();
    this.bloomShader = bloomBuffer.getQuadShader(Gdx.files.local("engine/shader/bloom.quad.glsl"));
    this.ssaoBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * ssaoTexScale).addFloatAttachment("ssao").build();
    this.fxaaBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * fxaaTexScale).addFloatAttachment("fxaa").build();
    this.outlineBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * outlineTexScale).addFloatAttachment("outline")
                                   .build();
    this.outlineShader = outlineBuffer.getQuadShader(Gdx.files.local("engine/shader/outline.quad.glsl"));
    this.finalShader = fxaaBuffer.getQuadShader(Gdx.files.local("engine/shader/final.quad.glsl"));
    this.fxaaShader = fxaaBuffer.getQuadShader(Gdx.files.local("engine/shader/fxaa.quad.glsl"));
  }

  public Texture getFinalTexture() {
    return fxaaBuffer.texture();
  }

  public PRenderContext.PhaseHandler getPhaseHandler() {
    return new PRenderContext.PhaseHandler("PostProcess", null, true) {
      @Override public void begin() {
        bloomStep();
        outlineStep();
        finalStep();
      }

      @Override public void end() {
      }
    };
  }

  private void bloomStep() {
    Texture sourceTex = delegate.getTextureForPostProcessing();
    bloomBuffer.begin(true);
    PGLUtils.clearScreen(0, 0, 0, 0);
    bloomShader.start(PRenderContext.activeContext());
    bloomShader.set("u_bloomThreshold", bloomThreshold);
    bloomShader.set("u_bloomScale", bloomScale);
    bloomShader.setWithUniform("u_sourceTex", sourceTex);
    bloomBuffer.renderQuad(bloomShader);
    bloomShader.end();
    bloomBuffer.end();
    bloomBuffer.blurSelf(1, 1);
  }

  private void outlineStep() {
    Texture sourceTex = delegate.getTextureForPostProcessing();
    Texture depthTex = delegate.getDepthTextureForPostProcessing();
    Texture normalRTex = delegate.getNormalRTextureForPostProcessing();
    outlineBuffer.begin(true);
    PGLUtils.clearScreen(0, 0, 0, 0);
    outlineShader.start(PRenderContext.activeContext());
    outlineShader.setWithUniform("u_sourceTex", sourceTex);
    outlineShader.setWithUniform("u_depthTex", depthTex);
    outlineShader.setWithUniform("u_normalRTex", normalRTex);
    outlineShader.set("u_cameraViewProInv", PRenderContext.activeContext().viewProjInvTransform());
    outlineShader.set("u_cameraDir", PRenderContext.activeContext().cameraDir());
    outlineShader.set("u_cameraPos", PRenderContext.activeContext().cameraPos());
    outlineBuffer.renderQuad(outlineShader);
    outlineShader.end();
    outlineBuffer.end();
  }

  private void finalStep() {
    Texture sourceTex = delegate.getTextureForPostProcessing();
    // Combine step.
    fxaaBuffer.begin();
    finalShader.start(PRenderContext.activeContext());
    finalShader.setWithUniform("u_sourceTex", sourceTex);
    finalShader.setWithUniform("u_outlineTex", outlineBuffer.texture());
    finalShader.setWithUniform("u_bloomTex", bloomBuffer.texture());
    fxaaBuffer.renderQuad(finalShader);
    finalShader.end();
    fxaaBuffer.end();
    fxaaBuffer.begin();
    fxaaShader.start(PRenderContext.activeContext());
    fxaaShader.setWithUniform("u_sourceTex", fxaaBuffer.getTexturePrev("fxaa"));
    fxaaBuffer.renderQuad(fxaaShader);
    fxaaShader.end();
    fxaaBuffer.end();
  }

  public interface Delegate {
    Texture getTextureForPostProcessing();
    Texture getDepthTextureForPostProcessing();
    Texture getNormalRTextureForPostProcessing();
  }
}
