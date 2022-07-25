package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.shader.PShader;

public class PPostProcessor {
  private final Delegate delegate;
  private PRenderBuffer bloomBuffer, ssaoBuffer, fxaaBuffer, outlineBuffer;
  private float bloomTexScale = .25f;
  private float fxaaTexScale = 1;
  private float outlineTexScale = 1;
  private float ssaoTexScale = .2f;
  private float totalTexScale = 1;
  private float bloomScale = 1, bloomThreshold =.9f;
  private final PShader bloomShader;

  public PPostProcessor(Delegate delegate) {
    this.delegate = delegate;
    this.bloomBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * bloomTexScale).addFloatAttachment("bloom").build();
    this.bloomShader =
        bloomBuffer.getQuadShader(Gdx.files.local("engine/shader/bloom.quad.glsl"));
    this.ssaoBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * ssaoTexScale).addFloatAttachment("ssao").build();
    this.fxaaBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * fxaaTexScale).addFloatAttachment("fxaa").build();
    this.outlineBuffer =
        new PRenderBuffer.Builder().setWindowScale(totalTexScale * outlineTexScale).addFloatAttachment("outline").build();
  }

  public PRenderContext.PhaseHandler getPhaseHandler() {
    return new PRenderContext.PhaseHandler("PostProcess", null, true) {
      @Override public void begin() {
        bloomBuffer.begin(true);
        Texture sourceTex = delegate.getTextureForPostProcessing();
        PGLUtils.clearScreen(0,0,0,0);
        bloomShader.start(PRenderContext.activeContext());
        bloomShader.set("u_bloomThreshold", bloomThreshold);
        bloomShader.set("u_bloomScale", bloomScale);
        bloomShader.setWithUniform("u_sourceTex", sourceTex);
        bloomBuffer.renderQuad(bloomShader);
        bloomShader.end();
        bloomBuffer.end();
        bloomBuffer.blurSelf();
      }

      @Override public void end() {
      }
    };
  }

  public Texture getFinalTexture() {
    return bloomBuffer.texture();
  }

  public interface Delegate {
    Texture getTextureForPostProcessing();
  }
}
