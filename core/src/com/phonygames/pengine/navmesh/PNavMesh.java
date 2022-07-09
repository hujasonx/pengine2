package com.phonygames.pengine.navmesh;

import com.badlogic.gdx.math.Vector3;

import org.recast4j.detour.DefaultQueryFilter;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshParams;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.QueryFilter;
import org.recast4j.detour.Result;
import org.recast4j.detour.StraightPathItem;

import java.util.ArrayList;
import java.util.List;

public class PNavMesh extends NavMesh {
  public PNavMesh(MeshData data, int maxVertsPerPoly, int flags) {
    super(data, maxVertsPerPoly, flags);
  }

  public PNavMesh(NavMeshParams params, int maxVertsPerPoly) {
    super(params, maxVertsPerPoly);
  }
}
