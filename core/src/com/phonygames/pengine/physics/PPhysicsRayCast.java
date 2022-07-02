package com.phonygames.pengine.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.LocalRayResult;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.phonygames.pengine.math.PVec3;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PPhysicsRayCast {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PVec3 rayFromWorld = PVec3.obtain(), rayToWorld = PVec3.obtain();
  private final ClosestRayResultCallback backingCallback;

  public PPhysicsRayCast() {
    backingCallback = new ClosestRayResultCallback(new Vector3(), new Vector3()) {
      @Override public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
        return super.addSingleResult(rayResult, normalInWorldSpace);
      }
    };
  }
}
