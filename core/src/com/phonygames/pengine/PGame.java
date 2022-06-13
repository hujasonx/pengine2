package com.phonygames.pengine;

public interface PGame {
  void frameUpdate();
  void init();
  void logicUpdate();
  void postFrameUpdate();
  void postLogicUpdate();
  void preFrameUpdate();
  void preLogicUpdate();
}
