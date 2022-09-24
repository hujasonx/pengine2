package com.phonygames.cybertag.world.grid;

import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBlockingTaskTracker;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Contains one or more TileRooms. */
public class TileBuilding implements PRenderContext.DataBufferEmitter {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PIntAABB> excludedTileBounds = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The rooms. */ private final PList<TileRoom> rooms = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The tile bounds of the building. */ private final PIntAABB tileBounds = new PIntAABB();
  /** The tiles in this building. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileGrid tileGrid = new TileGrid();
  /** The world that contains this tile buildling. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final World world;
  /** The tracker that tracks this building being generated. Should be null when the building is finished. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PBlockingTaskTracker genTaskTracker;
  /** The model instance. */
  private PModelInstance modelInstance;

  public TileBuilding(PBlockingTaskTracker genTaskTracker, World world) {
    this.genTaskTracker = genTaskTracker;
    genTaskTracker.addBlocker(this);
    this.world = world;
  }

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
  }

  public void frameUpdate() {
  }

  public void logicUpdate() {
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

  /** Returns the tile room in this building at the given tile position. */
  public TileRoom roomAtTilePosition(int x, int y, int z) {
    for (int a = 0; a < rooms.size(); a++) {
      TileRoom room = rooms.get(a);
      if (room.tileGrid().hasTileAt(x, y, z)) {
        return room;
      }
    }
    return null;
  }

  /** Returns the tile in this building at the given tile position. */
  public GridTile tileAtTilePosition(int x, int y, int z) {
    return tileGrid.getTileAt(x, y, z);
  }

  /** Returns whether or not the position is in the building. */
  public boolean tilePositionInBuilding(int x, int y, int z) {
    if (!tileBounds.contains(x, y, z)) {return false;}
    for (int a = 0; a < excludedTileBounds.size(); a++) {
      PIntAABB bounds = excludedTileBounds.get(a);
      if (bounds.contains(x, y, z)) {
        return false;
      }
    }
    return true;
  }

  /** Unblocks the task tracker if it is tracking this building being generated. */
  public void unblockTaskTracker() {
    PAssert.isNotNull(genTaskTracker);
    genTaskTracker.removeBlocker(this);
    genTaskTracker = null;
  }
}
