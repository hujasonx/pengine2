package com.phonygames.pengine.graphics.model;

import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PImplementsEquals;
import com.phonygames.pengine.util.PString;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A list of vertex attributes. */
public class PVertexAttributes implements PImplementsEquals<PVertexAttributes> {
  /** The vertex attributes. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PStringMap<PVertexAttribute> attributes = new PStringMap<>();
  /** The vertex attributes. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PVertexAttribute> attributesList = new PList<>();
  //  @Getter
  //  private static PVertexAttributes PHYSICS = new PVertexAttributes(new VertexAttribute[]{VertexAttribute.Position
  //  ()});
  //  @Getter
  //  private static PVertexAttributes POS, POS_NOR_UV0_UV1, POS_UV0_COL0, POS_NOR_UV0, POS2D_UV0_COLPACKED0;
  //  @Getter
  //  /** Pos, nor, uv, col */ private static PVertexAttributes POS_NOR_UV0_COL0;
  //  @Getter
  //  private final VertexAttributes backingVertexAttributes;
  //  //  private final Map<String, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  //  private final Map<String, Integer> vertexAttributeFloatIndexInVertex = new HashMap<>();
  //  @Getter
  //  private int bytesPerVertex;
  //  @Getter
  //  private int numFloatsPerVertex;
  /** The backing libGDX vertex attributes. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final VertexAttributes backingVertexAttributes;
  /** The prefix that should be applied to shader code. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final String prefix;
  /** The size of a vertex, in bytes. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int sizeInBytes;
  /** The size of a vertex, in floats. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int sizeInFloats;

  /** Returns the number of vertex attributes. */
  public int count() {
    return attributesList.size();
  }

  public PVertexAttributes(PVertexAttribute.Definition[] definitions) {
    PList<VertexAttribute> vaList = new PList<>();
    int sizeInBytes = 0;
    PString prefix = PString.obtain();
    for (PVertexAttribute.Definition definition : definitions) {
      PVertexAttribute pva = new PVertexAttribute(definition, /* offsetInOwnerBytes */ sizeInBytes);
      attributes.put(definition.alias, pva);
      attributesList.add(pva);
      vaList.add(pva.backingAttr());
      sizeInBytes += pva.sizeInBytes();
      prefix.append("#define ").append(pva.definition().alias).append("Flag\n");
    }
    VertexAttribute[] vaArray = new VertexAttribute[vaList.size()];
    for (int a = 0; a < vaArray.length; a++) {
      vaArray[a] = vaList.get(a);
    }
    this.backingVertexAttributes = new VertexAttributes(vaArray);
    this.sizeInBytes = sizeInBytes;
    this.sizeInFloats = sizeInBytes / 4;
    this.prefix = prefix.toString();
    prefix.free();
  }

  /** Initializes the PVertexAttribute class and creates templates. */
  public static void init() {
    PVertexAttribute.init();
    Templates.POS = new PVertexAttributes(new PVertexAttribute.Definition[]{PVertexAttribute.Definitions.pos});
    Templates.PHYSICS = new PVertexAttributes(new PVertexAttribute.Definition[]{PVertexAttribute.Definitions.pos});
    Templates.POS_UV0_COL0 = new PVertexAttributes(
        new PVertexAttribute.Definition[]{PVertexAttribute.Definitions.pos, PVertexAttribute.Definitions.uv[0],
                                          PVertexAttribute.Definitions.col[0]});
    Templates.POS_NOR_UV0_COL0 = new PVertexAttributes(
        new PVertexAttribute.Definition[]{PVertexAttribute.Definitions.pos, PVertexAttribute.Definitions.nor,
                                          PVertexAttribute.Definitions.uv[0], PVertexAttribute.Definitions.col[0]});
    Templates.POS2D_UV0_COLPACKED0 = new PVertexAttributes(
        new PVertexAttribute.Definition[]{PVertexAttribute.Definitions.pos2d, PVertexAttribute.Definitions.uv[0],
                                          PVertexAttribute.Definitions.colPacked[0]});
    //    Attribute.init();
  }

  @Override public boolean equalsT(PVertexAttributes other) {
    return backingVertexAttributes.equals(other.backingVertexAttributes);
  }
  //  public static PVec3 transformVecWithMatrix(@NonNull VertexAttribute vertexAttribute, @NonNull PVec3 inout,
  //                                             @NonNull PMat4 mat4) {
  //    if (vertexAttribute.alias.equals(Attribute.Keys.pos)) {
  //      return inout.mul(mat4, 1);
  //    }
  //    if (vertexAttribute.alias.equals(Attribute.Keys.nor)) {
  //      return inout.mul(mat4, 0);
  //    }
  //    // TODO: binormal, tangent
  //    return inout;
  //  }

  /** Returns the index in a float buffer for each vertex where the vertex attribute begins. */
  public int floatIndexForVertexAttribute(VertexAttribute vertexAttribute) {
    return floatIndexForVertexAttribute(vertexAttribute.alias);
  }

  /** Returns the index in a float buffer for each vertex where the vertex attribute begins. */
  public int floatIndexForVertexAttribute(String alias) {
    PVertexAttribute pva = pva(alias);
    PAssert.isNotNull(pva, alias + " not found in PVertexAttributes");
    return pva.offsetInOwnerBytes() / 4;
  }

