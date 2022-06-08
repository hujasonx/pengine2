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

    orthoCamera.setToOrtho(true, rawWidth, rawHeight);
    orthoCamera.update(true);
    windowSpriteBatch.setProjectionMatrix(orthoCamera.combined);

    for (ResizeListener resizeListener : resizeListeners) {
      resizeListener.onApplicationWindowResize(rawWidth, rawHeight);
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

  public static interface ResizeListener {
    void onApplicationWindowResize(int width, int height);
  }
}
