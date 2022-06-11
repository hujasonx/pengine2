package com.phonygames.pengine.exception;

import com.phonygames.pengine.PEngine;

public class PAssert {

  public static void failNotImplemented() {
    fail("Not implemented");
  }

  public static void fail(String message) {
    throw new PRuntimeException(message);
  }

  public static void warnNotImplemented(String info) {
    warn("Not implemented: " + info);
  }

  public static void warn(String message) {
    try {
      throw new PRuntimeException(message);
    } catch (PRuntimeException e) {
      System.err.println(e);
    }
  }

  public static void warn(PRuntimeException runtimeException) {
    try {
      throw runtimeException;
    } catch (PRuntimeException e) {
      System.err.println(e);
    }
  }

  public static void equals(float x, float y) {
    isTrue(x == y);
  }

  public static void equals(float x, float y, String message) {
    isTrue(x == y, message);
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

  public static void isFalse(boolean b) {
    isTrue(!b, "*** PAssert :: isFalse ***");
  }

  public static void isNotNull(Object o, String message) {
    if (o == null) {
      throw new PRuntimeException(message);
    }
  }

  public static void isNull(Object o, String message) {
    if (o != null) {
      throw new PRuntimeException(message);
    }
  }


  public static void isNull(Object o) {
    isNull(o, "*** PAssert :: isNull ***");
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