  /** Returns the PVertexAttribute with the given alias. */
  public @Nullable PVertexAttribute pva(String alias) {
    return attributes.get(alias);
  }

  /** Returns the PVertexAttribute at the given index. */
  public @Nullable PVertexAttribute pva(int index) {
    return attributesList.get(index);
  }

  /** Returns true if this attributes has an attribute with the given alias. */
  public boolean has(String alias) {
    return attributes.has(alias);
  }

  @Override public int hashCode() {
    return backingVertexAttributes.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (o instanceof PVertexAttributes) {
      PVertexAttributes other = (PVertexAttributes) o;
      return backingVertexAttributes.equals(other.backingVertexAttributes);
    }
    return false;
  }
  //  public static final class Attribute {
  //    private static final PStringMap<VertexAttribute> list = new PStringMap<>();
  //
  //    public static VertexAttribute genGenericAttribute(String alias, int numComponents) {
  //      return new VertexAttribute(0 /* unused*/, numComponents, alias);
  //    }
  //
  //    public static VertexAttribute genGenericColorPackedAttribute(String alias) {
  //      return new VertexAttribute(VertexAttributes.Usage.ColorPacked /* unused*/, 4, alias);
  //    }
  //
  //    private static void init() {
  //      registerAttribute(Keys.pos, Keys.pos, 3, VertexAttributes.Usage.Position);
  //      registerAttribute(Keys.pos2d, Keys.pos, 2, VertexAttributes.Usage.Position);
  //      registerAttribute(Keys.nor, Keys.nor, 3, VertexAttributes.Usage.Normal);
  //      for (int a = 0; a < Keys.uv.length; a++) {
  //        Keys.uv[a] = "a_uv" + a;
  //        registerAttribute(Keys.uv[a], Keys.uv[a], 2, VertexAttributes.Usage.TextureCoordinates);
  //      }
  //      for (int a = 0; a < Keys.col.length; a++) {
  //        Keys.col[a] = "a_col" + a;
  //        Keys.colPacked[a] = "a_colPacked" + a;
  //        registerAttribute(Keys.col[a], Keys.col[a], 4, VertexAttributes.Usage.ColorUnpacked);
  //        registerAttribute(Keys.colPacked[a], Keys.col[a], 4, VertexAttributes.Usage.ColorPacked);
  //      }
  //      for (int a = 0; a < Keys.bon.length; a++) {
  //        Keys.bon[a] = "a_bon" + a;
  //        registerAttribute(Keys.bon[a], Keys.bon[a], 2, VertexAttributes.Usage.BoneWeight);
  //      }
  //      POS_NOR_UV0_UV1 = new PVertexAttributes(
  //          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor),
  //                                Attribute.get(Attribute.Keys.uv[0]), Attribute.get(Attribute.Keys.uv[1])});
  //      POS = new PVertexAttributes(new VertexAttribute[]{Attribute.get(Attribute.Keys.pos)});
  //      POS_UV0_COL0 = new PVertexAttributes(
  //          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Keys.uv[0]),
  //                                Attribute.get(Attribute.Keys.col[0])});
  //      POS_NOR_UV0_COL0 = new PVertexAttributes(
  //          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor),
  //                                Attribute.get(Attribute.Keys.uv[0]), Attribute.get(Keys.col[0])});
  //      POS_NOR_UV0 = new PVertexAttributes(
  //          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos), Attribute.get(Attribute.Keys.nor),
  //                                Attribute.get(Attribute.Keys.uv[0])});
  //      POS2D_UV0_COLPACKED0 = new PVertexAttributes(
  //          new VertexAttribute[]{Attribute.get(Attribute.Keys.pos2d), Attribute.get(Attribute.Keys.uv[0]),
  //                                Attribute.get(Attribute.Keys.colPacked[0])});
  //    }
  //
  //    private static void registerAttribute(String id, String alias, int numComponents, int usage) {
  //      list.put(id, new VertexAttribute(usage /* unused */, numComponents,
  //                                       usage == VertexAttributes.Usage.ColorPacked ? GL20.GL_UNSIGNED_BYTE :
  //                                       GL20.GL_FLOAT, usage == VertexAttributes.Usage.ColorPacked, alias));
  //    }
  //
  //    public static VertexAttribute get(String key) {
  //      VertexAttribute va = list.get(key);
  //      // int usage, int numComponents, int type, boolean normalized, String alias, int unit
  //      return new VertexAttribute(va.usage, va.numComponents, va.type, va.normalized, va.alias, va.unit);
  //    }
  //
  //    public static final class Keys {
  //      public static final String bon[] = new String[8];
  //      public static final String col[] = new String[8];
  //      public static final String colPacked[] = new String[8];
  //      public static final String nor = "a_nor";
  //      public static final String pos = "a_pos";
  //      public static final String pos2d = "a_pos2d";
  //      public static final String uv[] = new String[8];
  //    }
  //  }

  public static class Templates {
    /** PVertexAttributes for static phyisics parts. */
    public static PVertexAttributes PHYSICS;
    /** PVertexAttributes for with just position. */
    public static PVertexAttributes POS;
    /** PVertexAttributes for spritebatch. */
    public static PVertexAttributes POS2D_UV0_COLPACKED0;
    /** PVertexAttributes with basic everything. */
    public static PVertexAttributes POS_NOR_UV0_COL0;
    /** PVertexAttributes without normal. */
    public static PVertexAttributes POS_UV0_COL0;
  }
}
