package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PVec3;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class LasertagTile implements PRenderContext.DataBufferEmitter {
  public final int x, y, z;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected @Nullable
  LasertagBuilding building;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected @Nullable
  LasertagRoom room;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  public final String id;

  protected LasertagTile(String id, final int x, final int y, final int z) {
    this.id = id;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
  }

  public void frameUpdate() {
  }

  public void logicUpdate() {
  }

  public void getCorners(PVec3 v000, PVec3 v001, PVec3 v010, PVec3 v011,PVec3 v100, PVec3 v101, PVec3 v110, PVec3 v111) {
    if (room == null || room.building == null) {
      PAssert.failNotImplemented("tiles that are not part of a room or building");
      return;
    }
    room.building.worldPosForTile(v000, x + 0, y + 0, z + 0);
    room.building.worldPosForTile(v001, x + 0, y + 0, z + 1);
    room.building.worldPosForTile(v010, x + 0, y + 1, z + 0);
    room.building.worldPosForTile(v011, x + 0, y + 1, z + 1);
    room.building.worldPosForTile(v100, x + 1, y + 0, z + 0);
    room.building.worldPosForTile(v101, x + 1, y + 0, z + 1);
    room.building.worldPosForTile(v110, x + 1, y + 1, z + 0);
    room.building.worldPosForTile(v111, x + 1, y + 1, z + 1);
  }

  public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
