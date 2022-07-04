package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.util.PList;

import java.util.Collections;

public class LasertagBuildGenDoorProcessor {
  public static void processPossibleDoorsIntoAcutal(LasertagBuildingGen buildingGen) {
    PList<LasertagDoorGen.PossibleDoor> possibleDoors = buildingGen.possibleDoors;
    if (possibleDoors == null) {
      return;
    }

    while (!possibleDoors.isEmpty()) {
      possibleDoors.sort();
      LasertagDoorGen.PossibleDoor nextDoor = possibleDoors.removeLast();
      if (!nextDoor.checkStillValid()) {
        continue;
      }
      if (nextDoor.score() < 0) {
        return; // Stop emitting doors once no good doors remain.
      }
      LasertagDoorGen doorGen = nextDoor.toDoorGen();
    }
  }
}
