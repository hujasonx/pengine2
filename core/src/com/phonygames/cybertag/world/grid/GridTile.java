package com.phonygames.cybertag.world.grid;

import android.support.annotation.Nullable;

import com.phonygames.pengine.util.PFacing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Builder public class GridTile {
  /** The index used for the lower x, lower z corner in array fields. */
  public static final int CORNER00 = 0;
  /** The index used for the lower x, higher z corner in array fields. */
  public static final int CORNER01 = 3;
  /** The index used for the higher x, lower z corner in array fields. */
  public static final int CORNER10 = 1;
  /** The index used for the higher x, higher z corner in array fields. */
  public static final int CORNER11 = 2;
  /** The emit options for this tile. */
  public final EmitOptions emitOptions = new EmitOptions(this);
  /** The coordinates of the GridTile in grid space. */
  public final int x, y, z;
  /** The vColIndex offset in the room model vColIndex buffer that corrresponds to this tile. */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int vColIndexOffset;

  /** Returns whether or not this gridtile has been initalized. */
  public boolean isInitialized() {
    return emitOptions.isFinalized();
  }

  /** Options for the model emit. The fields in this class can be freely modified until it is finalized. */
  public static class EmitOptions {
    /** The owner GridTile object. */
    private final GridTile gridTile;
    /** The height offsets of the ceiling model at the corners, in grid space. */
    public final float[] ceilingCornerVerticalOffsets = new float[4];
    /** The height offsets of the floor model at the corners, in grid space. */
    public final float[] floorCornerVerticalOffsets = new float[4];
    /** The wall options. */
    public final Wall[] walls =
        new Wall[]{new Wall(this, PFacing.get(0)), new Wall(this, PFacing.get(1)), new Wall(this, PFacing.get(2)),
                   new Wall(this, PFacing.get(3))};
    /** The id of the ceiling model template. */
    public @Nullable
    String ceilingModelTemplateID;
    /** The id of the floor model template. */
    public @Nullable
    String floorModelTemplateID;
    /** Whether or not these options are finalized. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private boolean isFinalized = false;

    private EmitOptions(GridTile tile) {
      this.gridTile = tile;
    }

    /**
     * Options for an individual wall edge of the tile.
     */
    public static class Wall {
      /**
       * The edge of the tile that this wall is associated with. The normal of the wall should be the opposite of the
       * facing.
       */
      public final PFacing facing;
      /** The owner emitOptions object. */
      private final EmitOptions emitOptions;
      /** The id of the wall model template. */
      public String wallModelTemplateID;

      public Wall(EmitOptions emitOptions, PFacing facing) {
        this.emitOptions = emitOptions;
        this.facing = facing;
      }
    }
  }
}
