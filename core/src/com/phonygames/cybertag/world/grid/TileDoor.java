package com.phonygames.cybertag.world.grid;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.color.PVColIndexBuffer;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PBlockingTaskTracker;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A door in the tile grid building. */
@Builder public class TileDoor implements PRenderContext.DataBufferEmitter {
  /** The building that this door is part of. */
  private final TileBuilding building;
  /** The room[s] that this door connects. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<TileRoom> rooms = new PList<>();
  /** The walls[s] that this door occupies. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<GridTile.EmitOptions.Wall> walls = new PList<>();
  /** The vcol index buffer. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVColIndexBuffer vColors = new PVColIndexBuffer();
  /** The tracker that tracks this door being generated. Should be null when the door is finished. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PBlockingTaskTracker genTaskTracker;
  /** The model instance. */
  private PModelInstance modelInstance;
  private final float randTemp = MathUtils.random();

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
    vColors.emitColorData(renderContext);
  }

  public void frameUpdate() {
    vColors.setDiffuse(0, PVec4.obtain().setHSVA(randTemp, 1, 1, 1), false);
    // Window.
    vColors.setDiffuse(1, PVec4.obtain().setHSVA(randTemp, 1, 1, .2f), true);
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

  /** Unblocks the task tracker if it is tracking this door being generated. */
  public void unblockTaskTracker() {
    PAssert.isNotNull(genTaskTracker);
    genTaskTracker.removeBlocker(this);
    genTaskTracker = null;
  }
}
