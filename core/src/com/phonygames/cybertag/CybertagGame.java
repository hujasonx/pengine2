package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PRenderBuffer;

public class CybertagGame implements PGame {

  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];
  public void init() {
    for (int a = 0; a < gBuffers.length; a++) {
      gBuffers[a] = new PRenderBuffer.Builder().build();
    }

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

  }

  public void postFrameUpdate() {
    PApplicationWindow.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(gBuffers[0].getTexture());
  }
}