package com.phonygames.cybertag.world.grid;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.collection.PIntMap3d;

/**
 * Represents a world that uses a 3d grid representation.
 */
public class TileGrid {
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

  /** Adds the given tile to the list of tiles managed by this grid. */
  public void trackTile(GridTile tile) {
    PAssert.isFalse(hasTileAt(tile.x(),tile.y(),tile.z()));
    tiles.put(tile.x(), tile.y(), tile.z(), tile);
  }
}
