package com.phonygames.pengine.math.kinematics;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.PVectorUtils;
import com.phonygames.pengine.util.PPool;

public class PInverseKinematicsUtils {
  /**
   * Applies two joint IK.
   * @param a
   * @param b
   * @param t
   */
  public static void oneJointIK(PModelInstance.Node a, PModelInstance.Node b, PVec3 t) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 aPos = a.worldTransform().getTranslation(pool.vec3());
    PVec3 bPos = b.worldTransform().getTranslation(pool.vec3());
    PVec4 aGRot = a.worldTransform().getRotation(pool.vec4());
    PVec4 outALRotChange = pool.vec4();
    oneJointRotationToPointTo(outALRotChange, null, aPos, bPos, t, aGRot);
    a.transform().rotate(outALRotChange);
    if (a.parent() != null) {
      a.recalcNodeWorldTransformsRecursive(a.parent().worldTransform(), true);
    }
    pool.free();
  }

  public static void oneJointRotationToPointTo(PVec4 outALRotChange, @Nullable PVec4 outAGRotChange, PVec3 a, PVec3 b, PVec3 t, PVec4 aGRot) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    /*
    float ac_at_0 = acos(clamp(dot(
    normalize(c - a),
    normalize(t - a)), -1, 1));

    vec3 axis1 = normalize(cross(c - a, t - a));

    quat r2 = quat_angle_axis(ac_at_0,  quat_mul(quat_inv(a_gr), axis1)));
     */
    PVec3 abN = pool.vec3().set(b).sub(a).nor();
    PVec3 atN = pool.vec3().set(t).sub(a).nor();
    float abAT0 = PNumberUtils.acos(abN.dot(atN));
    PVec3 axisWorld = pool.vec3().set(abN).crs(atN);
    if (!axisWorld.isZero()) {
      axisWorld.nor();
      PVec3 axisLocal = pool.vec4().set(aGRot).invQuat().applyAsQuat(pool.vec3().set(axisWorld));
      outALRotChange.setToRotation(axisLocal, abAT0);
      if (outAGRotChange != null) { outAGRotChange.setToRotation(axisWorld,abAT0); }
    } else {
      outALRotChange.setIdentityQuaternion();
      if (outAGRotChange != null) { outAGRotChange.setIdentityQuaternion(); }
    }
    pool.free();
  }

  public static void oneJointRotationToAngleKneeTo(PVec4 outALRotChange, PVec3 basePos, PVec3 kneePos,
                                                   PVec3 rotationAnchor, PVec3 poleTarget, PVec4 aGRot) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 baseKnee = pool.vec3().set(kneePos).sub(basePos);
    PVec3 axisWorldSpace = pool.vec3().set(rotationAnchor).sub(basePos).nor();
    if (baseKnee.isCollinear(axisWorldSpace)) {
      pool.free();
      return;
    }
    PVec3 baseTarget = pool.vec3().set(poleTarget).sub(basePos);
    PVec4 aGRotInv = pool.vec4().set(aGRot).invQuat();
    PVec3 axisLocalSpace = aGRotInv.applyAsQuat(pool.vec3().set(axisWorldSpace)).nor();
    float angleRotateAmount = PVectorUtils.angleToRotateOntoPlane(baseKnee, axisWorldSpace,
                                                                  pool.vec3().set(baseTarget).crs(axisWorldSpace)
                                                                      .scl(-1));
    //    float angleRotateAmount = -baseKnee.angleWithAlongAxis(baseTarget, axisWorldSpace);
    System.out.println(angleRotateAmount + ", " + axisLocalSpace + ", " + axisWorldSpace);
    outALRotChange.setToRotation(axisLocalSpace, -angleRotateAmount);
    //    outALRotChange.setToRotation(axisLocalSpace, -angleRotateAmount);
    PVec3 resultingKnee = outALRotChange.applyAsQuat(pool.vec3().set(baseKnee)).add(basePos);
    System.out.println("Resulting base knee " + resultingKnee);
    pool.free();
  }

  //  public static void twoJointIK(PVec4 outALRotChange, PVec4 outBLRotChange, PVec3 a, PVec3 b, PVec3 c, PVec3 t,
  //                                float epsilon, PVec4 aGRot, PVec4 bGRot, PVec3 poleWS, PVec3 targetPoleWS) {
  //    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
  //      // First, rotate the segments such that the combined length is the desired length.
  //      PVec3 outWorldAxis = pool.vec3();
  //      twoJointRotationsToLength(outALRotChange, outBLRotChange, outWorldAxis, a, b, c, t, epsilon, aGRot, bGRot,
  //      poleWS);
  //      nodeA.transform().rotate(outALRotChange);
  //      nodeB.transform().rotate(outBLRotChange);
  //      nodeA.recalcNodeWorldTransformsRecursive(true);
  //      // Now, rotate the base joint to point the tip to the target.
  //      cPos.set(tipLocalTransformFromB).mul(nodeB.worldTransform(), 1.0f);
  //      nodeA.worldTransform().getRotation(aGRot);
  //      oneJointRotationToPointTo(outALRotChange, aPos, cPos, t, aGRot);
  //      nodeA.transform().rotate(outALRotChange);
  //      nodeA.recalcNodeWorldTransformsRecursive(true);
  //    }
  //  }
  public static void twoJointIK(PModelInstance.Node nodeA, PModelInstance.Node nodeB, PVec3 tipLocalTranslationFromB,
                                PVec3 t, float epsilon, @Nullable PVec3 poleWS, @Nullable PVec3 targetPoleWS) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      // First, rotate the segments such that the combined length is the desired length.
      PVec3 a = nodeA.worldTransform().getTranslation(pool.vec3());
      PVec3 b = nodeB.worldTransform().getTranslation(pool.vec3());
      PVec3 c = pool.vec3().set(tipLocalTranslationFromB).mul(nodeB.worldTransform(), 1.0f);
      PVec4 aGRot = nodeA.worldTransform().getRotation(pool.vec4());
      PVec4 bGRot = nodeB.worldTransform().getRotation(pool.vec4());
      PVec4 outALRotChange = pool.vec4();
      PVec4 outBLRotChange = pool.vec4();
      PVec3 outWorldAxis = pool.vec3();
      twoJointRotationsToLength(outALRotChange, outBLRotChange, outWorldAxis, a, b, c, t, epsilon, aGRot, bGRot,
                                poleWS);
      nodeA.transform().rotate(outALRotChange);
      nodeB.transform().rotate(outBLRotChange);
      nodeA.recalcNodeWorldTransformsRecursive(true);
      // Now, rotate the base joint to point the tip to the target.
      c.set(tipLocalTranslationFromB).mul(nodeB.worldTransform(), 1.0f);
      nodeA.worldTransform().getRotation(aGRot);
      PVec4 outAGRotChange = pool.vec4();
      oneJointRotationToPointTo(outALRotChange, outAGRotChange, a, c, t, aGRot);
      PVec3 transformedAxisWS = outAGRotChange.applyAsQuat(pool.vec3().set(outWorldAxis)).nor();
      nodeA.transform().rotate(outALRotChange);
      nodeA.recalcNodeWorldTransformsRecursive(true);
      if (poleWS != null && targetPoleWS != null) {
        // Finally, attempt to rotate the axes to align from the poles.
        PVec3 newC = pool.vec3().set(tipLocalTranslationFromB).mul(nodeB.worldTransform(), 1.0f);
        PVec3 newAc = pool.vec3().set(newC).sub(a);
        PVec3 goalAxisFromTargetPoleWS = pool.vec3().set(newAc).crs(targetPoleWS).nor();
        PVec3 axisChangeAxis = pool.vec3().set(transformedAxisWS).crs(goalAxisFromTargetPoleWS).nor();
        float ang = PNumberUtils.acos(transformedAxisWS.dot(goalAxisFromTargetPoleWS));
        PVec4 newAGRotInv = nodeA.worldTransform().getRotation(pool.vec4()).invQuat();
        PVec3 axisChangeAxisLocalA = newAGRotInv.applyAsQuat(pool.vec3().set(axisChangeAxis));
        PVec4 axisChangeRotationLocalA = pool.vec4().setToRotation(axisChangeAxisLocalA, ang);
        nodeA.transform().rotate(axisChangeRotationLocalA);
        nodeA.recalcNodeWorldTransformsRecursive(true);
      }
    }
  }

  public static void twoJointRotationsToLength(PVec4 outALRotChange, PVec4 outBLRotChange, @Nullable PVec3 outWorldAxis,
                                               PVec3 a, PVec3 b, PVec3 c, PVec3 t, float epsilon, PVec4 aGRot,
                                               PVec4 bGRot, @Nullable PVec3 poleWorldSpace) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    // Handle the pole.
    PVec3 ac = pool.vec3().set(c).sub(a);
    PVec3 polePlaneNormal = null;
    if (poleWorldSpace != null) {
      //      // Flatten b to the a-c-pole plane.
      polePlaneNormal = pool.vec3().set(ac).crs(poleWorldSpace).nor();
      b = pool.vec3().set(b).projectOntoPlane(polePlaneNormal, a);
    }
    PVec3 ab = pool.vec3().set(b).sub(a);
    PVec3 abN = pool.vec3().set(ab).nor();
    PVec3 acN = pool.vec3().set(ac).nor();
    PVec3 bc = pool.vec3().set(c).sub(b);
    PVec3 bcN = pool.vec3().set(bc).nor();
    PVec3 at = pool.vec3().set(t).sub(a);
    PVec3 atN = pool.vec3().set(at).nor();
    // Get lengths.
    float lab = a.dst(b);
    float lcb = c.dst(b);
    float lat = PNumberUtils.clamp(t.dst(a), epsilon, lab + lcb - epsilon);
    // Math magic.
    float acAB0 = PNumberUtils.acos(acN.dot(abN));
    float baBC0 = PNumberUtils.acos(-abN.dot(bcN));
    float acAB1 = PNumberUtils.acos((lcb * lcb - lab * lab - lat * lat) / (-2 * lab * lat));
    float baBC1 = PNumberUtils.acos((lat * lat - lab * lab - lcb * lcb) / (-2 * lab * lcb));
    PVec3 axis0 = polePlaneNormal != null ? polePlaneNormal : pool.vec3().set(acN).crs(abN).nor();
    if (outWorldAxis != null) {
      outWorldAxis.set(axis0);
    }
    //    PVec3 axis0 = pool.vec3().set(ac).crs(pole == null ? ab : aGRot.applyAsQuat(pool.vec3().set(pole))).nor();
    PVec3 r0Axis = pool.vec4().set(aGRot).invQuat().applyAsQuat(pool.vec3().set(axis0));
    PVec3 r1Axis = pool.vec4().set(bGRot).invQuat().applyAsQuat(pool.vec3().set(axis0));
    PVec4 r0 = pool.vec4().setToRotation(r0Axis, acAB1 - acAB0);
    PVec4 r1 = pool.vec4().setToRotation(r1Axis, baBC1 - baBC0);
    outALRotChange.set(r0);
    outBLRotChange.set(r1);
    pool.free();
  }

