package com.phonygames.cybertag.world.grid;

import android.support.annotation.Nullable;

import com.phonygames.cybertag.world.grid.gen.helper.TileBuildingHallwayAndDoorPlacer;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.collection.PList;

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

  /** Helper class for getting the corners of this tile in world space. Vector params can be null. */
  public void worldSpaceCorners(float scaleX, float scaleY, float scaleZ, @Nullable PVec3 p000, @Nullable PVec3 p100,
                                @Nullable PVec3 p101, @Nullable PVec3 p001, @Nullable PVec3 p010, @Nullable PVec3 p110,
                                @Nullable PVec3 p111, @Nullable PVec3 p011) {
    if (p000 != null) {p000.set((x + 0) * scaleX, (y + 0) * scaleY, (z + 0) * scaleZ);}
    if (p100 != null) {p100.set((x + 1) * scaleX, (y + 0) * scaleY, (z + 0) * scaleZ);}
    if (p101 != null) {p101.set((x + 1) * scaleX, (y + 0) * scaleY, (z + 1) * scaleZ);}
    if (p001 != null) {p001.set((x + 0) * scaleX, (y + 0) * scaleY, (z + 1) * scaleZ);}
    if (p010 != null) {p010.set((x + 0) * scaleX, (y + 1) * scaleY, (z + 0) * scaleZ);}
    if (p110 != null) {p110.set((x + 1) * scaleX, (y + 1) * scaleY, (z + 0) * scaleZ);}
    if (p111 != null) {p111.set((x + 1) * scaleX, (y + 1) * scaleY, (z + 1) * scaleZ);}
    if (p011 != null) {p011.set((x + 0) * scaleX, (y + 1) * scaleY, (z + 1) * scaleZ);}
  }

  /** Options for the model emit. The fields in this class can be freely modified until it is finalized. */
  public static class EmitOptions {
    /** The height offsets of the ceiling model at the corners, in grid space. */
    public final float[] ceilingCornerVerticalOffsets = new float[4];
    /** The owner GridTile object. */
    public final GridTile gridTile;
    /** The height offsets of the walkway model at the corners, in grid space. */
    public final float[] walkwayCornerVerticalOffsets = new float[4];
    /** The wall options. */
    public final Wall[] walls = new Wall[4];
    /** The id of the ceiling model template. */
    public @Nullable
    String ceilingModelTemplateID;
    /** The id of the floor model template. */
    public @Nullable
    String floorModelTemplateID;
    /** The id of the walkway model template. */
    public @Nullable
    String walkwayModelTemplateID;
    /** Whether or not these options are finalized. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private boolean isFinalized = false;

    private EmitOptions(GridTile tile) {
      this.gridTile = tile;
      for (int a = 0; a < 4; a++) {
        walls[a] = new Wall(this, PFacing.get(a));
      }
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
      /** The owner GridTile. */
      public final GridTile owner;
      /** The ids of the wall model template. */
      public final PList<String> wallModelTemplateIDs = new PList<>();
      /** The owner emitOptions object. */
      private final EmitOptions emitOptions;
      /**
       * The possibleDoorOrigin object associated with this wall. Will only be generated if this wall could have a door,
       * and is only generated once.
       */
      public @Nullable
      TileBuildingHallwayAndDoorPlacer.PossibleDoorOrigin __possibleDoorOrigin;
      /** The door at this wall location, if any. */
      public TileDoor door;
      /** The score for door placement here. If zero, doors cannot be placed at this spot. */
      public float doorPlacementScore;

      public Wall(EmitOptions emitOptions, PFacing facing) {
        this.emitOptions = emitOptions;
        this.owner = emitOptions.gridTile;
        this.facing = facing;
      }

      /** Returns the GridTile on the other side of this wall in the given grid. */
      public @Nullable GridTile tileOnOtherSideIn(TileGrid grid) {
        return grid.getTileAt(owner.x + facing.forwardX(), owner.y, owner.z + facing.forwardZ());
      }

      /** Returns the wall on the other side of this wall in the given grid. */
      public @Nullable Wall wallOnOtherSideIn(TileGrid grid) {
        GridTile tile = tileOnOtherSideIn(grid);
        if (tile == null) { return null;}
        return tile.emitOptions.walls[facing.opposite().intValue()];
      }
    }
  }
}
