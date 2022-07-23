package com.phonygames.pengine.navmesh;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import org.recast4j.detour.DefaultQueryFilter;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.MeshTile;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.Poly;
import org.recast4j.detour.QueryFilter;
import org.recast4j.detour.Result;
import org.recast4j.detour.StraightPathItem;
import org.recast4j.detour.tilecache.TileCache;

import java.util.ArrayList;
import java.util.List;

public class PTileCache {
  public final TileCache tileCache;

  public PTileCache(TileCache tileCache) {
    this.tileCache = tileCache;
  }

  public NavMesh getNavMesh() {
    return tileCache.getNavMesh();
  }

  public PList<PVec3> getPath(PVec3 start, PVec3 end, PVec3 extent) {
    NavMeshQuery query = new NavMeshQuery(tileCache.getNavMesh());
    QueryFilter filter = new DefaultQueryFilter();
    float[] startPosFloats = new float[]{start.x(), start.y(), start.z()};
    float[] endPosFloats = new float[]{end.x(), end.y(), end.z()};
    float[] extents = {extent.x(), extent.y(), extent.z()};


    PList<PVec3> wayPoints = new PList<>();
    Result<FindNearestPolyResult> startPos = query.findNearestPoly(startPosFloats, extents, filter);
    Result<FindNearestPolyResult> endPos = query.findNearestPoly(endPosFloats, extents, filter);
    // Check to make sure both the start and end position are on the mesh.
    if (startPos.result.getNearestRef() != 0 && endPos.result.getNearestRef() != 0) {
      Result<List<Long>> paths =
          query.findPath(startPos.result.getNearestRef(), endPos.result.getNearestRef(), startPos.result.getNearestPos(), endPos.result.getNearestPos(), filter);
      Result<List<StraightPathItem>> straightPath = query.findStraightPath(startPos.result.getNearestPos(), endPos.result.getNearestPos(), paths.result, 100, 0);

      for (int i = 0; i < straightPath.result.size(); i++) {
        float[] pos = straightPath.result.get(i).getPos();
        PVec3 vector = PVec3.obtain().set(pos[0], pos[1], pos[2]);
        wayPoints.add(vector);
      }
      return wayPoints;
    }
    return wayPoints;
  }

  private final float[] tempFloatAr3_0 = new float[3];
  private final float[] tempFloatAr3_1 = new float[3];

  public long addObstacle(PVec3 position, PVec3 size, float rotation) {
    return tileCache.addBoxObstacle(position.emit(tempFloatAr3_0), size.emit(tempFloatAr3_1), rotation);
  }

  public void removeObstacle(long id) {
    tileCache.removeObstacle(id);
  }

  public boolean update() {
    while (!tileCache.update()) {
      System.out.println("Updating cache");
    }
    return true;
  }

  public void previewNavmesh() {

      PVec3 v0 = PVec3.obtain();
      PVec3 v1 = PVec3.obtain();
      PVec3 v2 = PVec3.obtain();

      if (tileCache != null) {
        NavMesh navMesh = tileCache.getNavMesh();
        int max = navMesh.getMaxTiles();
        for (int t = 0; t < max; t++) {
          MeshTile tile = navMesh.getTile(t);
          MeshData meshData = tile.data;
          if (meshData == null) {
            continue;
          }


          Poly[] polies = meshData.polys;
          float[] verts = meshData.verts;
          for (int a = 0; a < polies.length; a++) {
            Poly poly = polies[a];
            for (int b = 2; b < poly.vertCount; b += 3) {
              int i0 = poly.verts[b - 2];
              int i1 = poly.verts[b - 1];
              int i2 = poly.verts[b - 0];

              v0.set(verts[(i0) * 3 + 0], verts[(i0) * 3 + 1], verts[(i0) * 3 + 2]);
              v1.set(verts[(i1) * 3 + 0], verts[(i1) * 3 + 1], verts[(i1) * 3 + 2]);
              v2.set(verts[(i2) * 3 + 0], verts[(i2) * 3 + 1], verts[(i2) * 3 + 2]);
              PVec4 c = PColor.BLUE;
              if (poly.getType() == Poly.DT_POLYTYPE_GROUND) {
                c = PColor.RED;
              }
              PDebugRenderer.line(v0, v1, c, c, 1, 1);
              PDebugRenderer.line(v1, v2, c, c, 1, 1);
              PDebugRenderer.line(v2, v0, c, c, 1, 1);
            }
          }
        }
      }
     v0.free();
     v1.free();
     v2.free();
  }
}
