package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.val;

public class PVertexAttributes {
  final VertexAttributes vertexAttributes;

  private final Map<String, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  @Getter
  private int numFloatsPerVertex;
  @Getter
  private String prefix;

  public static final class Attribute {
    private static int maxUsage = 1;

    public static final class Keys {
      public static final String pos = "a_pos";
      public static final String nor = "a_nor";
      public static final String uv[] = new String[4];
      public static final String col[] = new String[4];
      public static final String colPacked[] = new String[4];
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
        Keys.uv[a] = "a_uv" + a;
        registerAttribute(Keys.uv[a], 2);
      }

      for (int a = 0; a < Keys.col.length; a++) {
        Keys.col[a] = "a_col" + a;
        Keys.colPacked[a] = "a_col" + a;
        registerAttribute(Keys.col[a], 4);
//        registerAttribute(Keys.colPacked[a], 4); // TODO: handle packed.
      }

      DEFAULT = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor), Attribute.get(Attribute.Keys.uv[0]), Attribute.get(Attribute.Keys.uv[1])});
      POSITION = new PVertexAttributes(
          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos)});
      GLTF_UNSKINNED =
          new PVertexAttributes(new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor), Attribute.get(Attribute.Keys.uv[0]), Attribute.get(
              Keys.col[0])});
    }
  }

  public static void init() {
    Attribute.init();
  }

  @Getter
  private static PVertexAttributes DEFAULT;
  @Getter
  private static PVertexAttributes GLTF_UNSKINNED;
  @Getter
  private static PVertexAttributes PHYSICS =
      new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position()});
  @Getter
  private static PVertexAttributes POSITION;

  @Getter
  private int bytesPerVertex;

  public PVertexAttributes(Iterable<VertexAttribute> vertexAttributes) {
    PList<VertexAttribute> vaList = new PList<>();
    for (VertexAttribute va : vertexAttributes) {
      vaList.add(va);
    }

    VertexAttribute[] vaArray = new VertexAttribute[vaList.size()];
    for (int a = 0; a < vaArray.length; a++) {
      vaArray[a] = vaList.get(a);
    }
    this.vertexAttributes = genVertexAttributes(vaArray);
  }

  public PVertexAttributes(VertexAttribute[] vertexAttributes) {
    this.vertexAttributes = genVertexAttributes(vertexAttributes);
  }

  private VertexAttributes genVertexAttributes(VertexAttribute[] vertexAttributes) {
    int floatsPerVertex = 0;
    StringBuilder prefix = new StringBuilder();
    for (int a = 0; a < vertexAttributes.length; a++) {
      val va = vertexAttributes[a];
      vertexAttributeFloatIndexInVertex.put(va.alias, floatsPerVertex);
      prefix.append("#define ").append(va.alias).append("Flag\n");

      floatsPerVertex += va.numComponents;
    }
    this.prefix = prefix.toString();
    this.numFloatsPerVertex = floatsPerVertex;
//    this.bytesPerVertex = calculateOffsets();

    return new VertexAttributes(vertexAttributes);
  }

  @Override
  public int hashCode() {
    return prefix.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PVertexAttributes) {
      val other = (PVertexAttributes) o;
      return prefix.equals(other.prefix);
    }

    return false;
  }

  public int indexForVertexAttribute(String alias) {
    PAssert.isTrue(vertexAttributeFloatIndexInVertex.containsKey(alias), alias + " not vertexAttributeFloatIndexInVertex");
    return vertexAttributeFloatIndexInVertex.get(alias);
  }

  public int indexForVertexAttribute(VertexAttribute vertexAttribute) {
    return indexForVertexAttribute(vertexAttribute.alias);
  }

//  private int calculateOffsets() {
//    int count = 0;
//    for (val attr : vertexAttributes) {
//      attr.offset = count;
//      count += attr.getSizeInBytes();
//    }
//
//    return count;
//  }

}
