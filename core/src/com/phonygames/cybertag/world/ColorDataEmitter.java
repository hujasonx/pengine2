package com.phonygames.cybertag.world;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class ColorDataEmitter {
  public final PVec4[] colorData;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int numVColIndices;

  public ColorDataEmitter(int numVColIndices) {
    this.numVColIndices = numVColIndices;
    this.colorData = new PVec4[numVColIndices * 2]; // Two components per color.
    for (int a = 0; a < colorData.length; a++) {
      this.colorData[a] = PVec4.obtain();
    }
  }

  public void outputColorData(PFloat4Texture colData) {
    for (int a = 0; a < colorData.length; a++) {
      colData.addData(colorData[a]);
    }
  }
}
