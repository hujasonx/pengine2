package com.phonygames.pengine.logging;

import java.util.HashSet;
import java.util.Set;

public class PLogMessage {
  public static final int CRITICAL = 0;
  public static final int ERROR = 1;
  public static final int WARNING = 2;
  public static final int INFO = 3;
  public static final int VERBOSE = 4;
  public static final String[] LOG_LEVEL_NAMES = new String[]{"Critical", "Error", "Warning", "Info", "Verbose"};

  private String message = "[Message Unset]";
  private int level = 0;
  private Exception exception;

  private Set<String> tags = new HashSet<>();

  public PLogMessage() {
  }

  public PLogMessage setMessage(String message) {
    this.message = message;
    return this;
  }

  public PLogMessage setLevel(int level) {
    this.level = level;
    return this;
  }

  public Set<String> getTags() {
    return tags;
  }

  public PLogMessage clearTags() {
    tags.clear();
    return this;
  }

  public PLogMessage reset() {
    tags.clear();
    exception = null;
    message = "";
    return this;
  }

  public Exception getException() {
    return exception;
  }

  public PLogMessage setException(Exception e) {
    this.exception = e;
    return this;
  }

  public PLogMessage tag(String tag) {
    tags.add(tag);
    return this;
  }

  public boolean hasTag(String tag) {
    return tags.contains(tag);
  }


  /**
   * Adds the "PEngine" tag to the PLogMessage.
   *
   * @return self for chaining
   */
  public PLogMessage pEngine() {
    tags.add("PEngine");
    return this;
  }

  public String getMessage() {
    return this.message;
  }
}
