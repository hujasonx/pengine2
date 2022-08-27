package com.phonygames.pengine.navmesh;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;

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

import java.util.List;

public class PTileCache {
  public final TileCache tileCache;
  private final float[] tempFloatAr3_0 = new float[3];
  private final float[] tempFloatAr3_1 = new float[3];
  private final float[] tempFloatAr3_2 = new float[3];
  private NavMeshQuery navMeshQuery;

  public PTileCache(TileCache tileCache) {
    this.tileCache = tileCache;
  }

  public long addObstacle(PVec3 position, PVec3 size, float rotation) {
    return tileCache.addBoxObstacle(position.emit(tempFloatAr3_0), size.emit(tempFloatAr3_1), rotation);
  }

  public NavMesh getNavMesh() {
    return tileCache.getNavMesh();
  }

  public PPath obtainPath(PVec3 start, PVec3 end, PVec3 extent) {
    PPath path = PPath.obtain();
    if (navMeshQuery == null) {
      navMeshQuery = new NavMeshQuery(tileCache.getNavMesh());
    }
    QueryFilter filter = new DefaultQueryFilter();
    start.emit(tempFloatAr3_0);
    end.emit(tempFloatAr3_1);
    extent.emit(tempFloatAr3_2);
    Result<FindNearestPolyResult> startPos = navMeshQuery.findNearestPoly(tempFloatAr3_0, tempFloatAr3_2, filter);
    Result<FindNearestPolyResult> endPos = navMeshQuery.findNearestPoly(tempFloatAr3_1, tempFloatAr3_2, filter);
    // Check to make sure both the start and end position are on the mesh.
    if (startPos.result.getNearestRef() != 0 && endPos.result.getNearestRef() != 0) {
      Result<List<Long>> paths = navMeshQuery.findPath(startPos.result.getNearestRef(), endPos.result.getNearestRef(),
                                                startPos.result.getNearestPos(), endPos.result.getNearestPos(), filter);
      Result<List<StraightPathItem>> straightPath =
          navMeshQuery.findStraightPath(startPos.result.getNearestPos(), endPos.result.getNearestPos(), paths.result, 100, 0);
      for (int i = 0; i < straightPath.result.size(); i++) {
        float[] pos = straightPath.result.get(i).getPos();
        path.backingCurve.addKeyFrame(i, pos[0], pos[1], pos[2]);
      }
    }
    return path;
  }

  public void previewNavmesh() {
    PVec3 v0 = PVec3.obtain();
    PVec3 v1 = PVec3.obtain();
    PVec4 c = PVec4.obtain().set(1, 0, 0, .025f);
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
          for (int b = 0; b <= poly.vertCount; b++) {
            int i0 = poly.verts[b % poly.vertCount];
            int i1 = poly.verts[(b + 1) % poly.vertCount];
            if (i0 * 3 + 2 >= verts.length || i1 * 3 + 2 >= verts.length) {
              PAssert.warn("Invalid polygon vertices in tilecache");
              continue;
            }
            v0.set(verts[(i0) * 3 + 0], verts[(i0) * 3 + 1], verts[(i0) * 3 + 2]);
            v1.set(verts[(i1) * 3 + 0], verts[(i1) * 3 + 1], verts[(i1) * 3 + 2]);
            PDebugRenderer.line(v0, v1, c, c, 1, 1);
          }
        }
      }
    }
    c.free();
    v0.free();
    v1.free();
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
}
