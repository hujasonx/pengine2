package com.phonygames.cybertag.world.lasertag;

import android.support.annotation.NonNull;

import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.navmesh.recast.PRecastMeshBuilder;
import com.phonygames.pengine.util.PArrayUtils;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PSet;

import org.recast4j.detour.MeshData;
import org.recast4j.detour.MeshTile;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.Poly;
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
    this.lasertagWorld = new LasertagWorld();
  }

  protected LasertagWorldGen addBlockingTask(Object o) {
    genBlockers.add(o);
    System.out.println("ADD " + o);
    return this;
  }

  public LasertagWorld build() {
    lockBuilder();
    lasertagWorld.buildings = new LasertagBuilding[buildingGens.size()];
    for (int a = 0; a < buildingGens.size(); a++) {
      LasertagBuilding building = buildingGens.get(a).build();
      lasertagWorld.buildings[a] = building;
    }
    buildModelInstance();
    return lasertagWorld;
  }

  private void buildModelInstance() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      @Override protected void modelEnd() {
        clearBlockingTask(LasertagWorldGen.this);
      }
    });
  }

  public boolean isLoaded() {
    return genBlockers.isEmpty();
  }

  protected LasertagWorldGen clearBlockingTask(Object o) {
    if (genBlockers.removeValue(o, true)) {
      System.out.println("DEL " + o + ", " + genBlockers.size() + " remaining");
      if (genBlockers.isEmpty()) {
        onGenFinished();
      }
    } else {
      PLog.w("Attempt to remove genBlocker that was not blocking: " + o);
    }
    return this;
  }

  private void onGenFinished() {
    float[] physicsVs = PArrayUtils.floatListToArray(lasertagWorld.physicsVertexPositions);
    int[] physicsIs = PArrayUtils.intListToArray(lasertagWorld.physicsVertexIndices);
        PRecastMeshBuilder recastBuilder = new PRecastMeshBuilder(new SimpleInputGeomProvider(physicsVs, physicsIs));
        lasertagWorld.tileCache = recastBuilder.getTileCache();
  }
}
