package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;

public interface PWriteLockable<T extends PWriteLockable> {
  default T forWriting() {
    PAssert.isFalse(isWriteLocked(), "Cannot write to write-locked object");
    return (T) this;
  }
  default boolean isWriteLocked() {
    return isLockWriting();
  }
  boolean isLockWriting();
  void setLockWriting(boolean lockWriting);
  default void lockWriting() {
    setLockWriting(true);
  }
  default void unlockWriting() {
    setLockWriting(false);
  }
}
