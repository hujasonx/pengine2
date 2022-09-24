package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Helper class for emitting PModelGenTemplates. */
public class PModelGenTemplateOptionsOld implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PModelGenTemplateOptionsOld> staticPool = new PPool<PModelGenTemplateOptionsOld>() {
    @Override protected PModelGenTemplateOptionsOld newObject() {
      return new PModelGenTemplateOptionsOld();
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 wallPos0 = PVec3.obtain(), wallPos1 = PVec3.obtain(), flatQuad00 = PVec3.obtain(), flatQuad10 = PVec3.obtain(), flatQuad11 =
      PVec3.obtain(), flatQuad01 = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec1 wallHeight0 = PVec1.obtain(), wallHeight1 = PVec1.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private Type type = Type.DEFAULT;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PMat4 defaultTransform = PMat4.obtain();


  public static PModelGenTemplateOptionsOld obtainDefault() {
    PModelGenTemplateOptionsOld options = staticPool.obtain();
    options.type = Type.DEFAULT;
    return options;
  }

  public static PModelGenTemplateOptionsOld obtainFlatQuad() {
    PModelGenTemplateOptionsOld options = staticPool.obtain();
    options.type = Type.FLATQUAD;
    return options;
  }

  public static PModelGenTemplateOptionsOld obtainWall() {
    PModelGenTemplateOptionsOld options = staticPool.obtain();
    options.type = Type.WALL;
    return options;
  }

  @Override public void reset() {
    type = Type.DEFAULT;
    wallPos0.setZero();
    wallPos1.setZero();
    wallHeight0.setZero();
    wallHeight1.setZero();
    flatQuad00.setZero();
    flatQuad10.setZero();
    flatQuad11.setZero();
    flatQuad01.setZero();
  }

  public enum Type {
    DEFAULT, WALL, FLATQUAD
  }

  public PVec3 processPosition(PVec3 inout) {
    switch (type) {
      case WALL:
        return processPositionForWall(inout);
      case FLATQUAD:
        return processPositionForFlatQuad(inout);
      case DEFAULT:
        return inout.mul(defaultTransform, 1);
    }
    PAssert.fail("Should not reach!");
    return inout;
  }

  public PVec3 processNormal(PVec3 rawPos, PVec3 inout) {
    switch (type) {
      case WALL:
        return processNormalForWall(rawPos, inout);
      case FLATQUAD:
        return processNormalForFlatQuad(rawPos, inout);
      case DEFAULT:
        return inout.mul(defaultTransform, 0);
    }
    PAssert.fail("Should not reach!");
    return inout;
  }

  private final PVec3 processPositionForWall(PVec3 out) {
    out.roundComponents(1000); // Round positions of the input vector to the nearest 1000 to improve vertex shader
    // precision.
    PPool.PoolBuffer pool = PPool.getBuffer();
    float wallDx = wallPos1.x() - wallPos0.x();
    float wallDz = wallPos1.z() - wallPos0.z();
    PVec3 flatWallDir = pool.vec3().set(wallDx, 0, wallDz); // Z forward.
    PVec3 flatWallLeftDirNor = pool.vec3().set(wallDz, 0, -wallDx).nor(); // X left.
    PVec3 wallCornerFlatA = pool.vec3().set(wallPos0.x(), 0, wallPos0.z());
    PVec3 wallCornerFlatB = pool.vec3().set(wallPos1.x(), 0, wallPos1.z());
    // Transform from the 1x1x1 template model to the desired coordinates.
    float x = wallPos0.x() + out.x() * flatWallLeftDirNor.x() + out.z() * flatWallDir.x();
    float z = wallPos0.z() + out.x() * flatWallLeftDirNor.z() + out.z() * flatWallDir.z();
    float lengthAlongLine = pool.vec3().set(x, 0, z).progressAlongLineSegment(wallCornerFlatA, wallCornerFlatB);
    float y = out.y() * ((wallHeight1.x() - wallHeight0.x()) * lengthAlongLine + wallHeight0.x()) +
              ((wallPos1.y() - wallPos0.y()) * lengthAlongLine + wallPos0.y());
    pool.free();
    return out.set(x, y, z);
  }

  private final PVec3 processNormalForWall(PVec3 rawPos, PVec3 out) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    // Calculate the x and z axes using the wall angle, and y axis depending on the raw Y and Z position.
    PVec3 pos0Bottom = pool.vec3(wallPos0);
    PVec3 pos0Top = pool.vec3(wallPos0);
    pos0Top.y(pos0Top.y() + wallHeight0.x());
    PVec3 pos1Bottom = pool.vec3(wallPos1);
    PVec3 pos1Top = pool.vec3(wallPos1);
    pos1Top.y(pos1Top.y() + wallHeight1.x());
    PVec3 flatDir = pool.vec3().set(pos1Bottom).sub(pos0Bottom).y(0);
    PVec3 flatDirNor = pool.vec3().set(flatDir).nor();
    PVec3 flatLeftNor = pool.vec3().set(flatDir.z(), 0, -flatDir.x());
    PVec3 yNor0 = pool.vec3().set(pos1Bottom).sub(pos0Bottom).rotate(flatLeftNor, -MathUtils.HALF_PI);
    PVec3 yNor1 = pool.vec3().set(pos1Top).sub(pos0Top).rotate(flatLeftNor, -MathUtils.HALF_PI);
    float bottomYForRawZ = pos0Bottom.y() + rawPos.z() * (pos1Bottom.y() - pos0Bottom.y());
    float topYForRawZ = pos0Top.y() + rawPos.z() * (pos1Top.y() - pos0Top.y());
    float yNorMixAmount = bottomYForRawZ + (topYForRawZ - bottomYForRawZ) * rawPos.y();
    PVec3 xNor = flatLeftNor;
    PVec3 yNor = pool.vec3().set(yNor0).lerp(yNor1, yNorMixAmount);
    PVec3 zNor = flatDirNor;
    float x = xNor.x() * out.x() + yNor.x() * out.y() + zNor.x() * out.z();
    float y = xNor.y() * out.x() + yNor.y() * out.y() + zNor.y() * out.z();
    float z = xNor.z() * out.x() + yNor.z() * out.y() + zNor.z() * out.z();
    pool.free();
    return out.set(x, y, z).nor();
  }

