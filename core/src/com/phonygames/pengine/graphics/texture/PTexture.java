package com.phonygames.pengine.graphics.texture;

import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PDeepCopyable;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PTexture implements PPool.Poolable, PDeepCopyable<PTexture> {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PTexture WHITE_PIXEL = new PTexture(PFloat4Texture.getWHITE_PIXEL());
  @Getter(lazy = true)
  private final PVec4 uvOS = PVec4.obtain().set(0, 0, 1, 1);
  @Setter
  private Texture backingTexture;
  @Getter
  @Setter
  private PPool ownerPool;

  public PTexture() {
  }

  public PTexture(Texture texture) {
    set(texture);
  }

  public PTexture set(Texture texture) {
    this.backingTexture = texture;
    return this;
  }

  public void applyShaderWithUniform(String uniform, PShader shader) {
    shader.setWithUniform(uniform, getBackingTexture());
    shader.set(uniform + "UVOS", getUvOS());
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
    return "[PTexture ref:" + (backingTexture.toString().split("@")[1]) + " " + width() + "x" + height() + "]";
  }

  public int width() {
    return getBackingTexture().getWidth();
  }

  public int height() {
    return getBackingTexture().getHeight();
  }

  @Override public void reset() {
    this.backingTexture = null;
  }

  public PTexture tryDeepCopy() {
    return new PTexture(backingTexture);
  }
}
