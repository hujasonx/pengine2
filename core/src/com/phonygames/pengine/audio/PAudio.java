package com.phonygames.pengine.audio;

import static com.phonygames.pengine.audio.jni.PFmodInterface.__set3dListenerTransform;
import static com.phonygames.pengine.audio.jni.PFmodInterface.__setEventInstance3dTransform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.cybertag.CybertagGame;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.audio.jni.PFmodInterface;
import com.phonygames.pengine.input.PMouse;

public class PAudio {
  public static boolean isSupported = false;
  private static long musicEventInstances[] = new long[]{-1l, -1l, -1l};
  private static boolean musicPaused = false;

  public static void close() {
    PFmodInterface.close();
  }

  public static void preFrameUpdate() {
    if (!PFmodInterface.isInitalized()) {
      return;
    }
    float bandPassAmount = PMouse.isDown() ? 0 : 0;
    PFmodInterface.setSystemParameterByName("BgmLowPass", 1 - .3f *bandPassAmount, false);
    PFmodInterface.setSystemParameterByName("BgmHighPass", .3f *bandPassAmount, false);
    if (Gdx.input.isKeyJustPressed(Input.Keys.SLASH)) {
      PFmodInterface.setEventInstancePaused(musicEventInstances[0], (musicPaused = !musicPaused));
    }
  }

  public static void init() {
    if (!isSupported) {
      return;
    }
    PFmodInterface.init();
    String mediaFolder = "fsb/";
    PFmodInterface.loadBank(mediaFolder + "Master Bank.bank");
    PFmodInterface.loadBank(mediaFolder + "Master Bank.strings.bank");
    PFmodInterface.loadBank(mediaFolder + "All.bank");
    musicEventInstances[0] = PFmodInterface.getEventInstance("event:/TestMusic");
    PFmodInterface.setEventInstanceVolume(musicEventInstances[0], .1f);
    PFmodInterface.startPlaybackForEventInstance(musicEventInstances[0]);
  }

  public static void logicUpdate() {
    if (!PFmodInterface.isInitalized()) {
      return;
    }
  }

  public static void postFrameUpdate() {
    if (!PFmodInterface.isInitalized()) {
      return;
    }
    PFmodInterface.update();
  }

  public static void set3dListenerTransform(int index, Vector3 position, Vector3 velocity, Vector3 forward,
                                            Vector3 up) {
    __set3dListenerTransform(index, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, forward.x,
                             forward.y, forward.z, up.x, up.y, up.z);
  }

  public static void setEventInstance3dTransform(long instanceId, Vector3 position, Vector3 velocity, Vector3 forward,
                                                 Vector3 up) {
    __setEventInstance3dTransform(instanceId, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z,
                                  forward.x, forward.y, forward.z, up.x, up.y, up.z);
  }
}
