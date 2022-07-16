package com.phonygames.pengine.math;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PParametricCurve<T extends PVec<T>> implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PParametricCurve.PParametricCurve1> pool1 =
      new PPool<PParametricCurve.PParametricCurve1>() {
        @Override protected PParametricCurve.PParametricCurve1 newObject() {
          return new PParametricCurve.PParametricCurve1();
        }
      };
  private static final PPool<PParametricCurve.PParametricCurve2> pool2 =
      new PPool<PParametricCurve.PParametricCurve2>() {
        @Override protected PParametricCurve.PParametricCurve2 newObject() {
          return new PParametricCurve.PParametricCurve2();
        }
      };
  private final PList<KeyFrame<T>> keyFrames;
  private final PPool<T> tPool;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private Interpolation interpolation = Interpolation.LINEAR;

  private PParametricCurve(PPool<T> tPool, PPool<KeyFrame<T>> keyFramePPool) {
    this.tPool = tPool;
    keyFrames = new PList<>(keyFramePPool);
    reset();
  }

  @Override public void reset() {
    interpolation = Interpolation.LINEAR;
    clear();
  }

  public void clear() {
    keyFrames.clearAndFreePooled();
  }

  public static PParametricCurve.PParametricCurve1 obtain1() {
    return pool1.obtain();
  }

  public static PParametricCurve.PParametricCurve2 obtain2() {
    return pool2.obtain();
  }

  public PParametricCurve<T> addKeyFrame(float time, T t) {
    KeyFrame<T> keyFrame = keyFrames.genPooledAndAdd();
    keyFrame.t = time;
    keyFrame.value.set(t);
    keyFrames.sort();
    return this;
  }

  public T get(T out, float time) {
    return getValueAtT(out, time);
  }

  private final T getValueAtT(T out, float t) {
    if (keyFrames.isEmpty()) {
      return out.setZero();
    }
    int keyFrameIndexLower = getLowerNodeKeyframeIndexForTime(t);
    out.set(keyFrames.get(keyFrameIndexLower).value);
    if (interpolation == Interpolation.STEP) {
      return out;
    }
    float mixAmountWithUpperKeyframe = getMixAmountWithNextKeyframe(keyFrameIndexLower, t);
    if (mixAmountWithUpperKeyframe == 0) {
      return out;
    }
    if (interpolation == Interpolation.LINEAR) {
      out.lerp(keyFrames.get(keyFrameIndexLower + 1).value, mixAmountWithUpperKeyframe);
    } else {
      // Cubic.
      PAssert.failNotImplemented("Cubic interpolation");
    }
    return out;
  }

  /**
   * Returns the lower Keyframe index for a given time.
   *
   * @param time >
   * @return
   */
  private final int getLowerNodeKeyframeIndexForTime(float time) {
    PAssert.isTrue(!keyFrames.isEmpty());
    if (time >= length()) {
      return keyFrames.size() - 1;
    }
    int lowerIndex = 0;
    int higherIndex = keyFrames.size() - 1;
    while (lowerIndex < higherIndex) {
      int guessIndex = (lowerIndex + higherIndex) / 2;
      float guessTime = (keyFrames.get(guessIndex)).t;
      if (guessTime > time) {
        higherIndex = guessIndex - 1;
      } else if (guessTime < time) {
        lowerIndex = guessIndex;
      } else {
        return guessIndex;
      }
      if (lowerIndex >= higherIndex - 1) {
        return lowerIndex;
      }
    }
    return keyFrames.size() - 1;
  }

  private final float getMixAmountWithNextKeyframe(int lowerKeyframeIndex, float time) {
    if (lowerKeyframeIndex >= keyFrames.size() - 1) {
      return 0;
    }
    PAssert.isFalse(time < 0);
    float timeOfLower = (keyFrames.get(lowerKeyframeIndex)).t;
    float timeOfUpper = (keyFrames.get(lowerKeyframeIndex + 1)).t;
    return (time - timeOfLower) / (timeOfUpper - timeOfLower);
  }

  public float length() {
    if (keyFrames.isEmpty()) {return 0;}
    return keyFrames.peek().t;
  }

  public enum Interpolation {
    LINEAR, CUBIC, STEP
  }

  private static class KeyFrame<T extends PVec<T>> implements PPool.Poolable, Comparable<KeyFrame<T>> {
    // #pragma mark - PPool.Poolable
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    // #pragma end - PPool.Poolable
    final T value;
    float t = 0;

    private KeyFrame(PPool<T> pool) {
      value = pool.obtain();
    }

    @Override public int compareTo(KeyFrame<T> tKeyFrame) {
      return PNumberUtils.compareTo(t, tKeyFrame.t);
    }

    @Override public void reset() {
      t = 0;
      value.setZero();
    }
  }

  public static class PParametricCurve1 extends PParametricCurve<PVec1> {
    private static PPool<PParametricCurve.KeyFrame<PVec1>> keyFramePool =
        new PPool<PParametricCurve.KeyFrame<PVec1>>() {
          @Override protected PParametricCurve1.KeyFrame newObject() {
            return new KeyFrame();
          }
        };
    private static PVec1 temp = PVec1.obtain();

    private PParametricCurve1() {
      super(PVec1.getStaticPool(), keyFramePool);
    }

    public PParametricCurve1 addKeyFrame(float time, float x) {
      addKeyFrame(time, temp.set(x));
      return this;
    }

    static class KeyFrame extends PParametricCurve.KeyFrame<PVec1> {
      private KeyFrame() {
        super(PVec1.getStaticPool());
      }
    }
  }

  public static class PParametricCurve2 extends PParametricCurve<PVec2> {
    private static PPool<PParametricCurve.KeyFrame<PVec2>> keyFramePool =
        new PPool<PParametricCurve.KeyFrame<PVec2>>() {
          @Override protected PParametricCurve2.KeyFrame newObject() {
            return new KeyFrame();
          }
        };
    private static PVec2 temp = PVec2.obtain();

    private PParametricCurve2() {
      super(PVec2.getStaticPool(), keyFramePool);
    }

    public PParametricCurve2 addKeyFrame(float time, float x, float y) {
      addKeyFrame(time, temp.set(x, y));
      return this;
    }

    static class KeyFrame extends PParametricCurve.KeyFrame<PVec2> {
      private KeyFrame() {
        super(PVec2.getStaticPool());
      }
    }
  }
}
