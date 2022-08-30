package com.phonygames.pengine.util;

import android.support.annotation.NonNull;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Stores an approximate history of a vector. */
public class PVecTracker<T extends PVec<T>> {
  private final PPool<T> pool;
  private final PList<T> samples;
  private int headIndex = 0, tailIndex = 0;
  private float lastSampleT;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The number of previous positions to remember. */ private int previousPositionsToKeep;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The length of time the previous position tracking should track. */ private float
      previousPositionsTrackingDuration;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float startTrackingT;
  private T startVal;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private T trackedVec;

  public PVecTracker(@NonNull PPool<T> pool) {
    this.pool = pool;
    startVal = (T) pool.obtain();
    this.samples = new PList<>(pool);
    reset();
  }

  public void reset() {
    headIndex = 0;
    tailIndex = 0;
    startTrackingT = 0;
    lastSampleT = -1;
    trackedVec = null;
    previousPositionsTrackingDuration = 1;
    previousPositionsToKeep = 10;
    samples.clearAndFreePooled();
  }

  public PVecTracker<T> beginTracking(T trackedVec, float previousPositionsTrackingDuration,
                                      int previousPositionsToKeep) {
    this.trackedVec = trackedVec;
    this.previousPositionsTrackingDuration = previousPositionsTrackingDuration;
    this.previousPositionsToKeep = previousPositionsToKeep;
    this.samples.clearAndFreePooled();
    startTrackingT = PEngine.t;
    // Set initial values to the initial position.
    for (int a = 0; a < previousPositionsToKeep; a++) {
      this.samples.genPooledAndAdd().set(trackedVec);
    }
    // Sample the trackedVec once.
    addSampleVec(trackedVec);
    lastSampleT = startTrackingT;
    return this;
  }

  private void addSampleVec(T value) {
    PAssert.isFalse(this.samples.isEmpty(), "Empty samples for PVecTracker (Call beginTracking first)");
    this.samples.get(headIndex).set(value);
    headIndex = PNumberUtils.mod(headIndex + 1, previousPositionsToKeep);
    if (tailIndex == headIndex) {
      tailIndex = PNumberUtils.mod(tailIndex + 1, previousPositionsToKeep);
    }
  }

  public void frameUpdate() {
    if (trackedVec == null) {return;}
    float timeBetweenSamples = previousPositionsTrackingDuration / (previousPositionsToKeep - 1);
    // Check to see if we should add another sample.
    while (lastSampleT + timeBetweenSamples < PEngine.t) {
      T newValue = pool.obtain().set(getSamplePointFromLast(0));
      float timeElapsedSinceLastSample = PEngine.t - lastSampleT;
      newValue.lerp(trackedVec, timeBetweenSamples / timeElapsedSinceLastSample);
      addSampleVec(trackedVec);
      lastSampleT += timeBetweenSamples;
      newValue.free();
    }
  }

  /**
   * @param out
   * @param normalizeTime the time [0, 1] ago.
   * @return
   */
  public T getPreviousPositionNormalizedTime(T out, float normalizeTime) {
    T ret = getPreviousPosition(out, normalizeTime * previousPositionsTrackingDuration);
    return ret;
  }

  public T getPreviousPosition(T out, float timeAgo) {
    if (lastSampleT == -1) {
      return trackedVec;
    }
    float timeBetweenSamples = previousPositionsTrackingDuration / (previousPositionsToKeep - 1);
    float indexOffset = (timeAgo - (PEngine.t - lastSampleT)) / timeBetweenSamples;
    if (indexOffset < 0) {
      T p0 = getSamplePointFromLast(0);
      if (p0 == null) {return null;}
      return out.set(trackedVec).lerp(p0, timeAgo / (PEngine.t - lastSampleT));
    }
    int lowerIndex = (int) indexOffset;
    float lerpAmount = indexOffset - lowerIndex;
    T p0 = getSamplePointFromLast(lowerIndex);
    T p1 = getSamplePointFromLast(lowerIndex + 1);
    if (p0 == null || p1 == null) {return null;}
    return out.set(p0).lerp(p1, lerpAmount);
  }

  /** Returns null if it was not found. */
  private T getSamplePointFromLast(int sampleIndexOffsetFromLast) {
    if (trackedVec == null) {return null;}
    if (sampleIndexOffsetFromLast < 0) {
      // Return the current value.
      return trackedVec;
    }
    if (sampleIndexOffsetFromLast >= samples.size()) {
      // Return the oldest recorded value.
      return samples.get(tailIndex);
    }
    return this.samples.get(PNumberUtils.mod(headIndex - 1 - sampleIndexOffsetFromLast, this.samples.size()));
  }
}
