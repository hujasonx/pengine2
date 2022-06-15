package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.graphics.PGlDrawCall;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PDeepCopyable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PGlNode implements PDeepCopyable<PGlNode> {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PGlDrawCall drawCall;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final ArrayMap<String, PMat4> invBoneTransforms = new ArrayMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransform = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransformInvTra = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter
  @Accessors(fluent = true)
  private String id;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter
  @Accessors(fluent = true)
  private PModelInstance.Node ownerModelInstanceNode;

  public PGlNode(String id) {
    this.id = id;
    drawCall = PGlDrawCall.genNewTemplate();
  }

  @Override public PGlNode deepCopy() {
    return new PGlNode(this.id).deepCopyFrom(this);
  }

  @Override public PGlNode deepCopyFrom(@NonNull PGlNode other) {
    id = other.id;
    drawCall.deepCopyFrom(other.drawCall);
    worldTransform().set(other.worldTransform());
    worldTransformInvTra().set(other.worldTransformInvTra());
    invBoneTransforms().putAll(other.invBoneTransforms());
    ownerModelInstanceNode = other.ownerModelInstanceNode;
    return this;
  }

  public final PGlNode setWorldTransform(PMat4 worldTransform, PMat4 worldTransformInvTra) {
    this.worldTransform().set(worldTransform);
    this.worldTransformInvTra().set(worldTransformInvTra);
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
