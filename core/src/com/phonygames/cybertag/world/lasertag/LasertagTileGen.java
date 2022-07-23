package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.Nullable;

import com.phonygames.pengine.util.PBuilder;

public class LasertagTileGen extends PBuilder {
  public final int x, y, z;
  protected final LasertagBuildingGen buildingGen;
  protected final LasertagTile tile;
  protected final LasertagTileWallGen wallX, wallZ, wallMX, wallMZ;
  protected LasertagRoomGen roomGen;

  public LasertagTileGen(LasertagBuildingGen buildingGen, String id, int x, int y, int z) {
    this.buildingGen = buildingGen;
    buildingGen.worldGen.addBlockingTask(this);
    this.x = x;
    this.y = y;
    this.z = z;
    tile = new LasertagTile(id, x, y, z);
    wallX = new LasertagTileWallGen(LasertagTileWall.Facing.X, this);
    wallZ = new LasertagTileWallGen(LasertagTileWall.Facing.Z, this);
    wallMX = new LasertagTileWallGen(LasertagTileWall.Facing.mX, this);
    wallMZ = new LasertagTileWallGen(LasertagTileWall.Facing.mZ, this);
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
    buildingGen.worldGen.clearBlockingTask(this);
  }

  public @Nullable LasertagTileGen tileGenInRoomWithLocationOffset(int x, int y, int z) {
    if (this.roomGen == null) {return null;}
    return roomGen.tileGens.get(this.x + x, this.y + y, this.z + z);
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
