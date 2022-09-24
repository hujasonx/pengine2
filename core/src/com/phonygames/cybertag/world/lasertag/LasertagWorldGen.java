package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;

import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.graphics.model.PModelGenOld;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.navmesh.recast.PRecastMeshBuilder;
import com.phonygames.pengine.util.collection.PArrayUtils;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.collection.PList;

import org.recast4j.recast.geom.SimpleInputGeomProvider;

public class LasertagWorldGen extends PBuilder {
  protected final PList<LasertagBuildingGen> buildingGens = new PList<>();
  protected final LasertagWorld lasertagWorld;
  protected final World world;
  /** A set of objects that are blocking this world gen from being done. */
  private final PList<Object> genBlockers = new PList<>();

  public LasertagWorldGen(@NonNull final World world) {
    this.world = world;
    addBlockingTask(this);
    this.lasertagWorld = new LasertagWorld(world);
  }

  protected LasertagWorldGen addBlockingTask(Object o) {
    genBlockers.add(o);
    return this;
  }

  public LasertagWorld build() {
    lockBuilder();
//    lasertagWorld.buildings = new LasertagBuilding[buildingGens.size()];
    for (int a = 0; a < buildingGens.size(); a++) {
      LasertagBuilding building = buildingGens.get(a).build();
//      lasertagWorld.buildings[a] = building;
    }
    buildModelInstance();
    return lasertagWorld;
  }

  private void buildModelInstance() {
    PModelGenOld.getPostableTaskQueue().enqueue(new PModelGenOld() {
      @Override protected void modelEnd() {
        clearBlockingTask(LasertagWorldGen.this);
      }
    });
  }

  protected LasertagWorldGen clearBlockingTask(Object o) {
    if (genBlockers.removeValue(o, true)) {
      if (genBlockers.isEmpty()) {
        onGenFinished();
      }
    } else {
      PLog.w("Attempt to remove genBlocker that was not blocking: " + o);
    }
    return this;
  }

  private void onGenFinished() {
    float[] physicsVs = lasertagWorld.world.physicsVertexPositions.emit();
    int[] physicsIs = PArrayUtils.intListToArray(lasertagWorld.world.physicsVertexIndices);
    PRecastMeshBuilder recastBuilder = new PRecastMeshBuilder(new SimpleInputGeomProvider(physicsVs, physicsIs));
    lasertagWorld.world.tileCache = recastBuilder.getTileCache();
  }

  public boolean isLoaded() {
    return genBlockers.isEmpty();
  }
}
