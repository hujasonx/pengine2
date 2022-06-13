package com.phonygames.pengine.graphics.animation;

import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.Getter;

public class PAnimation {
  @Getter
  private final String name;
  @Getter(lazy = true)
  private final PStringMap<PNodeAnimation> nodeAnimations = new PStringMap<>();

  private PAnimation(String name) {
    this.name = name;
  }

  @Getter
  private float length = 0;

  public static class Builder extends PBuilder {
    private final PAnimation animation;

    public Builder(String name) {
      animation = new PAnimation(name);
    }

    public Builder addNodeAnimation(PNodeAnimation nodeAnimation) {
      animation.getNodeAnimations().put(nodeAnimation.getNodeName(), nodeAnimation);
      animation.length = Math.max(animation.length, nodeAnimation.getLength());
      return this;
    }

    public PAnimation build() {
      lockBuilder();
      return animation;
    }
  }

  /**
   * Additively applys the animation with the given weight.
   *
   * @param transforms
   * @param t
   * @param weight
   */
  public void apply(PStringMap<PMat4> transforms, float t, float weight) {
    for (PMap.Entry<String, PMat4> e : transforms) {
      PNodeAnimation nodeAnimation = getNodeAnimations().get(e.k());
      if (nodeAnimation == null) {
        continue;
      }

      nodeAnimation.apply(e.v(), t, weight);
    }

    return;
  }
}