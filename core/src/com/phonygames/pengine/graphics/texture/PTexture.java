package com.phonygames.pengine.graphics.texture;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec4;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PTexture {
  @Getter
  private static Texture WHITE_PIXEL;

  @Getter
  private final PVec4 uvOS = new PVec4().set(0, 0, 1, 1);

  public static void init() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    WHITE_PIXEL = new Texture(pixmap);
  }

  public PTexture() {
    this(WHITE_PIXEL);
  }

  public PTexture(Texture texture) {
    set(texture);
  }

  @Getter
  @Setter
  private Texture backingTexture;

  public boolean has() {
    return backingTexture != WHITE_PIXEL;
  }

  public int width() {
    return backingTexture.getWidth();
  }

  public int height() {
    return backingTexture.getHeight();
  }

  private int bind() {
    return PRenderContext.getActiveContext().getTextureBinder().bind(backingTexture);
  }

  public void applyShader(String uniform, PShader shader) {
    shader.setI(uniform, bind());
    shader.set(uniform + "Size", width(), height());
    shader.set(uniform + "UVOS", uvOS);
  }

  public PTexture reset() {
    set(WHITE_PIXEL);
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
    if (backingTexture == WHITE_PIXEL) {
      return "[PTexture UNSET]";
    }
    return "[PTexture ref:" + (backingTexture.toString().split("@")[1]) + " " + width() + "x" + height() + "]";
  }

  @Override
  public int hashCode() {
    return backingTexture.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof  PTexture)) {
      return false;
    }

    val other = (PTexture)o;
    return other.backingTexture == backingTexture;
  }
}
