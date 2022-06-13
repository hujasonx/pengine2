package com.phonygames.pengine.graphics.animation;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

public class PNodeAnimation {
  @Getter
  private final Interpolation interpolation;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  private final PList<NodeKeyFrame> nodeKeyFrames = new PList<>();
  @Getter(value = AccessLevel.MODULE)
  private final String nodeName;
  private PNodeAnimation(String nodeName, Interpolation interpolation) {
    this.nodeName = nodeName;
    this.interpolation = interpolation;
  }

  public void apply(PMat4 output, float t, float weight) {
    PMat4 temp = PMat4.obtain();
    getTransformAtT(temp, t);
    output.mulAdd(temp, weight);
    temp.free();
  }

  public float getLength() {
    if (getNodeKeyFrames().isEmpty()) {return 0;}
    return getNodeKeyFrames().peek().t;
  }

  /**
   * Returns the lower keyframe index for a given time.
   *
   * @param time >
   * @return
   */
  private final int getLowerNodeKeyFrameIndexForTime(float time) {
    PAssert.isTrue(!getNodeKeyFrames().isEmpty());
    for (int a = 1; a < getNodeKeyFrames().size; a++) {
      if (getNodeKeyFrames().get(a).t > time) {
        return a - 1;
      }
    }

    return getNodeKeyFrames().size - 1;
  }

  private final PMat4 getTransformAtT(PMat4 out, float t) {

    return out;
  }

  public enum Interpolation {
    LINEAR,
    CUBIC,
    STEP
  }

  public static class Builder extends PBuilder {
    private final PNodeAnimation nodeAnimation;

    Builder(String name, Interpolation interpolation) {
      nodeAnimation = new PNodeAnimation(name, interpolation);
    }

    public Builder addNodeKeyFrame(float time,
                                   @NonNull PVec3 translation,
                                   @NonNull PVec4 rotation,
                                   @NonNull PVec3 scaling) {
      checkLock();
      NodeKeyFrame keyFrame = new NodeKeyFrame();
      keyFrame.t = time;
      keyFrame.translation.set(translation);
      keyFrame.rotation.set(rotation);
      keyFrame.scaling.set(scaling);
      nodeAnimation.getNodeKeyFrames().add(keyFrame);
      return this;
    }

    public PNodeAnimation build() {
      lockBuilder();
      return nodeAnimation;
    }
  }

  private static class NodeKeyFrame {
    @Getter(lazy = true)
    private final PVec4 rotation = PVec4.obtain();
    @Getter(lazy = true)
    private final PVec3 translation = PVec3.obtain(), scaling = PVec3.obtain();
    float t;
  }
}
