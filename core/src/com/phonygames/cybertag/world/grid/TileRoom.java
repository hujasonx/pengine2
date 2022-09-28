package com.phonygames.cybertag.world.grid;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.grid.gen.TileRoomParameters;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.color.PVColIndexBuffer;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PBlockingTaskTracker;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A room in a building that uses a tile grid system. */
@Builder public class TileRoom implements PRenderContext.DataBufferEmitter {
  /** The number of shared base vcols for a tileroom before tile specific vCols kick in. */
  public static final int NUM_BASE_VCOLS = 16;
  /** The owner building. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileBuilding building;
  /** The settings about the doors that this room has. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final Doors doors = new Doors();
  /** The emit options for this tileRoom. Only useful when still building the room. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final EmitOptions emitOptions = new EmitOptions(this);
  /** The parameters that the tileRoom was generated with. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileRoomParameters parameters;
  private final float randTemp = MathUtils.random();
  /** The tiles in this room. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileGrid tileGrid = new TileGrid();
  /** The room's type. */
  private final String type;
  /** The vcol index buffer. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
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

  /** Unblocks the task tracker if it is tracking this room being generated. */
  public void unblockTaskTracker() {
    PAssert.isNotNull(genTaskTracker);
    genTaskTracker.removeBlocker(this);
    genTaskTracker = null;
  }

  /** Helper class that stores useful options for generating this room, but should not be used much afterwards. */
  public static class EmitOptions {
    /** The owner room. */
    private final TileRoom owner;

    public EmitOptions(TileRoom owner) {
      this.owner = owner;
    }
  }

  /** Helper class to store information about the doors this room has. */
  public class Doors {
    /** A map of rooms -> number of doors that DIRECTLY connect from this room to the room. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PMap<TileRoom, PList<TileDoor>> doors = new PMap<TileRoom, PList<TileDoor>>() {
      @Override public PList<TileDoor> newUnpooled(TileRoom room) {
        return new PList<>();
      }
    };
//    /** A list of all the directly connected rooms; should be the keys of doors. */
//    @Getter(value = AccessLevel.PUBLIC)
//    @Accessors(fluent = true)
//    private final PList<TileRoom> directlyConnectedRooms = new PList<>();
    /** A map storing how many doors are between this room and the key room. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PMap<TileRoom, PInt> doorsBetween = new PMap<>(PInt.getStaticPool());
  }
}
