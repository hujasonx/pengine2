package com.phonygames.pengine.graphics.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class PGLUtils {
  public static void clearScreen(float r, float g, float b, float a) {
    Gdx.gl.glClearColor(r, g, b, a);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
  }
}
