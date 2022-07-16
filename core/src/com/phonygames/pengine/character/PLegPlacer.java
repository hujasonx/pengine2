package com.phonygames.pengine.character;

import android.support.annotation.NonNull;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PParametricCurve;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec2;
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
  private int cycleTimeFrameIndex = -1;
  private float cycleTimeThisFrame = -1;
  private Leg lastMovedLeg = null, queuedMoveLeg = null;
  private PList<Leg> legs = new PList<>(legPool);
  private PModelInstance modelInstance;
  private int numMovingLegs = 0;

  private PLegPlacer() {}

  public static PLegPlacer obtain(PModelInstance modelInstance) {
    PLegPlacer ret = legPlacerPool.obtain();
    ret.modelInstance = modelInstance;
    return ret;
  }

  public PLegPlacer addLeg(@NonNull PPlanarIKLimb limb, @NonNull String endEffector) {
    assert modelInstance != null;
    Leg leg = legs.genPooledAndAdd();
    leg.legPlacer = this;
    leg.limb = limb;
    leg.endEffector = modelInstance.getNode(endEffector);
    leg.recalc();
    return this;
  }

  private float cycleTime() {
    if (cycleTimeFrameIndex != PEngine.frameCount) {
      cycleTimeFrameIndex = PEngine.frameCount;
      this.cycleTimeThisFrame = 1;// TODO: variable cycle time.
    }
    return this.cycleTimeThisFrame;
  }

  public void frameUpdate(PVec3 velocity) {
    if (legs.isEmpty()) {return;}
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 pos = modelInstance.worldTransform().getTranslation(pool.vec3());
      PVec3 left = modelInstance.worldTransform().getXAxis(pool.vec3());
      PVec3 up = modelInstance.worldTransform().getYAxis(pool.vec3());
      PVec3 forward = modelInstance.worldTransform().getZAxis(pool.vec3());
      PVec4 rot = modelInstance.worldTransform().getRotation(pool.vec4());
      numMovingLegs = 0;
      // Initial pass to frame update all the legs.
      for (int a = 0; a < legs.size(); a++) {
        Leg leg = legs.get(a);
        leg.frameUpdate(pool, pos, rot, left, up, forward, velocity);
        if (leg.inCycle) {
          numMovingLegs++;
        }
      }
      float minimumDeviationForKickOff = .003f;
      // Next pass to trigger moves.
      float timeBetweenConsecutiveLegMoves = 1f / legs.size();
      if (numMovingLegs != 0) {
        for (int a = 0; a < legs.size(); a++) {
          Leg leg = legs.get(a);
          if (!leg.inCycle && (leg == queuedMoveLeg && lastMovedLeg != null && lastMovedLeg.inCycle &&
                                lastMovedLeg.cycleT >= timeBetweenConsecutiveLegMoves)) {
            // If there are no moving legs or the cycle just finished, we can trigger the next leg cycle if needed.
            float deviation = leg.calcDeviationFromNatural();
            if (deviation > minimumDeviationForKickOff) {
              kickOffMove(leg,a);
            }
          }
        }
      } else {
        // If there are no moving legs, find the leg with the highest deviation and kick it off.

        Leg bestLeg = null;
        float bestDeviation = 0;
        int bestLegIndex = -1;
        for (int a = 0; a < legs.size(); a++) {
          Leg leg = legs.get(a);
          float deviation = leg.calcDeviationFromNatural();
          if (deviation > minimumDeviationForKickOff && (bestLeg == null || deviation > bestDeviation)) {
            bestDeviation = deviation;
            bestLeg = leg;
            bestLegIndex = a;
          }
        }
        if (bestLeg != null) {
          kickOffMove(bestLeg,bestLegIndex);
        }
      }
    }
  }

  private void kickOffMove(Leg leg, int legIndex) {
    if (leg.inCycle) {return;}
    numMovingLegs++;
    leg.triggerCycle();
    System.out.println("Triggered cycle: " + leg + ", t=" + PEngine.t);
    lastMovedLeg = leg;
    queuedMoveLeg = legs.get(legIndex + 1);
  }

  @Override public void reset() {
    legs.clearAndFreePooled();
    lastMovedLeg = null;
    queuedMoveLeg = null;
    numMovingLegs = 0;
  }

  public static class Leg implements PPool.Poolable {
    // #pragma mark - PPool.Poolable
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    // #pragma end - PPool.Poolable
    private final PVec3 curEEPos = PVec3.obtain();
    private final PSODynamics.PSODynamics3 curEEPosGoal = PSODynamics.obtain3();
    private final PVec4 curEERot = PVec4.obtain();
    private final PVec4 curEERotGoal = PVec4.obtain();
    private final PVec3 naturalEEPosLocal = PVec3.obtain();
    private final PVec4 naturalEERotLocal = PVec4.obtain();
    private final PVec3 prevEEPosGoal = PVec3.obtain();
    private final PVec4 prevEERotGoal = PVec4.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** X: step up time[0, 1], Y: step down time[0, 1]. Parameter: cycleTime. */
        PParametricCurve.PParametricCurve2 stepTimeOffsetsCurve = PParametricCurve.obtain2().addKeyFrame(0, .25f, .75f);
    /** Range: [0, 1]. */
    private float cycleT = 0;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PModelInstance.Node endEffector;
    private PLegPlacer legPlacer;
    private PPlanarIKLimb limb;
    private boolean upThisCycle = false, downThisCycle = false, inCycle = false, eeGoalFlatSetThisCycle = false;

    private Leg() {
      reset();
    }

    @Override public void reset() {
      legPlacer = null;
      curEEPos.setZero();
      curEERot.setIdentityQuaternion();
      curEEPosGoal.reset();
      curEERotGoal.setIdentityQuaternion();
      prevEEPosGoal.setZero();
      prevEERotGoal.setIdentityQuaternion();
      naturalEEPosLocal.setZero();
      naturalEERotLocal.setIdentityQuaternion();
      curEEPosGoal.setGoalFlat(PVec3.ZERO);
      curEEPosGoal.setDynamicsParams(8, 1, 0);
      upThisCycle = false;
      downThisCycle = false;
      eeGoalFlatSetThisCycle = false;
      cycleT = 0;
      inCycle = false;
    }

    /**
     * Calculates the deviation from the natural position and orientation of the end effector.
     *
     * @return
     */
    private float calcDeviationFromNatural() {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        PVec3 naturalPos = calcNaturalEEPos(pool.vec3());
        float posDist = naturalPos.dst(curEEPos);
        return posDist;
      }
    }

    private PVec3 calcNaturalEEPos(PVec3 out) {
      return out.set(naturalEEPosLocal).mul(legPlacer.modelInstance.worldTransform(), 1f);
    }

    /** Returns true if the cycle was finished on this timestep. */
    boolean frameUpdate(PPool.PoolBuffer pool, PVec3 modelPosition, PVec4 modelRotation, PVec3 left, PVec3 up,
                        PVec3 forward, PVec3 velocity) {
      boolean justFinishedCycle = false;
      float cycleDt = PEngine.dt / legPlacer.cycleTime();
      float nextCycleT = cycleT + cycleDt;
      boolean justUp = false;
      if (inCycle) {
        PVec2 stepTimeOffsets = stepTimeOffsetsCurve.get(pool.vec2(), legPlacer.cycleTime());
        if (!upThisCycle) {
          // Before foot up.
          if (nextCycleT > stepTimeOffsets.x()) {
            upThisCycle = true;
            justUp = true;
          }
        } else if (!downThisCycle) {
          // Before foot down.
          // Figure out the goal position and orientation of the foot at the down time.
          float timeLeftBeforeEnd = (1 - cycleT) * legPlacer.cycleTime();
          float timeLeftBeforeFootDown = (stepTimeOffsets.y() - cycleT) * legPlacer.cycleTime();
          PVec3 expectedModelTranslationDeltaAtEnd = pool.vec3(velocity).scl(timeLeftBeforeEnd);
          PVec3 expectedFootPositionAtEnd = calcNaturalEEPos(pool.vec3()).add(expectedModelTranslationDeltaAtEnd);
          // The EEPosGoal lets us smooth out the goal.
          curEEPosGoal.setGoal(expectedFootPositionAtEnd);
          if (!eeGoalFlatSetThisCycle) {
            curEEPosGoal.vel().setZero();
            curEEPosGoal.pos().set(expectedFootPositionAtEnd);
            eeGoalFlatSetThisCycle = true;
          }
          curEEPosGoal.frameUpdate();
          PVec3 eePosGoal = curEEPosGoal.pos();
          // Lerp between the previous position goal and the smoothed goal value.
          float lerpMix = 1 - (stepTimeOffsets.y() - cycleT) / (stepTimeOffsets.y() - stepTimeOffsets.x());
          lerpMix = PNumberUtils.clamp(lerpMix, 0, 1);
          PVec3 lerpedPos = pool.vec3(prevEEPosGoal).lerp(eePosGoal, lerpMix);
          curEEPos.set(lerpedPos);
          if (endEffector.id().equals("Foot.L")) {
            System.out.println(expectedFootPositionAtEnd + ", " + curEEPos + ", " + lerpMix);
          }
          if (nextCycleT > stepTimeOffsets.y()) {
            downThisCycle = true;
          }
        } else {
          // After foot down.
        }
        cycleT = nextCycleT;
        if (cycleT > 1) {
          cycleT = 0;
          upThisCycle = false;
          downThisCycle = false;
          inCycle = false;
          eeGoalFlatSetThisCycle = false;
          justFinishedCycle = true;
        }
      }
      limb.performIkToReach(curEEPos);
      if (justUp) {
        // Now that we've applied IK, if we are just up, set the move start variables.
        endEffector.worldTransform().getTranslation(curEEPos);
        prevEEPosGoal.set(curEEPos);
        endEffector.worldTransform().getRotation(prevEERotGoal);
      }
      return justFinishedCycle;
    }

    private void recalc() {
      endEffector.templateNode().modelSpaceTransform().getTranslation(naturalEEPosLocal);
      endEffector.templateNode().modelSpaceTransform().getRotation(naturalEERotLocal);
      calcNaturalEEPos(curEEPos);
      calcNaturalEERot(curEERot);
    }

    private PVec4 calcNaturalEERot(PVec4 out) {
      PVec4 modelWRot = legPlacer.modelInstance.worldTransform().getRotation(PVec4.obtain());
      out.set(modelWRot).mul(naturalEERotLocal);
      modelWRot.free();
      return out;
    }

    public String toString() {
      return "[Leg ee: " + endEffector.id() + "]";
    }

    void triggerCycle() {
      inCycle = true;
    }
  }
}
