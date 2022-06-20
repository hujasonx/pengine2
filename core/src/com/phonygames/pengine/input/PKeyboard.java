package com.phonygames.pengine.input;

import lombok.AccessLevel;
import lombok.Getter;

public class PKeyboard {
  private static final int INPUTS = 255;
  private static final ButtonStatus[] frameButtonStatus = new ButtonStatus[INPUTS];
  private static final ButtonStatus[] frameButtonStatusQueued = new ButtonStatus[INPUTS];
  private static final ButtonStatus[] logicButtonStatus = new ButtonStatus[INPUTS];
  private static final ButtonStatus[] logicButtonStatusQueued = new ButtonStatus[INPUTS];

  protected static void init() {
    for (int a = 0; a < INPUTS; a++) {
      logicButtonStatus[a] = ButtonStatus.Up;
      logicButtonStatusQueued[a] = ButtonStatus.Up;
      frameButtonStatus[a] = ButtonStatus.Up;
      frameButtonStatusQueued[a] = ButtonStatus.Up;
    }
  }

  public static boolean isDown(int keyCode) {
    return frameButtonStatus[keyCode].isPressed();
  }

  public static boolean isFrameJustDown(int keyCode) {
    return frameButtonStatus[keyCode] == ButtonStatus.JustDown;
  }

  public static boolean isFrameJustTyped(int keyCode) {
    return frameButtonStatus[keyCode] == ButtonStatus.JustTyped;
  }

  public static boolean isFrameJustUp(int keyCode) {
    return frameButtonStatus[keyCode] == ButtonStatus.JustDown;
  }

  public static boolean isLogicJustDown(int keyCode) {
    return logicButtonStatus[keyCode] == ButtonStatus.JustDown;
  }

  public static boolean isLogicJustTyped(int keyCode) {
    return logicButtonStatus[keyCode] == ButtonStatus.JustTyped;
  }

  public static boolean isLogicJustUp(int keyCode) {
    return logicButtonStatus[keyCode] == ButtonStatus.JustDown;
  }

  public static boolean isUp(int keyCode) {
    return !frameButtonStatus[keyCode].isPressed();
  }

  protected static boolean keyDown(int keyCode) {
    logicButtonStatusQueued[keyCode] = ButtonStatus.JustDown;
    frameButtonStatusQueued[keyCode] = ButtonStatus.JustDown;
    return true;
  }

  protected static boolean keyUp(int keyCode) {
    logicButtonStatusQueued[keyCode] = ButtonStatus.JustUp;
    frameButtonStatusQueued[keyCode] = ButtonStatus.JustUp;
    return true;
  }

  protected static void preFrameUpdate() {
    for (int a = 0; a < INPUTS; a++) {
      frameButtonStatus[a] = frameButtonStatusQueued[a];
      switch (frameButtonStatus[a]) {
        case JustUp:
          frameButtonStatusQueued[a] = ButtonStatus.Up;
          break;
        case JustDown:
        case JustTyped:
          frameButtonStatusQueued[a] = ButtonStatus.Down;
          break;
      }
    }
  }

  protected static void preLogicUpdate() {
    for (int a = 0; a < INPUTS; a++) {
      logicButtonStatus[a] = logicButtonStatusQueued[a];
      switch (logicButtonStatus[a]) {
        case JustUp:
          logicButtonStatusQueued[a] = ButtonStatus.Up;
          break;
        case JustDown:
        case JustTyped:
          logicButtonStatusQueued[a] = ButtonStatus.Down;
          break;
      }
    }
  }

  public enum ButtonStatus {
    Up(false), JustDown(true), Down(true), JustUp(false), JustTyped(true);
    @Getter(value = AccessLevel.PUBLIC)
    private final boolean pressed;

    ButtonStatus(final boolean isPressed) {
      this.pressed = isPressed;
    }
  }
}
