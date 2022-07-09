package com.phonygames.pengine.math;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.PPool;

public class PInverseKinematicsUtils {
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
    PVec3 aPos = a.worldTransform().getTranslation(pool.vec3());
    PVec3 bPos = b.worldTransform().getTranslation(pool.vec3());
    PVec3 cPos = c.worldTransform().getTranslation(pool.vec3());
    PVec4 aGRot = a.worldTransform().getRotation(pool.vec4());
    PVec4 bGRot = b.worldTransform().getRotation(pool.vec4());
    PVec4 aLRot = a.transform().getRotation(pool.vec4());
    PVec4 bLRot = b.transform().getRotation(pool.vec4());
    PVec4 outALRotChange = pool.vec4();
    PVec4 outBLRotChange = pool.vec4();
    twoJointIK(aPos, bPos, cPos, t, epsilon, aGRot, bGRot, pole, outALRotChange, outBLRotChange);
    a.transform().rotate(outALRotChange);
    b.transform().rotate(outBLRotChange);
    pool.free();
  }
  /**
  void two_joint_ik(
      vec3 a, vec3 b, vec3 c, vec3 t, float eps,
      quat a_gr, quat b_gr,
      quat &a_lr, quat &b_lr) {

    float lab = length(b - a);
    float lcb = length(b - c);
    float lat = clamp(length(t - a), eps, lab + lcb - eps);

    float ac_ab_0 = acos(clamp(dot(normalize(c - a), normalize(b - a)), -1, 1));
    float ba_bc_0 = acos(clamp(dot(normalize(a - b), normalize(c - b)), -1, 1));
    float ac_at_0 = acos(clamp(dot(normalize(c - a), normalize(t - a)), -1, 1));

    float ac_ab_1 = acos(clamp((lcb*lcb-lab*lab-lat*lat) / (-2*lab*lat), -1, 1));
    float ba_bc_1 = acos(clamp((lat*lat-lab*lab-lcb*lcb) / (-2*lab*lcb), -1, 1));

    vec3 axis0 = normalize(cross(c - a, b - a));
    vec3 axis1 = normalize(cross(c - a, t - a));

    quat r0 = quat_angle_axis(ac_ab_1 - ac_ab_0, quat_mul(quat_inv(a_gr), axis0));
    quat r1 = quat_angle_axis(ba_bc_1 - ba_bc_0, quat_mul(quat_inv(b_gr), axis0));
    quat r2 = quat_angle_axis(ac_at_0, quat_mul(quat_inv(a_gr), axis1));

    a_lr = quat_mul(a_lr, quat_mul(r0, r2));
    b_lr = quat_mul(b_lr, r1);
  }
  */
  public static void twoJointIK(PVec3 a, PVec3 b, PVec3 c, PVec3 t, float epsilon, PVec4 aGRot, PVec4 bGRot,
                                @Nullable PVec3 pole, PVec4 outALRotChange, PVec4 outBLRotChange) {
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
    float acAT0 = PNumberUtils.acos(acN.dot(atN));
    float acAB1 = PNumberUtils.acos((lcb * lcb - lab * lab - lat * lat) / (-2 * lab * lat));
    float baBC1 = PNumberUtils.acos((lat * lat - lab * lab - lcb * lcb) / (-2 * lab * lcb));
    PVec3 axis0 = pool.vec3().set(ac).crs(pole == null ? ab : bGRot.applyAsQuat(pool.vec3().set(pole))).nor();
    PVec3 axis1 = pool.vec3().set(ac).crs(at).nor();
    PVec3 r0Axis = pool.vec4().set(aGRot).invQuat().applyAsQuat(pool.vec3().set(axis0));
    PVec3 r1Axis = pool.vec4().set(bGRot).invQuat().applyAsQuat(pool.vec3().set(axis0));
    PVec3 r2Axis = pool.vec4().set(aGRot).invQuat().applyAsQuat(pool.vec3().set(axis1));
    PVec4 r0 = pool.vec4().setToRotation(r0Axis, acAB1 - acAB0);
    PVec4 r1 = pool.vec4().setToRotation(r1Axis, baBC1 - baBC0);
    PVec4 r2 = pool.vec4().setToRotation(r2Axis, acAT0);
    outALRotChange.set(r0.mulQuat(r2));
    outBLRotChange.set(r1);
    pool.free();
  }

  public static void test() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    pool.free();
  }
}
