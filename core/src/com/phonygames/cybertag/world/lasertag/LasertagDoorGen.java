package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.util.PBuilder;

public class LasertagDoorGen extends PBuilder {
  protected final LasertagDoor door;
  protected final LasertagRoomGen ownerRoomGen, otherRoomGen;

  protected LasertagDoorGen(LasertagRoomGen ownerRoomGen, LasertagRoomGen otherRoomGen) {
    door = new LasertagDoor();
    this.otherRoomGen = otherRoomGen;
    this.ownerRoomGen = ownerRoomGen;
    ownerRoomGen.directlyConnectedRooms.add(otherRoomGen);
    otherRoomGen.directlyConnectedRooms.add(ownerRoomGen);
  }

  public LasertagDoor build() {
    lockBuilder();
    return door;
  }
}
