package com.phonygames.cybertag.world.lasertag;

import lombok.val;

public class LasertagRoomGenWalkwayProcessor {

  public static void processRoomWalkways(LasertagRoomGen roomGen) {
    for (val e : roomGen.tileGens) {
      int x = e.x();
      int y = e.y();
      int z = e.z();
      LasertagTileGen tileGen = e.val();
      LasertagTile tile = tileGen.tile;
    }
  }}
