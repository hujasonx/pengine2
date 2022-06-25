package com.phonygames.pengine.logging;

import com.phonygames.pengine.math.PNumberUtils;

public class PLog {
  private static final int MESSAGE_BUFFER_SIZE = 10000;
  private static PLogMessage[] MESSAGE_BUFFER = new PLogMessage[MESSAGE_BUFFER_SIZE];
  private static int nextIndex = 0;
  private static boolean printToStdOut = true;

  public static PLogMessage c(String message) {
    return log(PLogMessage.CRITICAL, message);
  }

  public static PLogMessage log(int level, String message) {
    PLogMessage pLogMessage = MESSAGE_BUFFER[nextIndex];
    pLogMessage.reset();
    pLogMessage.setLevel(level);
    pLogMessage.setMessage(message);
    if (printToStdOut) {
      if (level == PLogMessage.WARNING || level == PLogMessage.ERROR || level == PLogMessage.CRITICAL) {
        System.err.println("PLog : " + pLogMessage.getMessage());
      } else {
        System.out.println("PLog : " + pLogMessage.getMessage());
      }
    }
    nextIndex++;
    if (nextIndex >= MESSAGE_BUFFER_SIZE) {
      nextIndex = 0;
    }
    return pLogMessage;
  }

  public static PLogMessage e(Exception e) {
    return log(PLogMessage.ERROR, e.getMessage()).setException(e);
  }

  /**
   * Returns the <a href="#{@link}">{@link PLogMessage}</a> at the provided location in history.
   * @param indexFromRear the index from the rear, such that 0 is the last element.
   * @return the logmessage
   */
  public static PLogMessage getLogMessage(int indexFromRear) {
    int index = nextIndex - 1 - indexFromRear;
    return MESSAGE_BUFFER[PNumberUtils.mod(index, MESSAGE_BUFFER_SIZE)];
  }

  public static PLogMessage i(String message) {
    return log(PLogMessage.INFO, message);
  }

  public static void init() {
    for (int a = 0; a < MESSAGE_BUFFER_SIZE; a++) {
      MESSAGE_BUFFER[a] = new PLogMessage();
    }
  }

  public static PLogMessage v(String message) {
    return log(PLogMessage.VERBOSE, message);
  }

  public static PLogMessage verbose(String message) {
    return log(PLogMessage.VERBOSE, message);
  }

  public static PLogMessage w(String message) {
    return log(PLogMessage.WARNING, message);
  }

  public static PLogMessage w(String message, Exception e) {
    return log(PLogMessage.WARNING, message).setException(e);
  }
}
