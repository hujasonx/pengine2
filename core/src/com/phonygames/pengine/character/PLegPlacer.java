package com.phonygames.pengine.character;

import android.support.annotation.NonNull;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.model.PModelInstance;
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
      // Initial pass just to count the number of moving legs.
      for (int a = 0; a < legs.size(); a++) {
        Leg leg = legs.get(a);
        if (leg.inCycle) {
          numMovingLegs++;
        }
      }
      // Next pass to update each leg, and possibly trigger moves.
      float timeBetweenConsecutiveLegMoves = 1f / legs.size();
      for (int a = 0; a < legs.size(); a++) {
        Leg leg = legs.get(a);
        boolean legCycleJustFinished = leg.frameUpdate(pool, pos, rot, left, up, forward, velocity);
        if (legCycleJustFinished) {
          System.out.println("Ended cycle: " + leg + ", t=" + PEngine.t);
          numMovingLegs--;
        }
        if (!leg.inCycle && (numMovingLegs == 0 ||
                             (leg == queuedMoveLeg && lastMovedLeg != null && lastMovedLeg.inCycle &&
                              lastMovedLeg.currentStepTime >= timeBetweenConsecutiveLegMoves))) {
          // If there are no moving legs or the cycle just finished, we can trigger the next leg cycle if needed.
          float deviation = leg.calcDeviationFromNatural();
          if (deviation > .001f) {
            leg.triggerCycle();
            System.out.println("Triggered cycle: " + leg + ", t=" + PEngine.t);
            lastMovedLeg = leg;
            queuedMoveLeg = legs.get(a + 1);
            numMovingLegs++;
          }
        }
      }
    }
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
    @Accessors(fluent = true)
    private PModelInstance.Node endEffector;
    private PPlanarIKLimb limb;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** X: step up time[0, 1], Y: step down time[0, 1]. Parameter: cycleTime. */
        PParametricCurve.PParametricCurve2 stepTimeOffsetsCurve = PParametricCurve.obtain2().addKeyFrame(0, .25f, .75f);
    /** Range: [0, 1]. */
    private float currentStepTime = 0;
    private PLegPlacer legPlacer;
    private boolean upThisCycle = false, downThisCycle = false, inCycle = false, eeGoalFlatSetThisCycle = false;

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
      if (inCycle) {
        PVec2 stepTimeOffsets = stepTimeOffsetsCurve.get(pool.vec2(), legPlacer.cycleTime());
        if (!upThisCycle) {
          // Before foot up.
          if (currentStepTime > stepTimeOffsets.x()) {
            endEffector.worldTransform().getTranslation(prevEEPosGoal);
            endEffector.worldTransform().getRotation(prevEERotGoal);
            upThisCycle = true;
          }
        } else if (!downThisCycle) {
          // Before foot down.
          // Figure out the goal position and orientation of the foot at the down time.
          float timeLeftBeforeFootDown = (stepTimeOffsets.y() - currentStepTime) * legPlacer.cycleTime();
          PVec3 expectedModelTranslationDeltaAtFootDown = pool.vec3(velocity).scl(timeLeftBeforeFootDown);
          PVec3 expectedFootPositionAtFootDown =
              calcNaturalEEPos(pool.vec3()).add(expectedModelTranslationDeltaAtFootDown);
          // The EEPosGoal lets us smooth out the goal.
          if (eeGoalFlatSetThisCycle) {
            curEEPosGoal.setGoal(expectedFootPositionAtFootDown);
          } else {
            curEEPosGoal.setGoalFlat(expectedFootPositionAtFootDown);
            eeGoalFlatSetThisCycle = true;
          }
          curEEPosGoal.frameUpdate();
          if (endEffector.id().equals("Foot.R")) {
            System.out.println(expectedFootPositionAtFootDown + ", " + curEEPos);
          }
          PVec3 eePosGoal = curEEPosGoal.pos();
          // Lerp between the previous position goal and the smoothed goal value.
          float lerpMix = (stepTimeOffsets.y() - currentStepTime) / (stepTimeOffsets.y() - stepTimeOffsets.x());
          PVec3 lerpedPos = pool.vec3(prevEEPosGoal).lerp(eePosGoal, lerpMix);
          curEEPos.set(lerpedPos);
          if (currentStepTime > stepTimeOffsets.y()) {
            downThisCycle = true;
          }
        } else {
          // After foot down.
        }
        currentStepTime += PEngine.dt / legPlacer.cycleTime();
        if (currentStepTime > 1) {
          currentStepTime = 0;
          upThisCycle = false;
          downThisCycle = false;
          inCycle = false;
          eeGoalFlatSetThisCycle = false;
          justFinishedCycle = true;
        }
      }
      limb.performIkToReach(curEEPos);
      endEffector.worldTransform().getTranslation(curEEPos);
      endEffector.worldTransform().getRotation(curEERot);
      return justFinishedCycle;
    }

    private void recalc() {
      endEffector.templateNode().modelSpaceTransform().getTranslation(naturalEEPosLocal);
      endEffector.templateNode().modelSpaceTransform().getRotation(naturalEERotLocal);
      PModelInstance modelInstance = legPlacer.modelInstance;
      calcNaturalEEPos(curEEPos);
      calcNaturalEERot(curEERot);
    }

    private PVec4 calcNaturalEERot(PVec4 out) {
      PVec4 modelWRot = legPlacer.modelInstance.worldTransform().getRotation(PVec4.obtain());
      out.set(modelWRot).mul(naturalEERotLocal);
      modelWRot.free();
      return out;
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
      curEEPosGoal.setDynamicsParams(8,1,0);
      upThisCycle = false;
      downThisCycle = false;
      eeGoalFlatSetThisCycle = false;
      currentStepTime = 0;
      inCycle = false;
    }

    public String toString() {
      return "[Leg ee: " + endEffector.id() + "]";
    }

    private Leg() {
      reset();
    }
    void triggerCycle() {
      inCycle = true;
    }
  }
}
