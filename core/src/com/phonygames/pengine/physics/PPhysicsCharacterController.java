package com.phonygames.pengine.physics;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.physics.collisionshape.PPhysicsCapsuleShape;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PPhysicsCharacterController implements Disposable, PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  public float radius, height, crouchHeight, stepHeight, mass;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 manualVelocity = PVec3.obtain();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  /** The newly calculated velocity calculated in postLogicUpdate
   and applied to the rigidbody in the next preLogicUpdate. */ private final PVec3 newLinVel = PVec3.obtain();
  private float rayCastFloorExtra = .2f;
  public float selfAcceleration = 30;
  private transient float capsuleOffsetYFromOrigin = 0;
  private PRigidBody capsuleRigidBody;
  private PPhysicsCapsuleShape capsuleShape;
//  private transient float crouchCapsuleOffsetYFromOrigin;
//  private PRigidBody crouchCapsuleRigidBody;
//  private PPhysicsCapsuleShape crouchCapsuleShape;
  private boolean forceCrouching = false;
  private transient boolean isOnGround = false;
  private transient boolean isOnGroundPrev = false;
  private float jumpTimeRequirement = .1f;
  private transient float timeSinceLastJump = -1, timeSinceLastTouchingGround, jumpVerticalVelocity;
  // How long after a jump is triggered that the jump will be enforced (e.g. not bringing the character to the
  // ground)

  private PPhysicsCharacterController() {

  }

  private static PPool<PPhysicsCharacterController> staticPool = new PPool<PPhysicsCharacterController>() {
    @Override protected PPhysicsCharacterController newObject() {
      return new PPhysicsCharacterController();
    }
  };

  public static PPhysicsCharacterController obtain(float mass, float radius, float height, float crouchHeight, float stepHeight) {
    PPhysicsCharacterController ret = staticPool.obtain();
    ret.mass = mass;
    ret.radius = radius;
    ret.height = height;
    ret.crouchHeight = crouchHeight;
    ret.stepHeight = stepHeight;
    ret.capsuleOffsetYFromOrigin = (height - stepHeight) / 2 + stepHeight;
    ret.capsuleShape = new PPhysicsCapsuleShape(new btCapsuleShape(radius, (height - stepHeight - 2 * radius)));
    ret.capsuleRigidBody = PRigidBody.obtain(ret.capsuleShape, mass, PPhysicsEngine.CHARACTER_CONTROLLER_FLAG,
                                         PPhysicsEngine.STATIC_FLAG | PPhysicsEngine.CHARACTER_CONTROLLER_FLAG);
    ret.capsuleRigidBody.setAngularFactor(0, 0, 0);
    ret.capsuleRigidBody.setFriction(0);
    ret.capsuleRigidBody.runnableOnPostLogicUpdate(new PRigidBody.RunnableOnPostLogicUpdate() {
      @Override public void afterPostLogicUpdate(PRigidBody rigidBody) {
        ret.processRigidBodyAfterLogicUpdate(rigidBody);
      }
    });
    return ret;
  }

  public PPhysicsCharacterController addToDynamicsWorld() {
    capsuleRigidBody.addToDynamicsWorld();
    return this;
  }

  public PPhysicsCharacterController removeFromDynamicsWorld() {
    capsuleRigidBody.removeFromDynamicsWorld();
    return this;
  }

  private void processRigidBodyAfterLogicUpdate(PRigidBody rigidBody) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PAssert.isTrue(rigidBody == capsuleRigidBody);
    if (timeSinceLastJump != -1) {
      timeSinceLastJump += PEngine.logictimestep;
    }
    isOnGroundPrev = isOnGround;
    isOnGround = false;
    // Update the velocity.
    PVec3 rawLinVelocity = rigidBody.getLinearVelocity(pool.vec3());
    // Perform raycasting to determine if we are on the ground.
    PMat4 rigidBodyTransform = pool.mat4();
    PVec3 rigidBodyPos = rigidBody.getLogicWorldTransform(rigidBodyTransform).getTranslation(pool.vec3());
    PPhysicsRayCast rayCast = PPhysicsRayCast.obtain();
    rayCast.rayFromWorld().set(rigidBodyPos);
    // TODO: perhaps search further based on the vertical velocity?
    rayCast.rayToWorld()
           .set(rigidBodyPos.x(), rigidBodyPos.y() - capsuleOffsetYFromOrigin - rayCastFloorExtra, rigidBodyPos.z());
    rayCast.setCollisionFilterGroup(PPhysicsEngine.ALL_FLAG).setCollisionFilterMask(PPhysicsEngine.STATIC_FLAG);
    rayCast.setOnlyStaticBodies(true);
    rayCast.cast();
    if (rayCast.hasHit()) {
      // Set the y velocity to 0 since we are on the ground, unless we just jumped.
      if (timeSinceLastJump == -1 || timeSinceLastJump > jumpTimeRequirement) {
        isOnGround = true;
        float pushUp = capsuleOffsetYFromOrigin - rayCast.hitDistance();
        if (pushUp > 0) {
          pushUp *= Math.min(1, PEngine.logictimestep * 5); // If we are being pushed up, push us up slowly.
        } else {
          pushUp *= Math.min(1, PEngine.logictimestep *
                                30); // However, being pushed down to stay on the ground should be almost instantaneous.
        }
        rigidBodyTransform.translate(0, pushUp, 0);
        rigidBody.setWorldTransform(rigidBodyTransform);
      }
    }
    rayCast.free();
    // Nudge the new linear velocity in the manual velocity direction we want.
    PVec2 newHorVelocity = pool.vec2().set(rawLinVelocity.x(), rawLinVelocity.z());
    PVec2 goalHorVelocity = pool.vec2().set(manualVelocity().x(), manualVelocity().z());
    PVec2 horVelocityDelta = pool.vec2().set(goalHorVelocity).sub(newHorVelocity);
    float deltaLen = horVelocityDelta.len();
    if (!horVelocityDelta.isZero()) {
      horVelocityDelta.nor().scl(Math.min(selfAcceleration, deltaLen));
    }
    newHorVelocity.add(horVelocityDelta);
    newLinVel().x(newHorVelocity.x()).z(newHorVelocity.y());
    if (isOnGround) {
      newLinVel().y(0);
      timeSinceLastTouchingGround = 0;
      jumpVerticalVelocity = 0;
    } else {
      newLinVel().y(Math.max(jumpVerticalVelocity, rawLinVelocity.y()));
      timeSinceLastTouchingGround += PEngine.logictimestep;
      jumpVerticalVelocity -= PPhysicsEngine.gravity * PEngine.logictimestep;
    }
    pool.free();
  }

  @Override public void reset() {
    if (capsuleRigidBody != null) {
      capsuleRigidBody.free();
    }
  }

  @Override public void dispose() {
  }

  public PVec3 getPos(PVec3 out) {
    PMat4 temp = PMat4.obtain();
    capsuleRigidBody.getWorldTransform(temp).getTranslation(out).add(0, -capsuleOffsetYFromOrigin, 0);
    temp.free();
    return out;
  }

  public PVec3 getVel(PVec3 out) {
    out.set(capsuleRigidBody.vel());
    return out;
  }

  public boolean isOnGround() {
    // Return the prev value because rigidbody velocities and positions are one logic timestep behind.
    return isOnGroundPrev;
  }

  public void preLogicUpdate() {
    //    if (newLinVel().isZero()) {
    //      // Deactivate the rigidbody if we are not moving.
    //      if (capsuleRigidBody.getActivationState() != 0) {
    //        capsuleRigidBody.setActivationState(0);
    //        PLog.i("Deactivating character controller rigidbody due to stopped movement").pEngine();
    //      }
    //    } else {
    //      // Activate the rigidbody if we are moving.
    //      if (capsuleRigidBody.getActivationState() != 1) {
    //        capsuleRigidBody.setActivationState(1);
    //        PLog.i("Activating character controller rigidbody due to movement").pEngine();
    //      }
    //    }
    if (PKeyboard.isLogicJustDown(Input.Keys.Y)) {
      setPos(MathUtils.random(10f), MathUtils.random(10f), MathUtils.random(10f));
      newLinVel().setZero();
    }
    capsuleRigidBody.activate();
    // Set the rigid body velocity from the new linear velocity.
    capsuleRigidBody.setLinearVelocity(newLinVel());
  }

  public PPhysicsCharacterController setPos(float x, float y, float z) {
    PVec3 rigidBodyPos = PVec3.obtain();
    PMat4 tempMat = PMat4.obtain();
    rigidBodyPos.set(x, y + capsuleOffsetYFromOrigin, z);
    tempMat.setToTranslation(rigidBodyPos);
    capsuleRigidBody.setWorldTransformFlat(tempMat);
    rigidBodyPos.free();
    tempMat.free();
    capsuleRigidBody.activate();
    return this;
  }

  public PPhysicsCharacterController velXZ(float x, float z) {
    manualVelocity().x(x).z(z);
    return this;
  }
}
