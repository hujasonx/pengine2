package com.phonygames.pengine.physics;

import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.physics.collisionshape.PPhysicsCapsuleShape;

public class PPhysicsCharacterController implements Disposable {
  public final float radius, height, crouchHeight, stepHeight, mass;
  private PRigidBody capsuleRigidBody;
  private PPhysicsCollisionShape<btCapsuleShape> capsuleShape;

  public PPhysicsCharacterController(float mass, float radius, float height, float crouchHeight, float stepHeight) {
    this.mass = mass;
    this.radius = radius;
    this.height = height;
    this.crouchHeight = crouchHeight;
    this.stepHeight = stepHeight;
    capsuleShape = new PPhysicsCapsuleShape(new btCapsuleShape(radius, (height - stepHeight - 2 * radius)));
    capsuleRigidBody =
        PRigidBody.obtain(capsuleShape, mass, PPhysicsEngine.CHARACTER_CONTROLLER_FLAG, PPhysicsEngine.STATIC_FLAG);
  }

  @Override public void dispose() {
  }

  public void setPosition(float x, float y, float z) {
  }
}
