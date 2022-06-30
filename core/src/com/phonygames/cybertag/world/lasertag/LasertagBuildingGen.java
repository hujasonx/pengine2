package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

public class LasertagBuildingGen extends PBuilder {
  protected final PList<PIntAABB> aabbs = new PList<>();
  protected final LasertagBuilding building;
  protected final PList<LasertagRoomGen> roomGens = new PList<>();
  protected final PIntMap3d<LasertagTileGen> tilesBuilders = new PIntMap3d<LasertagTileGen>() {
    @Override protected LasertagTileGen newUnpooled(int x, int y, int z) {
      return new LasertagTileGen(LasertagBuildingGen.this.building.id + "(" + x + "," + y + "," + z + ")", x, y, z);
    }
  };

  public LasertagBuildingGen(LasertagWorldGen worldGen) {
    building = new LasertagBuilding("building" + worldGen.buildingGens.size, worldGen.lasertagWorld);
    worldGen.buildingGens.add(this);
  }

  protected void addAABB(int offsetX, int offsetY, int offsetZ, int xSize, int ySize, int zSize) {
    checkLock();
    PIntAABB aabb = new PIntAABB().set(offsetX, offsetY, offsetZ, offsetX + xSize, offsetY + ySize, offsetZ + zSize);
    aabbs.add(aabb);
    for (int x = 0; x < xSize; x++) {
      for (int y = 0; y < ySize; y++) {
        for (int z = 0; z < zSize; z++) {
          tilesBuilders.genUnpooled(offsetX + x, offsetY + y, offsetZ + z);
        }
      }
    }
  }

  public LasertagBuildingGen setTileScale(float x, float y, float z) {
    this.building.tileScale().set(x, y, z);
    return this;
  }

  public LasertagBuildingGen setTileTranslation(float x, float y, float z) {
    this.building.tileTranslation().set(x, y, z);
    return this;
  }

  public LasertagBuildingGen setTileRotation(float rotation) {
    this.building.tileRotation().setToRotation(0, 1, 0, rotation);
    return this;
  }

  public LasertagBuilding build() {
    lockBuilder();
    building.aabbs = new PIntAABB[aabbs.size];
    for (int a = 0; a < aabbs.size; a++) {
      building.aabbs[a] = aabbs.get(a);
    }
    building.rooms = new LasertagRoom[roomGens.size];
    for (int a = 0; a < roomGens.size; a++) {
      building.rooms[a] = roomGens.get(a).build();
    }
    buildModelInstance();
    return building;
  }

  private void buildModelInstance() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {});
  }
}
