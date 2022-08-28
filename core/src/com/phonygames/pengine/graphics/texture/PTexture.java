package com.phonygames.pengine.graphics.texture;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PDeepCopyable;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PTexture implements PPool.Poolable, PDeepCopyable<PTexture> {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private static final PTexture WHITE_PIXEL = new PTexture(PFloat4Texture.getWHITE_PIXEL());
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec4 uvOS = PVec4.obtain().set(0, 0, 1, 1);
  @Setter
  private Texture backingTexture;

  public PTexture() {
  }

  public PTexture(Texture texture) {
    set(texture);
  }

  public PTexture set(Texture texture) {
    this.backingTexture = texture;
    this.uvOS.set(0, 0, 1, 1);
    return this;
  }

  public void applyShaderWithUniform(String uniform, PShader shader) {
    shader.setWithUniform(uniform, getBackingTexture());
    shader.set(PStringUtils.concat(uniform, "UVOS"), uvOS);
  }

  public Texture getBackingTexture() {
    return backingTexture == null ? PFloat4Texture.getWHITE_PIXEL() : backingTexture;
  }

  private int bind() {
    return PRenderContext.activeContext().getTextureBinder().bind(getBackingTexture());
  }

  @Override public PTexture deepCopy() {
    return new PTexture().set(this);
  }

  @Override public PTexture deepCopyFrom(PTexture other) {
    return this.set(other);
  }

  public PTexture set(PTexture other) {
    set(other.backingTexture);
    return this;
  }

  public boolean has() {
    return backingTexture != null;
  }

  @Override public int hashCode() {
    if (backingTexture == null) {return 0;}
    return backingTexture.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof PTexture)) {
      return false;
    }
    val other = (PTexture) o;
    return other.backingTexture == backingTexture;
  }

  public String toString() {
    if (backingTexture == null) {
      return "[PTexture UNSET]";
    }
    return "[PTexture ref:" + (backingTexture.toString().split("@")[1]) + " " + width() + "x" + height() + ", uvOS: " +
           uvOS + "]";
  }

  public int width() {
    return getBackingTexture().getWidth();
  }

  public int height() {
    return getBackingTexture().getHeight();
  }

  @Override public void reset() {
    this.backingTexture = null;
    this.uvOS.set(0, 0, 1, 1);
  }

  /**
   * @param x [0, width]
   * @param y [0, height]
   * @param w [0, width]
   * @param h [0, height]
   */
  public void setRegionByPixel(int x, int y, int w, int h) {
    uvOS.set(((float) x) * backingTexture.getWidth(), ((float) y) * backingTexture.getHeight(),
             ((float) w) * backingTexture.getWidth(), ((float) h) * backingTexture.getHeight());
  }

  /**
   * @param x [0, 1]
   * @param y [0, 1]
   * @param w [0, 1]
   * @param h [0, 1]
   */
  public void setRegionByRatio(float x, float y, float w, float h) {
    uvOS.set(x, y, w, h);
  }

  public PTexture tryDeepCopy() {
    return new PTexture(backingTexture);
  }

  public PTexture set(TextureRegion region) {
    this.backingTexture = region.getTexture();
    this.uvOS.set(region.getU(), region.getV(), region.getU2() - region.getU(), region.getV2() - region.getV());
    return this;
  }
}
