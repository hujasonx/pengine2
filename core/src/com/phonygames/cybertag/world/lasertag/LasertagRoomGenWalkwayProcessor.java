package com.phonygames.cybertag.world.lasertag;

import lombok.val;

public class LasertagRoomGenWalkwayProcessor {

  public static void processRoomWalkways(LasertagRoomGen roomGen) {
    try (val it = roomGen.tileGens.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        int x = e.x();
        int y = e.y();
        int z = e.z();
        LasertagTileGen tileGen = e.val();
        LasertagTile tile = tileGen.tile;
      }
    }
  }}
