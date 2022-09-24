package com.phonygames.cybertag.world.grid;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PPooledIterable;

/**
 * Represents a world that uses a 3d grid representation.
 */
public class TileGrid extends PPooledIterable<PIntMap3d.Entry<GridTile>> {
  /** The grid tiles managed by this grid. */
  private final PIntMap3d<GridTile> tiles = new PIntMap3d<GridTile>() {
    @Override
    protected GridTile newUnpooled(int x, int y, int z) {
      GridTile.GridTileBuilder builder = new GridTile.GridTileBuilder();
      builder.x(x);
      builder.y(y);
      builder.z(z);
      return builder.build();
    }
  };

  /** Returns the tile at the given coordinates. */
  public GridTile getTileAt(int x, int y, int z) {
    return tiles.get(x,y,z);
  }

  /** Returns true if there is a tile at the coordinates. */
  public boolean hasTileAt(int x, int y, int z) {
    return getTileAt(x, y, z) != null;
  }

  @Override public PPoolableIterator<PIntMap3d.Entry<GridTile>> obtainIterator() {
    return tiles.obtainIterator();
  }

  /** Adds the given tile to the list of tiles managed by this grid. */
  public void trackTile(GridTile tile) {
    PAssert.isFalse(hasTileAt(tile.x,tile.y,tile.z));
    tiles.put(tile.x, tile.y, tile.z, tile);
  }

  /** Tracks all tiles from the other tile grid. */
  public void trackAll(TileGrid other) {
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = other.tiles.obtainIterator()){
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        trackTile(e.val());
      }
    }
  }
}
