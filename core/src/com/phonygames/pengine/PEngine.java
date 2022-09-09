package com.phonygames.pengine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.profiling.GLErrorListener;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.phonygames.pengine.audio.PAudio;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.input.PInput;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.input.PMouse;
import com.phonygames.pengine.lighting.PLight;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.physics.PPhysicsEngine;
import com.phonygames.pengine.util.PApplicationUtils;
import com.phonygames.pengine.util.PStringUtils;

import lombok.Getter;

public class PEngine extends ApplicationAdapter {
  // #pragma end
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
      PAudio.isSupported = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    Gdx.graphics.setVSync(false);
    initStatic();
    game.init();
  }

  private static void initStatic() {
    PAudio.init();
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
    PMouse.__resetUncatchedTimer();
    PApplicationWindow.triggerResize(width, height);
  }

  @Override public void render() {
    PInput.preFrameUpdate();
    float gdxdt = Gdx.graphics.getDeltaTime();
    gdxdt = Math.max(gdxdt, 1f / 1000f);
    uidtWindowedMean.addValue(gdxdt);
    uidt = uidtWindowedMean.hasEnoughData() ? uidtWindowedMean.getMean() : gdxdt;
    dt = uidt * timeScale;
    t += dt;
    uit += uidt;
    frameCount++;
    processLogicUpdateForFrame();
    frameUpdate();
    debugInputs();
  }

  private void debugInputs() {
    // Ctrl + S + R to reload all shaders.
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Input.Keys.S) &&
        Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      PShader.reloadAllFromSources();
    }
    // Ctrl + P + D to toggle physics debugdraw.
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Input.Keys.P) &&
        Gdx.input.isKeyJustPressed(Input.Keys.D)) {
      PPhysicsEngine.enableDebugRender(!PPhysicsEngine.enableDebugRender());
    }
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
      timeScale *= 1.25;
    }
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
      timeScale /= 1.25;
    }
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
      timeScale = 1;
    }
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.INSERT)) {
      System.gc();
    }
  }

  private void processLogicUpdateForFrame() {
    currentPhase = Phase.LOGIC;
    if (logict < t - 1) { // After a large lag spike, don't process the logic update too quickly.
      logict = t;
    }
    // Run the game loop. Logict will always end up being >= t, so t can be used to interpolate between the game
    // state at logict - logictimestep and that at logict.
    while (logict < t) {
      logict += logictimestep;
      logicUpdateCount++;
      PInput.preLogicUpdate();
      game.preLogicUpdate();
      game.logicUpdate();
      game.postLogicUpdate();
      postLogicUpdateStatic();
    }
    logicupdateframeratio = ((t - (logict - logictimestep)) / logictimestep);
  }

  private void frameUpdate() {
    if ((int)PEngine.uit > (int)(PEngine.uit - PEngine.uidt)) {
      Gdx.graphics.setTitle(
          "Cybertag: " + PStringUtils.prependSpacesToLength("" + Gdx.graphics.getFramesPerSecond(), 3) + "FPS, Mem: " +
          PStringUtils.prependSpacesToLength(
              PStringUtils.roundNearestHundredth(PApplicationUtils.runtimeUsedMemoryMb() / 1024), 5) + "GB");
    }
    preFrameUpdateStatic();
    game.preFrameUpdate();
    game.frameUpdate();
    game.postFrameUpdate();
    postFrameUpdateStatic();
  }

  private static void preFrameUpdateStatic() {
    PApplicationWindow.preFrameUpdate();
    PAssetManager.preFrameUpdate();
    PAudio.preFrameUpdate();
  }

  private static void postLogicUpdateStatic() {
    PPhysicsEngine.postLogicUpdate();
  }

  private static void postFrameUpdateStatic() {
    PPhysicsEngine.postFrameUpdate();
    glProfiler.reset();
    if (PKeyboard.isDown(Input.Keys.ALT_RIGHT) && PKeyboard.isFrameJustDown(Input.Keys.ENTER)) {
      if (Gdx.graphics.isFullscreen()) {
        Gdx.graphics.setWindowedMode(1920, 1080);
      } else {
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
      }
    }
    PAudio.postFrameUpdate();
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
