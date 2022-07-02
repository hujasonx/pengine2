package com.phonygames.pengine.physics.collisionshape;

import android.support.annotation.NonNull;

import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;

public class PPhysicsCapsuleShape extends PPhysicsCollisionShape<btCapsuleShape> {
  public PPhysicsCapsuleShape(@NonNull btCapsuleShape collisionShape) {
    super(collisionShape);
  }
}
