package com.phonygames.pengine.character;

import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PParametricCurve;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.kinematics.PPlanarIKLimb;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * This class will help you automatically animate leg limbs.
 */
public class PLegPlacer implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PLegPlacer> legPlacerPool = new PPool<PLegPlacer>() {
    @Override protected PLegPlacer newObject() {
      return new PLegPlacer();
    }
  };
  private static PPool<Leg> legPool = new PPool<Leg>() {
    @Override protected Leg newObject() {
      return new PLegPlacer.Leg();
    }
  };
  private PList<Leg> legs = new PList<>(legPool);
  private PModelInstance modelInstance;

  private PLegPlacer() {}

  public static PLegPlacer obtain(PModelInstance modelInstance) {
    PLegPlacer ret = legPlacerPool.obtain();
    ret.modelInstance = modelInstance;
    return ret;
  }

  public PLegPlacer addLeg(PPlanarIKLimb limb, PModelInstance.Node endEffector) {
    Leg leg = legs.genPooledAndAdd();
    leg.legPlacer = this;
    leg.limb = limb;
    leg.endEffector = endEffector;
    leg.recalc();
    return this;
  }

  public void frameUpdate(PVec3 velocity) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 pos = modelInstance.worldTransform().getTranslation(pool.vec3());
      PVec3 left = modelInstance.worldTransform().getXAxis(pool.vec3());
      PVec3 up = modelInstance.worldTransform().getYAxis(pool.vec3());
      PVec3 forward = modelInstance.worldTransform().getZAxis(pool.vec3());
      for (int a = 0; a < legs.size(); a++) {
        Leg leg = legs.get(a);
        leg.frameUpdate(pool, pos, left, up, forward, velocity);
      }
    }
  }

  @Override public void reset() {
    legs.clearAndFreePooled();
  }

  private static class Leg implements PPool.Poolable {
    // #pragma mark - PPool.Poolable
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    // #pragma end - PPool.Poolable
    final PVec3 curEEPos = PVec3.obtain();
    final PVec3 curEEPosGoal = PVec3.obtain();
    final PVec3 prevEEPosGoal = PVec3.obtain();
    final PVec4 curEERot = PVec4.obtain();
    final PVec4 curEERotGoal = PVec4.obtain();
    final PVec4 prevEERotGoal = PVec4.obtain();
    final PVec3 naturalLEEPosLocal = PVec3.obtain();
    final PVec4 naturalLEERotLocal = PVec4.obtain();
    PModelInstance.Node endEffector;
    PPlanarIKLimb limb;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** X: step up time[0, 1], Y: step down time[0, 1]. */
        PParametricCurve.PParametricCurve2 stepTimeOffsetsCurve = PParametricCurve.obtain2();
    /** Range: [0, 1]. */
    private float currentStepTime = 0;
    private PLegPlacer legPlacer;

    void frameUpdate(PPool.PoolBuffer pool, PVec3 modelPosition, PVec3 left, PVec3 up, PVec3 forward, PVec3 velocity) {
      PVec3 expectedModelPositionAtFootDown = pool.vec3(modelPosition);
    }

    private void recalc() {
      endEffector.templateNode().modelSpaceTransform().getTranslation(naturalLEEPosLocal);
      endEffector.templateNode().modelSpaceTransform().getRotation(naturalLEERotLocal);
    }

    @Override public void reset() {
      legPlacer = null;
      curEEPos.setZero();
      curEERot.setIdentityQuaternion();
      curEEPosGoal.setZero();
      curEERotGoal.setIdentityQuaternion();
      prevEEPosGoal.setZero();
      prevEERotGoal.setIdentityQuaternion();
      naturalLEEPosLocal.setZero();
      naturalLEERotLocal.setIdentityQuaternion();
    }
  }
}
