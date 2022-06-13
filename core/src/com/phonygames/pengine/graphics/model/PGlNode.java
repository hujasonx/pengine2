package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.graphics.PGlDrawCall;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PDeepCopyable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class PGlNode implements PDeepCopyable<PGlNode> {
  @Getter
  private final PGlDrawCall drawCall;
  @Getter(lazy = true)
  private final ArrayMap<String, PMat4> invBoneTransforms = new ArrayMap<>();
  @Getter(lazy = true)
  private final PMat4 worldTransform = PMat4.obtain();
  @Getter(lazy = true)
  private final PMat4 worldTransformInvTra = PMat4.obtain();
  @Getter
  @Setter
  // You can hook swap this out whenever you like.
  private PRenderContext.DataBufferEmitter dataBufferEmitter;
  @Getter
  @Setter
  private String id;

  public PGlNode(String id) {
    this.id = id;
    drawCall = PGlDrawCall.genTemplate();
  }

  @Override public PGlNode deepCopy() {
    return new PGlNode(this.id).deepCopyFrom(this);
  }

  @Override public PGlNode deepCopyFrom(@NonNull PGlNode other) {
    id = other.id;
    drawCall.deepCopyFrom(other.drawCall);
    getWorldTransform().set(other.getWorldTransform());
    getWorldTransformInvTra().set(other.getWorldTransformInvTra());
    getInvBoneTransforms().putAll(other.getInvBoneTransforms());
    dataBufferEmitter = other.dataBufferEmitter;
    return this;
  }

  public final PGlNode setWorldTransform(PMat4 worldTransform, PMat4 worldTransformInvTrad) {
    this.getWorldTransform().set(worldTransform);
    this.getWorldTransform().set(worldTransformInvTrad);
    return this;
  }

  @Getter public static class UniformConstants {
    public static class Float {}

    public static class Mat4 {
      public final static String u_worldTransform = "u_worldTransform";
      public final static String u_worldTransformInvTra = "u_worldTransformInvTra";
    }

    public static class Sampler2d {}

    public static class Vec2 {}

    public static class Vec3 {}

    public static class Vec4 {}
  }
}
