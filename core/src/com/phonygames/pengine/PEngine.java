package com.phonygames.pengine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.profiling.GLErrorListener;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.input.PInput;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.lighting.PLight;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.physics.PPhysicsEngine;
import com.phonygames.pengine.util.PApplicationUtils;
import com.phonygames.pengine.util.PStringUtils;

import lombok.Getter;

public class PEngine extends ApplicationAdapter {
  public static final String LIBRARY_NAME = "penginelib";
  public static int frameCount = 0, logicUpdateCount = 1;
  public static float logictimestep = 1f / 60f;
  public static float logicupdateframeratio = .1f;
  // The ratio between the prev and next logic updates that the current frame is at, temporally.public static
  // float uit = 0;
  public static float t = 0, dt = .1f, uit = 0, uidt = .1f, logict = 0; // dt and t are in frame time.
  @Getter
  private static Phase currentPhase = Phase.INIT;
  private static PGame game;
  private static GLProfiler glProfiler;
  @Getter
  private static PEngine pEngine;
  private static float timeScale = 1;
  private static WindowedMean uidtWindowedMean = new WindowedMean(8);

  public PEngine(PGame game) {
    PAssert.isNull(PEngine.pEngine);
    PEngine.game = game;
    pEngine = this;
  }

  @Override public void create() {
    try {
      new SharedLibraryLoader().load(LIBRARY_NAME);
    } catch (Exception e) {
    }
    initStatic();
    game.init();
  }

  private static void initStatic() {
    PInput.init();
    PVertexAttributes.init();
    PLight.initMeshes();
    PLog.init();
    PApplicationWindow.init();
    PMesh.init();
    PShaderProvider.init();
    PAssetManager.init();
    PPhysicsEngine.init();
    glProfiler = new GLProfiler(Gdx.graphics);
    glProfiler.enable();
    glProfiler.setListener(GLErrorListener.LOGGING_LISTENER);
    PLog.i("Local directory: " + Gdx.files.getLocalStoragePath());
  }

  @Override public void resize(int width, int height) {
    super.resize(width, height);
    if (width == 0 || height == 0) {
      PLog.i("Resize window: 0x0").pEngine();
      return;
    }
    PApplicationWindow.triggerResize(width, height);
  }

  @Override public void render() {
    PInput.preFrameUpdate();
    float gdxdt = Gdx.graphics.getDeltaTime();
    gdxdt = Math.max(gdxdt, 1f / 300f);
    uidtWindowedMean.addValue(gdxdt);
    uidt = uidtWindowedMean.hasEnoughData() ? uidtWindowedMean.getMean() : gdxdt;
    dt = uidt * timeScale;
    t += dt;
    uit += uidt;
    processLogicUpdateForFrame();
    frameUpdate();
    // Ctrl + S + R to reload all shaders.
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Input.Keys.S) &&
        Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      PShader.reloadAllFromSources();
    }
  }

  private void processLogicUpdateForFrame() {
    currentPhase = Phase.LOGIC;
    if (logict < t - 1) { // After a large lag spike, don't process the logic update too quickly.
      logict = t;
    }
    while (logict < t) {
      logict += logictimestep;
      PInput.preLogicUpdate();
      game.preLogicUpdate();
      game.logicUpdate();
      game.postLogicUpdate();
      postLogicUpdateStatic();
    }
  }

  private void frameUpdate() {
    Gdx.graphics.setTitle(
        "Cybertag: " + PStringUtils.prependSpacesToLength("" + Gdx.graphics.getFramesPerSecond(), 3) + "FPS, Mem: " +
        PStringUtils.prependSpacesToLength(
            PStringUtils.roundNearestHundredth(PApplicationUtils.runtimeUsedMemoryMb() / 1024), 5) + "GB");
    PApplicationWindow.preFrameUpdate();
    PAssetManager.preFrameUpdate();
    game.preFrameUpdate();
    game.frameUpdate();
    game.postFrameUpdate();
    postFrameUpdateStatic();
  }

  private static void postLogicUpdateStatic() {
    PPhysicsEngine.postLogicUpdate();
  }

  private static void postFrameUpdateStatic() {
    PPhysicsEngine.postFrameUpdate();
    glProfiler.reset();

    if (PKeyboard.isDown(Input.Keys.ALT_RIGHT) && PKeyboard.isFrameJustDown(Input.Keys.ENTER)) {
      if (Gdx.graphics.isFullscreen()) {
        Gdx.graphics.setWindowedMode(1600, 900);
      } else {
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
      }
    }
  }

  @Override public void dispose() {
    disposeStatic();
  }

  private static void disposeStatic() {
    PPhysicsEngine.dispose();
  }

  public enum Phase {
    INIT, LOGIC, FRAME
  }
}
