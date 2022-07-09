package com.phonygames.pengine.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.LocalRayResult;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PPhysicsRayCast implements PPool.Poolable {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PPhysicsRayCast> staticPool = new PPool<PPhysicsRayCast>() {
    @Override protected PPhysicsRayCast newObject() {
      return new PPhysicsRayCast();
    }
  };
  private final ClosestRayResultCallback backingCallback;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 rayFromWorld = PVec3.obtain(), rayToWorld = PVec3.obtain();
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  private boolean hasHit = false;
  private float hitDistance, hitFraction;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean onlyStaticBodies = false;
  private float searchRayLength;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean valid = false;

  private PPhysicsRayCast() {
    backingCallback = new ClosestRayResultCallback(new Vector3(), new Vector3()) {
      @Override public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
        PStaticBody staticBody = rayResult.getCollisionObject().userData instanceof PStaticBody ?
                                 (PStaticBody) rayResult.getCollisionObject().userData : null;
        if (onlyStaticBodies && staticBody == null) {
          return 1;
        }
        return super.addSingleResult(rayResult, normalInWorldSpace);
      }
    };
  }

  public static PPhysicsRayCast obtain() {
    return staticPool().obtain();
  }

  public PPhysicsRayCast cast() {
    PAssert.isFalse(valid, "Call reset() first.");
    valid = true;
    searchRayLength = rayFromWorld().dst(rayToWorld());
    PPhysicsEngine.dynamicsWorld.rayTest(rayFromWorld().backingVec3(), rayToWorld().backingVec3(), backingCallback);
    hasHit = backingCallback.hasHit();
    if (hasHit) {
      hitFraction = backingCallback.getClosestHitFraction();
      hitDistance = searchRayLength * hitFraction;
    }
    return this;
  }

  @Override public void free() {
    staticPool().free(this);
  }

  public boolean hasHit() {
    PAssert.isTrue(valid);
    return hasHit;
  }

  public float hitDistance() {
    PAssert.isTrue(valid && hasHit);
    return hitDistance;
  }

  public PVec3 hitLocation(PVec3 out) {
    PAssert.isTrue(valid && hasHit);
    backingCallback.getHitPointWorld(out.backingVec3());
    return out;
  }

  public PVec3 hitNormal(PVec3 out) {
    PAssert.isTrue(valid && hasHit);
    backingCallback.getHitNormalWorld(out.backingVec3());
    return out;
  }

  @Override public void reset() {
    rayFromWorld().setZero();
    rayToWorld().setZero();
    valid = false;
    hasHit = false;
    hitDistance = 0;
    backingCallback.setCollisionObject(null);
    backingCallback.setClosestHitFraction(1f);
    backingCallback.setCollisionFilterMask(PPhysicsEngine.ALL_FLAG);
    backingCallback.setCollisionFilterGroup(PPhysicsEngine.ALL_FLAG);
  }

  public PPhysicsRayCast setCollisionFilterGroup(int group) {
    PAssert.isFalse(valid);
    backingCallback.setCollisionFilterGroup(group);
    return this;
  }

  public PPhysicsRayCast setCollisionFilterMask(int mask) {
    PAssert.isFalse(valid);
    backingCallback.setCollisionFilterMask(mask);
    return this;
  }

  public PPhysicsRayCast setOnlyStaticBodies(boolean onlyStaticBodies) {
    PAssert.isFalse(valid);
    this.onlyStaticBodies = onlyStaticBodies;
    return this;
  }
}
