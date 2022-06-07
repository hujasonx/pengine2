package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import lombok.Getter;

public class PRenderBuffer {
  @Getter
  private static PRenderBuffer activeBuffer = null;

  public enum SizeMode {
    WINDOWSCALE, STATIC
  }

  @Getter
  private SizeMode sizeMode;
  @Getter
  private float windowScale = 1;
  @Getter
  private int staticWidth, staticHeight;

  private static Texture testTexture = null;

  public Texture getTexture() {
    if (testTexture == null) {
      testTexture = new Texture(Gdx.files.internal("badlogic.jpg"));
    }
    return testTexture;
  }

  public static class Builder {

  }
}
