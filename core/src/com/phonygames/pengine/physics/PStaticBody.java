package com.phonygames.pengine.physics;

import android.support.annotation.NonNull;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PStaticBody implements PPool.Poolable {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PStaticBody> staticPool = new PPool<PStaticBody>() {
    @Override protected PStaticBody newObject() {
      return new PStaticBody();
    }
  };
  private static final Vector3 tempVec3 = new Vector3();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 localInertia = PVec3.obtain();
  private btCollisionObject collisionObject;
  private PPhysicsCollisionShape collisionShape;
  private int group, mask;
  private boolean isInDynamicsWorld;
  @Getter
  @Setter
  private PPool ownerPool;

  private PStaticBody() {
  }

  public static PStaticBody obtain(@NonNull PPhysicsCollisionShape collisionShape, float mass, int group, int mask) {
    PStaticBody ret = staticPool().obtain();
    ret.collisionShape = collisionShape;
    ret.collisionObject = new btCollisionObject();
    ret.collisionObject.setCollisionShape(collisionShape.collisionShape);
    ret.group = group;
    ret.mask = mask;
    return ret;
  }

  public void addToDynamicsWorld() {
    PAssert.isFalse(isInDynamicsWorld);
    isInDynamicsWorld = true;
    PPhysicsEngine.dynamicsWorld.addCollisionObject(collisionObject, group, mask);
  }

  public void removeFromDynamicsWorld() {
    PAssert.isTrue(isInDynamicsWorld);
    PPhysicsEngine.dynamicsWorld.addCollisionObject(collisionObject);
    isInDynamicsWorld = false;
  }

  @Override public void reset() {
    PAssert.isNotNull(collisionObject);
    if (isInDynamicsWorld) {
      PPhysicsEngine.dynamicsWorld.removeCollisionObject(collisionObject);
      isInDynamicsWorld = false;
    }
    collisionObject.dispose();
    collisionObject = null;
    collisionShape = null;
  }
}
