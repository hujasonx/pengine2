package com.phonygames.pengine.util.game;

import com.badlogic.gdx.jnigen.AntScriptGenerator;
import com.badlogic.gdx.jnigen.BuildConfig;
import com.badlogic.gdx.jnigen.BuildTarget;
import com.badlogic.gdx.jnigen.NativeCodeGenerator;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.util.jni.PJniAntBuilder;

public class PUtilGameFMODGen {

  public static boolean generate() {
    try {
      final String rootDir = "D:/Coding/pengine2/";
      final String coreDir = rootDir + "core/";
      final String jniDir = rootDir + "jni/";
      NativeCodeGenerator jnigen = new NativeCodeGenerator();
      jnigen.generate(coreDir + "src", coreDir + "build/classes/java/main", jniDir + "", null, null);
      BuildTarget win64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Windows, true);
      win64.cppFlags += " -std=c++11 ";
      BuildTarget linux64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Linux, true);
      BuildTarget mac = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.MacOsX, true);
      AntScriptGenerator antScriptGenerator = new AntScriptGenerator();
      BuildConfig config = new BuildConfig(PEngine.LIBRARY_NAME);
      antScriptGenerator.generate(config, win64);
      PJniAntBuilder.executeAnt(jniDir + "build.xml", "-v", "-DcompilerSuffix=.exe", "-Dlibraries=\""+ jniDir +"deps/*\"");
//      PJniAntBuilder.executeAnt(jniDir + "build.xml", "-v", "-DcompilerSuffix=.exe",
//                                "-Dlibraries=\"-L " + jniDir + "deps/* " + jniDir + "deps/fmod.dll \"");
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
