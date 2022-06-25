package com.phonygames.cybertag.world;

import com.phonygames.cybertag.world.gen.LasertagWorldGen;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec4;

public class LasertagWorldRoom {
  private final LasertagWorldBuilding building;
  private final int index;

  private final int vColIndexLength;
  private final PVec4[] colorData;

  public LasertagWorldRoom(LasertagWorldBuilding building, int index, int vColIndexLength) {
    this.building = building;
    this.index = index;
    this.vColIndexLength = vColIndexLength;
    this.colorData = new PVec4[vColIndexLength * 2]; // Two components per color.
    for (int a = 0; a < colorData.length; a++) {
      this.colorData[a] = PVec4.obtain();
    }
  }

  // Keep in sync with the equivalent function in LasertagWorldGenRoom.
  public String partNamePrefix() {
    return new StringBuilder().append("building").append(building.index).append("_room").append(index).toString();
  }
  protected void outputColorData(PFloat4Texture colData) {
    for (int a = 0; a < colorData.length; a++ ) {
      colData.addData(colorData[a]);
    }
  }

  public void frameUpdate() {
    for (int a = 0; a < colorData.length; a+= 2) {
      // Note, we use emissiveR, but the shader will output emissiveM and normalR. But we don't want to edit
      // the normal or the Index with this buffer.
      colorData[a].setHSVA(index * .03f + .32f * building.index, 1, 1, 1); // diffuseM.
      colorData[a+1].set(0, 0, 0, .9f); // emissiveR.
    }
  }
}