  private final PVec3 processPositionForFlatQuad(PVec3 out) {
    out.roundComponents(1000);
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 lerpPosX0 = pool.vec3().set(flatQuad00).lerp(flatQuad01, out.z());
    PVec3 lerpPosX1 = pool.vec3().set(flatQuad10).lerp(flatQuad11, out.z());
    PVec3 lerpPosXZ = pool.vec3().set(lerpPosX0).lerp(lerpPosX1, out.x());
    float outY = lerpPosXZ.y() + out.y();
    out.set(lerpPosXZ.x(), outY, lerpPosXZ.z());
    pool.free();
    return out;
  }

  private final PVec3 processNormalForFlatQuad(PVec3 rawPos, PVec3 out) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    // Calculate the x and z axes at the edges of the 1x1 quad (using the normal of the quad's edge)
    PVec3 xAtX0 = pool.vec3().set(flatQuad01.z() - flatQuad00.z(), 0, flatQuad00.x() - flatQuad01.x());
    PVec3 xAtX1 = pool.vec3().set(flatQuad11.z() - flatQuad10.z(), 0, flatQuad10.x() - flatQuad11.x());
    PVec3 zAtZ0 = pool.vec3().set(flatQuad00.z() - flatQuad10.z(), 0, flatQuad10.x() - flatQuad00.x());
    PVec3 zAtZ1 = pool.vec3().set(flatQuad01.z() - flatQuad11.z(), 0, flatQuad11.x() - flatQuad01.x());
    PVec3 yNor0 =
        pool.vec3().set(flatQuad11).sub(flatQuad00).crs(pool.vec3().set(flatQuad10).sub(flatQuad00));
    PVec3 yNor1 =
        pool.vec3().set(flatQuad01).sub(flatQuad00).crs(pool.vec3().set(flatQuad11).sub(flatQuad00));
    PVec3 xNor = pool.vec3().set(xAtX0).lerp(xAtX1, rawPos.x());
    PVec3 yNor = pool.vec3().set(yNor0).lerp(yNor1, 0.5f); // Average of the normals for triangles 012 and 023.
    PVec3 zNor = pool.vec3().set(zAtZ0).lerp(zAtZ1, rawPos.z());
    float x = xNor.x() * out.x() + yNor.x() * out.y() + zNor.x() * out.z();
    float y = xNor.y() * out.x() + yNor.y() * out.y() + zNor.y() * out.z();
    float z = xNor.z() * out.x() + yNor.z() * out.y() + zNor.z() * out.z();
    pool.free();
    return out.set(x, y, z).nor();
  }

  public PModelGenTemplateOptionsOld setFlatQuad(PVec3 v00, PVec3 v10, PVec3 v11, PVec3 v01) {
    this.flatQuad00().set(v00);
    this.flatQuad10().set(v10);
    this.flatQuad11().set(v11);
    this.flatQuad01().set(v01);
    type = Type.FLATQUAD;
    return this;
  }

  public PModelGenTemplateOptionsOld setFlatQuad(float x00, float y00, float z00, float x10, float y10, float z10, float x11,
                                                 float y11, float z11, float x01, float y01, float z01) {
    this.flatQuad00().set(x00, y00, z00);
    this.flatQuad10().set(x10, y10, z10);
    this.flatQuad11().set(x11, y11, z11);
    this.flatQuad01().set(x01, y01, z01);
    type = Type.FLATQUAD;
    return this;
  }

  public PModelGenTemplateOptionsOld setTransform(PMat4 mat4) {
    this.defaultTransform.set(mat4);
    type = Type.DEFAULT;
    return this;
  }

  public PModelGenTemplateOptionsOld setWall(PVec3 v0, float height0, PVec3 v1, float height1) {
    this.wallPos0.set(v0.x(), v0.y(), v0.z());
    this.wallHeight0.set(height0);
    this.wallPos1.set(v1.x(), v1.y(), v1.z());
    this.wallHeight1.set(height1);
    type = Type.WALL;
    return this;
  }

  public PModelGenTemplateOptionsOld setWall(float x0, float y0, float z0, float height0, float x1, float y1, float z1,
                                             float height1) {
    this.wallPos0.set(x0, y0, z0);
    this.wallHeight0.set(height0);
    this.wallPos1.set(x1, y1, z1);
    this.wallHeight1.set(height1);
    type = Type.WALL;
    return this;
  }
}
