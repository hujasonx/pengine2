package com.phonygames.pengine.physics.collisionshape;

import android.support.annotation.NonNull;

import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;

public class PPhysicsBvhTriangleMeshShape extends PPhysicsCollisionShape<btBvhTriangleMeshShape> {
  public PPhysicsBvhTriangleMeshShape(@NonNull btBvhTriangleMeshShape collisionShape) {
    super(collisionShape);
  }
}