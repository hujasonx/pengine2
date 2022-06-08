package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PCollectionUtils;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.val;

public class PVertexAttributes {
  final VertexAttributes vertexAttributes;

  private final Map<String, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  @Getter
  private final int numFloatsPerVertex;

  private final String combinedAttributeAliases;

  public static final class Attribute {
    private static int maxUsage = 1;

    public static final class Keys {
      public static final String pos = "pos";
      public static final String nor = "nor";
      public static final String uv[] = new String[4];
      public static final String col[] = new String[4];
    }

    public static final Class[] VectorClasses = new Class[]{null, null, PVec2.class, PVec3.class, PVec4.class};

    private static final PMap<String, VertexAttribute> list = new PMap<>();

    private static void registerAttribute(String id, int numComponents) {
      list.put(id, new VertexAttribute(maxUsage *= 2, numComponents, id));
    }

    public static VertexAttribute get(String key) {
      return list.get(key);
    }

    private static void init() {
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

  public static void init() {
    Attribute.init();
  }

  public static final PVertexAttributes DEFAULT =
      new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0), VertexAttribute.ColorUnpacked()});
  public static final PVertexAttributes PHYSICS =
      new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position()});

  public PVertexAttributes(VertexAttribute[] vertexAttributes) {
    int floatsPerVertex = 0;
    StringBuilder combinedAliases = new StringBuilder();
    for (int a = 0; a < vertexAttributes.length; a++) {
      val va = vertexAttributes[a];
      vertexAttributeFloatIndexInVertex.put(va.alias, floatsPerVertex);
      combinedAliases.append("|").append(va.alias);

      floatsPerVertex += va.numComponents;
    }

    this.vertexAttributes = new VertexAttributes(vertexAttributes);

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

  public int indexForVertexAttribute(String alias) {
    return vertexAttributeFloatIndexInVertex.get(alias);
  }

  public int indexForVertexAttribute(VertexAttribute vertexAttribute) {
    return vertexAttributeFloatIndexInVertex.get(vertexAttribute.alias);
  }
}
