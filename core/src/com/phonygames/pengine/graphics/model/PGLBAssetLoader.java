package com.phonygames.pengine.graphics.model;

import android.support.annotation.Nullable;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.utils.Array;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.animation.PNodeAnimation;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;
import com.phonygames.pengine.physics.collisionshape.PPhysicsBoxShape;
import com.phonygames.pengine.physics.collisionshape.PPhysicsCompoundShape;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.shared.animation.Interpolation;
import net.mgsx.gltf.scene3d.animation.NodeAnimationHack;
import net.mgsx.gltf.scene3d.model.NodePlus;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import lombok.val;

public class PGLBAssetLoader extends AsynchronousAssetLoader<PModel, PGLBAssetLoader.LoaderParameters> {
  private static final Vector3 tempVector3_111 = new Vector3(1, 1, 1);
  private final PStringMap<PhysicsNode> physicsNodes = new PStringMap<PhysicsNode>() {
    @Override public PhysicsNode newUnpooled(String s) {
      PhysicsNode ret = new PhysicsNode();
      ret.nodeName = s;
      return ret;
    }
  };

  public PGLBAssetLoader() {
    this(new InternalFileHandleResolver());
  }

  public PGLBAssetLoader(FileHandleResolver resolver) {
    super(resolver);
  }

