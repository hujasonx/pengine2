package com.phonygames.pengine.character;

import android.support.annotation.NonNull;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PParametricCurve;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.kinematics.PPlanarIKLimb;
import com.phonygames.pengine.physics.PPhysicsEngine;
import com.phonygames.pengine.physics.PPhysicsRayCast;
import com.phonygames.pengine.util.collection.PList;
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
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PParametricCurve.PParametricCurve1 cycleTimeCurve = PParametricCurve.obtain1();
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

  public Leg addLeg(@NonNull PPlanarIKLimb limb, @NonNull String endEffector) {
    assert modelInstance != null;
    Leg leg = legs.genPooledAndAdd();
    leg.legPlacer = this;
    leg.limb = limb;
    leg.endEffector = modelInstance.getNode(endEffector);
    leg.recalc();
    return leg;
  }

  private float cycleTime(float speed) {
    if (cycleTimeFrameIndex != PEngine.frameCount) {
      cycleTimeFrameIndex = PEngine.frameCount;
      this.cycleTimeThisFrame = cycleTimeCurve.get(speed);
    }
    return this.cycleTimeThisFrame;
  }

  public void frameUpdate(PVec3 velocity, boolean isOnGround) {
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
        leg.frameUpdate(pool, pos, rot, left, up, forward, velocity, isOnGround);
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
              kickOffMove(leg, a);
            }
          }
        }
      } else {
        // If there are no moving legs, find the leg with the highest deviation and kick it off.
        Leg bestLeg = null;
        float bestDeviation = 0;
        float bestVelocityScore = 0;
        int bestLegIndex = -1;
        PVec3 modelSpaceVelocity = pool.vec3(left.dot(velocity), up.dot(velocity), forward.dot(velocity));
        for (int a = 0; a < legs.size(); a++) {
          Leg leg = legs.get(a);
          float deviation = leg.calcDeviationFromNatural();
          if (deviation > minimumDeviationForKickOff && (bestLeg == null || deviation > bestDeviation - .001f)) {
            if (bestLegIndex != -1 && Math.abs(deviation - bestDeviation) < .01f) {
              // If the deviations are pretty similar, compare velocity scores instead. This should help select
              // the right foot if moving right, etc.
              // TODO: velocity score doesn't work.
              PVec3 endEffectorMSBindTranslation =
                  leg.endEffector.templateNode().modelSpaceTransform().getTranslation(pool.vec3());
              float velocityScore = modelSpaceVelocity.dot(endEffectorMSBindTranslation);
              if (velocityScore > bestVelocityScore) {
                bestVelocityScore = velocityScore;
              } else {continue;}
            }
            bestDeviation = deviation;
            bestLeg = leg;
            bestLegIndex = a;
          }
        }
        if (bestLeg != null) {
          kickOffMove(bestLeg, bestLegIndex);
        }
      }
    }
  }

  private void kickOffMove(Leg leg, int legIndex) {
    if (leg.inCycle) {return;}
    numMovingLegs++;
    leg.triggerCycle();
    lastMovedLeg = leg;
    queuedMoveLeg = legs.get(legIndex + 1);
  }

  @Override public void reset() {
    legs.clearAndFreePooled();
    cycleTimeCurve.reset();
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
    /**
     * CurEEPos is the current end effector pos (affected by smin, etc), curCalcedEEPos is the raw value that generates
     * curEEPos.
     */
    private final PVec3 curEEPos = PVec3.obtain(), curCalcedEEPos = PVec3.obtain();
    private final PSODynamics.PSODynamics3 curEEPosGoal = PSODynamics.obtain3();
    /** Spring just to help smooth movement. */
    private final PSODynamics.PSODynamics3 curEEPosMS = PSODynamics.obtain3();
    private final PVec4 curEERot = PVec4.obtain();
    private final PVec4 curEERotGoal = PVec4.obtain();
    private final PVec3 naturalEEPosLocal = PVec3.obtain();
    private final PVec4 naturalEERotLocal = PVec4.obtain();
    private final PVec3 prevEEPosGoal = PVec3.obtain();
    private final PVec4 prevEERotGoal = PVec4.obtain();
    private final PPhysicsRayCast rayCast = PPhysicsRayCast.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** X: step up time[0, 1], Y: step down time[0, 1]. Parameter: cycleTime. */
        PParametricCurve.PParametricCurve2 stepTimeOffsetsCurve = PParametricCurve.obtain2().addKeyFrame(0, .25f, .75f);
    /** Range: [0, 1]. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private float cycleT = 0, cycleStrength = 0;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PModelInstance.Node endEffector;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private boolean inCycle = false;
    private PLegPlacer legPlacer;
    private PPlanarIKLimb limb;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The displacement distance threshold for the maximum cycle strength */ private float maximumStrengthDis = 0;
    private transient boolean modelWasOnGroundPrev = true;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private boolean preventEEAboveBase = false;
    private boolean upThisCycle = false, downThisCycle = false, eeGoalFlatSetThisCycle = false;

    private Leg() {
      reset();
    }

    @Override public void reset() {
      legPlacer = null;
      curEEPos.setZero();
      curCalcedEEPos.setZero();
      curEERot.setIdentityQuaternion();
      curEEPosGoal.reset();
      curEERotGoal.setIdentityQuaternion();
      prevEEPosGoal.setZero();
      prevEERotGoal.setIdentityQuaternion();
      naturalEEPosLocal.setZero();
      naturalEERotLocal.setIdentityQuaternion();
      curEEPosGoal.setGoalFlat(PVec3.ZERO);
      curEEPosGoal.setDynamicsParams(8, 1, 0);
      curEEPosMS.setGoalFlat(PVec3.ZERO);
      curEEPosMS.setDynamicsParams(16, 1, 0);
      maximumStrengthDis = 0;
      preventEEAboveBase = false;
      modelWasOnGroundPrev = true;
      upThisCycle = false;
      downThisCycle = false;
      eeGoalFlatSetThisCycle = false;
      cycleT = 0;
      cycleStrength = 0;
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
                        PVec3 forward, PVec3 velocity, boolean isOnGround) {
      boolean justFinishedCycle = false;
      PVec3 basePosWS = limb.getNodes().get(0).worldTransform().getTranslation(pool.vec3());
      PVec3 bindEndPosWS = endEffector.worldTransform().getTranslation(pool.vec3());
      float speed = velocity.len();
      float cycleDt = PEngine.dt / legPlacer.cycleTime(speed);
      float nextCycleT = cycleT + cycleDt;
      PVec2 stepTimeOffsets = stepTimeOffsetsCurve.get(pool.vec2(), legPlacer.cycleTime(speed));
      boolean justUp = false, justDown = false;
      if (!isOnGround) {
        // At this point, the end effector should be at the bind position. Set it as the goal pos, but with a weak
        // spring.
        curCalcedEEPos.set(bindEndPosWS);
        curEEPosMS.setDynamicsParams(4, 1, 0);
        cycleT = stepTimeOffsets.y() + .001f;
        inCycle = true;
        upThisCycle = true;
        downThisCycle = false;
        eeGoalFlatSetThisCycle = false;
      } else {
        // Not on ground.
        if (!modelWasOnGroundPrev) {
          // Just landed on the ground.
          // Reset the calced ee pos.
          PVec3 rayCastHitPos = pool.vec3(), rayCastHitNor = pool.vec3();
          PVec3 startEndBindWSDelta = pool.vec3(bindEndPosWS).sub(basePosWS);
          PVec3 rayCastEnd = pool.vec3(basePosWS).add(startEndBindWSDelta, 2);
          if (!rayTest(rayCastHitPos, rayCastHitNor, basePosWS, rayCastEnd)) {
            rayCastHitPos.set(bindEndPosWS);
            rayCastHitNor.set(0, 1, 0);
          }
          curCalcedEEPos.set(rayCastHitPos);
          downThisCycle = true;
        }
        // While on the ground, the ee pos ms spring should be fast.
        curEEPosMS.setDynamicsParams(16, 1, 0);
        if (inCycle) {
          if (!upThisCycle) {
            // Before foot up.
            if (nextCycleT > stepTimeOffsets.x()) {
              upThisCycle = true;
              justUp = true;
            }
          } else if (!downThisCycle) {
            // Before foot down.
            // Figure out the goal position and orientation of the foot at the down time.
            float timeLeftBeforeEnd = (1 - cycleT) * legPlacer.cycleTime(speed);
            float timeLeftBeforeFootDown = (stepTimeOffsets.y() - cycleT) * legPlacer.cycleTime(speed);
            PVec3 expectedModelTranslationDeltaAtEnd = pool.vec3(velocity).scl(timeLeftBeforeEnd);
            PVec3 expectedFootPositionAtEnd = calcNaturalEEPos(pool.vec3()).add(expectedModelTranslationDeltaAtEnd);
            PVec3 expectedBasePositionAtEnd = pool.vec3(basePosWS).add(expectedModelTranslationDeltaAtEnd);
            // Do a raycast at the expected end position.
            PVec3 rayCastHitPos = pool.vec3(), rayCastHitNor = pool.vec3();
            PVec3 startEndBindWSDelta = pool.vec3(expectedFootPositionAtEnd).sub(expectedBasePositionAtEnd);
            PVec3 rayCastEnd = pool.vec3(expectedBasePositionAtEnd).add(startEndBindWSDelta, 2);
            if (!rayTest(rayCastHitPos, rayCastHitNor, expectedBasePositionAtEnd, rayCastEnd)) {
              rayCastHitPos.set(bindEndPosWS);
              rayCastHitNor.set(0, 1, 0);
            }
            expectedFootPositionAtEnd.set(rayCastHitPos);
//            PDebugRenderer.line(expectedFootPositionAtEnd, curEEPos, PColor.BLUE, PColor.BLUE, 2, 2);
            // The EEPosGoal lets us smooth out the goal.
            curEEPosGoal.setGoal(expectedFootPositionAtEnd);
            // Initialize the deltas; this should only be done once per cycle.
            if (!eeGoalFlatSetThisCycle) {
              curEEPosGoal.vel().setZero();
              curEEPosGoal.pos().set(expectedFootPositionAtEnd);
              eeGoalFlatSetThisCycle = true;
            }
            // Get the cycle strength.
            PVec3 goalDelta = pool.vec3(expectedFootPositionAtEnd).sub(prevEEPosGoal);
            if (maximumStrengthDis == 0) {
              cycleStrength = 1;
            } else {
              cycleStrength = Math.min(1, goalDelta.len() / maximumStrengthDis);
            }
            curEEPosGoal.frameUpdate();
            PVec3 eePosGoal = curEEPosGoal.pos();
            // Lerp between the previous position goal and the smoothed goal value.
            float lerpMix = 1 - (stepTimeOffsets.y() - cycleT) / (stepTimeOffsets.y() - stepTimeOffsets.x());
            lerpMix = PNumberUtils.generalSmoothStep(1, lerpMix);
            PVec3 lerpedPos = pool.vec3(prevEEPosGoal).lerp(eePosGoal, lerpMix);
            curCalcedEEPos.set(lerpedPos);
            if (nextCycleT > stepTimeOffsets.y()) {
              downThisCycle = true;
              justDown = true;
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
      }
      // Modify and then smooth the model-space end effector position.
      PVec3 curCalcedEEPosMS =
          pool.vec3(curCalcedEEPos).mul(pool.mat4(legPlacer.modelInstance.worldTransform()).inv(), 1);
      PVec3 curModifiedEEPosMS = modifyCalcedEEPosMS(pool, pool.vec3(), curCalcedEEPosMS);
      curEEPosMS.setGoal(curModifiedEEPosMS);
      curEEPosMS.frameUpdate();
      curEEPos.set(curEEPosMS.pos()).mul(legPlacer.modelInstance.worldTransform(), 1);
      limb.performIkToReach(curEEPos);
      if (justUp) {
        // Now that we've applied IK, if we are just up, set the move start variables.
        endEffector.worldTransform().getTranslation(curCalcedEEPos);
        prevEEPosGoal.set(curCalcedEEPos);
        curEEPos.set(curCalcedEEPos);
        endEffector.worldTransform().getRotation(prevEERotGoal);
      }
      if (justDown) {
        // Now that we've applied IK, if we just placed our foot down, move the calculated end position variable.
        endEffector.worldTransform().getTranslation(curCalcedEEPos);
      }
      modelWasOnGroundPrev = isOnGround;
      PVec3 tempP = pool.vec3();
      endEffector.worldTransform().getTranslation(tempP);
//      PDebugRenderer.line(tempP, curEEPos, PColor.YELLOW, PColor.YELLOW, 2, 2);
      return justFinishedCycle;
    }

    private boolean rayTest(PVec3 outPos, PVec3 outNor, PVec3 startPos, PVec3 endPos) {
      rayCast.setOnlyStaticBodies(true);
      rayCast.rayFromWorld().set(startPos);
      rayCast.rayToWorld().set(endPos);
      rayCast.setCollisionFilterGroup(PPhysicsEngine.ALL_FLAG).setCollisionFilterMask(PPhysicsEngine.STATIC_FLAG);
      rayCast.cast();
      boolean hasHit = rayCast.hasHit();
      if (hasHit) {
        rayCast.hitLocation(outPos);
        rayCast.hitNormal(outNor);
      }
      rayCast.reset();
      return hasHit;
    }

    /**
     * Modifies the ms EE pos to ensure it's valid.
     *
     * @return
     */
    private PVec3 modifyCalcedEEPosMS(PPool.PoolBuffer pool, PVec3 out, PVec3 in) {
      PVec3 basePos = limb.getNodes().get(0).templateNode().modelSpaceTransform().getTranslation(pool.vec3());
      PVec3 delta = pool.vec3(in).sub(basePos);
      if (delta.y() > 0 && preventEEAboveBase) {
        delta.y(0);
      }
      float deltaLen = delta.len();
      return out.set(basePos).add(delta, Math.min(1, limb.maximumExtendedLength() / deltaLen));
    }

    private void recalc() {
      endEffector.templateNode().modelSpaceTransform().getTranslation(naturalEEPosLocal);
      endEffector.templateNode().modelSpaceTransform().getRotation(naturalEERotLocal);
      calcNaturalEEPos(curCalcedEEPos);
      curEEPos.set(curCalcedEEPos);
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
