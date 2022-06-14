package com.phonygames.pengine.graphics.animation;

import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.Getter;

public class PAnimation {
  @Getter
  private final String name;
  @Getter(lazy = true)
  private final PStringMap<PNodeAnimation> nodeAnimations = new PStringMap<>();
  @Getter
  private float length = 0;

  private PAnimation(String name) {
    this.name = name;
  }

  /**
   * Additively applys the animation with the given weight.
   * @param transforms
   * @param t
   * @param alpha
   */
  public void apply(PStringMap<PMat4> transforms, float t, float alpha) {
    for (PMap.Entry<String, PMat4> e : transforms) {
      PNodeAnimation nodeAnimation = getNodeAnimations().get(e.k());
      if (nodeAnimation == null) {
        continue;
      }
      nodeAnimation.apply(e.v(), t, alpha);
    }
    return;
  }

  public static class Builder extends PBuilder {
    private final PAnimation animation;

    public Builder(String name) {
      animation = new PAnimation(name);
    }

    public Builder addNodeAnimation(PNodeAnimation nodeAnimation) {
      animation.getNodeAnimations().put(nodeAnimation.nodeName(), nodeAnimation);
      animation.length = Math.max(animation.length, nodeAnimation.length());
      return this;
    }

    public PAnimation build() {
      lockBuilder();
      return animation;
    }
  }
}
