package com.phonygames.cybertag.world;

import com.phonygames.cybertag.world.lasertag.LasertagBuildingGen;
import com.phonygames.cybertag.world.lasertag.LasertagBuildingGenAABBPlacer;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGen;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGenRoomPlacer;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;
import com.phonygames.cybertag.world.lasertag.LasertagWorldGen;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.aabb.PIntAABB;

public class World {
  public final LasertagWorld lasertagWorld;

  public World() {
    LasertagWorldGen worldGen = new LasertagWorldGen(this);
    LasertagBuildingGen buildingGen = new LasertagBuildingGen(worldGen);
    buildingGen.setTileTranslation(0, 0, 0).setTileRotation(.2f).setTileScale(3, 3, 3);
    LasertagBuildingGenAABBPlacer.addAABBs(buildingGen);
    LasertagRoomGenRoomPlacer.reset();
    for (int a = 0; a < 10; a++) {
      PIntAABB roomAABB = LasertagRoomGenRoomPlacer.getValidAABBForRoomPlacement(buildingGen);
      if (roomAABB != null) {
        LasertagRoomGen roomGen = new LasertagRoomGen(buildingGen, roomAABB);
        LasertagRoomGenTileProcessor.processRoomWalls(roomGen);
        LasertagRoomGenTileProcessor.processRoomFloors(roomGen);
        LasertagRoomGenTileProcessor.processRoomCeilings(roomGen);
      }
    }
    this.lasertagWorld = worldGen.build();
  }

  public void frameUpdate() {
    lasertagWorld.frameUpdate();
  }

  public void render(PRenderContext renderContext) {
    lasertagWorld.render(renderContext);
  }
}
