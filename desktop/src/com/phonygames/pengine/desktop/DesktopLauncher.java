package com.phonygames.pengine.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.phonygames.cybertag.CybertagGame;
import com.phonygames.pengine.PEngine;

public class DesktopLauncher {
  private static Lwjgl3Application application;

  public static void main(String[] arg) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3);
    config.setAudioConfig(64, 1024, 9);
    config.setWindowedMode(1920, 1080);
    application = new Lwjgl3Application(new PEngine(new CybertagGame()), config);
  }
}
