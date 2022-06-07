package com.phonygames.pengine.exception;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.phonygames.pengine.logging.PLog;

public class PRuntimeException extends GdxRuntimeException {
  public PRuntimeException(String message) {
    super(message);

    PLog.e(this).pEngine();
  }
}
