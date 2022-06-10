package com.phonygames.pengine.graphics;

import com.phonygames.pengine.graphics.gl.PGLUtils;

import lombok.val;

public class PPbrPipeline {
  protected final PRenderBuffer gBuffer;
  protected final PRenderBuffer lightedBuffer;

  private PRenderContext.PhaseHandler[] phases;

  public PPbrPipeline(PRenderBuffer gBuffer, PRenderBuffer lightedBuffer) {
    super();
    this.gBuffer = gBuffer;
    this.lightedBuffer = lightedBuffer;

    setPhases();
  }

  public void attach(PRenderContext renderContext) {
    renderContext.setPhaseHandlers(phases);
  }

  protected void setPhases() {
    phases = new PRenderContext.PhaseHandler[]{
        new PRenderContext.PhaseHandler("PBR", gBuffer) {
          @Override
          public void begin() {
            gBuffer.begin();
            PGLUtils.clearScreen(0, 1, 1, 1);
          }

          @Override
          public void end() {
            gBuffer.end();
          }
        }
    };
  }
}
