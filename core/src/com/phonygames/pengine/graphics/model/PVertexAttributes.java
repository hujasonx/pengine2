package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.phonygames.pengine.util.PCollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;

public class PVertexAttributes {
  @Getter
  private final List<VertexAttribute> vertexAttributes = new ArrayList<>();

  private final Map<VertexAttribute, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  @Getter
  private final int numFloatsPerVertex;

  private final String combinedAttributeAliases;

  public static final class Attribute {
    public static final class Keys {
      public static final String pos = "pos";
      public static final String nor = "nor";
      public static final String uv[] = new String[4];
      public static final String col[] = new String[4];
    }

    private static final Map<String, VertexAttribute> attributes = new HashMap<>();

    private static void registerAttribute(String id, int numComponents) {
      attributes.put(id, new VertexAttribute(0 /* Usage is ignored */, numComponents, id));
    }

    static {
      registerAttribute(Keys.pos, 3);
      registerAttribute(Keys.nor, 3);

      for (int a = 0; a < Keys.uv.length; a++) {
        Keys.uv[a] = "uv" + a;
        registerAttribute(Keys.uv[a], 2);
      }

      for (int a = 0; a < Keys.col.length; a++) {
        Keys.col[a] = "col" + a;
        registerAttribute(Keys.col[a], 4);
      }
    }
  }

  public static final class Templates {
    public static final PVertexAttributes DEFAULT =
        new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0), VertexAttribute.ColorUnpacked()});
    public static final PVertexAttributes PHYSICS =
        new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position()});
  }

  public PVertexAttributes(VertexAttribute[] vertexAttributes) {
    int floatsPerVertex = 0;
    StringBuilder combinedAliases = new StringBuilder();
    for (int a = 0; a < vertexAttributes.length; a++) {
      val va = vertexAttributes[a];
      this.vertexAttributes.add(va);
      vertexAttributeFloatIndexInVertex.put(va, floatsPerVertex);
      floatsPerVertex += va.numComponents;
      combinedAliases.append("|").append(va.alias);
    }

    this.combinedAttributeAliases = combinedAliases.toString();
    this.numFloatsPerVertex = floatsPerVertex;
  }

  @Override
  public int hashCode() {
    return combinedAttributeAliases.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PVertexAttributes) {
      val other = (PVertexAttributes) o;
      return combinedAttributeAliases.equals(other.combinedAttributeAliases);
    }

    return false;
  }

  public int indexForVertexAttribute(VertexAttribute vertexAttribute) {
    return vertexAttributeFloatIndexInVertex.get(vertexAttribute);
  }
}
