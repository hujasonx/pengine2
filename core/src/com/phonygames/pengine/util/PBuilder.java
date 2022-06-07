package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;

import lombok.Getter;

/**
 * A helper class for builder objects, which allows easily locking itself.
 */
public class PBuilder {
  @Getter
  private boolean locked = false;

  protected void checkLock() {
    PAssert.isTrue(!locked);
  }

  protected void lockBuilder() {
    locked = true;
  }
}
