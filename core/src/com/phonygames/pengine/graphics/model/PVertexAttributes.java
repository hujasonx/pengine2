package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PStringMap;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.val;

public class PVertexAttributes {
  @Getter
  private static PVertexAttributes PHYSICS = new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position()});
  @Getter
  private static PVertexAttributes POS, POS_NOR_UV0_UV1, POS_UV0_COL0, POS_NOR_UV0, POS2D_UV0_COLPACKED0;
  @Getter
  /** Pos, nor, uv, col */ private static PVertexAttributes POS_NOR_UV0_COL0;
  @Getter
  private final VertexAttributes backingVertexAttributes;
  //  private final Map<String, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  private final Map<String, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  @Getter
  private int bytesPerVertex;
  @Getter
  private int numFloatsPerVertex;
  @Getter
  private String prefix;

  public PVertexAttributes(Iterable<VertexAttribute> backingVertexAttributes) {
    PList<VertexAttribute> vaList = new PList<>();
    for (VertexAttribute va : backingVertexAttributes) {
      vaList.add(va);
    }
    VertexAttribute[] vaArray = new VertexAttribute[vaList.size()];
    for (int a = 0; a < vaArray.length; a++) {
      vaArray[a] = vaList.get(a);
    }
    this.backingVertexAttributes = genVertexAttributes(vaArray);
  }

  private VertexAttributes genVertexAttributes(VertexAttribute[] vertexAttributes) {
    int floatsPerVertex = 0;
    StringBuilder prefix = new StringBuilder();
    for (int a = 0; a < vertexAttributes.length; a++) {
      val va = vertexAttributes[a];
      vertexAttributeFloatIndexInVertex.put(va.alias, floatsPerVertex);
      prefix.append("#define ").append(va.alias).append("Flag\n");
      floatsPerVertex += va.getSizeInBytes() / 4;
    }
    this.prefix = prefix.toString();
    this.numFloatsPerVertex = floatsPerVertex;
    return new VertexAttributes(vertexAttributes);
  }

  public PVertexAttributes(VertexAttribute[] backingVertexAttributes) {
    this.backingVertexAttributes = genVertexAttributes(backingVertexAttributes);
  }

  public static void init() {
    Attribute.init();
  }

  public static PVec3 transformVecWithMatrix(@NonNull VertexAttribute vertexAttribute, @NonNull PVec3 inout,
                                             @NonNull PMat4 mat4) {
    if (vertexAttribute.alias.equals(Attribute.Keys.pos)) {
      return inout.mul(mat4, 1);
    }
    if (vertexAttribute.alias.equals(Attribute.Keys.nor)) {
      return inout.mul(mat4, 0);
    }
    // TODO: binormal, tangent
    return inout;
  }

  public boolean hasAttributeWithName(String name) {
    return vertexAttributeFloatIndexInVertex.containsKey(name);
  }

  @Override public int hashCode() {
    return backingVertexAttributes.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (o instanceof PVertexAttributes) {
      val other = (PVertexAttributes) o;
      return backingVertexAttributes.equals(other.backingVertexAttributes);
    }
    return false;
  }

  /** Returns the index in a float buffer for each vertex where the vertex attribute begins. */
  public int indexForVertexAttribute(VertexAttribute vertexAttribute) {
    return indexForVertexAttribute(vertexAttribute.alias);
  }

  /** Returns the index in a float buffer for each vertex where the vertex attribute begins. */
  public int indexForVertexAttribute(String alias) {
    PAssert.isTrue(vertexAttributeFloatIndexInVertex.containsKey(alias),
                   alias + " not found in vertexAttributeFloatIndexInVertex");
    if (!vertexAttributeFloatIndexInVertex.containsKey(alias)) {return -1;}
    return vertexAttributeFloatIndexInVertex.get(alias);
  }

  public static final class Attribute {
    private static final PStringMap<VertexAttribute> list = new PStringMap<>();

    public static VertexAttribute genGenericAttribute(String alias, int numComponents) {
      return new VertexAttribute(0 /* unused*/, numComponents, alias);
    }

    public static VertexAttribute genGenericColorPackedAttribute(String alias) {
      return new VertexAttribute(VertexAttributes.Usage.ColorPacked /* unused*/, 4, alias);
    }

    private static void init() {
      registerAttribute(Keys.pos, Keys.pos, 3, VertexAttributes.Usage.Position);
      registerAttribute(Keys.pos2d, Keys.pos, 2, VertexAttributes.Usage.Position);
      registerAttribute(Keys.nor, Keys.nor, 3, VertexAttributes.Usage.Normal);
      for (int a = 0; a < Keys.uv.length; a++) {
        Keys.uv[a] = "a_uv" + a;
        registerAttribute(Keys.uv[a], Keys.uv[a], 2, VertexAttributes.Usage.TextureCoordinates);
      }
      for (int a = 0; a < Keys.col.length; a++) {
        Keys.col[a] = "a_col" + a;
        Keys.colPacked[a] = "a_colPacked" + a;
        registerAttribute(Keys.col[a], Keys.col[a], 4, VertexAttributes.Usage.ColorUnpacked);
        registerAttribute(Keys.colPacked[a], Keys.col[a], 4, VertexAttributes.Usage.ColorPacked);
      }
      for (int a = 0; a < Keys.bon.length; a++) {
        Keys.bon[a] = "a_bon" + a;
        registerAttribute(Keys.bon[a], Keys.bon[a], 2, VertexAttributes.Usage.BoneWeight);
      }
      POS_NOR_UV0_UV1 = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor),
                                Attribute.get(Attribute.Keys.uv[0]), Attribute.get(Attribute.Keys.uv[1])});
      POS = new PVertexAttributes(new VertexAttribute[]{Attribute.get(Attribute.Keys.pos)});
      POS_UV0_COL0 = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Keys.uv[0]),
                                Attribute.get(Attribute.Keys.col[0])});
      POS_NOR_UV0_COL0 = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor),
                                Attribute.get(Attribute.Keys.uv[0]), Attribute.get(Keys.col[0])});
      POS_NOR_UV0 = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor),
                                Attribute.get(Attribute.Keys.uv[0])});
      POS2D_UV0_COLPACKED0 = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos2d), Attribute.get(Attribute.Keys.uv[0]),
                                Attribute.get(Attribute.Keys.colPacked[0])});
    }

    private static void registerAttribute(String id, String alias, int numComponents, int usage) {
      list.put(id, new VertexAttribute(usage /* unused */, numComponents,
                                       usage == VertexAttributes.Usage.ColorPacked ? GL20.GL_UNSIGNED_BYTE :
                                       GL20.GL_FLOAT, usage == VertexAttributes.Usage.ColorPacked, alias));
    }

    public static VertexAttribute get(String key) {
      VertexAttribute va = list.get(key);
      // int usage, int numComponents, int type, boolean normalized, String alias, int unit
      return new VertexAttribute(va.usage, va.numComponents, va.type, va.normalized, va.alias, va.unit);
    }

    public static final class Keys {
      public static final String bon[] = new String[8];
      public static final String col[] = new String[8];
      public static final String colPacked[] = new String[8];
      public static final String nor = "a_nor";
      public static final String pos = "a_pos";
      public static final String pos2d = "a_pos2d";
      public static final String uv[] = new String[8];
    }
  }
}
