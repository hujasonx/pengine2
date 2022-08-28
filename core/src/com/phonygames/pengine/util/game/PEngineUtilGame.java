package com.phonygames.pengine.util.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.font.generator.PFreetypeFontGenerator;
import com.phonygames.pengine.graphics.sdf.PSDFGenerator;

/**
 * Helper util game.
 */
public class PEngineUtilGame implements PGame {
  PUtilGameFontGen fontGen = new PUtilGameFontGen();
  @Override public void frameUpdate() {
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
      fontGen.debugFontGen();
    }
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      fontGen.testRenderFont();
    }
  }

  @Override public void init() {
  }

  @Override public void logicUpdate() {
  }

  @Override public void postFrameUpdate() {
  }

  @Override public void postLogicUpdate() {
  }

  @Override public void preFrameUpdate() {
  }

  @Override public void preLogicUpdate() {
  }
}
