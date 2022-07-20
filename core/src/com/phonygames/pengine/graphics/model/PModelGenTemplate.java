package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringUtils;

import lombok.val;

/**
 * A template model that can be copied into a modelGen.
 *
 * Material ID's should be named as follows:
 * id is separated by periods. Options are:
 * alphaBlend: the part will generate a unique alphablend part.
 * alsoStaticBody will cause this part to generate both a normal and static body.
 * onlyStaticBody will cause this part to only affect physics.
 */
public class PModelGenTemplate {
  private static final PMap<String, PModelGenTemplate> staticTemplates = new PMap<>();
  public final PList<Boolean> emitMesh = new PList<>();
  public final PList<Boolean> emitPhysics = new PList<>();
  public final PList<Boolean> isAlphaBlend = new PList<>();
  public final PList<PMesh> meshes = new PList<>();
  public final PMap<PMesh, PList<Triangle>> triangleMap = new PMap<PMesh, PList<Triangle>>() {
    @Override public PList<Triangle> newUnpooled(PMesh mesh) {
      return new PList<>();
    }
  };
  public final PList<Integer> vColIndexBaseOffsets = new PList<>();
  public final PList<Integer> vColIndexOffsets = new PList<>();

  private PModelGenTemplate(String name) {
    PModel model = PAssetManager.model(name, true);
    try (val it = model.glNodes().obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        PMesh mesh = e.v().drawCall().mesh();
        meshes.add(mesh);
        String materialId = e.v().drawCall().material().id();
        // Determine the vCol index from the material.
        String vColIBaseString = PStringUtils.extract(materialId, ".vColBase", ".", true);
        vColIndexBaseOffsets.add(vColIBaseString == null ? -1 : Integer.parseInt(vColIBaseString));
        if (vColIBaseString == null) {
          String vColIString = PStringUtils.extract(materialId, ".vCol", ".", true);
          vColIndexOffsets.add(vColIString == null ? -1 : Integer.parseInt(vColIString));
        } else {
          vColIndexOffsets.add(-1);
        }
        isAlphaBlend.add(materialId.contains(".alphaBlend"));
        if (materialId.contains(".alsoStaticBody")) {
          emitPhysics.add(true);
          emitMesh.add(true);
        } else if (materialId.contains(".onlyStaticBody")) {
          emitPhysics.add(true);
          emitMesh.add(false);
        } else {
          emitPhysics.add(false);
          emitMesh.add(true);
        }
        short[] meshShorts = mesh.getBackingMeshShorts();
        for (int a = 0; a < meshShorts.length; a += 3) {
          triangleMap.genUnpooled(mesh)
                     .add(new Triangle(mesh, meshShorts[a + 0], meshShorts[a + 1], meshShorts[a + 2]));
        }
      }
    }
  }

  public static PModelGenTemplate get(String name) {
    if (staticTemplates.has(name)) {
      return staticTemplates.get(name);
    }
    PModelGenTemplate ret = new PModelGenTemplate(name);
    staticTemplates.put(name, ret);
    return ret;
  }

  /**
   * @param modelGen
   * @param options
   * @param basePart
   * @param staticPhysicsPart
   * @param vColIndexOffset
   */
  public void emit(PModelGen modelGen, PModelGenTemplateOptions options, PModelGen.Part basePart,
                   PModelGen.StaticPhysicsPart staticPhysicsPart, int vColIndexOffset,
                   PList<PModelGen.Part> alphaBlendParts) {
    for (int a = 0; a < this.meshes.size(); a++) {
      PList<Triangle> meshTriangles = triangleMap.get(meshes.get(a));
      boolean emitMesh = this.emitMesh.get(a);
      boolean emitPhysics = this.emitPhysics.get(a);
      if (emitMesh) {
        boolean isAlphaBlend = this.isAlphaBlend.get(a);
        int vColOffset = this.vColIndexOffsets.get(a);
        int vColBaseOffset = this.vColIndexBaseOffsets.get(a);
        int vColIndex = vColOffset == -1 ? (vColBaseOffset == -1 ? 0 : vColBaseOffset) : (vColIndexOffset + vColOffset);
        PModelGen.Part part = basePart;
        if (isAlphaBlend) {
          part = modelGen.addPart(basePart.name() + ".alphaBlend" + ".id" + a + "_" + vColIndexOffset + "",
                                  basePart.vertexAttributes());
          alphaBlendParts.add(part);
        }
        for (int b = 0; b < meshTriangles.size(); b++) {
          meshTriangles.get(b).emit(options, part, emitPhysics ? staticPhysicsPart : null, vColIndex);
        }
      } else {
        for (int b = 0; b < meshTriangles.size(); b++) {
          meshTriangles.get(b).emit(options, null, staticPhysicsPart, 0);
        }
      }
    }
  }

  private static class Triangle {
    final boolean hasNormal;
    final short index0, index1, index2;
    final PMesh mesh;
    final PVec3 nor[] = new PVec3[3];
    final PVec3 pos[] = new PVec3[3];
    private final short[] indices = new short[3];
    //    public final PList<Boolean> emitMesh = new PList<>();
    //    public final PList<Boolean> emitPhysics = new PList<>();
    //    public final PList<Boolean> isAlphaBlend = new PList<>();
    //    public final PList<PMesh> meshes = new PList<>();
    //    public final PList<Integer> vColIndexBaseOffsets = new PList<>();
    //    public final PList<Integer> vColIndexOffsets = new PList<>();

    public Triangle(PMesh mesh, short index0, short index1, short index2) {
      this.mesh = mesh;
      this.index0 = index0;
      this.index1 = index1;
      this.index2 = index2;
      indices[0] = index0;
      indices[1] = index1;
      indices[2] = index2;
      float[] meshFloats = mesh.getBackingMeshFloats();
      int idxOForPos = mesh.vertexAttributes().indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
      int idxOForNor = mesh.vertexAttributes().indexForVertexAttribute(PVertexAttributes.Attribute.Keys.nor);
      int fsPerVertex = mesh.vertexAttributes().getNumFloatsPerVertex();
      pos[0] = PVec3.obtain().set(meshFloats[index0 * fsPerVertex + idxOForPos + 0],
                                  meshFloats[index0 * fsPerVertex + idxOForPos + 1],
                                  meshFloats[index0 * fsPerVertex + idxOForPos + 2]);
      pos[1] = PVec3.obtain().set(meshFloats[index1 * fsPerVertex + idxOForPos + 0],
                                  meshFloats[index1 * fsPerVertex + idxOForPos + 1],
                                  meshFloats[index1 * fsPerVertex + idxOForPos + 2]);
      pos[2] = PVec3.obtain().set(meshFloats[index2 * fsPerVertex + idxOForPos + 0],
                                  meshFloats[index2 * fsPerVertex + idxOForPos + 1],
                                  meshFloats[index2 * fsPerVertex + idxOForPos + 2]);
      if (idxOForNor != -1) {
        nor[0] = PVec3.obtain().set(meshFloats[index0 * fsPerVertex + idxOForNor + 0],
                                    meshFloats[index0 * fsPerVertex + idxOForNor + 1],
                                    meshFloats[index0 * fsPerVertex + idxOForNor + 2]);
        nor[1] = PVec3.obtain().set(meshFloats[index1 * fsPerVertex + idxOForNor + 0],
                                    meshFloats[index1 * fsPerVertex + idxOForNor + 1],
                                    meshFloats[index1 * fsPerVertex + idxOForNor + 2]);
        nor[2] = PVec3.obtain().set(meshFloats[index2 * fsPerVertex + idxOForNor],
                                    meshFloats[index2 * fsPerVertex + idxOForNor + 1],
                                    meshFloats[index2 * fsPerVertex + idxOForNor + 2]);
        hasNormal = true;
      } else {
        hasNormal = false;
      }
    }

    /**
     * @param options
     * @param part              either the main part, or the alpha blend part.
     * @param staticPhysicsPart
     * @param vColIndex         the *raw* vCol index. If -1, then no overriding will occur.
     */
    public void emit(PModelGenTemplateOptions options, PModelGen.Part part,
                     PModelGen.StaticPhysicsPart staticPhysicsPart, int vColIndex) {
      boolean partHasCol0 =
          part == null ? false : part.vertexAttributes().hasAttributeWithName(PVertexAttributes.Attribute.Keys.col[0]);
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        if (part != null) {
          for (int a = 0; a < 3; a++) {
            short index = indices[a];
            // Process position, normal, and vCol separately.
            PVec3 transformedPos = options.processPosition(pool.vec3(pos[a]));
            part.set(PVertexAttributes.Attribute.Keys.pos, transformedPos);
            if (hasNormal) {
              PVec3 transformedNor = options.processNormal(pos[a], pool.vec3(nor[a]));
              part.set(PVertexAttributes.Attribute.Keys.nor, transformedNor);
            }
            if (vColIndex != -1 && partHasCol0) {
              part.set(PVertexAttributes.Attribute.Keys.col[0], PMesh.vColForIndex(pool.vec4(), vColIndex));
            }
            // For each vertex, loop through the vertex attributes and emit accordingly.
            for (int b = 0; b < mesh.vertexAttributes().getBackingVertexAttributes().size(); b++) {
              VertexAttribute va = mesh.vertexAttributes().getBackingVertexAttributes().get(b);
              if (va.alias.equals(PVertexAttributes.Attribute.Keys.pos)) {
                // Pos. Skip.
              } else if (va.alias.equals(PVertexAttributes.Attribute.Keys.nor)) {
                // Nor. Skip.
              } else if (va.alias.equals(PVertexAttributes.Attribute.Keys.col[0]) && vColIndex != -1) {
                // Col 0 was overriden so skip.
              } else {
                // Just emit the info like normal.
                int fPerV = mesh.vertexAttributes().getNumFloatsPerVertex();
                int offsetI = mesh.vertexAttributes().indexForVertexAttribute(va.alias);
                switch (va.getSizeInBytes() / 4) {
                  case 4:
                    part.set(va.alias, mesh.getBackingMeshFloats()[index * fPerV + offsetI + 0],
                             mesh.getBackingMeshFloats()[index * fPerV + offsetI + 1],
                             mesh.getBackingMeshFloats()[index * fPerV + offsetI + 2],
                             mesh.getBackingMeshFloats()[index * fPerV + offsetI + 3]);
                    break;
                  case 3:
                    part.set(va.alias, mesh.getBackingMeshFloats()[index * fPerV + offsetI + 0],
                             mesh.getBackingMeshFloats()[index * fPerV + offsetI + 1],
                             mesh.getBackingMeshFloats()[index * fPerV + offsetI + 2]);
                    break;
                  case 2:
                    part.set(va.alias, mesh.getBackingMeshFloats()[index * fPerV + offsetI + 0],
                             mesh.getBackingMeshFloats()[index * fPerV + offsetI + 1]);
                    break;
                  case 1:
                    part.set(va.alias, mesh.getBackingMeshFloats()[index * fPerV + offsetI + 0]);
                    break;
                }
              }
            }
            part.emitVertex();
          }
          part.tri(false);
        }
        if (staticPhysicsPart != null) {
          staticPhysicsPart.addTri(options.processPosition(pool.vec3(pos[0])),
                                   options.processPosition(pool.vec3(pos[1])),
                                   options.processPosition(pool.vec3(pos[2])));
        }
      }
    }
  }
}
