package com.phonygames.pengine.util;

import com.phonygames.pengine.util.collection.PList;

/** Tracks blocking tasks. */
public class PBlockingTaskTracker {
  /** All the blocking tasks owners. */
  private final PList<Object> blockers = new PList<>();

  public boolean isBlocked() {
    return !blockers.isEmpty();
  }

  /** Adds the object to the task queue.*/
  public PBlockingTaskTracker addBlocker(Object o) {
    blockers.add(o);
    return this;
  }

  /** Removes the object from the task queue.*/
  public PBlockingTaskTracker removeBlocker(Object o) {
    blockers.removeValue(o, true);
    return this;
  }
}
