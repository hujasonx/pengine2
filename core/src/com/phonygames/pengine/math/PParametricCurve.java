package com.phonygames.pengine.math;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.util.collection.PList;
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
  private static final PPool<PParametricCurve.PParametricCurve3> pool3 =
      new PPool<PParametricCurve.PParametricCurve3>() {
        @Override protected PParametricCurve.PParametricCurve3 newObject() {
          return new PParametricCurve.PParametricCurve3();
        }
      };
  protected final PList<KeyFrame<T>> keyFrames;
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

  public static PParametricCurve.PParametricCurve3 obtain3() {
    return pool3.obtain();
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
    if (time <= keyFrames.get(0).t) {
      return 0;
    }
    int lowerIndex = 0;
    int higherIndex = keyFrames.size() - 1;
    while (lowerIndex < higherIndex) {
      int guessIndex = (lowerIndex + higherIndex) / 2;
      if (guessIndex == keyFrames.size() - 1) { return guessIndex; }
      float guessTime = (keyFrames.get(guessIndex)).t;
      float nextTime = (keyFrames.get(guessIndex + 1)).t;
      if (guessTime <= time && nextTime > time) {
        return guessIndex;
      }
      if (guessTime > time) {
        higherIndex = guessIndex;
      } else {
        lowerIndex = guessIndex;
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
    if (time <= timeOfLower) { return 0; }
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

    public float get(float t) {
      get(temp, t);
      return temp.x();
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

  public static class PParametricCurve3 extends PParametricCurve<PVec3> {
    private static PPool<PParametricCurve.KeyFrame<PVec3>> keyFramePool =
        new PPool<PParametricCurve.KeyFrame<PVec3>>() {
          @Override protected PParametricCurve3.KeyFrame newObject() {
            return new KeyFrame();
          }
        };
    private static PVec3 temp = PVec3.obtain();

    private PParametricCurve3() {
      super(PVec3.getStaticPool(), keyFramePool);
    }

    public PParametricCurve3 addKeyFrame(float time, float x, float y, float z) {
      addKeyFrame(time, temp.set(x, y, z));
      return this;
    }

    public void debugRender() {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        PVec3 p0 = pool.vec3();
        PVec3 p1 = pool.vec3();
        PVec4 c0 = pool.vec4(.5f, .5f, .5f, .5f);
        PVec4 c1 = pool.vec4();
        for (int a = 0; a < this.keyFrames.size() - 1; a++) {
          p0.set(keyFrames.get(a).value);
          p1.set(keyFrames.get(a + 1).value);
          PDebugRenderer.line(p0, p1, c0, c0, 2, 2);
        }
        float step = .01f;
        for (float t = 0; t <= length() - step; t+= step) {
          get(p0,t);
          get(p1,t + step);
          c0.setHSVA(t,1,1,.3f);
          c1.setHSVA(t + step,1,1,.3f);
          PDebugRenderer.line(p0,p1,c0, c1, 2, 2);
        }
      }
    }

    static class KeyFrame extends PParametricCurve.KeyFrame<PVec3> {
      private KeyFrame() {
        super(PVec3.getStaticPool());
      }
    }
  }
}
