package com.phonygames.cybertag.world;

import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.lighting.PLight;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class LasertagWorldRoom {
  private final LasertagWorldBuilding building;
  private final int index;
  private final LightFixture[] lightFixtures;

  public LasertagWorldRoom(LasertagWorldBuilding building, int index, LightFixture[] lightFixtures) {
    this.building = building;
    this.index = index;
    this.lightFixtures = lightFixtures;
  }

  public void frameUpdate() {
//    for (int a = 0; a < colorData.length; a += 2) {
//      // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
//      // the normal or the Index with this buffer.
//      colorData[a].setHSVA(index * .03f + .32f * building.index, 1, 1, 1); // diffuseM.
//      colorData[a + 1].set(0, 0, 0, .9f); // emissiveR.
//    }
    for (LightFixture lightFixture: lightFixtures) {
      lightFixture.frameUpdateColorData();
    }
  }

  protected void outputColorData(PFloat4Texture colData) {
//    for (int a = 0; a < colorData.length; a++) {
//      colData.addData(colorData[a]);
//    }
    for (LightFixture lightFixture: lightFixtures) {
      lightFixture.outputColorData(colData);
    }
  }

  // Keep in sync with the equivalent function in LasertagWorldGenRoom.
  public String partNamePrefix() {
    return new StringBuilder().append("building").append(building.index).append("_room").append(index).toString();
  }

  public static class LightFixture extends ColorDataEmitter {
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PLight light;

    private LightFixture(PLight light, int numVColIndices) {
      super(numVColIndices);
      this.light = light;
    }

    @Override public void frameUpdateColorData() {
      colorData[1].set(light.color()).w(1); // Set the emissive of the first color index to match the light color.
    }
  }
}
