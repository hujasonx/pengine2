package com.phonygames.cybertag.world.lasertag;

public class LasertagBuildingGenAABBPlacer {
  /**
   * Sets the bounding aabbs that this building can fill with tiles.
   * @param lasertagBuildingGen
   */
  public static void addAABBs(LasertagBuildingGen lasertagBuildingGen) {
    lasertagBuildingGen.addAABB(0, 0, 0, 14, 3, 14);
    lasertagBuildingGen.addAABB(3, 2, 0, 8, 2, 10);
  }

}
