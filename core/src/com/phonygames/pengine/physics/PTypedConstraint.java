package com.phonygames.pengine.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.bullet.dynamics.btConeTwistConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PTypedConstraint implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static PPool<PTypedConstraint> staticPool = new PPool<PTypedConstraint>() {
    @Override protected PTypedConstraint newObject() {
      return new PTypedConstraint();
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 limits0 = PVec3.obtain(), limits1 = PVec3.obtain();
  ;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PMat4 localA = PMat4.obtain(), localB = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float damping;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private Type type = Type.None;

  private PTypedConstraint() {
    reset();
  }

  @Override public void reset() {
    type = Type.None;
    localA.idt();
    localB.idt();
    limits0.setZero();
    limits1.setZero();
    damping = 1;
  }

  public static PTypedConstraint obtain(Type type) {
    PTypedConstraint ret = staticPool.obtain();
    ret.type = type;
    ret.setLimitsToDefaultForType();
    return ret;
  }

  public PTypedConstraint setLimitsToDefaultForType() {
    switch (type) {
      case ConeTwist:
        setConeTwistLimits(MathUtils.PI * 2, MathUtils.PI * 2, MathUtils.PI * 2, .05f, 0, 1);
        break;
      default:
        PAssert.fail("Invalid typed constraint type: " + type.name());
    }
    return this;
  }

  public PTypedConstraint setConeTwistLimits(float swingSpan1, float swingSpan2, float twistSpan, float softness,
                                             float biasFactor, float relaxationFactor) {
    PAssert.isTrue(type == Type.ConeTwist);
    limits0.set(swingSpan1, swingSpan2, twistSpan);
    limits1.set(softness, biasFactor, relaxationFactor);
    return this;
  }

  public btTypedConstraint genBtTypedConstraint(PRigidBody rigidBodyA, PRigidBody rigidBodyB) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      switch (type) {
        case ConeTwist:
          btConeTwistConstraint coneTwistConstraint =
              new btConeTwistConstraint(rigidBodyA.getRigidBody(), rigidBodyB.getRigidBody(),
                                        localA.getBackingMatrix4(), localB.getBackingMatrix4());
          coneTwistConstraint.setLimit(limits0.x(), limits0.y(), limits0.z(), limits1.x(), limits1.y(), limits1.z());
          coneTwistConstraint.setDamping(damping);
          return coneTwistConstraint;
        default:
          PAssert.fail("Invalid typed constraint type: " + type.name());
          return null;
      }
    }
  }

  /**
   * Calculates localA and localB based on the bind transforms of nodeA and nodeB.
   *
   * @param nodeA
   * @param nodeB
   * @return
   */
  public PTypedConstraint setLocalTransformsFromModelInstanceNodes(PModelInstance.Node nodeA,
                                                                   PModelInstance.Node nodeB) {
    PAssert.isTrue(nodeA.rigidBody() != null && nodeB.rigidBody() != null, "Nodes must have rigid bodies");
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PMat4 nodeABindMSTransform = nodeA.templateNode().modelSpaceTransform();
      PMat4 nodeBBindMSTransform = nodeB.templateNode().modelSpaceTransform();
      PMat4 bodyABindMSTransform =
          pool.mat4().set(nodeABindMSTransform).mul(nodeA.templateNode().physicsCollisionShapeOffset());
      PMat4 bodyBBindMSTransform =
          pool.mat4().set(nodeBBindMSTransform).mul(nodeB.templateNode().physicsCollisionShapeOffset());
      localA.set(bodyABindMSTransform).inv().mul(nodeBBindMSTransform);
      localB.set(nodeB.templateNode().physicsCollisionShapeOffsetInv());
    }
    return this;
  }

  public enum Type {
    None, ConeTwist, Hinge, BallSocket, Point2Point, Generic6DOF, Generic6DOFSpring, Test
  }
}