//  /**
//   * Applies two joint IK.
//   * @param a
//   * @param b
//   * @param c
//   * @param t
//   * @param epsilon
//   */
//  public static void twoJointIk(PModelInstance.Node a, PModelInstance.Node b, PModelInstance.Node c, PVec3 t,
//                                float epsilon, @Nullable PVec3 pole) {
//    //    PPool.PoolBuffer pool = PPool.getBuffer();
//    //    // First, rotate the segments such that the combined length is the desired length.
//    //    PVec3 aPos = a.worldTransform().getTranslation(pool.vec3());
//    //    PVec3 bPos = b.worldTransform().getTranslation(pool.vec3());
//    //    PVec3 cPos = c.worldTransform().getTranslation(pool.vec3());
//    //    PVec4 aGRot = a.worldTransform().getRotation(pool.vec4());
//    //    PVec4 bGRot = b.worldTransform().getRotation(pool.vec4());
//    //    PVec4 outALRotChange = pool.vec4();
//    //    PVec4 outBLRotChange = pool.vec4();
//    //    twoJointRotationsToLength(outALRotChange, outBLRotChange, aPos, bPos, cPos, t, epsilon, aGRot, bGRot, pole);
//    //    a.transform().rotate(outALRotChange);
//    //    b.transform().rotate(outBLRotChange);
//    //    a.recalcNodeWorldTransformsRecursive(true);
//    //    // Now, rotate the base joint to point the tip to the target.
//    //    c.worldTransform().getTranslation(cPos);
//    //    a.worldTransform().getRotation(aGRot);
//    //    oneJointRotationToPointTo(outALRotChange, aPos, cPos, t, aGRot);
//    //    a.transform().rotate(outALRotChange);
//    //    a.recalcNodeWorldTransformsRecursive(true);
//    //    pool.free();
//    PMat4 transformBInv = PMat4.obtain().set(b.worldTransform()).inv();
//    PVec3 tipTranslationLocalB = c.worldTransform().getTranslation(PVec3.obtain()).mul(transformBInv, 1);
//    twoJointIk(a, b, tipTranslationLocalB, t, epsilon, pole);
//    tipTranslationLocalB.free();
//    transformBInv.free();
//  }

