package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.util.PBuilder;

public class LasertagTileGen extends PBuilder {
  public final int x, y, z;
  protected final LasertagTile tile;
  protected LasertagRoomGen roomGen;
  protected final LasertagTileWallGen wallMX = new LasertagTileWallGen(LasertagTileWall.Facing.mX, this);
  protected final LasertagTileWallGen wallMZ = new LasertagTileWallGen(LasertagTileWall.Facing.mZ, this);
  protected final LasertagTileWallGen wallX = new LasertagTileWallGen(LasertagTileWall.Facing.X, this);
  protected final LasertagTileWallGen wallZ = new LasertagTileWallGen(LasertagTileWall.Facing.Z, this);

  public LasertagTileGen(String id, int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
    tile = new LasertagTile(id, x, y, z);
    tile.wallX = wallX.wall;
    tile.wallZ = wallZ.wall;
    tile.wallMX = wallMX.wall;
    tile.wallMZ = wallMZ.wall;
  }

  public LasertagTile build() {
    lockBuilder();
    buildModelInstance();
    return tile;
  }

  private void buildModelInstance() {
  }

  public LasertagTileWallGen wallGen(LasertagTileWall.Facing facing) {
    switch (facing) {
      case X:
        return wallX;
      case Z:
        return wallZ;
      case mX:
        return wallMX;
      case mZ:
      default:
        return wallMZ;
    }
  }
}
