package com.phonygames.cybertag.world.grid;

import com.phonygames.cybertag.world.World;
import com.phonygames.cybertag.world.grid.gen.TileBuildingGen;
import com.phonygames.cybertag.world.grid.gen.TileBuildingParameters;
import com.phonygames.cybertag.world.grid.gen.TileRoomParameters;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;
import com.phonygames.pengine.util.PBlockingTaskTracker;

public class LasertagGridWorldGen {
  /** Generates the world. */
  public static LasertagWorld gen(PBlockingTaskTracker taskTracker, World world) {
    // Generate a building.
    TileBuilding tileBuilding = new TileBuilding(taskTracker, new TileBuildingParameters(), world);
    tileBuilding.tileBounds().set(5, 3, 5,15,6, 15);
    TileBuildingGen.onFinishedSettingBuildingBounds(tileBuilding);
    for (int a = 0; a < 10; a++) {
      TileBuildingGen.addRoom(tileBuilding, new TileRoomParameters.Standard());
    }
    TileBuildingGen.onFinishedAddingRooms(tileBuilding);
    LasertagWorld lasertagWorld = new LasertagWorld(world);
    lasertagWorld.buildings().add(tileBuilding);
    return lasertagWorld;
  }
}
