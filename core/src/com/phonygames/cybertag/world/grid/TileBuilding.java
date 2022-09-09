package com.phonygames.cybertag.world.grid;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Contains one or more TileRooms. */
public class TileBuilding implements PRenderContext.DataBufferEmitter {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The rooms. */
  private final PList<TileRoom> rooms = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The tile bounds of the building. */
  private final PIntAABB tileBounds = new PIntAABB();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PIntAABB> excludedTileBounds = new PList<>();
  /** The model instance. */
  private PModelInstance modelInstance;

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
  }

  public void frameUpdate() {
  }

  public void logicUpdate() {
  }

  /** Returns whether or not the position is in the building. */
  public boolean tilePositionInBuilding(int x, int y, int z) {
    if (!tileBounds.contains(x, y,z)) return false;
    for (int a = 0; a < excludedTileBounds.size(); a++) {
      PIntAABB bounds = excludedTileBounds.get(a);
      if (bounds.contains(x, y, z)) {
        return false;
      }
    }
    return true;
  }

  public void render(PRenderContext renderContext) {
    for (int a = 0; a < rooms.size(); a++) {
      TileRoom room = rooms.get(a);
      room.render(renderContext);
    }
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
