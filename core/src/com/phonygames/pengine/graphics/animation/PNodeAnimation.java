package com.phonygames.pengine.graphics.animation;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

public class PNodeAnimation {
  @Getter(value = AccessLevel.MODULE)
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
//    PMat4 temp = PMat4.obtain();
//    getTransformAtT(temp, t);
//    output.lerp(temp, alpha);
//    temp.free();
  }

  private final PMat4 getTransformAtT(PMat4 out, float t) {
    PAssert.isTrue(t >= 0);
    out.idt();
    PVec4 outRot = PVec4.obtain().setIdentityQuaternion();
    PVec3 outTra = PVec3.obtain().setZero();
    PVec3 outScl = PVec3.obtain().setOne();
    if (!rotationKeyframes().isEmpty()) {
      int keyFrameIndexLower = getLowerNodeKeyframeIndexForTime(rotationKeyframes(), t);
      outRot.set(rotationKeyframes().get(keyFrameIndexLower).value);
    }
    out.set(outTra, outRot, outScl);
    outTra.free();
    outRot.free();
    outScl.free();
    return out;
  }

  /**
   * Returns the lower Keyframe index for a given time.
   * @param time >
   * @return
   */
  private final int getLowerNodeKeyframeIndexForTime(PList Keyframes, float time) {
    PAssert.isTrue(!Keyframes.isEmpty());
    for (int a = 1; a < Keyframes.size; a++) {
      if (((NodeKeyframe) Keyframes.get(a)).keytime > time) {
        return a - 1;
      }
    }
    return Keyframes.size - 1;
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

    public Builder setInterpolationRotation(Interpolation interpolation) {
      nodeAnimation.interpolationRotation = interpolation;
      return this;
    }

    public Builder setInterpolationScale(Interpolation interpolation) {
      nodeAnimation.interpolationScale = interpolation;
      return this;
    }

    public Builder setInterpolationTranslation(Interpolation interpolation) {
      nodeAnimation.interpolationTranslation = interpolation;
      return this;
    }
  }
}
