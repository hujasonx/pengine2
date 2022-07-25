package com.phonygames.pengine.graphics.animation;

import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

import lombok.Getter;
import lombok.val;

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
   *
   * @param transforms
   * @param t
   * @param alpha
   */
  public void apply(PStringMap<PMat4> transforms, float t, float alpha) {
    try (val it = transforms.obtainIterator()) {
      while (it.hasNext()) {
        PMap.Entry<String, PMat4> e = it.next();
        PNodeAnimation nodeAnimation = getNodeAnimations().get(e.k());
        if (nodeAnimation == null) {
          continue;
        }
        nodeAnimation.apply(e.v(), t, alpha);
      }
    }
    return;
  }

  /**
   * Multiplicitavely applys the animation with the given weight.
   *
   * @param transforms
   * @param t
   * @param alpha
   */
  public void mul(PStringMap<PMat4> transforms, PModel model, float t, float alpha) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      try (val it = transforms.obtainIterator()) {
        while (it.hasNext()) {
          PMap.Entry<String, PMat4> e = it.next();
          PNodeAnimation nodeAnimation = getNodeAnimations().get(e.k());
          if (nodeAnimation == null) {
            continue;
          }

          PMat4 animationOutput = pool.mat4(model.nodes().get(e.k()).transform());
          nodeAnimation.apply(animationOutput, t, alpha);
          PMat4 animationOutputRelative = pool.mat4(model.nodes().get(e.k()).transform()).inv().mul(animationOutput);
          e.v().mul(animationOutputRelative);
        }
      }
    }
    return;
  }

  public PStringMap<PMat4> outputNodeTransformsToMap(final PStringMap<PMat4> map, float t) {
    try (val it = getNodeAnimations().obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        if (map.has(e.k())) {
          // There was already a maxtrix in the map for this node.
          PMat4 mat4 = map.get(e.k());
          e.v().apply(mat4, t, 1);
        } else {
          // Put in a new matrix into the map for this node, ignoring alpha.
          PMat4 mat4 = map.genPooled(e.k());
          e.v().apply(mat4, t, 1);
        }
      }
    }
    return map;
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
