package com.phonygames.pengine.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.PGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		config.useOpenGL3(true, 3, 3);
		config.setAudioConfig(64, 1024, 9);
		config.setWindowedMode(1600, 900);
		new Lwjgl3Application(new PEngine(new PGame()), config);
	}
}
