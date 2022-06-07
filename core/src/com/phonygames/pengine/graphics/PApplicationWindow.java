package com.phonygames.pengine.graphics;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

public class PApplicationWindow {
  @Getter
  private static int width, height;

  private static Set<ResizeListener> resizeListeners = new HashSet<>();

  // A SpriteBatch that will draw things on the window at 1-1 pixel scale.
  private static final SpriteBatch windowSpriteBatch = new SpriteBatch();
  private static final OrthographicCamera orthoCamera = new OrthographicCamera();

  public static void init() {

  }

  public static void preFrameUpdate() {

  }

  public static void triggerResize(int rawWidth, int rawHeight) {
    width = rawWidth;
    height = rawHeight;

    orthoCamera.setToOrtho(false, rawWidth, rawHeight);
    orthoCamera.update(true);
    windowSpriteBatch.setProjectionMatrix(orthoCamera.combined);

    for (ResizeListener resizeListener : resizeListeners) {
      resizeListener.onResize(rawWidth, rawHeight);
    }
  }

  public static void registerResizeListener(ResizeListener resizeListener) {
    resizeListeners.add(resizeListener);
  }

  public static void removeResizeListener(ResizeListener resizeListener) {
    resizeListeners.remove(resizeListener);
  }

  public static void drawTextureToScreen(Texture texture) {
    windowSpriteBatch.begin();
    windowSpriteBatch.draw(texture, 0, 0, width, height);
    windowSpriteBatch.end();
  }

  public static void clearScreen(float r, float g, float b, float a) {
    Gdx.gl.glClearColor(r, g, b, a);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
  }

  public static class ResizeListener {
    public void onResize(int width, int height) {}
  }
}
