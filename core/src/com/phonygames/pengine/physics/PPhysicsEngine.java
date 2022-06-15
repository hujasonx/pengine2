package com.phonygames.pengine.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifold;
import com.badlogic.gdx.physics.bullet.dynamics.btMultiBodyConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btMultiBodyDynamicsWorld;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PSet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class PPhysicsEngine {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PSet<PPhysicsCollisionShape> collisionShapes = new PSet<>();

  private static boolean inited = false, enableDebugRender = false;
  public static btMultiBodyDynamicsWorld dynamicsWorld;
  private static btDbvtBroadphase broadphase;
  private static ClosestRayResultCallback closestRayResultCallback;
  private static btCollisionConfiguration collisionConfiguration;
  private static btMultiBodyConstraintSolver constraintSolver;
  //  private static ContactListener bulletContactListener;
  private static DebugDrawer debugDrawer;
  private static btDispatcher dispatcher;
  private static btGhostPairCallback ghostPairCallback;
  private static btPersistentManifold sharedManifold;

  public static void dispose() {
    for (val e : collisionShapes()) {
      e.dispose();
    }
    collisionShapes().clear();
  }

  public static void init() {
    Bullet.init();
    collisionConfiguration = new btDefaultCollisionConfiguration();
    dispatcher = new btCollisionDispatcher(collisionConfiguration);
    sharedManifold = new btPersistentManifold();
    ghostPairCallback = new btGhostPairCallback();
    broadphase = new btDbvtBroadphase();
    broadphase.getOverlappingPairCache().setInternalGhostPairCallback(ghostPairCallback);
    //    constraintSolver = new btSequentialImpulseConstraintSolver();
    //    dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfiguration);
    constraintSolver = new btMultiBodyConstraintSolver();
    dynamicsWorld = new btMultiBodyDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfiguration);
    dynamicsWorld.setGravity(new Vector3(0, -9.81f, 0));
    //
    debugDrawer = new DebugDrawer();
    debugDrawer.setDebugMode(
        btIDebugDraw.DebugDrawModes.DBG_DrawWireframe | btIDebugDraw.DebugDrawModes.DBG_DrawConstraintLimits |
        btIDebugDraw.DebugDrawModes.DBG_DrawConstraints);
    dynamicsWorld.setDebugDrawer(debugDrawer);
    //    bulletContactListener = new PPhysicsContactListener();
    closestRayResultCallback = new ClosestRayResultCallback(new Vector3(), new Vector3());
    inited = true;
  }

  public static void postLogicUpdate() {
    if (!inited) {
      return;
    }
    dynamicsWorld.stepSimulation(PEngine.logictimestep);
  }

  public static void postFrameUpdate() {

  }

  public static void enableDebugRender(boolean enabled) {
    enableDebugRender = enabled;
  }

  protected static void trackShape(PPhysicsCollisionShape collisionShape) {
    PAssert.isTrue(inited);
    collisionShapes().add(collisionShape);
  }
}
