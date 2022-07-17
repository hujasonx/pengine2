package com.phonygames.pengine.physics;

import com.badlogic.gdx.graphics.Camera;
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
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PSet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PPhysicsEngine {
  public static final short ALL_FLAG = -1;
  public static final short STATIC_FLAG = 0x1 << 0;
  public static final short CHARACTER_CONTROLLER_FLAG = 0x1 << 1;
  public static final short BONE_FLAG = 0x2 << 1;
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PSet<PPhysicsCollisionShape> collisionShapes = new PSet<>();
  public static btMultiBodyDynamicsWorld dynamicsWorld;
  private static btDbvtBroadphase broadphase;
  private static ClosestRayResultCallback closestRayResultCallback;
  private static btCollisionConfiguration collisionConfiguration;
  private static btMultiBodyConstraintSolver constraintSolver;
  //  private static ContactListener bulletContactListener;
  private static DebugDrawer debugDrawer;
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static Camera debugDrawerCamera;
  private static btDispatcher dispatcher;
  private static btGhostPairCallback ghostPairCallback;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static boolean inited = false, enableDebugRender = false;
  private static btPersistentManifold sharedManifold;
  protected static PList<PRigidBody> rigidBodiesInSimulation = new PList<>();
  public static float gravity = 9.81f;

  public static void dispose() {
    try (val it = collisionShapes().obtainIterator()) {
      while (it.hasNext()) {
        val collisionShape = it.next();
        collisionShape.dispose();
    }}
    collisionShapes().clear();
  }

  public static void enableDebugRender(boolean enabled) {
    enableDebugRender = enabled;
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

  public static void preFrameUpdate() {
    for (int a = 0; a < rigidBodiesInSimulation.size(); a++) {
      PRigidBody rigidBody = rigidBodiesInSimulation.get(a);
      rigidBody.preFrameUpdate();
    }
  }

  public static void postFrameUpdate() {
    if (!enableDebugRender || !inited) {
      return;
    }
    PAssert.isNotNull(debugDrawerCamera);
    debugDrawer.begin(debugDrawerCamera);
    dynamicsWorld.debugDrawWorld();
    debugDrawer.end();
  }

  public static void postLogicUpdate() {
    if (!inited) {
      return;
    }
    if (PEngine.t < 5) {return;}
    dynamicsWorld.stepSimulation(PEngine.logictimestep);
    for (int a = 0; a < rigidBodiesInSimulation.size(); a++) {
      PRigidBody rigidBody = rigidBodiesInSimulation.get(a);
      rigidBody.postLogicUpdate();
    }
  }

  protected static void trackShape(PPhysicsCollisionShape collisionShape) {
    PAssert.isTrue(inited);
    collisionShapes().add(collisionShape);
  }
}
