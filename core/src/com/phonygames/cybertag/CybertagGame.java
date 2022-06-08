package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.shader.PShader;

public class CybertagGame implements PGame {

  protected PShader testShader;
  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];

  public void init() {
    for (int a = 0; a < gBuffers.length; a++) {
      gBuffers[a] = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuse", GL30.GL_RGBA16F, GL30.GL_RGBA).build();
    }

    testShader = new PShader(Gdx.files.local("shader/default.vert.glsl"), Gdx.files.local("shader/default.frag.glsl"));
  }

  public void preLogicUpdate() {

  }

  public void logicUpdate() {

  }

  public void postLogicUpdate() {

  }

  public void preFrameUpdate() {

  }

  public void frameUpdate() {
    gBuffers[0].begin();
    PGLUtils.clearScreen(0, 1, 1, 1);
    gBuffers[0].end();

  }

  public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(gBuffers[0].getTexture());
  }
}