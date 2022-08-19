package com.phonygames.pengine.util;

import android.support.annotation.NonNull;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Stores an approximate history of a vector. */
public class PVecTracker<T extends PVec<T>> {
  private final PPool pool;
  private final PList<T> samples;
  private int headIndex = 0, tailIndex = 0;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The number of previous positions to remember. */ private int previousPositionsToKeep;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The length of time the previous position tracking should track. */ private float
      previousPositionsTrackingDuration;
  private float lastSampleT;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private T trackedVec;
  private T startVal;

  public PVecTracker(@NonNull PPool pool) {
    this.pool = pool;
    startVal = (T)pool.obtain();
    this.samples = new PList<>(pool);
    reset();
  }

  public void frameUpdate() {
    if (trackedVec == null) {return;}
    float timeBetweenSamples = previousPositionsTrackingDuration / (previousPositionsToKeep - 1);
    if (lastSampleT == -1 || lastSampleT + timeBetweenSamples < PEngine.t) {
      lastSampleT = PEngine.t;
      addSamplePoint(trackedVec);
    }


    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
//      for (int a = 0; a < previousPositionsToKeep - 1; a++) {
//        int b = a + 1;
//        PVec3 previousPositionA = (PVec3)getSamplePointFromLast(a);
//        PVec3 previousPositionB = (PVec3)getSamplePointFromLast(b);
//        PVec4 colA = pool.vec4().setHSVA(1f / previousPositionsToKeep * a, 1, 1, 1);
//        PVec4 colB = pool.vec4().setHSVA(1f / previousPositionsToKeep * b,1, 1, 1);
//        if (previousPositionA != null && previousPositionB != null) {
//          PDebugRenderer.line(previousPositionA, previousPositionB, colA, colB, 2, 2);
//        }
//      }
      for (float a = 0; a < 1; a += .1f) {
        float b = .1f + a;
        PVec3 previousPositionA = (PVec3)getPreviousPositionNormalizedTime((T)pool.vec3(), a);
        PVec3 previousPositionB = (PVec3)getPreviousPositionNormalizedTime((T)pool.vec3(), b);
        PVec4 colA = pool.vec4().setHSVA(1f / previousPositionsToKeep * a, 1, 1, 1);
        PVec4 colB = pool.vec4().setHSVA(1f / previousPositionsToKeep * b,1, 1, 1);
        if (previousPositionA != null && previousPositionB != null) {
          PDebugRenderer.line(previousPositionA, previousPositionB, colA, colB, 2, 2);
        }
      }
    }
  }

  private void addSamplePoint(T value) {
    PAssert.isFalse(this.samples.isEmpty(), "Empty samples for PVecTracker (Call beginTracking first)");
    this.samples.get(headIndex).set(value);
    headIndex = PNumberUtils.mod(headIndex + 1, previousPositionsToKeep);
    if (tailIndex == headIndex) {
      tailIndex = PNumberUtils.mod(tailIndex + 1, previousPositionsToKeep);
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
      return out.set(trackedVec).lerp(p0,timeAgo / (PEngine.t - lastSampleT));
    }
    int lowerIndex = (int) indexOffset;
    float lerpAmount = indexOffset - lowerIndex;
    T p0 = getSamplePointFromLast(lowerIndex);
    T p1 = getSamplePointFromLast(lowerIndex + 1);
    if (p0 == null || p1 == null) {return null;}
    return out.set(p0).lerp(p1, lerpAmount);
  }

  public PVecTracker<T> beginTracking(T trackedVec, float previousPositionsTrackingDuration, int previousPositionsToKeep) {
    this.trackedVec = trackedVec;
    this.previousPositionsTrackingDuration = previousPositionsTrackingDuration;
    this.previousPositionsToKeep = previousPositionsToKeep;
    this.samples.clearAndFreePooled();
    // Set initial values to the initial position.
    for (int a = 0; a < previousPositionsToKeep; a++) {
      this.samples.genPooledAndAdd().set(trackedVec);
    }
    return this;
  }

  /** Returns null if it was not found. */
  private T getSamplePointFromLast(int sampleIndexOffsetFromLast) {
    if (sampleIndexOffsetFromLast < 0 || sampleIndexOffsetFromLast >= samples.size()) {
      return null;
    }
    return this.samples.get(PNumberUtils.mod(headIndex - 1 - sampleIndexOffsetFromLast, this.samples.size()));
  }

  public void reset() {
    headIndex = 0;
    tailIndex = 0;
    lastSampleT = -1;
    trackedVec = null;
    previousPositionsTrackingDuration = 1;
    previousPositionsToKeep = 10;
    samples.clearAndFreePooled();
  }
}
