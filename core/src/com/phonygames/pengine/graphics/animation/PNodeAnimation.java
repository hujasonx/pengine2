package com.phonygames.pengine.graphics.animation;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PNodeAnimation {
  @Getter(value = AccessLevel.MODULE)
  @Accessors(fluent = true)
  private final PVec4 defaultRotation = PVec4.obtain();
  @Getter(value = AccessLevel.MODULE)
  @Accessors(fluent = true)
  private final PVec3 defaultScale = PVec3.obtain();
  @Getter(value = AccessLevel.MODULE)
  @Accessors(fluent = true)
  private final PVec3 defaultTranslation = PVec3.obtain();
  @Getter(value = AccessLevel.MODULE)
  @Accessors(fluent = true)
  private final String nodeName;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PList<NodeKeyframe<PVec4>> rotationKeyframes = new PList<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PList<NodeKeyframe<PVec3>> scaleKeyframes = new PList<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PList<NodeKeyframe<PVec3>> translationKeyframes = new PList<>();
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private Interpolation interpolationTranslation, interpolationRotation, interpolationScale;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float length;

  private PNodeAnimation(String nodeName) {
    this.nodeName = nodeName;
  }

  public void apply(PMat4 output, float t, float alpha) {
    PMat4 temp = PMat4.obtain();
    getTransformAtT(temp, t);
    output.lerp(temp, alpha);
    temp.free();
  }

  private final PMat4 getTransformAtT(PMat4 out, float t) {
    PAssert.isTrue(t >= 0);
    out.idt();
    PVec3 outTra = getTranslationAtT(PVec3.obtain(), t);
    PVec4 outRot = getRotationAtT(PVec4.obtain(), t);
    PVec3 outScl = getScaleAtT(PVec3.obtain(), t);
    out.set(outTra, outRot, outScl);
    outTra.free();
    outRot.free();
    outScl.free();
    return out;
  }

  private final PVec4 getRotationAtT(PVec4 out, float t) {
    if (rotationKeyframes().isEmpty()) {
      return out.set(defaultRotation());
    }
    int keyFrameIndexLower = getLowerNodeKeyframeIndexForTime(rotationKeyframes(), t);
    out.set(rotationKeyframes().get(keyFrameIndexLower).value);
    if (interpolationRotation() == Interpolation.STEP) {
      return out;
    }
    float mixAmountWithUpperKeyframe = getMixAmountWithNextKeyframe(rotationKeyframes(), keyFrameIndexLower, t);
    if (mixAmountWithUpperKeyframe == 0) {
      return out;
    }
    if (interpolationRotation() == Interpolation.LINEAR) {
      out.slerp(rotationKeyframes().get(keyFrameIndexLower + 1).value, mixAmountWithUpperKeyframe);
    } else {
      // Cubic.
      PAssert.failNotImplemented("Cubic interpolation");
    }
    return out;
  }

  /**
   * Returns the lower Keyframe index for a given time.
   * @param time >
   * @return
   */
  private final int getLowerNodeKeyframeIndexForTime(PList keyFrames, float time) {
    PAssert.isTrue(!keyFrames.isEmpty());
//    for (int a = 1; a < keyFrames.size; a++) {
//      if (((NodeKeyframe) keyFrames.get(a)).keytime > time) {
//        return a - 1;
//      }
//    }
//    return keyFrames.size - 1;
    int lowerIndex = 0; int higherIndex = keyFrames.size - 1;
    while (lowerIndex < higherIndex) {
      int guessIndex = (lowerIndex + higherIndex) / 2;
      float guessTime = ((NodeKeyframe) keyFrames.get(guessIndex)).keytime;
      if (guessTime > time) {
        higherIndex = guessIndex;
      } else if (guessTime < time) {
        lowerIndex = guessIndex;
      }

      if (lowerIndex >= higherIndex - 1) {
        return lowerIndex;
      }
    }
    return keyFrames.size - 1;
  }

  private final float getMixAmountWithNextKeyframe(PList keyFrames, int lowerKeyframeIndex, float time) {
    if (lowerKeyframeIndex >= keyFrames.size - 1) {
      return 0;
    }
    PAssert.isFalse(time < 0);
    float timeOfLower = ((NodeKeyframe) keyFrames.get(lowerKeyframeIndex)).keytime;
    float timeOfUpper = ((NodeKeyframe) keyFrames.get(lowerKeyframeIndex + 1)).keytime;
    return (time - timeOfLower) / (timeOfUpper - timeOfLower);
  }

  private final PVec3 getScaleAtT(PVec3 out, float t) {
    if (scaleKeyframes().isEmpty()) {
      return out.set(defaultScale());
    }
    int keyFrameIndexLower = getLowerNodeKeyframeIndexForTime(scaleKeyframes(), t);
    out.set(scaleKeyframes().get(keyFrameIndexLower).value);
    if (interpolationRotation() == Interpolation.STEP) {
      return out;
    }
    float mixAmountWithUpperKeyframe = getMixAmountWithNextKeyframe(scaleKeyframes(), keyFrameIndexLower, t);
    if (mixAmountWithUpperKeyframe == 0) {
      return out;
    }
    if (interpolationRotation() == Interpolation.LINEAR) {
      out.lerp(scaleKeyframes().get(keyFrameIndexLower + 1).value, mixAmountWithUpperKeyframe);
    } else {
      // Cubic.
      PAssert.failNotImplemented("Cubic interpolation");
    }
    return out;
  }

  private final PVec3 getTranslationAtT(PVec3 out, float t) {
    if (translationKeyframes().isEmpty()) {
      return out.set(defaultTranslation());
    }
    int keyFrameIndexLower = getLowerNodeKeyframeIndexForTime(translationKeyframes(), t);
    out.set(translationKeyframes().get(keyFrameIndexLower).value);
    if (interpolationRotation() == Interpolation.STEP) {
      return out;
    }
    float mixAmountWithUpperKeyframe = getMixAmountWithNextKeyframe(translationKeyframes(), keyFrameIndexLower, t);
    if (mixAmountWithUpperKeyframe == 0) {
      return out;
    }
    if (interpolationRotation() == Interpolation.LINEAR) {
      out.lerp(translationKeyframes().get(keyFrameIndexLower + 1).value, mixAmountWithUpperKeyframe);
    } else {
      // Cubic.
      PAssert.failNotImplemented("Cubic interpolation");
    }
    return out;
  }

  public enum Interpolation {
    LINEAR, CUBIC, STEP
  }

  public static class Builder extends PBuilder {
    private final PNodeAnimation nodeAnimation;

    public Builder(String name) {
      nodeAnimation = new PNodeAnimation(name);
    }

    public Builder addNodeKeyframe(float time, @NonNull PVec3 translation, @NonNull PVec4 rotation,
                                   @NonNull PVec3 scale) {
      addTranslationKeyframe(time, translation);
      return this;
    }

    public Builder addTranslationKeyframe(float time, @NonNull PVec3 translation) {
      checkLock();
      NodeKeyframe<PVec3> t = new NodeKeyframe<>(time, translation);
      nodeAnimation.translationKeyframes().add(t);
      nodeAnimation.length = Math.max(nodeAnimation.length, time);
      return this;
    }

    public Builder addRotationKeyframe(float time, @NonNull PVec4 rotation) {
      checkLock();
      NodeKeyframe<PVec4> t = new NodeKeyframe<>(time, rotation);
      nodeAnimation.rotationKeyframes().add(t);
      nodeAnimation.length = Math.max(nodeAnimation.length, time);
      return this;
    }

    public Builder addScaleKeyframe(float time, @NonNull PVec3 scale) {
      checkLock();
      NodeKeyframe<PVec3> s = new NodeKeyframe<>(time, scale);
      nodeAnimation.scaleKeyframes().add(s);
      nodeAnimation.length = Math.max(nodeAnimation.length, time);
      return this;
    }

    public PNodeAnimation build() {
      lockBuilder();
      return nodeAnimation;
    }

    public Builder setDefaultTRS(@NonNull Vector3 t, @NonNull Quaternion r, @NonNull Vector3 s) {
      nodeAnimation.defaultTranslation.set(t);
      nodeAnimation.defaultRotation.set(r);
      nodeAnimation.defaultScale().set(s);
      return this;
    }

    public Builder setInterpolationRotation(@NonNull Interpolation interpolation) {
      nodeAnimation.interpolationRotation = interpolation;
      return this;
    }

    public Builder setInterpolationScale(@NonNull Interpolation interpolation) {
      nodeAnimation.interpolationScale = interpolation;
      return this;
    }

    public Builder setInterpolationTranslation(@NonNull Interpolation interpolation) {
      nodeAnimation.interpolationTranslation = interpolation;
      return this;
    }
  }
}
