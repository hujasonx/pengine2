package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

import lombok.val;

public class LasertagBuildingGen extends PBuilder {
  protected final PList<PIntAABB> aabbs = new PList<>();
  protected final LasertagBuilding building;
  protected final PList<LasertagDoorGen> doorGens = new PList<>();
  protected final PList<LasertagDoorGen.PossibleDoor> possibleDoors = new PList<>();
  protected final PList<LasertagRoomGen> roomGens = new PList<>();
  protected final PIntMap3d<LasertagTileGen> tileGens = new PIntMap3d<LasertagTileGen>() {
    @Override protected LasertagTileGen newUnpooled(int x, int y, int z) {
      return new LasertagTileGen(LasertagBuildingGen.this.building.id + "(" + x + "," + y + "," + z + ")", x, y, z);
    }
  };

  public LasertagBuildingGen(LasertagWorldGen worldGen) {
    building = new LasertagBuilding("building" + worldGen.buildingGens.size, worldGen.lasertagWorld);
    worldGen.buildingGens.add(this);
  }

  /**
   * After doors and windows are placed on tilewalls, this method can copy those settings to the opposing walls.
   * @param buildingGen
   */
  public static void finalPassWalls(LasertagBuildingGen buildingGen) {
    try (val it = buildingGen.tileGens.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        LasertagTileGen otherX = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.X);
        LasertagTileGen otherZ = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.Z);
        LasertagTileGen otherMX = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.mX);
        LasertagTileGen otherMZ = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.mZ);
        LasertagTileWallGen wallX = e.val().wallX;
        LasertagTileWallGen wallZ = e.val().wallZ;
        LasertagTileWallGen wallMX = e.val().wallMX;
        LasertagTileWallGen wallMZ = e.val().wallMZ;
        wallX.copySettingsFromOrToOtherWall(true);
        wallZ.copySettingsFromOrToOtherWall(true);
        wallMX.copySettingsFromOrToOtherWall(true);
        wallMZ.copySettingsFromOrToOtherWall(true);
      }
    }
  }

  protected void addAABB(int offsetX, int offsetY, int offsetZ, int xSize, int ySize, int zSize) {
    checkLock();
    PIntAABB aabb = new PIntAABB().set(offsetX, offsetY, offsetZ, offsetX + xSize, offsetY + ySize, offsetZ + zSize);
    aabbs.add(aabb);
    for (int x = 0; x < xSize; x++) {
      for (int y = 0; y < ySize; y++) {
        for (int z = 0; z < zSize; z++) {
          tileGens.genUnpooled(offsetX + x, offsetY + y, offsetZ + z);
        }
      }
    }
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
    try (val it = tileGens.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        e.val().build();
      }
    }
    for (val e : doorGens) {
      e.build();
    }
    buildModelInstance();
    return building;
  }

  private void buildModelInstance() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {});
  }

  public void processTiles() {
    for (val roomGen : roomGens) {

      LasertagRoomGenTileProcessor.processRoomWalls(roomGen);
      LasertagRoomGenTileProcessor.processRoomFloors(roomGen);
      LasertagRoomGenTileProcessor.processRoomCeilings(roomGen);
      LasertagRoomGenWalkwayProcessor.processRoomWalkways(roomGen);
    }
//    try (val it = roomGens.obtainIterator()) {
//      while (it.hasNext()) {
//        val roomGen = it.next();
//        LasertagRoomGenTileProcessor.processRoomWalls(roomGen);
//        LasertagRoomGenTileProcessor.processRoomFloors(roomGen);
//        LasertagRoomGenTileProcessor.processRoomCeilings(roomGen);
//        LasertagRoomGenWalkwayProcessor.processRoomWalkways(roomGen);
//      }
//    }
  }

  public LasertagBuildingGen setTileRotation(float rotation) {
    this.building.tileRotation().setToRotation(0, 1, 0, rotation);
    return this;
  }

  public LasertagBuildingGen setTileScale(float x, float y, float z) {
    this.building.tileScale().set(x, y, z);
    return this;
  }

  public LasertagBuildingGen setTileTranslation(float x, float y, float z) {
    this.building.tileTranslation().set(x, y, z);
    return this;
  }
}
