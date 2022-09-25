package com.phonygames.cybertag.world.grid;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PPooledIterable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Represents a world that uses a 3d grid representation. Tiles should never be removed from the grid, only added.
 */
public class TileGrid extends PPooledIterable<PIntMap3d.Entry<GridTile>> {
  /** The grid tiles managed by this grid. */
  private final PIntMap3d<GridTile> tiles = new PIntMap3d<GridTile>() {
    @Override protected GridTile newUnpooled(int x, int y, int z) {
      GridTile.GridTileBuilder builder = new GridTile.GridTileBuilder();
      builder.x(x);
      builder.y(y);
      builder.z(z);
      return builder.build();
    }
  };
  /** The bounds of this grid, or null if there are no tiles. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private @Nullable
  PIntAABB bounds;

  @Override public PPoolableIterator<PIntMap3d.Entry<GridTile>> obtainIterator() {
    return tiles.obtainIterator();
  }

  /**
   * Returns how many tiles away from the edge of this grid the tile is in the direction, where being on the edge will
   * return 0.
   *
   * @param tile
   * @param facing
   * @return
   */
  public int offsetFromEdge(GridTile tile, PFacing facing) {
    PAssert.isNotNull(bounds, "No bounds were set for this tile grid; it's not tracking anything!");
    switch (facing) {
      case X:
        return bounds.x1() - tile.x;
      case mX:
        return tile.x - bounds.x0();
      case Z:
        return bounds.z1() - tile.z;
      case mZ:
        return tile.z - bounds.z0();
      default:
        PAssert.fail("Should not reach!");
        return 0;
    }
  }

  /**
   * Returns how many tiles away from the top of this grid, where being on the top will
   * return 0.
   *
   * @param tile
   * @return
   */
  public int offsetFromTop(GridTile tile) {
    PAssert.isNotNull(bounds, "No bounds were set for this tile grid; it's not tracking anything!");
    return bounds.y1() - tile.y;
  }

  /**
   * Returns how many tiles away from the bottom of this grid, where being on the bottom will
   * return 0.
   *
   * @param tile
   * @return
   */
  public int offsetFromBottom(GridTile tile) {
    PAssert.isNotNull(bounds, "No bounds were set for this tile grid; it's not tracking anything!");
    return tile.y - bounds.y0();
  }

  /** Tracks all tiles from the other tile grid. */
  public void trackAll(TileGrid other) {
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = other.tiles.obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        trackTile(e.val());
      }
    }
  }

  /** Adds the given tile to the list of tiles managed by this grid. */
  public void trackTile(GridTile tile) {
    PAssert.isFalse(hasTileAt(tile.x, tile.y, tile.z));
    if (bounds == null) {
      bounds = PIntAABB.getStaticPool().obtain().set(tile.x, tile.y, tile.z, tile.x, tile.y, tile.z);
    } else {
      if (tile.x < bounds.x0()) {bounds.x0(tile.x);}
      if (tile.y < bounds.y0()) {bounds.y0(tile.y);}
      if (tile.z < bounds.z0()) {bounds.z0(tile.z);}
      if (tile.x > bounds.x1()) {bounds.x1(tile.x);}
      if (tile.y > bounds.y1()) {bounds.y1(tile.y);}
      if (tile.z > bounds.z1()) {bounds.z1(tile.z);}
    }
    tiles.put(tile.x, tile.y, tile.z, tile);
  }

  /** Returns true if there is a tile at the coordinates. */
  public boolean hasTileAt(int x, int y, int z) {
    return getTileAt(x, y, z) != null;
  }

  /** Returns the tile at the given coordinates. */
  public GridTile getTileAt(int x, int y, int z) {
    return tiles.get(x, y, z);
  }
}
