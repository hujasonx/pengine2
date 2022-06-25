package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PPostableTask;
import com.phonygames.pengine.util.PPostableTaskQueue;
import com.phonygames.pengine.util.PStringMap;
import com.phonygames.pengine.util.PWindowedBuffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PModelGen implements PPostableTask {
  protected static final PList<PGlNode> tempGlNodesBuffer = new PList<>();
  @Getter
  private static final PPostableTaskQueue postableTaskQueue = new PPostableTaskQueue();
  private final PStringMap<Part> parts = new PStringMap<>();
  private final PStringMap<StaticPhysicsPart> staticPhysicsParts = new PStringMap<>();
  private boolean finished = false;

  public Part addPbrPart(String name) {
    return addPart(name, PVertexAttributes.getGLTF_UNSKINNED());
  }

  public Part addPart(String name, PVertexAttributes vertexAttributes) {
    Part p = new Part(name, vertexAttributes);
    parts.put(name, p);
    return p;
  }

  public StaticPhysicsPart addStaticPhysicsPart(String name) {
    StaticPhysicsPart p = new StaticPhysicsPart(name);
    staticPhysicsParts.put(name, p);
    return p;
  }

  public void buildSynchronous() {
    intro();
    middle();
    end();
  }

  @Override public void end() {
    modelEnd();
  }

  @Override public void intro() {
    modelIntro();
  }

  @Override public void middle() {
    modelMiddle();
  }

  protected void modelIntro() {
  }

  protected void modelMiddle() {
  }

  protected void modelEnd() {
  }

  protected PModelGen chainGlNode(@Nullable PList<PGlNode> list, @NonNull Part part, @NonNull PMaterial defaultMaterial,
                                  @Nullable ArrayMap<String, PMat4> boneInvBindTransforms, @NonNull String layer) {
    if (list == null) {
      list = new PList<>();
    }
    val glNode = new PGlNode(part.name);
    glNode.drawCall().setMesh(part.getMesh());
    glNode.drawCall().setMaterial(defaultMaterial);
    glNode.drawCall().setLayer(layer);
    if (boneInvBindTransforms != null) {
      glNode.invBoneTransforms().putAll(boneInvBindTransforms);
    }
    list.add(glNode);
    return this;
  }

  public void emitStaticPhysicsPartIntoModelBuilder(PModel.Builder builder) {
    for (val e : staticPhysicsParts) {
      if (e.v().indices().isEmpty()) {continue;}
      btBvhTriangleMeshShape triangleMeshShape = e.v().getTriangleMeshShape();
      PPhysicsCollisionShape<btBvhTriangleMeshShape> collisionShape =
          new PPhysicsCollisionShape<btBvhTriangleMeshShape>(triangleMeshShape) {};
      builder.model.staticCollisionShapes().put(e.k(), collisionShape);
    }
  }

  public static class Part {
    @Getter(value = AccessLevel.PROTECTED)
    @Accessors(fluent = true)
    private final float[] currentVertexValues;
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Short> indices = new PList<>();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PWindowedBuffer latestIndices = new PWindowedBuffer(4);
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final String name;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVertexAttributes vertexAttributes;
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Float> vertices = new PList<>();
    @Getter
    private int numVertices = 0;

    private Part(@NonNull String name, @NonNull PVertexAttributes vertexAttributes) {
      this.name = name;
      this.vertexAttributes = vertexAttributes;
      currentVertexValues = new float[vertexAttributes.getNumFloatsPerVertex()];
    }

    public Part clear() {
      vertices().clear();
      indices().clear();
      numVertices = 0;
      return this;
    }

    public Part emit(@NonNull PMesh mesh, @Nullable StaticPhysicsPart staticPhysicsPart, VertexProcessor processor,
                     @NonNull PVertexAttributes attributesToCopy) {
      @NonNull final float[] meshVerts = mesh.getBackingMeshFloats();
      @NonNull final short[] meshShorts = mesh.getBackingMeshShorts();
      int meshFloatsPerVert = mesh.vertexAttributes().getNumFloatsPerVertex();
      for (int meshVIndex = 0; meshVIndex < meshShorts.length; meshVIndex++) { // Loop through all vertices in the mesh.
        // Get the raw position.
        int posLookupI = meshShorts[meshVIndex] * meshFloatsPerVert +
                         vertexAttributes().indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
        float rawPosX = meshVerts[posLookupI + 0];
        float rawPosY = meshVerts[posLookupI + 1];
        float rawPosZ = meshVerts[posLookupI + 2];
        // Loop through all the vertex attributes and ensure they are valid.
        for (VertexAttribute vertexAttribute : mesh.vertexAttributes().getBackingVertexAttributes()) {
          if (attributesToCopy.hasAttributeWithName(vertexAttribute.alias) &&
              this.vertexAttributes.hasAttributeWithName(vertexAttribute.alias)) {
            PAssert.isFalse(vertexAttribute.usage == VertexAttribute.ColorUnpacked().usage);
            // Process the attribute data.
            int lookupIndexStart = meshShorts[meshVIndex] * meshFloatsPerVert +
                                   mesh.vertexAttributes().indexForVertexAttribute(vertexAttribute);
            switch (vertexAttribute.numComponents) {
              case 1:
                set(vertexAttribute.alias, meshVerts[lookupIndexStart]);
                break;
              case 2:
                set(vertexAttribute.alias, meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1]);
                break;
              // TODO: transform.
              case 3:
                PVec3 tempResult = PVec3.obtain().set(meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1],
                                                      meshVerts[lookupIndexStart + 2]);
                processor.process(rawPosX, rawPosY, rawPosZ, vertexAttribute, tempResult);
                set(vertexAttribute.alias, tempResult);
                tempResult.free();
                break;
              case 4:
                set(vertexAttribute.alias, meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1],
                    meshVerts[lookupIndexStart + 2], meshVerts[lookupIndexStart + 3]);
                break;
              default:
                PAssert.fail(
                    "Invalid vertexAttribute size: " + vertexAttribute.numComponents + " for " + vertexAttribute.alias);
            }
          }
        }
        emitVertex();
        if (meshVIndex % 3 == 2) { // Emit the triangle.
          tri(false, staticPhysicsPart);
        }
      }
      return this;
    }

    public Part set(String alias, float x) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 1);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      return this;
    }

    public Part set(String alias, float x, float y) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      currentVertexValues()[ind + 1] = y;
      return this;
    }

    public Part set(String alias, PVec3 vec) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
            currentVertexValues()[ind + 0] = vec.x();
            currentVertexValues()[ind + 1] = vec.y();
            currentVertexValues()[ind + 2] = vec.z();
      return this;
    }

    public Part set(String alias, float x, float y, float z, float w) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      currentVertexValues()[ind + 1] = y;
      currentVertexValues()[ind + 2] = z;
      currentVertexValues()[ind + 3] = w;
      return this;
    }

    public Part emitVertex() {
      return emitVertex(Integer.MAX_VALUE);
    }

    public Part tri(boolean flip, @Nullable StaticPhysicsPart copyTo) {
      indices().add((short) latestIndices().get(2));
      indices().add((short) latestIndices().get(flip ? 0 : 1));
      indices().add((short) latestIndices().get(flip ? 1 : 0));
      if (copyTo != null) {
        copyTo.copyLastTri(this);
      }
      return this;
    }

    public Part emitVertex(int maxLookbackAmount) {
      short index = (short) numVertices;
      for (int lookbackAmount = 1; lookbackAmount < maxLookbackAmount; lookbackAmount++) {
        int lookbackIndex = index - lookbackAmount;
        if (lookbackIndex < 0) {
          break;
        }
        int verticesIndex = lookbackIndex * vertexAttributes().getNumFloatsPerVertex();
        boolean isEqual = true;
        for (int a = 0; a < vertexAttributes().getNumFloatsPerVertex(); a++) {
          if (!PNumberUtils.epsilonEquals(vertices().get(verticesIndex + a), currentVertexValues()[a])) {
            isEqual = false;
            break;
          }
        }
        if (isEqual) {
          index = (short) lookbackIndex;
          break;
        }
      }
      // Need to add a new vertex.
      if (index == numVertices) {
        numVertices++;
        for (int a = 0; a < vertexAttributes().getNumFloatsPerVertex(); a++) {
          vertices().add(currentVertexValues()[a]);
        }
      }
      latestIndices().addInt(index);
      return this;
    }

    public PVec2 get(PVec2 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1);
    }

    public PVec3 get(PVec3 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1).z(ind + 2);
    }

    public PVec4 get(PVec4 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1).z(ind + 2).w(ind + 3);
    }

    public PMesh getMesh() {
      PMesh ret = new PMesh(true, vertices(), indices(), vertexAttributes());
      return ret;
    }

    public Part quad(boolean flip) {
      return quad(flip, null);
    }

    public Part quad(boolean flip, @Nullable StaticPhysicsPart copyTo) {
      indices().add((short) latestIndices().get(3));
      indices().add((short) latestIndices().get(flip ? 1 : 2));
      indices().add((short) latestIndices().get(flip ? 2 : 1));
      if (copyTo != null) {
        copyTo.copyLastTri(this);
      }
      indices().add((short) latestIndices().get(3));
      indices().add((short) latestIndices().get(flip ? 0 : 1));
      indices().add((short) latestIndices().get(flip ? 1 : 0));
      if (copyTo != null) {
        copyTo.copyLastTri(this);
      }
      return this;
    }

    public Part set(String alias, float x, float y, float z) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      currentVertexValues()[ind + 1] = y;
      currentVertexValues()[ind + 2] = z;
      return this;
    }

    public Part set(String alias, PVec2 vec) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = vec.x();
      currentVertexValues()[ind + 1] = vec.y();
      return this;
    }

    public Part set(String alias, PVec4 vec) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = vec.x();
      currentVertexValues()[ind + 1] = vec.y();
      currentVertexValues()[ind + 2] = vec.z();
      currentVertexValues()[ind + 3] = vec.w();
      return this;
    }

    public Part tri(boolean flip) {
      return tri(flip, null);
    }

    public static class VertexProcessor implements PPool.Poolable {
      @Getter(value = AccessLevel.PUBLIC, lazy = true)
      @Accessors(fluent = true)
      private static final PPool<VertexProcessor> staticPool = new PPool<VertexProcessor>() {
        @Override protected VertexProcessor newObject() {
          return new VertexProcessor();
        }
      };
      @Getter(value = AccessLevel.PRIVATE, lazy = true)
      @Accessors(fluent = true)
      private final PVec3 flatQuad00 = PVec3.obtain(), flatQuad01 = PVec3.obtain(), flatQuad11 = PVec3.obtain(),
          flatQuad10 = PVec3.obtain();
      @Getter(value = AccessLevel.PRIVATE, lazy = true)
      @Accessors(fluent = true)
      private final PMat4 transform = PMat4.obtain();
      @Getter(value = AccessLevel.PRIVATE, lazy = true)
      @Accessors(fluent = true)
      private final PVec4 wallCornerA = PVec4.obtain(), wallCornerB = PVec4.obtain();
      @Getter
      @Setter
      private PPool ownerPool;
      private Strategy strategy = Strategy.Transform;

      /**
       * Processes the vector, applying whatever transform using the strategy supplied.
       * @param attribute
       * @param out
       * @return
       */
      private final PVec3 process(float rawPosX, float rawPosY, float rawPosZ, @NonNull VertexAttribute attribute,
                                  @NonNull PVec3 out) {
        boolean isPos = attribute.alias.equals(PVertexAttributes.Attribute.Keys.pos);
        boolean isNor = attribute.alias.equals(PVertexAttributes.Attribute.Keys.nor);
        switch (strategy) {
          case Transform:
            if (isPos) {return out.mul(transform(), 1);}
            if (isNor) {return out.mul(transform(), 0);}
            return out;
          case Wall:
            if (isPos) {
              return processPositionForWall(out);
            }
            if (isNor) {
              return processNormalForWall(rawPosX, rawPosY, rawPosZ, out);
            }
            return out;
          case FlatQuad:
            if (isPos) {
              return processPositionForFlatQuad(out);
            }
            if (isNor) {
              return processNormalForFlatQuad(rawPosX, rawPosY, rawPosZ, out);
            }
            return out;
          default:
            PAssert.fail("No valid vertex processing strategy");
            return out;
        }
      }

      private final PVec3 processPositionForWall(PVec3 out) {
        out.roundComponents(1000); // Round positions of the input vector to the nearest 1000 to improve vertex shader
        // precision.
        PPool.PoolBuffer pool = PPool.getBuffer();
        float wallDx = wallCornerB().x() - wallCornerA().x();
        float wallDz = wallCornerB().z() - wallCornerA().z();
        PVec3 flatWallDir = pool.vec3().set(wallDx, 0, wallDz); // Z forward.
        PVec3 flatWallLeftDirNor = pool.vec3().set(wallDz, 0, -wallDx).nor(); // X left.
        PVec3 wallCornerFlatA = pool.vec3().set(wallCornerA().x(), 0, wallCornerA().z());
        PVec3 wallCornerFlatB = pool.vec3().set(wallCornerB().x(), 0, wallCornerB().z());
        // Transform from the 1x1x1 template model to the desired coordinates.
        float x = wallCornerA().x() + out.x() * flatWallLeftDirNor.x() + out.z() * flatWallDir.x();
        float z = wallCornerA().z() + out.x() * flatWallLeftDirNor.z() + out.z() * flatWallDir.z();
        float lengthAlongLine = pool.vec3().set(x, 0, z).progressAlongLineSegment(wallCornerFlatA, wallCornerFlatB);
        float y = out.y() * ((wallCornerB().w() - wallCornerA().w()) * lengthAlongLine + wallCornerA().w()) +
                  ((wallCornerB().y() - wallCornerA().y()) * lengthAlongLine + wallCornerA().y());
        pool.finish();
        return out.set(x, y, z);
      }

      private final PVec3 processNormalForWall(float rawPosX, float rawPosY, float rawPosZ, PVec3 out) {
        PPool.PoolBuffer pool = PPool.getBuffer();
        // Calculate the x and z axes using the wall angle, and y axis depending on the raw Y and Z position.
        PVec3 pos0Bottom = pool.vec3().setXYZ(wallCornerA());
        PVec3 pos0Top = pool.vec3().setXYZ(wallCornerA());
        pos0Top.y(pos0Top.y() + wallCornerA().w());
        PVec3 pos1Bottom = pool.vec3().setXYZ(wallCornerB());
        PVec3 pos1Top = pool.vec3().setXYZ(wallCornerB());
        pos1Top.y(pos1Top.y() + wallCornerB().w());
        PVec3 flatDir = pool.vec3().set(pos1Bottom).sub(pos0Bottom).y(0);
        PVec3 flatDirNor = pool.vec3().set(flatDir).nor();
        PVec3 flatLeftNor = pool.vec3().set(flatDir.z(), 0, -flatDir.x());
        PVec3 yNor0 = pool.vec3().set(pos1Bottom).sub(pos0Bottom).rotate(flatLeftNor, -MathUtils.HALF_PI);
        PVec3 yNor1 = pool.vec3().set(pos1Top).sub(pos0Top).rotate(flatLeftNor, -MathUtils.HALF_PI);
        float bottomYForRawZ = pos0Bottom.y() + rawPosZ * (pos1Bottom.y() - pos0Bottom.y());
        float topYForRawZ = pos0Top.y() + rawPosZ * (pos1Top.y() - pos0Top.y());
        float yNorMixAmount = bottomYForRawZ + (topYForRawZ - bottomYForRawZ) * rawPosY;
        PVec3 xNor = flatLeftNor;
        PVec3 yNor = pool.vec3().set(yNor0).lerp(yNor1, yNorMixAmount);
        PVec3 zNor = flatDirNor;
        float x = xNor.x() * out.x() + yNor.x() * out.y() + zNor.x() * out.z();
        float y = xNor.y() * out.x() + yNor.y() * out.y() + zNor.y() * out.z();
        float z = xNor.z() * out.x() + yNor.z() * out.y() + zNor.z() * out.z();
        pool.finish();
        return out.set(x, y, z).nor();
      }

      private final PVec3 processPositionForFlatQuad(PVec3 out) {
        out.roundComponents(1000);
        PPool.PoolBuffer pool = PPool.getBuffer();
        PVec3 lerpPosX0 = pool.vec3().set(flatQuad00()).lerp(flatQuad01(), out.z());
        PVec3 lerpPosX1 = pool.vec3().set(flatQuad10()).lerp(flatQuad11(), out.z());
        PVec3 lerpPosXZ = pool.vec3().set(lerpPosX0).lerp(lerpPosX1, out.x());
        float outY = lerpPosXZ.y() + out.y();
        out.set(lerpPosXZ.x(), outY, lerpPosXZ.z());
        pool.finish();
        return out;
      }

      private final PVec3 processNormalForFlatQuad(float rawPosX, float rawPosY, float rawPosZ, PVec3 out) {
        PPool.PoolBuffer pool = PPool.getBuffer();
        // Calculate the x and z axes at the edges of the 1x1 quad (using the normal of the quad's edge)
        PVec3 xAtX0 = pool.vec3().set(flatQuad01().z() - flatQuad00().z(), 0, flatQuad00().x() - flatQuad01().x());
        PVec3 xAtX1 = pool.vec3().set(flatQuad11().z() - flatQuad10().z(), 0, flatQuad10().x() - flatQuad11().x());
        PVec3 zAtZ0 = pool.vec3().set(flatQuad00().z() - flatQuad10().z(), 0, flatQuad10().x() - flatQuad00().x());
        PVec3 zAtZ1 = pool.vec3().set(flatQuad01().z() - flatQuad11().z(), 0, flatQuad11().x() - flatQuad01().x());
        PVec3 yNor0 =
            pool.vec3().set(flatQuad11()).sub(flatQuad00()).crs(pool.vec3().set(flatQuad10()).sub(flatQuad00()));
        PVec3 yNor1 =
            pool.vec3().set(flatQuad01()).sub(flatQuad00()).crs(pool.vec3().set(flatQuad11()).sub(flatQuad00()));
        PVec3 xNor = pool.vec3().set(xAtX0).lerp(xAtX1, rawPosX);
        PVec3 yNor = pool.vec3().set(yNor0).lerp(yNor1, 0.5f); // Average of the normals for triangles 012 and 023.
        PVec3 zNor = pool.vec3().set(zAtZ0).lerp(zAtZ1, rawPosY);
        float x = xNor.x() * out.x() + yNor.x() * out.y() + zNor.x() * out.z();
        float y = xNor.y() * out.x() + yNor.y() * out.y() + zNor.y() * out.z();
        float z = xNor.z() * out.x() + yNor.z() * out.y() + zNor.z() * out.z();
        pool.finish();
        return out.set(x, y, z).nor();
      }

      @Override public void reset() {
      }

      public VertexProcessor setFlatQuad(PVec3 v00, PVec3 v10, PVec3 v11, PVec3 v01) {
        this.flatQuad00().set(v00);
        this.flatQuad10().set(v10);
        this.flatQuad11().set(v11);
        this.flatQuad01().set(v01);
        strategy = Strategy.FlatQuad;
        return this;
      }

      public VertexProcessor setFlatQuad(float x00, float y00, float z00, float x10, float y10, float z10, float x11,
                                         float y11, float z11, float x01, float y01, float z01) {
        this.flatQuad00().set(x00, y00, z00);
        this.flatQuad10().set(x10, y10, z10);
        this.flatQuad11().set(x11, y11, z11);
        this.flatQuad01().set(x01, y01, z01);
        strategy = Strategy.FlatQuad;
        return this;
      }

      public VertexProcessor setTransform(PMat4 mat4) {
        this.transform().set(mat4);
        strategy = Strategy.Transform;
        return this;
      }

      public VertexProcessor setWall(float x0, float y0, float z0, float height0, float x1, float y1, float z1,
                                     float height1) {
        this.wallCornerA().set(x0, y0, z0, height0);
        this.wallCornerB().set(x1, y1, z1, height1);
        strategy = Strategy.Wall;
        return this;
      }

      enum Strategy {
        Transform, Wall, FlatQuad
      }
    }
  }

  public static class StaticPhysicsPart {
    private static final int MAX_SEARCH_DEPTH = 100;
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private static final PVertexAttributes vertexAttributes = PVertexAttributes.getPHYSICS();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Short> indices = new PList<>();
    @Getter(value = AccessLevel.PRIVATE)
    @Accessors(fluent = true)
    private final String name;
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Float> vertices = new PList<>();

    private StaticPhysicsPart(@NonNull String name) {
      this.name = name;
    }

    private StaticPhysicsPart copyLastTri(Part part) {
      int indexOfPosAttrInPartVertexFloatArray =
          part.vertexAttributes().indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
      int partFloatsPerVertex = part.vertexAttributes().getNumFloatsPerVertex();
      float x, y, z;
      x = part.vertices().get(partFloatsPerVertex * part.indices().get(-3) + indexOfPosAttrInPartVertexFloatArray + 0);
      y = part.vertices().get(partFloatsPerVertex * part.indices().get(-3) + indexOfPosAttrInPartVertexFloatArray + 1);
      z = part.vertices().get(partFloatsPerVertex * part.indices().get(-3) + indexOfPosAttrInPartVertexFloatArray + 2);
      indices().add(getIndexOrAdd(x, y, z, MAX_SEARCH_DEPTH));
      x = part.vertices().get(partFloatsPerVertex * part.indices().get(-2) + indexOfPosAttrInPartVertexFloatArray + 0);
      y = part.vertices().get(partFloatsPerVertex * part.indices().get(-2) + indexOfPosAttrInPartVertexFloatArray + 1);
      z = part.vertices().get(partFloatsPerVertex * part.indices().get(-2) + indexOfPosAttrInPartVertexFloatArray + 2);
      indices().add(getIndexOrAdd(x, y, z, MAX_SEARCH_DEPTH));
      x = part.vertices().get(partFloatsPerVertex * part.indices().get(-1) + indexOfPosAttrInPartVertexFloatArray + 0);
      y = part.vertices().get(partFloatsPerVertex * part.indices().get(-1) + indexOfPosAttrInPartVertexFloatArray + 1);
      z = part.vertices().get(partFloatsPerVertex * part.indices().get(-1) + indexOfPosAttrInPartVertexFloatArray + 2);
      indices().add(getIndexOrAdd(x, y, z, MAX_SEARCH_DEPTH));
      return this;
    }

    private StaticPhysicsPart addTri(PVec3 v0, PVec3 v1, PVec3 v2) {
      indices().add(getIndexOrAdd(v0.x(), v0.y(), v0.z(), MAX_SEARCH_DEPTH));
      indices().add(getIndexOrAdd(v1.x(), v1.y(), v1.z(), MAX_SEARCH_DEPTH));
      indices().add(getIndexOrAdd(v2.x(), v2.y(), v2.z(), MAX_SEARCH_DEPTH));
      return this;
    }

    private short getIndexOrAdd(final float x, final float y, final float z, final int maxSearchDepth) {
      if (!vertices().isEmpty()) {
        for (int a = 0; a < maxSearchDepth; a++) {
          int lookupIndex = vertices().size - 3 * (1 + a);
          if (!PNumberUtils.epsilonEquals(vertices().get(lookupIndex + 0), x)) {
            continue;
          }
          if (!PNumberUtils.epsilonEquals(vertices().get(lookupIndex + 1), y)) {
            continue;
          }
          if (!PNumberUtils.epsilonEquals(vertices().get(lookupIndex + 2), z)) {
            continue;
          }
          return (short) (lookupIndex / 3);
        }
      }
      // We reached the max search depth (or bottom) without finding a match for the position, so add new position.
      short ret = (short) (vertices().size / 3);
      vertices().add(x);
      vertices().add(y);
      vertices().add(z);
      return ret;
    }

    public btBvhTriangleMeshShape getTriangleMeshShape() {
      PMesh pMesh = new PMesh(true, vertices(), indices(), vertexAttributes());
      Mesh mesh = pMesh.getBackingMesh();
      ModelBuilder modelBuilder = new ModelBuilder();
      modelBuilder.begin();
      modelBuilder.part(name, mesh, GL20.GL_TRIANGLES, new Material());
      Model model = modelBuilder.end();
      btBvhTriangleMeshShape ret = new btBvhTriangleMeshShape(model.meshParts);
      return ret;
    }
  }
}
