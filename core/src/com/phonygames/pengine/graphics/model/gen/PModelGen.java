package com.phonygames.pengine.graphics.model.gen;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PWindowedBuffer;

import java.util.Optional;

import lombok.Getter;
import lombok.NonNull;

public class PModelGen {
  private boolean finished = false;
  private final PMap<String, Part> parts = new PMap<>();
  private final PMap<String, Part> physicsParts = new PMap<>();

  public Part addPart(String name, PVertexAttributes vertexAttributes) {
    return new Part(name, false, vertexAttributes);
  }

  public Part addPhysicsPart(String name) {
    return new Part(name, true, PVertexAttributes.PHYSICS);
  }

  static class Part {
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
      PMesh ret = new PMesh(true, vertices.size(), indices.size(), vertexAttributes);
      return ret;
    }
  }
}
