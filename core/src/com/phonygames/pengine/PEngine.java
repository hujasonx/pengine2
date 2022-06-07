package com.phonygames.pengine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.phonygames.pengine.exception.PRuntimeException;
import com.phonygames.pengine.graphics.PApplicationWindow;

import lombok.Getter;

public class PEngine extends ApplicationAdapter {
  public static final String LIBRARY_NAME = "penginelib";

  public static int frameCount = 0, logicUpdateCount = 1, t = 0, uit = 0, logict = 0;
  public static float logicupdateframeratio = .1f; // The ratio between the prev and next logic updates that the current frame is at, temporally.public static float uit = 0;
  public static float dt = .1f; // dt and t are in frame time.
  public static float logictimestep = 1f / 60f;
  //  public static float logictimestep = 1f / 240f;
  public static float uidt = .1f;
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
      e.printStackTrace();
    }

    PApplicationWindow.init();
    PAssetManager.init();
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
  }

  private void processLogicUpdateForFrame() {
    currentPhase = Phase.LOGIC;
    while (logict < t) {
      logict += logictimestep;

      game.preLogicUpdate();

      game.logicUpdate();

      game.postLogicUpdate();

    }

  }

  private void frameUpdate() {
    PApplicationWindow.preFrameUpdate();
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
