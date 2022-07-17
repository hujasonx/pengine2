package com.phonygames.pengine.physics.collisionshape;

import android.support.annotation.NonNull;

import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;

public class PPhysicsCompoundShape extends PPhysicsCollisionShape<btCompoundShape> {
  public PPhysicsCompoundShape(@NonNull btCompoundShape collisionShape) {
    super(collisionShape);
  }
}
