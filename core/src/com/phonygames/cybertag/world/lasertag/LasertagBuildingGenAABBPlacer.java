package com.phonygames.cybertag.world.lasertag;

public class LasertagBuildingGenAABBPlacer {
  /**
   * Sets the bounding aabbs that this building can fill with tiles.
   * @param lasertagBuildingGen
   */
  public static void addAABBs(LasertagBuildingGen lasertagBuildingGen) {
    lasertagBuildingGen.aabb.set(0, 0, 0, 14, 4, 14);
//    lasertagBuildingGen.addOutsideAABB(8, 2, 0, 14, 4, 14);
    // Place the valid tileGens.
    for (int x = lasertagBuildingGen.aabb.x0(); x <= lasertagBuildingGen.aabb.x1(); x++) {
      for (int y = lasertagBuildingGen.aabb.y0(); y <= lasertagBuildingGen.aabb.y1(); y++) {
        for (int z = lasertagBuildingGen.aabb.z0(); z <= lasertagBuildingGen.aabb.z1(); z++) {
          boolean valid = true;
          // Check for intersections with the invalid area.
          for (int b = 0; b < lasertagBuildingGen.outsideAabbs.size(); b++) {
            if (lasertagBuildingGen.outsideAabbs.get(b).contains(x, y, z)) {
              valid = false;
              break;
            }
          }
          if (valid) {
            lasertagBuildingGen.tileGens.genUnpooled(x, y, z);
          }
        }
      }
    }
  }
}
