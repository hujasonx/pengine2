package com.phonygames.pengine.physics;

import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PVec3;

public class PRagdollUtils {
  public static void addConstraintsForGenericRagdoll(PVec3 initialVelocity, PModelInstance modelInstance) {
    if (modelInstance == null) {return;}
    for (int a = 0; a < modelInstance.rootNodes().size(); a++) {
      PModelInstance.Node rootNode = modelInstance.rootNodes().get(a);
      addConstraintsToParentRecursiveForGenericRagdoll(initialVelocity, modelInstance, rootNode, null, null);
    }
  }

  private static void addConstraintsToParentRecursiveForGenericRagdoll(PVec3 initialVelocity, PModelInstance modelInstance,
                                                                       PModelInstance.Node node,
                                                                       PModelInstance.Node parentNode,
                                                                       PModelInstance.Node lastParentWithRigidBody) {
    if (node.rigidBody() == null) {
      for (int a = 0; a < node.children().size(); a++) {
        PModelInstance.Node childNode = node.children().get(a);
        addConstraintsToParentRecursiveForGenericRagdoll(initialVelocity, modelInstance,childNode,node,lastParentWithRigidBody);
      }
      return;
    }
    // Actually add the constraint.
    PRigidBody rigidBody = node.rigidBody();
    rigidBody.setLinearFactor(1, 1, 1);
    rigidBody.setAngularFactor(1, 1, 1);
    rigidBody.setSleepingThresholds(0, 0);
    rigidBody.activate();
    rigidBody.setLinearVelocity(initialVelocity);
    node.rigidBodyDrivesMovement(true);
    if (lastParentWithRigidBody != null) {
      // Add constraints.
      PRigidBody prevRigidBody = lastParentWithRigidBody.rigidBody();
      PTypedConstraint constraint = PTypedConstraint.obtain(PTypedConstraint.Type.ConeTwist);
      constraint.setLocalTransformsFromModelInstanceNodes(lastParentWithRigidBody, node);
      btTypedConstraint btConstraint = constraint.genBtTypedConstraint(prevRigidBody, rigidBody);
      rigidBody.addManagedConstraint(btConstraint);
      constraint.free();
    }

    for (int a = 0; a < node.children().size(); a++) {
      PModelInstance.Node childNode = node.children().get(a);
      addConstraintsToParentRecursiveForGenericRagdoll(initialVelocity, modelInstance,childNode,node,node);
    }
  }
}
