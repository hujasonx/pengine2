package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.collection.PIntMap3d;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class LasertagBuilding implements PRenderContext.DataBufferEmitter {
  public final String id;
  protected final LasertagWorld world;
  @Getter(value = AccessLevel.PROTECTED, lazy = true)
  @Accessors(fluent = true)
  private final PVec4 tileRotation = PVec4.obtain().setIdentityQuaternion();
  @Getter(value = AccessLevel.PROTECTED, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 tileScale = PVec3.obtain().set(1, 1, 1), tileTranslation = PVec3.obtain();
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final PIntMap3d<LasertagTile> tiles = new PIntMap3d<>();
  @Getter(value = AccessLevel.PROTECTED, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransform = PMat4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PIntAABB aabb = new PIntAABB();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PIntAABB[] outsideAabbs;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected LasertagRoom[] rooms;

  protected LasertagBuilding(String id, LasertagWorld world) {
    this.id = id;
    this.world = world;
  }

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
  }

  public void frameUpdate() {
    for (val room : rooms) {
      room.frameUpdate();
    }
  }

  public PVec3 localXAxis(PVec3 out) {
    PMat4 temp = PMat4.obtain();
    out.set(1, 0, 0).mul(temp.set(tileTranslation(), tileRotation(), tileScale()), 0);
    temp.free();
    return out;
  }

  public PVec3 localYAxis(PVec3 out) {
    PMat4 temp = PMat4.obtain();
    out.set(0, 1, 0).mul(temp.set(tileTranslation(), tileRotation(), tileScale()), 0);
    temp.free();
    return out;
  }

  public PVec3 localZAxis(PVec3 out) {
    PMat4 temp = PMat4.obtain();
    out.set(0, 0, 1).mul(temp.set(tileTranslation(), tileRotation(), tileScale()), 0);
    temp.free();
    return out;
  }

  public void logicUpdate() {
    for (val room : rooms) {
      room.logicUpdate();
    }
  }

  public void render(PRenderContext renderContext) {
    for (val room : rooms) {
      room.render(renderContext);
    }
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }

  public LasertagTile tileForWorldPos(PVec3 pos) {
    PMat4 temp = PMat4.obtain();
    PVec3 tempv = PVec3.obtain();
    tempv.set(pos).mul(temp.set(tileTranslation(), tileRotation(), tileScale()).inv(), 1);
    LasertagTile ret = tiles().get((int)Math.floor(tempv.x()), (int)Math.floor(tempv.y()), (int)Math.floor(tempv.z()));
    temp.free();
    tempv.free();
    return ret;
  }

  public PVec3 worldPosForTile(PVec3 out, float x, float y, float z) {
    PMat4 temp = PMat4.obtain();
    out.set(x, y, z).mul(temp.set(tileTranslation(), tileRotation(), tileScale()), 1);
    temp.free();
    return out;
  }
}
