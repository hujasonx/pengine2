package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.navmesh.PTileCache;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class LasertagWorld {
  protected final PList<Integer> physicsVertexIndices = new PList<>();
  protected final PList<Float> physicsVertexPositions = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected LasertagBuilding[] buildings;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  protected PTileCache tileCache;

  protected LasertagWorld() {
  }

  public void frameUpdate() {
    for (val building : buildings) {
      building.frameUpdate();
    }
    if (tileCache != null) {
      tileCache.previewNavmesh();
    }
  }

  public void logicUpdate() {
    for (val building : buildings) {
      building.logicUpdate();
    }
  }

  public void previewNavMeshData() {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      for (int a = 0; a < physicsVertexIndices.size(); a += 3) {
        int i0 = physicsVertexIndices.get(a + 0);
        int i1 = physicsVertexIndices.get(a + 1);
        int i2 = physicsVertexIndices.get(a + 2);
        PVec3 v0 = pool.vec3(physicsVertexPositions.get(i0 * 3 + 0), physicsVertexPositions.get(i0 * 3 + 1),
                             physicsVertexPositions.get(i0 * 3 + 2));
        PVec3 v1 = pool.vec3(physicsVertexPositions.get(i1 * 3 + 0), physicsVertexPositions.get(i1 * 3 + 1),
                             physicsVertexPositions.get(i1 * 3 + 2));
        PVec3 v2 = pool.vec3(physicsVertexPositions.get(i2 * 3 + 0), physicsVertexPositions.get(i2 * 3 + 1),
                             physicsVertexPositions.get(i2 * 3 + 2));
        PDebugRenderer.line(v0, v1, PColor.GREEN, PColor.GREEN, 1, 1);
        PDebugRenderer.line(v1, v2, PColor.GREEN, PColor.GREEN, 1, 1);
        PDebugRenderer.line(v2, v0, PColor.GREEN, PColor.GREEN, 1, 1);
      }
    }
  }

  public void render(PRenderContext renderContext) {
    for (val building : buildings) {
      building.render(renderContext);
    }
    if (modelInstance != null) {
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