//  /**
//   * Applies two joint IK.
//   * @param a
//   * @param b
//   * @param tipLocalTransformFromB
//   * @param t
//   * @param epsilon
//   */
//  public static void twoJointIk(PModelInstance.Node a, PModelInstance.Node b, PVec3 tipLocalTransformFromB, PVec3 t,
//                                float epsilon, @Nullable PVec3 pole) {
//    PPool.PoolBuffer pool = PPool.getBuffer();
//    // First, rotate the segments such that the combined length is the desired length.
//    PVec3 aPos = a.worldTransform().getTranslation(pool.vec3());
//    PVec3 bPos = b.worldTransform().getTranslation(pool.vec3());
//    PVec3 cPos = pool.vec3().set(tipLocalTransformFromB).mul(b.worldTransform(), 1.0f);
//    PVec4 aGRot = a.worldTransform().getRotation(pool.vec4());
//    PVec4 bGRot = b.worldTransform().getRotation(pool.vec4());
//    PVec4 outALRotChange = pool.vec4();
//    PVec4 outBLRotChange = pool.vec4();
//    twoJointRotationsToLength(outALRotChange, outBLRotChange, null, aPos, bPos, cPos, t, epsilon, aGRot, bGRot, pole);
//    a.transform().rotate(outALRotChange);
//    b.transform().rotate(outBLRotChange);
//    a.recalcNodeWorldTransformsRecursive(true);
//    // Now, rotate the base joint to point the tip to the target.
//    cPos.set(tipLocalTransformFromB).mul(b.worldTransform(), 1.0f);
//    a.worldTransform().getRotation(aGRot);
//    oneJointRotationToPointTo(outALRotChange, null, aPos, cPos, t, aGRot);
//    a.transform().rotate(outALRotChange);
//    a.recalcNodeWorldTransformsRecursive(true);
//    pool.free();
//  }

  /**
   * Applies two joint IK, using a strategy of finding the knee location and just pointing the nodes individually.
   * @param nodeA
   * @param nodeB
   * @param tipLocalTransformFromB
   * @param t
   * @param epsilon
   */
  public static void twoJointIk2(PModelInstance.Node nodeA, PModelInstance.Node nodeB, PVec3 tipLocalTransformFromB,
                                 PVec3 t, float epsilon, @Nullable PVec3 poleTarget) {
    if (poleTarget == null) {
//      twoJointIK(nodeA, nodeB, tipLocalTransformFromB, t, epsilon, null);
      return;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 goalJointPos = pool.vec3();
      // First, rotate the segments such that the combined length is the desired length.
      PVec3 a = nodeA.worldTransform().getTranslation(pool.vec3());
      PVec3 b = nodeB.worldTransform().getTranslation(pool.vec3());
      PVec3 c = pool.vec3().set(tipLocalTransformFromB).mul(nodeB.worldTransform(), 1.0f);
      PVec3 ab = pool.vec3().set(b).sub(a);
      PVec3 at = pool.vec3().set(t).sub(a);
      PVec3 atN = pool.vec3().set(at).nor();
      PVec3 bc = pool.vec3().set(c).sub(b);
      float bcLen = bc.len();
      float abLen = ab.len();
      float atLen = PNumberUtils.clamp(at.len(), epsilon, abLen + bcLen - epsilon);
      // Check that the chain can reach the target.
      if (atLen > abLen + bcLen + epsilon) {
        // The goal is too far, so just fully extend the chain.
        goalJointPos.set(t);
      } else {
        // Find the ideal goal position based on the pole target.
        // Angles should be <a, b, t>
        PVec3 anglesForEndTriangle = PVectorUtils.getAnglesForTriangleWithLengths(pool.vec3(), bcLen, atLen, abLen);
        float jointLenAlongAt = abLen * MathUtils.cos(anglesForEndTriangle.x());
        float jointLocationsCircleRadius = abLen * MathUtils.sin(anglesForEndTriangle.x());
        PVec3 jointLocationsCircleCenter = pool.vec3().set(a).add(atN, jointLenAlongAt);
        PVec3 jointLocationsCircleNormal = atN;
        PVec3 goalPoleTargetPos = pool.vec3().set(a).add(pool.vec3().set(poleTarget).nor(), abLen);
        PVectorUtils.closestPointOnCircle(goalJointPos, goalPoleTargetPos, jointLocationsCircleNormal,
                                          jointLocationsCircleCenter, jointLocationsCircleRadius);
      }
      // First, rotate nodeA to point towards the goal joint pos.
      PVec4 aGRot = nodeA.worldTransform().getRotation(pool.vec4());
      PVec4 outALRotChange = pool.vec4();
      oneJointRotationToPointTo(outALRotChange, null, a, b, goalJointPos, aGRot);
      nodeA.transform().rotate(outALRotChange);
      nodeA.recalcNodeWorldTransformsRecursive(true);
      // Next, rotate nodeB to point towards the target.
      PVec4 bGRot = nodeB.worldTransform().getRotation(pool.vec4());
      PVec4 outBLRotChange = pool.vec4();
      PVec3 newC = pool.vec3().set(tipLocalTransformFromB).mul(nodeB.worldTransform(), 1.0f);
      oneJointRotationToPointTo(outBLRotChange, null, b, newC, t, bGRot);
      nodeB.transform().rotate(outBLRotChange);
      nodeB.recalcNodeWorldTransformsRecursive(true);
    }
  }
}
