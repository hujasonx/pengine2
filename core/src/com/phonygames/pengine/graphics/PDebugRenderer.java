package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.Getter;
import lombok.Setter;

public class PDebugRenderer {
  private static final PList<Line> queuedLines = new PList<>(new PPool<Line>() {
    @Override protected Line newObject() {
      return new Line();
    }
  });
  private static final Vector3 tmpV3_1 = new Vector3(), tmpV3_2 = new Vector3();
  private static float[] verts = null;

  public static void clear() {
    queuedLines.clearAndFreePooled();
  }

  private static void lazyInit() {
    if (verts != null) {return;}
    verts = new float[20];
  }

  public static void line(PVec3 a, PVec3 b, PVec4 colA, PVec4 colB, float widthA, float widthB) {
    Line line = queuedLines.genPooledAndAdd();
    line.a.set(a);
    line.b.set(b);
    line.colA.set(colA);
    line.colB.set(colB);
    line.widthA.set(widthA);
    line.widthB.set(widthB);
  }

  public static void line(PVec3 a, PVec3 b, PVec4 col, float width) {
    Line line = queuedLines.genPooledAndAdd();
    line.a.set(a);
    line.b.set(b);
    line.colA.set(col);
    line.colB.set(col);
    line.widthA.set(width);
    line.widthB.set(width);
  }

  public static void render(PRenderContext renderContext) {
    for (int a = 0; a < queuedLines.size(); a++) {
      Line line = queuedLines.get(a);
      line.render(renderContext);
    }
  }

  private static class Line implements PPool.Poolable {
    // #pragma mark - PPool.Poolable
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    // #pragma end - PPool.Poolable
    final PVec3 a = PVec3.obtain(), b = PVec3.obtain();
    final PVec4 colA = PVec4.obtain(), colB = PVec4.obtain();
    final PVec1 widthA = PVec1.obtain(), widthB = PVec1.obtain();

    public void render(PRenderContext renderContext) {
      PVec3 aOut = PVec3.obtain().set(a);
      PVec3 bOut = PVec3.obtain().set(b);
      PApplicationWindow.windowSpriteBatch().begin();
      if (renderContext.projectIf(aOut) && renderContext.projectIf(bOut)) {
        render2d(aOut.x(), aOut.y(), bOut.x(), bOut.y(), widthA.x(), widthB.x(), colA, colB);
      }
      PApplicationWindow.windowSpriteBatch().end();
      aOut.free();
      bOut.free();
    }

    private void render2d(float x1, float y1, float x2, float y2, float lineWidth1, float lineWidth2, PVec4 c1,
                          PVec4 c2) {
      lazyInit();
      SpriteBatch spriteBatch = PApplicationWindow.windowSpriteBatch();
      float xdif = x2 - x1;
      float ydif = y2 - y1;
      float l2 = xdif * xdif + ydif * ydif;
      float invl1 = (float) (lineWidth1 / Math.sqrt(l2)) * .5f;
      float invl2 = (float) (lineWidth2 / Math.sqrt(l2)) * .5f;
      float xdif1 = xdif * invl1;
      float xdif2 = xdif * invl2;
      float ydif1 = ydif * invl1;
      float ydif2 = ydif * invl2;
      float floatBits1 = c1.toFloatBits();
      float floatBits2 = c2.toFloatBits();
      verts[0] = x1 + ydif1;
      verts[1] = y1 - xdif1;
      verts[2] = floatBits1;
      verts[5] = x1 - ydif1;
      verts[6] = y1 + xdif1;
      verts[7] = floatBits1;
      verts[10] = x2 - ydif2;
      verts[11] = y2 + xdif2;
      verts[12] = floatBits2;
      verts[15] = x2 + ydif2;
      verts[16] = y2 - xdif2;
      verts[17] = floatBits2;
      spriteBatch.draw(PFloat4Texture.getWHITE_PIXEL(), verts, 0, 20);
    }

    @Override public void reset() {
      a.setZero();
      b.setZero();
      colA.set(1, 1, 1, 1);
      colB.set(1, 1, 1, 1);
      widthA.set(1);
      widthB.set(1);
    }
  }
}
