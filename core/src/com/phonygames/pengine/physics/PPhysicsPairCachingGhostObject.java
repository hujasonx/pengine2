package com.phonygames.pengine.physics;

import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject;

public class PPhysicsPairCachingGhostObject extends btPairCachingGhostObject {

  public PPhysicsPairCachingGhostObject() {
    super();
    setCollisionFlags(getCollisionFlags() | CollisionFlags.CF_NO_CONTACT_RESPONSE);
  }
}