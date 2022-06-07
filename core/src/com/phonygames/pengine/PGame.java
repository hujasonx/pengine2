package com.phonygames.pengine;

import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PRenderBuffer;

public interface PGame {

  void init();

  void preLogicUpdate();

  void logicUpdate();

  void postLogicUpdate();

  void preFrameUpdate();

  void frameUpdate();

  void postFrameUpdate();
}
