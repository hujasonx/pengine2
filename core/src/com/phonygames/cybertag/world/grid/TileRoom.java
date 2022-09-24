package com.phonygames.cybertag.world.grid;

import com.phonygames.cybertag.world.grid.gen.TileRoomParameters;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.color.PVColIndexBuffer;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.PBlockingTaskTracker;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A room in a building that uses a tile grid system. */
@Builder public class TileRoom implements PRenderContext.DataBufferEmitter {
  /** The owner building. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileBuilding building;
  /** The parameters that the tileRoom was generated with. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileRoomParameters parameters;
  /** The tiles in this room. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileGrid tileGrid = new TileGrid();
  /** The room's type. */
  private final String type;
  /** The vcol index buffer. */
  private final PVColIndexBuffer vColors = new PVColIndexBuffer();
  /** The tracker that tracks this room being generated. Should be null when the room is finished. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PBlockingTaskTracker genTaskTracker;
  /** The model instance. */
  private PModelInstance modelInstance;

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
    vColors.emitColorData(renderContext);
  }

  public void frameUpdate() {
  }

  // TODO: This is temporary, figure out whre to put this.
  private void initColors() {
    vColors.registerName("skin", 0);
    vColors.setDiffuse("skin", 1, 1, 1);
  }

  public void logicUpdate() {
  }

  public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }

  /** Sets the model instance. */
  public void setModelInstance(PModelInstance modelInstance) {
    PAssert.isNull(this.modelInstance, "Can only set the model instance once!");
    this.modelInstance = modelInstance;
this.modelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
  }

  /** Unblocks the task tracker if it is tracking this room being generated. */
  public void unblockTaskTracker() {
    PAssert.isNotNull(genTaskTracker);
    genTaskTracker.removeBlocker(this);
    genTaskTracker = null;
  }
}
