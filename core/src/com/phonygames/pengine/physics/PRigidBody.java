package com.phonygames.pengine.physics;

import android.support.annotation.NonNull;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PRigidBody implements PPool.Poolable {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PRigidBody> staticPool = new PPool<PRigidBody>() {
    @Override protected PRigidBody newObject() {
      return new PRigidBody();
    }
  };
  private static final Vector3 tempVec3 = new Vector3();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 localInertia = PVec3.obtain();
  private PPhysicsCollisionShape collisionShape;
  private int group, mask;
  private boolean isInDynamicsWorld;
  @Getter
  @Setter
  private PPool ownerPool;
  private btRigidBody rigidBody;

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
    ret.rigidBody.setCollisionShape(collisionShape.collisionShape);
    ret.group = group;
    ret.mask = mask;
    return ret;
  }

  public void addToDynamicsWorld() {
    PAssert.isFalse(isInDynamicsWorld);
    isInDynamicsWorld = true;
    PPhysicsEngine.dynamicsWorld.addRigidBody(rigidBody, group, mask);
  }

  public void removeFromDynamicsWorld() {
    PAssert.isTrue(isInDynamicsWorld);
    PPhysicsEngine.dynamicsWorld.removeRigidBody(rigidBody);
    isInDynamicsWorld = false;
  }

  @Override public void reset() {
    PAssert.isNotNull(rigidBody);
    if (isInDynamicsWorld) {
      PPhysicsEngine.dynamicsWorld.removeRigidBody(rigidBody);
      isInDynamicsWorld = false;
    }
    rigidBody.dispose();
    rigidBody = null;
    collisionShape = null;
  }
}
