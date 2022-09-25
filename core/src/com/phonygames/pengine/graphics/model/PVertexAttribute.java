package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PImplementsEquals;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A vertex attribute. */
public class PVertexAttribute implements PImplementsEquals<PVertexAttribute> {
  /** The backing libGDX vertex attribute. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final VertexAttribute backingAttr;
  /** The definition that informed the creation of this vertex attribute. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final Definition definition;
  /** The offset of this vertex attribute in its owner PVertexAttributes object. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int offsetInOwnerBytes;
  /** The offset of this vertex attribute in its owner PVertexAttributes object. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int offsetInOwnerFloats;

  public PVertexAttribute(Definition definition, int offsetInOwnerBytes) {
    this.definition = definition;
    // int usage, int numComponents, int type, boolean normalized, String alias, int unit
    this.offsetInOwnerBytes = offsetInOwnerBytes;
    this.offsetInOwnerFloats = offsetInOwnerBytes / 4;
    this.backingAttr =
        new VertexAttribute(definition.usage, definition.numComponents, definition.type.glInt(), definition.normalized,
                            definition.alias);
  }

  /** Static init definitions. */
  public static void init() {
    for (int a = 0; a < Definitions.col.length; a++) {
      Definitions.col[a] =
          Definition.builder().alias("a_col" + a).numComponents(4).usage(VertexAttributes.Usage.ColorUnpacked).unit(a)
                    .build();
    }
    for (int a = 0; a < Definitions.colPacked.length; a++) {
      Definitions.colPacked[a] =
          Definition.builder().alias("a_col" + a).numComponents(4).usage(VertexAttributes.Usage.ColorPacked).unit(a)
                    .build();
    }
    for (int a = 0; a < Definitions.uv.length; a++) {
      Definitions.uv[a] =
          Definition.builder().alias("a_uv" + a).numComponents(2).usage(VertexAttributes.Usage.TextureCoordinates)
                    .unit(a).build();
    }
    for (int a = 0; a < Definitions.bon.length; a++) {
      Definitions.bon[a] =
          Definition.builder().alias("a_bon" + a).numComponents(2).usage(VertexAttributes.Usage.BoneWeight).unit(a).build();
    }
  }

  @Override public boolean equalsT(PVertexAttribute other) {
    if (!definition.equals(other.definition)) {return false;}
    if (offsetInOwnerBytes != other.offsetInOwnerBytes) {return false;}
    return true;
  }

  /** Returns the size of this attribute in bytes. It should always be a multiple of 4. */
  public int sizeInBytes() {
    return definition.numComponents * definition.type.getSizeInBytes();
  }

  /** Returns the size of this attribute in floats. */
  public int sizeInFloats() {
    return definition.numComponents * definition.type.getSizeInBytes() / 4;
  }

  /** The supported underlying data types for each component of the vertex attribute. */
  public enum Type {
    FLOAT, FIXED, UNSIGNED_BYTE, BYTE, UNSIGNED_SHORT, SHORT;

    /** Returns the Type that corresponds with the given glInt. */
    public static Type fromGlInt(int glInt) {
      switch (glInt) {
        case GL20.GL_FLOAT:
          return FLOAT;
        case GL20.GL_FIXED:
          return FIXED;
        case GL20.GL_UNSIGNED_BYTE:
          return UNSIGNED_BYTE;
        case GL20.GL_BYTE:
          return BYTE;
        case GL20.GL_UNSIGNED_SHORT:
          return UNSIGNED_SHORT;
        case GL20.GL_SHORT:
          return SHORT;
        default:
          PAssert.fail("Should not reach!");
          return FLOAT;
      }
    }
    /** The size of this type in bytes. */
    /** @return How many bytes this attribute uses. */
    public int getSizeInBytes() {
      int type = glInt();
      switch (type) {
        case GL20.GL_FLOAT:
        case GL20.GL_FIXED:
          return 4;
        case GL20.GL_UNSIGNED_BYTE:
        case GL20.GL_BYTE:
          return 1;
        case GL20.GL_UNSIGNED_SHORT:
        case GL20.GL_SHORT:
          return 2;
      }
      return 0;
    }

    /** The GL int corresponding to this type. */
    public int glInt() {
      switch (this) {
        case FLOAT:
          return GL20.GL_FLOAT;
        case FIXED:
          return GL20.GL_FIXED;
        case UNSIGNED_BYTE:
          return GL20.GL_UNSIGNED_BYTE;
        case BYTE:
          return GL20.GL_BYTE;
        case UNSIGNED_SHORT:
          return GL20.GL_UNSIGNED_SHORT;
        case SHORT:
          return GL20.GL_SHORT;
        default:
          PAssert.fail("Should not reach!");
          return -1;
      }
    }
  }

  /** The definition of a vertex attribute. */
  @Builder @EqualsAndHashCode public static class Definition {
    /** The alias used in shaders. */
    public final String alias;
    /** Whether the attribute should be normalized. */
    @Builder.Default
    public final boolean normalized = false;
    /** The number of components. */
    public final int numComponents;
    /** The underlying type. */
    @Builder.Default
    public final Type type = Type.FLOAT;
    /** The unit. Defaults to 0. */
    @Builder.Default
    public final int unit = 0;
    /** The usage. */
    public final int usage;

    /** Generates a Definition for a an with the given libGDX attribute. */
    public static Definition fromAttribute(VertexAttribute attr) {
      return Definition.builder().numComponents(attr.numComponents).alias(attr.alias).usage(attr.usage).unit(attr.unit)
                       .type(Type.fromGlInt(attr.type)).build();
    }

    /** Generates a Definition for a an attribute with the alias. */
    public static Definition genGeneric(String alias, int numComponents) {
      return Definition.builder().numComponents(numComponents).alias(alias).build();
    }

    /** Generates a Definition for a colPacked attribute with the alias. */
    public static Definition genGenericColPacked(String alias) {
      return Definition.builder().numComponents(4).alias(alias).usage(VertexAttributes.Usage.ColorPacked).build();
    }

    /** Whether or not this defition is for the position attribute. */
    public boolean isPos() {
      return usage == VertexAttributes.Usage.Position;
    }

    /** Whether or not this defition is for the normal attribute. */
    public boolean isNor() {
      return usage == VertexAttributes.Usage.Normal;
    }
  }

  public static final class Definitions {
    public static final Definition bon[] = new Definition[8];
    public static final Definition col[] = new Definition[8];
    public static final Definition colPacked[] = new Definition[8];
    public static final Definition nor =
        Definition.builder().alias("a_nor").numComponents(3).usage(VertexAttributes.Usage.Normal).build();
    public static final Definition vColI =
        Definition.builder().alias("a_vColI").numComponents(1).build();
    public static final Definition pos =
        Definition.builder().alias("a_pos").numComponents(3).usage(VertexAttributes.Usage.Position).build();
    public static final Definition pos2d =
        Definition.builder().alias("a_pos").numComponents(2).usage(VertexAttributes.Usage.Position).build();
    public static final Definition uv[] = new Definition[8];
  }

  /** Converts a packed color to a PVec4. */
  public static PVec4 vec4FromUnsignedByteColor(PVec4 out, float byteColor) {
    out.fromFloatBits(byteColor);
    return out;
  }

  /** Converts a packed color to a PVec4. */
  public static PVec4 vec4FromUnsignedShortColor(PVec4 out, float float0, float float1) {
    out.fromUnsignedShortBits(float0, float1);
    return out;
  }


}
