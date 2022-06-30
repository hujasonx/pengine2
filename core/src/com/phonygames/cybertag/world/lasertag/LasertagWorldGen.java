package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;

import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;

public class LasertagWorldGen extends PBuilder {
  protected final PList<LasertagBuildingGen> buildingGens = new PList<>();
  protected final LasertagWorld lasertagWorld;
  protected final World world;

  public LasertagWorldGen(@NonNull final World world) {
    this.world = world;
    this.lasertagWorld = new LasertagWorld();
  }

  public LasertagWorld build() {
    lockBuilder();
    lasertagWorld.buildings = new LasertagBuilding[buildingGens.size];
    for (int a = 0; a < buildingGens.size; a++) {
      lasertagWorld.buildings[a] = buildingGens.get(a).build();
    }
    buildModelInstance();
    return lasertagWorld;
  }

  private void buildModelInstance() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {

    });
  }
}
