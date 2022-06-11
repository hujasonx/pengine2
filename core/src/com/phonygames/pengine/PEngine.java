package com.phonygames.pengine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.phonygames.pengine.exception.PRuntimeException;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.lighting.PLight;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.util.PMap;

import lombok.Getter;

public class PEngine extends ApplicationAdapter {
  public static final String LIBRARY_NAME = "penginelib";

  public static int frameCount = 0, logicUpdateCount = 1;
  public static float logicupdateframeratio = .1f; // The ratio between the prev and next logic updates that the current frame is at, temporally.public static float uit = 0;
  public static float t = 0, dt = .1f, uit = 0, uidt = .1f, logict = 0; // dt and t are in frame time.
  public static float logictimestep = 1f / 60f;
  private static WindowedMean uidtWindowedMean = new WindowedMean(8);
  private static float timeScale = 1;
  private static PGame game;

  @Getter
  private static Phase currentPhase = Phase.INIT;
  @Getter
  private static PEngine pEngine;

  public PEngine(PGame game) {
    if (PEngine.pEngine != null) {
      throw new PRuntimeException("PEngine was already set!");
    }

    PEngine.game = game;
    pEngine = this;
  }

  public enum Phase {
    INIT, LOGIC, FRAME
  }

  @Override
  public void create() {
    try {
      new SharedLibraryLoader().load(LIBRARY_NAME);
    } catch (Exception e) {
    }

    PVertexAttributes.init();
    PTexture.init();
    PLight.initMeshes();
    PLog.init();
    PApplicationWindow.init();
    PMesh.init();
    PShaderProvider.init();
    PAssetManager.init();
    game.init();
  }

  @Override
  public void render() {
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
    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
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

      game.preLogicUpdate();

      game.logicUpdate();

      game.postLogicUpdate();

    }

  }

  private void frameUpdate() {
    PApplicationWindow.preFrameUpdate();
    PAssetManager.preFrameUpdate();
    game.preFrameUpdate();

    game.frameUpdate();

    game.postFrameUpdate();
  }

  @Override
  public void dispose() {
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    PApplicationWindow.triggerResize(width, height);
  }
}
