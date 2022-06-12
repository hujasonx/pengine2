package com.phonygames.pengine.graphics.model.gen;

import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPostableTask;
import com.phonygames.pengine.util.PPostableTaskQueue;
import com.phonygames.pengine.util.PWindowedBuffer;

import java.util.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

public class PModelGen implements PPostableTask {
  @Getter
  private static final PPostableTaskQueue postableTaskQueue = new PPostableTaskQueue();


  protected static final PList<PGlNode> tempGlNodesBuffer = new PList<>();

  private boolean finished = false;
  private final PMap<String, Part> parts = new PMap<>();
  private final PMap<String, Part> physicsParts = new PMap<>();

  public Part addPart(String name, PVertexAttributes vertexAttributes) {
    Part p = new Part(name, false, vertexAttributes);
    parts.put(name, p);
    return p;
  }

  public Part addPhysicsPart(String name) {
    Part p = new Part(name, false, PVertexAttributes.getPHYSICS());
    physicsParts.put(name, p);
    return p;
  }

  public Part addPbrPart(String name) {
    return addPart(name, PVertexAttributes.getGLTF_UNSKINNED());
  }

  public void buildSynchronous() {
    intro();
    middle();
    end();
  }

  @Override
  public void intro() {
    modelIntro();
  }

  protected void modelIntro() {

  }

  @Override
  public void middle() {
    modelMiddle();
  }

  protected void modelMiddle() {

  }

  @Override
  public void end() {
    modelEnd();
  }

  protected void modelEnd() {

  }

  protected PModelGen chainGlNode(PList<PGlNode> list,
                                  Part part,
                                  PMaterial defaultMaterial,
                                  ArrayMap<String, PMat4> boneInvBindTransforms) {
    if (list == null) {
      list = new PList<>();
    }

    val glNode = new PGlNode(part.name);
    glNode.getDrawCall().setMesh(part.getMesh());
    glNode.getDrawCall().setMaterial(defaultMaterial);
    if (boneInvBindTransforms != null) {
      glNode.getInvBoneTransforms().putAll(boneInvBindTransforms);
    }

    list.add(glNode);
    return this;
  }

  public static class Part {
    private final PVertexAttributes vertexAttributes;
    @Getter
    private final String name;
    @Getter
    private final Optional<String> footstepSoundStrategy = Optional.empty();
    @Getter
    private final boolean isPhysicsPart;

    private final float[] currentVertexValues;

    private final PWindowedBuffer latestIndices = new PWindowedBuffer(4);
    private final PList<Float> vertices = new PList<>();
    private final PList<Short> indices = new PList<>();

    @Getter
    private int numVertices = 0;

    private Part(@NonNull String name, boolean isPhysicsPart, PVertexAttributes vertexAttributes) {
      this.name = name;
      this.isPhysicsPart = isPhysicsPart;
      this.vertexAttributes = vertexAttributes;

      currentVertexValues = new float[vertexAttributes.getNumFloatsPerVertex()];
    }

    public PVec2 get(PVec2 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1);
    }

    public PVec3 get(PVec3 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1).z(ind + 2);
    }

    public PVec4 get(PVec4 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1).z(ind + 2).w(ind + 3);
    }

    public Part set(String alias, PVec2 out) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = out.x();
      currentVertexValues[ind + 1] = out.y();
      return this;
    }

    public Part set(String alias, PVec3 out) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = out.x();
      currentVertexValues[ind + 1] = out.y();
      currentVertexValues[ind + 2] = out.z();
      return this;
    }

    public Part set(String alias, PVec4 out) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = out.x();
      currentVertexValues[ind + 1] = out.y();
      currentVertexValues[ind + 2] = out.z();
      currentVertexValues[ind + 3] = out.w();
      return this;
    }

    public Part set(String alias, float x) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 1);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = x;
      return this;
    }

    public Part set(String alias, float x, float y) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = x;
      currentVertexValues[ind + 1] = y;
      return this;
    }

    public Part set(String alias, float x, float y, float z) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = x;
      currentVertexValues[ind + 1] = y;
      currentVertexValues[ind + 2] = z;
      return this;
    }

    public Part set(String alias, float x, float y, float z, float w) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues[ind + 0] = x;
      currentVertexValues[ind + 1] = y;
      currentVertexValues[ind + 2] = z;
      currentVertexValues[ind + 3] = w;
      return this;
    }

    public Part emitVertex() {
      return emitVertex(100);
    }

    public Part emitVertex(int maxLookbackAmount) {
      short index = (short) numVertices;
      for (int lookbackAmount = 1; lookbackAmount < maxLookbackAmount; lookbackAmount++) {
        int lookbackIndex = index - lookbackAmount;
        if (lookbackIndex < 0) {
          break;
        }

        int verticesIndex = lookbackIndex * vertexAttributes.getNumFloatsPerVertex();
        boolean isEqual = true;
        for (int a = 0; a < vertexAttributes.getNumFloatsPerVertex(); a++) {
          if (!PNumberUtils.epsilonEquals(vertices.get(verticesIndex + a), currentVertexValues[a])) {
            isEqual = false;
            break;
          }
        }

        if (isEqual) {
          index = (short) lookbackIndex;
          break;
        }
      }

      // Need to add a new vertex.
      if (index == numVertices) {
        numVertices++;
        for (int a = 0; a < vertexAttributes.getNumFloatsPerVertex(); a++) {
          vertices.add(currentVertexValues[a]);
        }
      }

      latestIndices.addInt(index);
      return this;
    }

    public Part tri(boolean flip) {
      indices.add((short) latestIndices.get(2));
      indices.add((short) latestIndices.get(flip ? 0 : 1));
      indices.add((short) latestIndices.get(flip ? 1 : 0));
      return this;
    }

    public Part quad(boolean flip) {
      indices.add((short) latestIndices.get(3));
      indices.add((short) latestIndices.get(flip ? 1 : 2));
      indices.add((short) latestIndices.get(flip ? 2 : 1));

      indices.add((short) latestIndices.get(3));
      indices.add((short) latestIndices.get(flip ? 0 : 1));
      indices.add((short) latestIndices.get(flip ? 1 : 0));
      return this;
    }

    public Part clear() {
      vertices.clear();
      indices.clear();
      numVertices = 0;
      return this;
    }

    public PMesh getMesh() {
      PMesh ret = new PMesh(true, vertices, indices, vertexAttributes);
      return ret;
    }
  }
}
