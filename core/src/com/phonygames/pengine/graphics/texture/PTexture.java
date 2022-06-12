package com.phonygames.pengine.graphics.texture;

import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec4;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PTexture {
  private static PTexture WHITE_PIXEL;

  public static PTexture getWHITE_PIXEL() {
    if (WHITE_PIXEL == null) {
      WHITE_PIXEL = new PTexture(PFloat4Texture.getWHITE_PIXEL());
    }

    return WHITE_PIXEL;
  }

  @Getter
  private final PVec4 uvOS = new PVec4().set(0, 0, 1, 1);

  public PTexture() {
  }

  public PTexture(Texture texture) {
    set(texture);
  }

  @Setter
  private Texture backingTexture;

  public Texture getBackingTexture() {
    return PFloat4Texture.getWHITE_PIXEL();
//    return backingTexture == null ? PFloat4Texture.getWHITE_PIXEL() : backingTexture;
  }

  public boolean has() {
    return backingTexture != null;
  }

  public int width() {
    return getBackingTexture().getWidth();
  }

  public int height() {
    return getBackingTexture().getHeight();
  }

  private int bind() {
    return PRenderContext.getActiveContext().getTextureBinder().bind(getBackingTexture());
  }

  public void applyShaderWithUniform(String uniform, PShader shader) {
    shader.setWithUniform(uniform, getBackingTexture());
    shader.set(uniform + "UVOS", uvOS);
  }

  public PTexture reset() {
    this.backingTexture = null;
    return this;
  }

  public PTexture set(Texture texture) {
    this.backingTexture = texture;
    return this;
  }

  public PTexture set(PTexture other) {
    set(other.backingTexture);
    return this;
  }

  public PTexture tryDeepCopy() {
    return new PTexture(backingTexture);
  }

  public String toString() {
    if (backingTexture == null) {
      return "[PTexture UNSET]";
    }
    return "[PTexture ref:" + (backingTexture.toString().split("@")[1]) + " " + width() + "x" + height() + "]";
  }

  @Override
  public int hashCode() {
    if (backingTexture == null) { return 0; }
    return backingTexture.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PTexture)) {
      return false;
    }

    val other = (PTexture) o;
    return other.backingTexture == backingTexture;
  }
}
