package com.phonygames.pengine.navmesh.recast;

import static org.recast4j.detour.DetourCommon.vCopy;
import static org.recast4j.recast.RecastVectors.copy;

import com.badlogic.gdx.Gdx;
import com.phonygames.pengine.navmesh.PNavMesh;
import com.phonygames.pengine.navmesh.PTileCache;

import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshDataCreateParams;
import org.recast4j.detour.NavMeshParams;
import org.recast4j.detour.tilecache.TileCache;
import org.recast4j.detour.tilecache.TileCacheBuilder;
import org.recast4j.detour.tilecache.TileCacheLayerHeader;
import org.recast4j.detour.tilecache.TileCacheMeshProcess;
import org.recast4j.detour.tilecache.TileCacheParams;
import org.recast4j.detour.tilecache.TileCacheStorageParams;
import org.recast4j.detour.tilecache.io.compress.TileCacheCompressorFactory;
import org.recast4j.recast.HeightfieldLayerSet;
import org.recast4j.recast.Recast;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilderConfig;
import org.recast4j.recast.RecastConfig;
import org.recast4j.recast.RecastConstants;
import org.recast4j.recast.geom.InputGeomProvider;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PRecastMeshBuilder {
  private final static float m_agentHeight = 1.8f;
  private final static float m_agentMaxClimb = 0.5f;
  private final static float m_agentMaxSlope = 45.0f;
  private final static float m_agentRadius = 0.5f;
  private final static float m_detailSampleDist = 3.0f;
  private final static float m_edgeMaxLen = 12.0f;
  private final static int m_regionMergeSize = 20;
  private final static int m_regionMinSize = 4;
  private final static int m_tileSize = 64;
  private final static int m_vertsPerPoly = 8;
  private final static float precision = 1;
  private final static float m_cellSize = 0.2f / precision;
  private final static float m_cellHeight = 0.2f / precision;
  private final static float m_edgeMaxError = 1.3f / precision;
  private final static float m_detailSampleMaxError = 0.8f / precision;
  protected final InputGeomProvider m_geom;
  private final RecastConfig rcConfig;
  private final int th;
  private final int tw;
  private PTileCache tileCache;

  public PRecastMeshBuilder(String objFile) {
    this(new PRecastObjImporter().load(Gdx.files.internal(objFile).read()), RecastConstants.PartitionType.WATERSHED,
         m_cellSize, m_cellHeight, m_agentHeight, m_agentRadius, m_agentMaxClimb, m_agentMaxSlope, m_regionMinSize,
         m_regionMergeSize, m_edgeMaxLen, m_edgeMaxError, m_vertsPerPoly, m_detailSampleDist, m_detailSampleMaxError);
  }

  public PRecastMeshBuilder(InputGeomProvider m_geom) {
    this(m_geom, RecastConstants.PartitionType.WATERSHED, m_cellSize, m_cellHeight, m_agentHeight, m_agentRadius,
         m_agentMaxClimb, m_agentMaxSlope, m_regionMinSize, m_regionMergeSize, m_edgeMaxLen, m_edgeMaxError,
         m_vertsPerPoly, m_detailSampleDist, m_detailSampleMaxError);
  }

  public PRecastMeshBuilder(InputGeomProvider m_geom, RecastConstants.PartitionType m_partitionType, float m_cellSize,
                            float m_cellHeight, float m_agentHeight, float m_agentRadius, float m_agentMaxClimb,
                            float m_agentMaxSlope, int m_regionMinSize, int m_regionMergeSize, float m_edgeMaxLen,
                            float m_edgeMaxError, int m_vertsPerPoly, float m_detailSampleDist,
                            float m_detailSampleMaxError) {
    this.m_geom = m_geom;
    rcConfig = new RecastConfig(true, m_tileSize, m_tileSize, RecastConfig.calcBorder(m_agentRadius, m_cellSize),
                                m_partitionType, m_cellSize, m_cellHeight, m_agentMaxSlope, true, true, true,
                                m_agentHeight, m_agentRadius, m_agentMaxClimb, m_regionMinSize, m_regionMergeSize,
                                m_edgeMaxLen, m_edgeMaxError, m_vertsPerPoly, true, m_detailSampleDist,
                                m_detailSampleMaxError, PRecastSampleAreaModifications.SAMPLE_AREAMOD_GROUND);
    float[] bmin = m_geom.getMeshBoundsMin();
    float[] bmax = m_geom.getMeshBoundsMax();
    int[] twh = Recast.calcTileCount(bmin, bmax, m_cellSize, m_tileSize, m_tileSize);
    tw = twh[0];
    th = twh[1];
    List<byte[]> layers = buildLayers(ByteOrder.LITTLE_ENDIAN, true, 7);
    TileCache tc = getTileCache(m_geom, ByteOrder.LITTLE_ENDIAN, true);
    for (byte[] data : layers) {
      try {
        long ref = tc.addTile(data, 0);
        tc.buildNavMeshTile(ref);
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
    this.tileCache = new PTileCache(tc);
  }

  public List<byte[]> buildLayers(int tx, int ty, ByteOrder order, boolean cCompatibility) {
    HeightfieldLayerSet lset = getHeightfieldSet(tx, ty);
    List<byte[]> result = new ArrayList<>();
    if (lset != null) {
      TileCacheBuilder builder = new TileCacheBuilder();
      for (int i = 0; i < lset.layers.length; ++i) {
        HeightfieldLayerSet.HeightfieldLayer layer = lset.layers[i];
        // Store header
        TileCacheLayerHeader header = new TileCacheLayerHeader();
        header.magic = TileCacheLayerHeader.DT_TILECACHE_MAGIC;
        header.version = TileCacheLayerHeader.DT_TILECACHE_VERSION;
        // Tile layer location in the navmesh.
        header.tx = tx;
        header.ty = ty;
        header.tlayer = i;
        vCopy(header.bmin, layer.bmin);
        vCopy(header.bmax, layer.bmax);
        // Tile info.
        header.width = layer.width;
        header.height = layer.height;
        header.minx = layer.minx;
        header.maxx = layer.maxx;
        header.miny = layer.miny;
        header.maxy = layer.maxy;
        header.hmin = layer.hmin;
        header.hmax = layer.hmax;
        result.add(
            builder.compressTileCacheLayer(header, layer.heights, layer.areas, layer.cons, order, cCompatibility));
      }
    }
    return result;
  }

  public List<byte[]> buildLayers(ByteOrder order, boolean cCompatibility, int threads) {
    return buildLayers(order, cCompatibility, threads, tw, th);
  }

  protected List<byte[]> buildLayers(ByteOrder order, boolean cCompatibility, int threads, int tw, int th) {
    if (threads == 1) {
      return buildLayersSingleThread(order, cCompatibility, tw, th);
    }
    return buildLayersMultiThread(order, cCompatibility, tw, th, threads);
  }

  @SuppressWarnings("unchecked")
  private List<byte[]> buildLayersMultiThread(ByteOrder order, boolean cCompatibility, int tw, int th, int threads) {
    ExecutorService ec = Executors.newFixedThreadPool(threads);
    List<?>[][] partialResults = new List[th][tw];
    for (int y = 0; y < th; ++y) {
      for (int x = 0; x < tw; ++x) {
        final int tx = x;
        final int ty = y;
        ec.submit((Runnable) () -> {
          partialResults[ty][tx] = buildLayers(tx, ty, order, cCompatibility);
        });
      }
    }
    ec.shutdown();
    try {
      ec.awaitTermination(1000, TimeUnit.HOURS);
    } catch (InterruptedException e) {
    }
    List<byte[]> layers = new ArrayList<>();
    for (int y = 0; y < th; ++y) {
      for (int x = 0; x < tw; ++x) {
        layers.addAll((List<byte[]>) partialResults[y][x]);
      }
    }
    return layers;
  }

  private List<byte[]> buildLayersSingleThread(ByteOrder order, boolean cCompatibility, int tw, int th) {
    List<byte[]> layers = new ArrayList<>();
    for (int y = 0; y < th; ++y) {
      for (int x = 0; x < tw; ++x) {
        layers.addAll(buildLayers(x, y, order, cCompatibility));
      }
    }
    return layers;
  }

  protected HeightfieldLayerSet getHeightfieldSet(int tx, int ty) {
    RecastBuilder rcBuilder = new RecastBuilder();
    float[] bmin = m_geom.getMeshBoundsMin();
    float[] bmax = m_geom.getMeshBoundsMax();
    RecastBuilderConfig cfg = new RecastBuilderConfig(rcConfig, bmin, bmax, tx, ty);
    HeightfieldLayerSet lset = rcBuilder.buildLayers(m_geom, cfg);
    return lset;
  }

  public int getTh() {
    return th;
  }

  public PTileCache getTileCache() {
    return tileCache;
  }

  public TileCache getTileCache(InputGeomProvider geom, ByteOrder order, boolean cCompatibility) {
    TileCacheParams params = new TileCacheParams();
    params.ch = m_cellHeight;
    params.cs = m_cellSize;
    vCopy(params.orig, geom.getMeshBoundsMin());
    params.height = m_tileSize;
    params.width = m_tileSize;
    params.walkableHeight = m_agentHeight;
    params.walkableRadius = m_agentRadius;
    params.walkableClimb = m_agentMaxClimb;
    params.maxSimplificationError = m_edgeMaxError;
    params.maxTiles = tw * th * 8;
    params.maxObstacles = 128;
    NavMeshParams navMeshParams = new NavMeshParams();
    copy(navMeshParams.orig, geom.getMeshBoundsMin());
    navMeshParams.tileWidth = m_tileSize * m_cellSize;
    navMeshParams.tileHeight = m_tileSize * m_cellSize;
    navMeshParams.maxTiles = params.maxTiles;
    navMeshParams.maxPolys = 65535;
    NavMesh navMesh = new PNavMesh(navMeshParams, m_vertsPerPoly);
    TileCache tc = new TileCache(params, new TileCacheStorageParams(order, cCompatibility), navMesh,
                                 TileCacheCompressorFactory.get(cCompatibility), new TestTileCacheMeshProcess());
    return tc;
  }

  public int getTw() {
    return tw;
  }

  public void prep() {
  }

  protected static class TestTileCacheMeshProcess implements TileCacheMeshProcess {
    @Override public void process(NavMeshDataCreateParams params) {
      for (int i = 0; i < params.polyCount; ++i) {
        params.polyFlags[i] = 1;
      }
    }
  }
}
