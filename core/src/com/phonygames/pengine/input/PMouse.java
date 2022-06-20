package com.phonygames.pengine.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.phonygames.pengine.exception.PAssert;

import lombok.AccessLevel;
import lombok.Getter;

public class PMouse {
  //  private static final Button[] buttons = new Button[3];
  private static final int INPUTS = 20;
  private static final Button[] buttons = new Button[3];
  private static final ButtonStatus[] frameButtonStatus = new ButtonStatus[INPUTS];
  private static final ButtonStatus[] frameButtonStatusQueued = new ButtonStatus[INPUTS];
  private static final ButtonStatus[] logicButtonStatus = new ButtonStatus[INPUTS];
  private static final ButtonStatus[] logicButtonStatusQueued = new ButtonStatus[INPUTS];
  private static final float[] x = new float[INPUTS], y = new float[INPUTS], xPrevFrame = new float[INPUTS],
      yPrevFrame = new float[INPUTS];

  protected static void init() {
    for (int a = 0; a < INPUTS; a++) {
      logicButtonStatus[a] = ButtonStatus.Up;
      logicButtonStatusQueued[a] = ButtonStatus.Up;
      frameButtonStatus[a] = ButtonStatus.Up;
      frameButtonStatusQueued[a] = ButtonStatus.Up;
    }
  }

  protected static void preFrameUpdate() {
    for (int a = 0; a < INPUTS; a++) {
      frameButtonStatus[a] = frameButtonStatusQueued[a];
      switch (frameButtonStatus[a]) {
        case JustUp:
          frameButtonStatusQueued[a] = ButtonStatus.Up;
          break;
        case JustDown:
          frameButtonStatusQueued[a] = ButtonStatus.Down;
          break;
      }
      xPrevFrame[a] = x[a];
      yPrevFrame[a] = y[a];
      x[a] = Gdx.input.getX(a);
      y[a] = Gdx.input.getY(a);
    }
  }

  protected static void preLogicUpdate() {
    for (int a = 0; a < INPUTS; a++) {
      logicButtonStatus[a] = logicButtonStatusQueued[a];
      logicButtonStatusQueued[a] = ButtonStatus.Up;
    }
  }

  protected static boolean touchDown(int screenX, int screenY, int pointer, Button button) {
    logicButtonStatusQueued[pointer] = ButtonStatus.JustDown;
    frameButtonStatusQueued[pointer] = ButtonStatus.JustDown;
    return true;
  }

  protected static boolean touchUp(int screenX, int screenY, int pointer, Button button) {
    logicButtonStatusQueued[pointer] = ButtonStatus.JustUp;
    frameButtonStatusQueued[pointer] = ButtonStatus.JustUp;
    return true;
  }

  public static float frameDx() {
    return frameDx(0);
  }

  public static float frameDx(int pointer) {
    return x[pointer] - xPrevFrame[pointer];
  }

  public static float frameDy() {
    return frameDy(0);
  }

  public static float frameDy(int pointer) {
    return y[pointer] - yPrevFrame[pointer];
  }

  public static float x() {
    return x(0);
  }

  public static float x(int pointer) {
    return x[pointer];
  }

  public static float y() {
    return y(0);
  }

  public static float y(int pointer) {
    return y[pointer];
  }

  public enum Button {
    LEFT(Input.Buttons.LEFT), MIDDLE(Input.Buttons.MIDDLE), RIGHT(Input.Buttons.RIGHT);
    private final int value;

    Button(final int newValue) {
      value = newValue;
      buttons[value] = this;
    }

    public static Button get(int val) {
      try {
        return buttons[val];
      } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
        PAssert.fail("Invalid enum value provided:" + val);
      }
      return null;
    }

    public int getValue() {return value;}
  }

  public enum ButtonStatus {
    Up(false), JustDown(true), Down(true), JustUp(false);
    @Getter(value = AccessLevel.PUBLIC)
    private final boolean pressed;

    ButtonStatus(final boolean isPressed) {
      this.pressed = isPressed;
    }
  }
}
