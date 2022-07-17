package com.phonygames.pengine.physics;

import android.support.annotation.NonNull;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ContactResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PRigidBody implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PRigidBody> staticPool = new PPool<PRigidBody>() {
    @Override protected PRigidBody newObject() {
      return new PRigidBody();
    }
  };
  private static final Vector3 tempSharedVec3 = new Vector3();
  private static final Vector3 tempVec3 = new Vector3();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 localInertia = PVec3.obtain();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransformLogicPrev = PMat4.obtain(), worldTransformLogicCurrent = PMat4.obtain(),
      frameWorldTransform = PMat4.obtain();
  private PPhysicsCollisionShape collisionShape;
  private int group, mask;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean isInDynamicsWorld;
  private float prevLogicT = -1, prevFrameT = -1;
  private btRigidBody rigidBody;
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private RunnableOnPostLogicUpdate runnableOnPostLogicUpdate;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 pos = PVec3.obtain(), vel = PVec3.obtain();
  private final PVec3 posPrev = PVec3.obtain();

  private PRigidBody() {
  }

  public static PRigidBody obtain(@NonNull PPhysicsCollisionShape collisionShape, float mass, int group, int mask) {
    PRigidBody ret = staticPool().obtain();
    collisionShape.calculateLocalInertia(ret.localInertia(), mass);
    btRigidBody.btRigidBodyConstructionInfo constructionInfo =
        new btRigidBody.btRigidBodyConstructionInfo(mass, null, collisionShape.collisionShape,
                                                    tempVec3.set(ret.localInertia().x(), ret.localInertia().y(),
                                                                 ret.localInertia().z()));
    ret.collisionShape = collisionShape;
    ret.rigidBody = new btRigidBody(constructionInfo);
    ret.rigidBody.userData = ret;
    ret.rigidBody.setCollisionShape(collisionShape.collisionShape);
    ret.group = group;
    ret.mask = mask;
    return ret;
  }

  public PRigidBody activate() {
    PAssert.isNotNull(rigidBody);
    rigidBody.activate();
    return this;
  }

  public void addToDynamicsWorld() {
    PAssert.isFalse(isInDynamicsWorld);
    isInDynamicsWorld = true;
    PPhysicsEngine.rigidBodiesInSimulation.add(this);
    PPhysicsEngine.dynamicsWorld.addRigidBody(rigidBody, group, mask);
  }

  public void checkGhostingCollision(ContactResultCallback contactResultCallback) {
    PAssert.isFalse(isInDynamicsWorld, "wouldCollideWith should only be used as a ghosting utility.");
    PPhysicsEngine.dynamicsWorld.getCollisionWorld().contactTest(rigidBody, contactResultCallback);
  }

  public int getActivationState() {
    PAssert.isNotNull(rigidBody);
    return rigidBody.getActivationState();
  }

  public PRigidBody setActivationState(int activationState) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setActivationState(activationState);
    return this;
  }

  public PVec3 getLinearVelocity(PVec3 out) {
    PAssert.isNotNull(rigidBody);
    out.set(rigidBody.getLinearVelocity());
    return out;
  }

  public PMat4 getLogicWorldTransform(PMat4 mat4) {
    PAssert.isNotNull(rigidBody);
    rigidBody.getWorldTransform(mat4.getBackingMatrix4());
    return mat4;
  }

  public PMat4 getWorldTransform(PMat4 out) {
    PAssert.isNotNull(rigidBody);
    if (prevFrameT != PEngine.t) {
      // Recalculate the frame transform.
      frameWorldTransform().set(worldTransformLogicPrev())
                           .lerp(worldTransformLogicCurrent(), PEngine.logicupdateframeratio);
      prevFrameT = PEngine.t;
    }
    out.set(frameWorldTransform());
    return out;
  }

  protected void postLogicUpdate() {
    if (rigidBody == null) {return;}
    posPrev.set(pos);
    worldTransformLogicCurrent().getTranslation(pos);
    vel.set(pos).sub(posPrev).scl(1f / PEngine.logictimestep);
    worldTransformLogicPrev().set(worldTransformLogicCurrent());
    rigidBody.getWorldTransform(worldTransformLogicCurrent().getBackingMatrix4());
    if (runnableOnPostLogicUpdate != null) {
      runnableOnPostLogicUpdate.afterPostLogicUpdate(this);
    }
    prevLogicT = PEngine.logict;
  }

  public void preFrameUpdate() {
    if (rigidBody == null) {return;}
  }

  public void removeFromDynamicsWorld() {
    PAssert.isTrue(isInDynamicsWorld);
    isInDynamicsWorld = false;
    PPhysicsEngine.rigidBodiesInSimulation.removeValue(this, true);
    PPhysicsEngine.dynamicsWorld.removeRigidBody(rigidBody);
  }

  @Override public void reset() {
    if (rigidBody != null) {
      PAssert.isNotNull(rigidBody);
      if (isInDynamicsWorld) {
        PPhysicsEngine.dynamicsWorld.removeRigidBody(rigidBody);
        isInDynamicsWorld = false;
      }
      rigidBody.dispose();
      rigidBody = null;
    }
    prevLogicT = -1;
    prevFrameT = -1;
    worldTransformLogicCurrent().idt();
    worldTransformLogicPrev().idt();
    collisionShape = null;
    runnableOnPostLogicUpdate = null;
    pos.setZero();
    posPrev.setZero();
  }

  public PRigidBody setAngularFactor(float x, float y, float z) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setAngularFactor(tempSharedVec3.set(x, y, z));
    return this;
  }

  public PRigidBody setFriction(float friction) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setFriction(friction);
    return this;
  }

  public PRigidBody setLinearVelocity(float x, float y, float z) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setLinearVelocity(tempSharedVec3.set(x, y, z));
    return this;
  }

  public PRigidBody setLinearVelocity(PVec3 vec3) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setLinearVelocity(vec3.backingVec3());
    return this;
  }

  public PRigidBody setWorldTransform(PMat4 mat4) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setWorldTransform(mat4.getBackingMatrix4());
    worldTransformLogicCurrent().set(mat4);
    return this;
  }

  public PRigidBody setWorldTransformFlat(PMat4 mat4) {
    PAssert.isNotNull(rigidBody);
    rigidBody.setWorldTransform(mat4.getBackingMatrix4());
    worldTransformLogicCurrent().set(mat4);
        worldTransformLogicPrev().set(mat4);
        frameWorldTransform().set(mat4);
    return this;
  }

  public abstract static class RunnableOnPostLogicUpdate {
    public abstract void afterPostLogicUpdate(PRigidBody rigidBody);
  }
}
