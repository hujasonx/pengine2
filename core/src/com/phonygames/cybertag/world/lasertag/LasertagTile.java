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
  public final String id;
  public final int x, y, z;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected @Nullable
  LasertagBuilding building;
  protected float ceilTile00OffsetY = 1;
  protected float ceilTile01OffsetY = 1;
  protected float ceilTile10OffsetY = 1;
  protected float ceilTile11OffsetY = 1;
  protected float floorTile00OffsetY = 0;
  protected float floorTile01OffsetY = 0;
  protected float floorTile10OffsetY = 0;
  protected float floorTile11OffsetY = 0;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected @Nullable
  LasertagRoom room;
  /** The vCol index data (per-tile) for this tile. They should succeed the shared-data block in the buffer. */
  protected int tileVColIndexStart = 0;
  protected static final int PER_TILE_VCOL_INDICES = 4;

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

  public void getCorners(PVec3 v000, PVec3 v001, PVec3 v010, PVec3 v011, PVec3 v100, PVec3 v101, PVec3 v110,
                         PVec3 v111) {
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

  public void getCornersFloorCeiling(PVec3 v000, PVec3 v001, PVec3 v010, PVec3 v011, PVec3 v100, PVec3 v101, PVec3 v110,
                                     PVec3 v111) {
    if (room == null || room.building == null) {
      PAssert.failNotImplemented("tiles that are not part of a room or building");
      return;
    }
    room.building.worldPosForTile(v000, x + 0, y + floorTile00OffsetY, z + 0);
    room.building.worldPosForTile(v001, x + 0, y + floorTile01OffsetY, z + 1);
    room.building.worldPosForTile(v010, x + 0, y + ceilTile00OffsetY, z + 0);
    room.building.worldPosForTile(v011, x + 0, y + ceilTile01OffsetY, z + 1);
    room.building.worldPosForTile(v100, x + 1, y + floorTile10OffsetY, z + 0);
    room.building.worldPosForTile(v101, x + 1, y + floorTile11OffsetY, z + 1);
    room.building.worldPosForTile(v110, x + 1, y + ceilTile10OffsetY, z + 0);
    room.building.worldPosForTile(v111, x + 1, y + ceilTile11OffsetY, z + 1);
  }

  public void logicUpdate() {
  }

  public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
