package com.phonygames.pengine.exception;

import com.phonygames.pengine.PEngine;

public class PAssert {
  public static void fail(String message) {
    throw new PRuntimeException(message);
  }

  public static boolean isTrue(boolean b, String message) {
    if (!b) {
      throw new PRuntimeException(message);
    }
    return b;
  }

  public static void isTrue(boolean b) {
    isTrue(b, "*** PAssert :: isTrue ***");
  }

  public static void isNotNull(Object o, String message) {
    if (o == null) {
      throw new PRuntimeException(message);
    }
    ;
  }

  public static void isNotNull(Object o) {
    isNotNull(o, "*** PAssert :: isNotNull ***");
  }

  public static float isNotNan(float f) {
    isTrue(!Float.isNaN(f));
    return f;
  }

  public static void isLogicUpdate() {
    isTrue(PEngine.getCurrentPhase() == PEngine.Phase.LOGIC);
  }

  public static void isFrameUpdate() {
    isTrue(PEngine.getCurrentPhase() == PEngine.Phase.FRAME);
  }
}