package com.phonygames.cybertag.world.grid;

import com.phonygames.cybertag.world.World;
import com.phonygames.cybertag.world.grid.gen.TileBuildingGen;
import com.phonygames.cybertag.world.grid.gen.TileRoomParameters;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;

public class LasertagGridWorldGen {
  public static LasertagWorld gen(World world) {
    // Generate a building.
    TileBuilding tileBuilding = new TileBuilding();
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
