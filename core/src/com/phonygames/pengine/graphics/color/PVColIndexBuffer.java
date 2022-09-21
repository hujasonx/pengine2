package com.phonygames.pengine.graphics.color;

import android.support.annotation.NonNull;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PStringMap;

/**
 * Helper class to contain a list of colors to send via vcolindex. Ultimately, callers should not need to know the
 * underlying storage strategy.
 */
public class PVColIndexBuffer {
  /**
   * The number of vec4s required to store one color. Colors are stored as follows: [diffuse r, diffuse g, diffuse b,
   * alpha or outline index] [emissive r, emissive g, emissive b, specular strength]
   */
  private static final int VECS_PER_COL = 2;
  /** The color data array. */
  private final PList<PVec4> colorData = new PList<>(PVec4.getStaticPool());
  /** A map of vColIndices to their corresponding name. */
  private final PMap<Integer, String> indexToNameMap = new PMap<>();
  /** A map of names to their corresponding vColIndex. */
  private final PStringMap<Integer> nameToIndexMap = new PStringMap<>();

  /** Clears the array. */
  public PVColIndexBuffer clear() {
    colorData.clearAndFreePooled();
    return this;
  }

  /** Clears the array and index / name maps. */
  public PVColIndexBuffer clearAll() {
    colorData.clearAndFreePooled();
    nameToIndexMap.clear();
    indexToNameMap.clear();
    return this;
  }

  /** Returns how many colors are stored in this buffer. */
  public int count() {
    return colorData.size() / VECS_PER_COL;
  }

  /** Emits the color data to the vColIndex floatbuffer. */
  public PVColIndexBuffer emitColorData(PRenderContext renderContext) {
    PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
    emitColorData(vColIndexBuffer);
    return this;
  }

  /** Outputs the color data to the float4texture, to pass data to the gpu. */
  public PVColIndexBuffer emitColorData(PFloat4Texture colData) {
    for (int a = 0; a < colorData.size(); a++) {
      colData.addData(colorData.get(a));
    }
    return this;
  }

  /** Returns the diffuse color at the location. Alpha will always be set to 1. */
  public PVec4 getDiffuse(@NonNull PVec4 out, String name) {
    return getDiffuse(out, nameToIndexMap.get(name));
  }

  /** Returns the diffuse color at the location. Alpha will always be set to 1. */
  public PVec4 getDiffuse(@NonNull PVec4 out, int index) {
    out.set(colorData.get(index * VECS_PER_COL));
    out.a(1);
    return out;
  }

  /** Returns the emissive color at the location. Alpha will always be set to 1. */
  public PVec4 getEmissive(@NonNull PVec4 out, int index) {
    out.set(colorData.get(index * VECS_PER_COL + 1));
    out.a(1);
    return out;
  }

  /**
   * Outputs the color data to another color buffer.
   *
   * @param other
   * @param offsetInOther
   * @param offsetInSelf
   * @param count
   */
  public PVColIndexBuffer outputColorData(PVColIndexBuffer other, int offsetInOther, int offsetInSelf, int count) {
    for (int a = VECS_PER_COL * offsetInSelf; a < Math.min(colorData.size(), VECS_PER_COL * (offsetInSelf + count));
         a++) {
      other.colorData.set(VECS_PER_COL * offsetInOther + a, colorData.get(a));
    }
    return this;
  }

  /** Registers a name with a vColIndex. */
  public PVColIndexBuffer registerName(String name, int index) {
    nameToIndexMap.put(name, index);
    indexToNameMap.put(index, name);
    return this;
  }

  /** Sets the diffuse color for the given name. */
  public PVColIndexBuffer setDiffuse(String name, PVec4 color, boolean emitAlpha) {
    return setDiffuse(nameToIndexMap.get(name), color, emitAlpha);
  }

  /** Sets the diffuse color at the location. */
  public PVColIndexBuffer setDiffuse(int index, PVec4 color, boolean emitAlpha) {
    colorData.fillToCapacityWithPooledValues((index + 1) * VECS_PER_COL);
    PVec4 v = colorData.get(index * VECS_PER_COL + 0);
    if (emitAlpha) {
      v.set(color);
    } else {
      v.x(color.x());
      v.y(color.y());
      v.z(color.z());
    }
    return this;
  }

  /** Sets the diffuse color at the location. */
  public PVColIndexBuffer setDiffuse(int index, float r, float g, float b) {
    colorData.fillToCapacityWithPooledValues((index + 1) * VECS_PER_COL);
    PVec4 v = colorData.get(index * VECS_PER_COL + 0);
    v.x(r);
    v.y(g);
    v.z(b);
    return this;
  }

  /** Sets the diffuse color for the given name. */
  public PVColIndexBuffer setDiffuse(String name, float r, float g, float b) {
    colorData.fillToCapacityWithPooledValues((nameToIndexMap.get(name) + 1) * VECS_PER_COL);
    PVec4 v = colorData.get(nameToIndexMap.get(name) * VECS_PER_COL + 0);
    v.x(r);
    v.y(g);
    v.z(b);
    return this;
  }

  /** Sets the diffuse color at the location. */
  public PVColIndexBuffer setDiffuseAlpha(int index, float r, float g, float b, float alpha) {
    colorData.fillToCapacityWithPooledValues((index + 1) * VECS_PER_COL);
    colorData.get(index * VECS_PER_COL + 0).set(r, g, b, alpha);
    return this;
  }

  /** Sets the emissive color at the location. */
  public PVColIndexBuffer setEmissive(int index, PVec4 color, boolean emitAlpha) {
    colorData.fillToCapacityWithPooledValues((index + 1) * VECS_PER_COL);
    PVec4 v = colorData.get(index * VECS_PER_COL + 1);
    if (emitAlpha) {
      v.set(color);
    } else {
      v.x(color.x());
      v.y(color.y());
      v.z(color.z());
    }
    return this;
  }
}
