package com.phonygames.pengine.navmesh;

import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;

import org.recast4j.detour.DefaultQueryFilter;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
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
}
