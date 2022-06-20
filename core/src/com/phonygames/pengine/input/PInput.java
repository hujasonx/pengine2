package com.phonygames.pengine.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.phonygames.pengine.exception.PAssert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PInput {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final InputMultiplexer inputMultiplexer = new InputMultiplexer();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PInputProcessor inputProcessor = new PInputProcessor();

  public static void init() {
    inputMultiplexer().addProcessor(inputProcessor());
    Gdx.input.setInputProcessor(inputMultiplexer());
    PMouse.init();
  }

  public static void preFrameUpdate() {
    PMouse.preFrameUpdate();
  }

  public static void preLogicUpdate() {
    PMouse.preLogicUpdate();
  }

  private static class PInputProcessor implements InputProcessor {
    @Override public boolean keyDown(int keycode) {
      return false;
    }

    @Override public boolean keyUp(int keycode) {
      return false;
    }

    @Override public boolean keyTyped(char character) {
      return false;
    }

    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
      PMouse.Button mouseButton = PMouse.Button.get(button);
      return PMouse.touchDown(screenX, screenY, pointer, mouseButton);
    }

    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
      PMouse.Button mouseButton = PMouse.Button.get(button);
      return PMouse.touchUp(screenX, screenY, pointer, mouseButton);
    }

    @Override public boolean touchDragged(int screenX, int screenY, int pointer) {
      return false;
    }

    @Override public boolean mouseMoved(int screenX, int screenY) {
      return false;
    }

    @Override public boolean scrolled(float amountX, float amountY) {
      return false;
    }
  }
}
