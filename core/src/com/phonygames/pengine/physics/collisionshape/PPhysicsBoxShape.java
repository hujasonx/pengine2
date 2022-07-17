package com.phonygames.pengine.physics.collisionshape;

import android.support.annotation.NonNull;

import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;

public class PPhysicsBoxShape extends PPhysicsCollisionShape<btBoxShape> {
  public PPhysicsBoxShape(@NonNull btBoxShape collisionShape) {
    super(collisionShape);
  }
}
