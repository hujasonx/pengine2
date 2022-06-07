package com.phonygames.pengine.graphics;


import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

public class PApplicationWindow {
  @Getter
  private static int width, height;

  private static Set<ResizeListener> resizeListeners = new HashSet<>();

  public static void init() {

  }

  public static void preFrameUpdate() {

  }

  public static void triggerResize(int rawWidth, int rawHeight) {
    width = rawWidth;
    height = rawHeight;

    for (ResizeListener resizeListener : resizeListeners) {
      resizeListener.onResize(width, height);
    }
  }

  public static void registerResizeListener(ResizeListener resizeListener) {
    resizeListeners.add(resizeListener);
  }

  public static void removeResizeListener(ResizeListener resizeListener) {
    resizeListeners.remove(resizeListener);
  }

  public static class ResizeListener {
    public void onResize(int width, int height) {}
  }
}
