package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

public class LasertagTileGen extends PBuilder {
  public final int x, y, z;
  protected final LasertagTile tile;

  public LasertagTileGen(String id, int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
    tile = new LasertagTile(id, x, y, z);
  }

  public LasertagTile build() {
    lockBuilder();
    buildModelInstance();
    return tile;
  }

  private void buildModelInstance() {
  }
}