  private static final boolean has(String[] arr, String value) {
    if (value == null || arr == null) {return false;}
    for (int a = 0; a < arr.length; a++) {
      if (arr[a].equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, LoaderParameters parameter) {
    return null;
  }

  @Override public void loadAsync(AssetManager manager, String fileName, FileHandle file, LoaderParameters parameter) {
  }

  @Override public PModel loadSync(AssetManager manager, String fileName, FileHandle file, LoaderParameters parameter) {
    physicsNodes.clear();
    SceneAsset sceneAsset = new GLBLoader().load(file, true);
    PModel model;
    PModel.Builder modelBuilder = new PModel.Builder();
    PList<NodePlus> nodesToProcess = new PList<>();
    PMap<NodePlus, PModel.Node> childToParentNodeMap = new PMap<>();
    for (val node : sceneAsset.scene.model.nodes) {
      NodePlus nodePlus = (NodePlus) node;
      nodesToProcess.add(nodePlus);
    }
    PList<PGlNode> glNodes = new PList<>();
    // Save the physics nodes to be processed at the end.
    PList<NodePlus> physicsNodePluses = new PList<>();
    while (nodesToProcess.size() > 0) {
      glNodes.clear();
      NodePlus nodePlus = nodesToProcess.removeLast();
      if (nodePlus.id.startsWith("__colShape.")) {
        // Store the physics node to be processed later. (It should not have any children).
        physicsNodePluses.add(nodePlus);
        continue;
      }
      // Generate meshes if needed.
      if (!nodePlus.parts.isEmpty()) {
        for (val gdxNodePart : nodePlus.parts) {
          PMesh mesh = new PMesh(gdxNodePart.meshPart.mesh);
          PGlNode node = new PGlNode(gdxNodePart.meshPart.id + "[" + glNodes.size() + "]");
          node.drawCall().mesh(mesh);
          node.drawCall().material(genMaterial(gdxNodePart.material));
          node.drawCall().setLayer(PGltf.Layer.PBR);
          if (gdxNodePart.invBoneBindTransforms != null) {
            for (val invBoneBT : gdxNodePart.invBoneBindTransforms) {
              node.invBoneTransforms().put(invBoneBT.key.id, PMat4.obtain(invBoneBT.value.val));
            }
          }
          glNodes.add(node);
        }
      }
      PMat4 transform = PMat4.obtain().set(nodePlus.translation, nodePlus.rotation, nodePlus.scale);
      PModel.Node node = modelBuilder.addNode(nodePlus.id, childToParentNodeMap.get(nodePlus), glNodes, transform);
      node.inheritTransform(nodePlus.inheritTransform);
      for (val child : nodePlus.getChildren()) {
        nodesToProcess.add((NodePlus) child);
        childToParentNodeMap.put((NodePlus) child, node);
      }
    }
    model = modelBuilder.build();
    // Add the physics nodes.
    for (int a = 0; a < physicsNodePluses.size(); a++) {
      NodePlus nodePlus = physicsNodePluses.get(a);
      processPhysicsNode(nodePlus, childToParentNodeMap.get(nodePlus), model);
    }
    try (val it = physicsNodes.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        e.v().emitToModelNode(model.nodes().get(e.k()));
      }
    }
    loadAnimationsFromAsset(sceneAsset, model);
    return model;
  }

  private PMaterial genMaterial(Material material) {
    val ret = new PMaterial(material.id, null);
    for (Attribute attribute : material) {
      ret.set(attribute);
    }
    return ret;
  }

  /**
   * Must be called after the model has already been generated.
   * <p>
   * Processes the NodePlus for its physics shape data.
   * <p>
   * The node id should be named as follows: __colShape.nodeName.[Box].[#]
   * <p>
   * if the nodeName has L or R,  a duplicate mirrored shape will be emitted. if # is specified, the node won't be used
   * for the center of mass calculation unless no other node is available.
   *
   * @param node
   */
  private void processPhysicsNode(NodePlus node, @Nullable PModel.Node parentModelNode, PModel model) {
    System.out.println("Process physics node for NodePlus: " + node.id);
    String idSplit[] = node.id.split("\\.");
    boolean couldBeCOMNode = true;
    PrimitiveShape shape = null;
    String modelNodeName = idSplit[1];
    String mirrorModelNodeName = null;
    int a = 0;
    a++; // Skip the first split, since it should just be "__colShape".
    a++; // Skip the next split since it should be the name of the node to apply to.
    if (idSplit.length >= 3) {
      if ("L".equals(idSplit[2])) {
        mirrorModelNodeName = modelNodeName + ".R";
        modelNodeName += ".L";
        a++;
      } else if ("R".equals(idSplit[2])) {
        mirrorModelNodeName = modelNodeName + ".L";
        modelNodeName += ".R";
        a++;
      }
    }
    PhysicsNode physicsNode = physicsNodes.genUnpooled(modelNodeName);
    PhysicsNode mirrorPhysicsNode = mirrorModelNodeName == null ? null : physicsNodes.genUnpooled(mirrorModelNodeName);
    // Process params in the node id.
    for (; a < idSplit.length; a++) {
      if (idSplit[a].length() == 0) {continue;}
      String[] colonSplit = idSplit[a].split(":");
      String param = colonSplit[0];
      // If the param is a valid integer, it signifies that this is not a center of mass node.
      try {
        Integer.parseInt(param);
        couldBeCOMNode = false;
        continue;
      } catch (Exception e) {}
      String value = colonSplit.length == 1 ? null : colonSplit[1];
      if ("Box".equals(param)) {
        shape = PrimitiveShape.BOX;
      } else if ("Mass".equals(param)) {
        physicsNode.mass = Float.parseFloat(value);
      }
    }
    // If we are a center of mass node, calculate the center orientations.
    if (couldBeCOMNode) {
      physicsNode.centerOrientation.getBackingMatrix4().set(node.translation, node.rotation, tempVector3_111);
      if (parentModelNode != null && node.inheritTransform) {
        physicsNode.centerOrientation.mulLeft(parentModelNode.modelSpaceTransform());
      }
      if (mirrorPhysicsNode != null) {
        try (PPool.PoolBuffer pool = PPool.getBuffer()) {
          // Mirror across the yz plane.
          PVec3 pos = physicsNode.centerOrientation.getTranslation(pool.vec3());
          PVec4 rot = physicsNode.centerOrientation.getRotation(pool.vec4());
          pos.x(-pos.x());
          PVec3 rotAxis = pool.vec3();
          //        float rotAngle = rot.getAxisAngle(rotAxis);
          //        rot.setToRotation(rotAxis.x(-rotAxis.x()), -rotAngle);
          rot.y(-rot.y());
          rot.z(-rot.z());
          mirrorPhysicsNode.centerOrientation.set(pos, rot, PVec3.ONE);
        }
      }
    }
    // Generate and add primitive shape instances to their respective physics nodes.
    PhysicsNode.PrimitiveShapeInstance primitiveShapeInstance = null;
    PhysicsNode.PrimitiveShapeInstance mirrorPrimitiveInstanceShape = null;
    switch (shape) {
      case BOX:
        primitiveShapeInstance = new PhysicsNode.BoxShapeInstance();
        mirrorPrimitiveInstanceShape = mirrorModelNodeName == null ? null : new PhysicsNode.BoxShapeInstance();
        break;
      default:
        PAssert.fail("Invalid shape");
        break;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 primitiveShapeScl = pool.vec3();
      primitiveShapeScl.backingVec3().set(node.scale);
      if (primitiveShapeInstance != null) {
        primitiveShapeInstance.bindPoseMS.getBackingMatrix4().set(node.translation, node.rotation, tempVector3_111);
        if (parentModelNode != null && node.inheritTransform) {
          primitiveShapeInstance.bindPoseMS.mulLeft(parentModelNode.modelSpaceTransform());
        }
        primitiveShapeInstance.scl.set(primitiveShapeScl);
        System.out.println(
            "Adding " + shape.name() + " shape to " + modelNodeName + " \tAt " + primitiveShapeInstance.bindPoseMS);
        physicsNode.primitiveInstances.add(primitiveShapeInstance);
      }
      // Add the mirror primitive instnace if necessary.
      if (mirrorPrimitiveInstanceShape != null && mirrorPhysicsNode != null) {
        mirrorPrimitiveInstanceShape.bindPoseMS.set(primitiveShapeInstance.bindPoseMS);
        mirrorPrimitiveInstanceShape.scl.set(primitiveShapeScl);
        PVec3 pos = mirrorPrimitiveInstanceShape.bindPoseMS.getTranslation(pool.vec3());
        PVec4 rot = mirrorPrimitiveInstanceShape.bindPoseMS.getRotation(pool.vec4());
        // Mirror across the yz plane.
        pos.x(-pos.x());
        PVec3 rotAxis = pool.vec3();
//        float rotAngle = rot.getAxisAngle(rotAxis);
//        rot.setToRotation(rotAxis.x(-rotAxis.x()), -rotAngle);
        rot.y(-rot.y());
        rot.z(-rot.z());
        mirrorPrimitiveInstanceShape.bindPoseMS.set(pos, rot, PVec3.ONE);
        System.out.println("Mirror " + shape.name() + " shape to " + mirrorModelNodeName + " \tAt " +
                           mirrorPrimitiveInstanceShape.bindPoseMS);
        mirrorPhysicsNode.primitiveInstances.add(mirrorPrimitiveInstanceShape);
      }
    }
  }

  private static void loadAnimationsFromAsset(SceneAsset asset, PModel model) {
    for (Animation baseAnim : asset.animations) {
      PAnimation.Builder builder = new PAnimation.Builder(baseAnim.id);
      for (val e : baseAnim.nodeAnimations) {
        val nodeAnimHack = (NodeAnimationHack) e;
        PNodeAnimation.Builder nodeBuilder = new PNodeAnimation.Builder(nodeAnimHack.node.id);
        nodeBuilder.setDefaultTRS(e.node.translation, e.node.rotation, e.node.scale);
        if (e.translation != null) {
          nodeBuilder.setInterpolationTranslation(convertInterpolation(nodeAnimHack.translationMode));
          for (val t : e.translation) {
            nodeBuilder.addTranslationKeyframe(t.keytime, PVec3.obtain().set(t.value));
          }
        }
        if (e.rotation != null) {
          nodeBuilder.setInterpolationRotation(convertInterpolation(nodeAnimHack.rotationMode));
          for (val t : e.rotation) {
            nodeBuilder.addRotationKeyframe(t.keytime, PVec4.obtain().set(t.value));
          }
        }
        if (e.scaling != null) {
          nodeBuilder.setInterpolationScale(convertInterpolation(nodeAnimHack.scalingMode));
          for (val t : e.scaling) {
            nodeBuilder.addScaleKeyframe(t.keytime, PVec3.obtain().set(t.value));
          }
        }
        builder.addNodeAnimation(nodeBuilder.build());
      }
      model.animations().put(baseAnim.id, builder.build());
    }
  }

  private static PNodeAnimation.Interpolation convertInterpolation(Interpolation i) {
    if (i == null) {
      return PNodeAnimation.Interpolation.STEP;
    }
    switch (i) {
      case STEP:
        return PNodeAnimation.Interpolation.STEP;
      case CUBICSPLINE:
        return PNodeAnimation.Interpolation.CUBIC;
      case LINEAR:
      default:
        return PNodeAnimation.Interpolation.LINEAR;
    }
  }

  enum PrimitiveShape {
    BOX
  }

  public static class LoaderParameters extends AssetLoaderParameters<PModel> {}

  private static class PhysicsNode {
    final PList<PrimitiveShapeInstance> primitiveInstances = new PList<>();
    PMat4 centerOrientation = PMat4.obtain();
    float mass = 1;
    String nodeName;

    public void emitToModelNode(PModel.Node modelNode) {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        PMat4 centerOrientationInv = pool.mat4(centerOrientation).inv();
        PMat4 modelNodeMSTransformInv = pool.mat4(modelNode.modelSpaceTransform()).inv();
        if (primitiveInstances.size() == 1) {
          // No need to use a compound shape.
          modelNode.physicsCollisionShape = primitiveInstances.get(0).genPShape();
        } else {
          btCompoundShape compoundShape = new btCompoundShape();
          for (int a = 0; a < primitiveInstances.size(); a++) {
            PrimitiveShapeInstance primitive = primitiveInstances.get(a);
            PMat4 primitiveLocalTransform = pool.mat4(centerOrientationInv).mul(primitive.bindPoseMS);
            btCollisionShape btShape = primitive.genShape();
            compoundShape.addChildShape(primitiveLocalTransform.getBackingMatrix4(), btShape);
          }
          modelNode.physicsCollisionShape = new PPhysicsCompoundShape(compoundShape);
        }
        modelNode.physicsCollisionShapeOffset().set(centerOrientation).mulLeft(modelNodeMSTransformInv);
        modelNode.physicsCollisionShapeOffsetInv().set(modelNode.physicsCollisionShapeOffset()).inv();
        System.out.println("AAA" + modelNode.physicsCollisionShapeOffset().getScale(pool.vec3()));
        modelNode.boneMass = mass;
      }
    }

    static class BoxShapeInstance extends PrimitiveShapeInstance<btBoxShape, PPhysicsBoxShape> {
      @Override PPhysicsBoxShape genPShape() {
        return new PPhysicsBoxShape(genShape());
      }

      @Override btBoxShape genShape() {
        return new btBoxShape(scl.backingVec3());
      }
    }

    static abstract class PrimitiveShapeInstance<U extends btCollisionShape, T extends PPhysicsCollisionShape<U>> {
      PMat4 bindPoseMS = PMat4.obtain();
      PrimitiveShape primitiveShape;
      PVec3 scl = PVec3.obtain();

      abstract T genPShape();
      abstract U genShape();
    }
  }
}
