package com.phonygames.pengine.graphics.animation;

import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;

import lombok.Getter;
import lombok.NonNull;

public class PNodeAnimation {
  private final PList<NodeKeyFrame> nodeKeyFrames = new PList<>();
  @Getter
  private final String nodeName;

  private PNodeAnimation(String nodeName) {
    this.nodeName = nodeName;
  }

  public static class Builder extends PBuilder {
    private final PNodeAnimation nodeAnimation;

    public Builder(String name) {
      nodeAnimation = new PNodeAnimation(name);
    }

    public Builder addNodeKeyFrame(float time, @NonNull PVec3 translation, @NonNull PVec4 rotation, @NonNull PVec3 scaling) {
      NodeKeyFrame keyFrame = new NodeKeyFrame();
      keyFrame.t = time;
      keyFrame.translation.set(translation);
      keyFrame.rotation.set(rotation);
      keyFrame.scaling.set(scaling);
      nodeAnimation.nodeKeyFrames.add(keyFrame);
      return this;
    }

    public PNodeAnimation build() {
      lockBuilder();
      return nodeAnimation;
    }
  }

  private static class NodeKeyFrame {
    float t;
    @Getter(lazy = true)
    private final PVec3 translation = PVec3.obtain(), scaling = PVec3.obtain();
    @Getter(lazy = true)
    private final PVec4 rotation = PVec4.obtain();
  }
}
