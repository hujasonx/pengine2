package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.graphics.PGlDrawCall;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PCopyable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class PGlNode implements PCopyable<PGlNode> {
  @Getter
  public static class UniformConstants {

    public static class Vec2 {
    }

    public static class Vec3 {
    }

    public static class Vec4 {
    }

    public static class Sampler2d {
    }

    public static class Float {
    }

    public static class Mat4 {
      public final static String u_worldTransform = "u_worldTransform";
      public final static String u_worldTransformInvTra = "u_worldTransformInvTra";
    }
  }

  @Getter
  private final PGlDrawCall drawCall;

  @Getter
  @Setter
  private String id;
  @Getter
  private final PMat4 worldTransform = new PMat4();
  @Getter
  private final PMat4 worldTransformInvTra = new PMat4();
  @Getter
  private final ArrayMap<String, PMat4> invBoneTransforms = new ArrayMap<>();
  @Getter
  @Setter
  // You can hook swap this out whenever you like.
  private PRenderContext.DataBufferEmitter dataBufferEmitter;

  public PGlNode(PGlNode other) {
    drawCall = PGlDrawCall.getTemp(other.getDrawCall());
    copyFrom(other);
  }

  public PGlNode(String id) {
    this.id = id;
    drawCall = PGlDrawCall.genTemplate();
  }

  public final PGlNode setWorldTransform(PMat4 worldTransform, PMat4 worldTransformInvTrad) {
    this.worldTransform.set(worldTransform);
    this.worldTransformInvTra.set(worldTransformInvTrad);
    return this;
  }

  @Override
  public PGlNode copyFrom(@NonNull PGlNode other) {
    id = other.id;
    drawCall.copyFrom(other.drawCall);
    worldTransform.set(other.worldTransform);
    worldTransformInvTra.set(other.worldTransformInvTra);
    invBoneTransforms.putAll(other.invBoneTransforms);
    dataBufferEmitter = other.dataBufferEmitter;
    return this;
  }
}
