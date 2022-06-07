package com.phonygames.pengine.graphics.framebuffer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * This is a {@link FrameBuffer} variant backed by a float texture.
 */
public class PFloatFrameBuffer extends PFrameBuffer {

  PFloatFrameBuffer() {}

  /**
   * Creates a GLFrameBuffer from the specifications provided by bufferBuilder
   *
   * @param bufferBuilder
   **/
  public PFloatFrameBuffer(PGLFrameBufferBuilder<? extends PGLFrameBuffer<Texture>> bufferBuilder) {
    super(bufferBuilder);
  }

  /**
   * Creates a new FrameBuffer with a float backing texture, having the given dimensions and potentially a depth buffer attached.
   *
   * @param width    the width of the framebuffer in pixels
   * @param height   the height of the framebuffer in pixels
   * @param hasDepth whether to attach a depth buffer
   * @throws GdxRuntimeException in case the FrameBuffer could not be created
   */
  public PFloatFrameBuffer(int width, int height, boolean hasDepth) {
    PFloatFrameBufferBuilder bufferBuilder = new PFloatFrameBufferBuilder(width, height);
    bufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA);
    if (hasDepth) { bufferBuilder.addBasicDepthRenderBuffer(); }
    this.bufferBuilder = bufferBuilder;

    build();
  }

  @Override
  public Texture createTexture(FrameBufferTextureAttachmentSpec attachmentSpec) {
    FloatTextureData data = new FloatTextureData(
        bufferBuilder.width, bufferBuilder.height,
        attachmentSpec.internalFormat, attachmentSpec.format, attachmentSpec.type,
        attachmentSpec.isGpuOnly
    );
    Texture result = new Texture(data);
    if (Gdx.app.getType() == ApplicationType.Desktop || Gdx.app.getType() == ApplicationType.Applet) { result.setFilter(TextureFilter.Linear, TextureFilter.Linear); } else
    // no filtering for float textures in OpenGL ES
    { result.setFilter(TextureFilter.Nearest, TextureFilter.Nearest); }
    result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    return result;
  }

}
