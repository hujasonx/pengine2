package com.phonygames.pengine.math;

import android.support.annotation.Nullable;

import com.phonygames.pengine.graphics.model.PModelInstance;
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
    oneJointRotationToPointTo(outALRotChange, aPos, bPos, t, aGRot);
    a.transform().rotate(outALRotChange);
    if (a.parent() != null) {
      a.recalcNodeWorldTransformsRecursive(a.parent().worldTransform(), true);
    }
    pool.free();
  }

  public static void oneJointRotationToPointTo(PVec4 outALRotChange, PVec3 a, PVec3 b, PVec3 t, PVec4 aGRot) {
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
    } else {
      outALRotChange.setIdentityQuaternion();
    }
    pool.free();
  }

  public static void test() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    pool.free();
  }

  /**
   * Applies two joint IK.
   * @param a
   * @param b
   * @param c
   * @param t
   * @param epsilon
   */
  public static void twoJointIk(PModelInstance.Node a, PModelInstance.Node b, PModelInstance.Node c, PVec3 t,
                                float epsilon, @Nullable PVec3 pole) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    // First, rotate the segments such that the combined length is the desired length.
    PVec3 aPos = a.worldTransform().getTranslation(pool.vec3());
    PVec3 bPos = b.worldTransform().getTranslation(pool.vec3());
    PVec3 cPos = c.worldTransform().getTranslation(pool.vec3());
    PVec4 aGRot = a.worldTransform().getRotation(pool.vec4());
    PVec4 bGRot = b.worldTransform().getRotation(pool.vec4());
    PVec4 outALRotChange = pool.vec4();
    PVec4 outBLRotChange = pool.vec4();
    twoJointRotationsToLength(outALRotChange, outBLRotChange, aPos, bPos, cPos, t, epsilon, aGRot, bGRot, pole);
    a.transform().rotate(outALRotChange);
    b.transform().rotate(outBLRotChange);
    if (a.parent() != null) {
      a.recalcNodeWorldTransformsRecursive(a.parent().worldTransform(), true);
      // Now, rotate the base joint to point the tip to the target.
      c.worldTransform().getTranslation(cPos);
      a.worldTransform().getRotation(aGRot);
      oneJointRotationToPointTo(outALRotChange, aPos, cPos, t, aGRot);
      a.transform().rotate(outALRotChange);
      a.recalcNodeWorldTransformsRecursive(a.parent().worldTransform(), true);
    }
    pool.free();
  }

  public static void twoJointRotationsToLength(PVec4 outALRotChange, PVec4 outBLRotChange, PVec3 a, PVec3 b, PVec3 c,
                                               PVec3 t, float epsilon, PVec4 aGRot, PVec4 bGRot, @Nullable PVec3 pole) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 ab = pool.vec3().set(b).sub(a);
    PVec3 abN = pool.vec3().set(ab).nor();
    PVec3 ac = pool.vec3().set(c).sub(a);
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
    PVec3 axis0 = pool.vec3().set(ac).crs(pole == null ? ab : bGRot.applyAsQuat(pool.vec3().set(pole))).nor();
    PVec3 axis1 = pool.vec3().set(ac).crs(at).nor();
    PVec3 r0Axis = pool.vec4().set(aGRot).invQuat().applyAsQuat(pool.vec3().set(axis0));
    PVec3 r1Axis = pool.vec4().set(bGRot).invQuat().applyAsQuat(pool.vec3().set(axis0));
    PVec4 r0 = pool.vec4().setToRotation(r0Axis, acAB1 - acAB0);
    PVec4 r1 = pool.vec4().setToRotation(r1Axis, baBC1 - baBC0);
    outALRotChange.set(r0);
    outBLRotChange.set(r1);
    pool.free();
  }
}
