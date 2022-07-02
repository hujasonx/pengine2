package com.phonygames.pengine.physics;

import android.support.annotation.NonNull;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.physics.PPhysicsEngine;

public abstract class PPhysicsCollisionShape<T extends btCollisionShape> implements Disposable {
  private static final Vector3 tempVec3 = new Vector3();
  protected final T collisionShape;
  private boolean disposed;

  public PPhysicsCollisionShape(@NonNull T collisionShape) {
    this.collisionShape = collisionShape;
    PPhysicsEngine.trackShape(this);
    disposed = false;
  }

  public void calculateLocalInertia(@NonNull PVec3 out, float mass) {
    collisionShape.calculateLocalInertia(mass, tempVec3);
    out.set(tempVec3);
  }

  @Override public void dispose() {
    PAssert.isFalse(disposed);
    disposed = true;
    collisionShape.dispose();
  }
}
