package com.phonygames.pengine.graphics.framebuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;

/**
 * <p>
 * Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses.
 * It will
 * automatically create a texture for the color attachment and a renderbuffer for the depth buffer. You can get a
 * hold of the
 * texture by {@link com.badlogic.gdx.graphics.glutils.FrameBuffer#getColorBufferTexture()}. This class will only
 * work with OpenGL ES 2.0.
 * </p>
 *
 * <p>
 * FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to
 * another
 * application or receives an incoming call, the framebuffer will be automatically recreated.
 * </p>
 *
 * <p>
 * A FrameBuffer must be disposed if it is no longer needed
 * </p>
 * @author mzechner, realitix
 */
public class PFrameBuffer extends PGLFrameBuffer<Texture> {
  PFrameBuffer() {}

  /**
   * Creates a GLFrameBuffer from the specifications provided by bufferBuilder
   * @param bufferBuilder
   **/
  public PFrameBuffer(PGLFrameBuffer.PGLFrameBufferBuilder<? extends PGLFrameBuffer<Texture>> bufferBuilder) {
    super(bufferBuilder);
  }

  /**
   * Creates a new FrameBuffer having the given dimensions and potentially a depth buffer attached.
   */
  public PFrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth) {
    this(format, width, height, hasDepth, false);
  }

  /**
   * Creates a new FrameBuffer having the given dimensions and potentially a depth and a stencil buffer attached.
   * @param format   the format of the color buffer; according to the OpenGL ES 2.0 spec, only RGB565, RGBA4444 and
   *                 RGB5_A1 are
   *                 color-renderable
   * @param width    the width of the framebuffer in pixels
   * @param height   the height of the framebuffer in pixels
   * @param hasDepth whether to attach a depth buffer
   * @throws com.badlogic.gdx.utils.GdxRuntimeException in case the FrameBuffer could not be created
   */
  public PFrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth, boolean hasStencil) {
    PFrameBufferBuilder frameBufferBuilder = new PFrameBufferBuilder(width, height);
    frameBufferBuilder.addBasicColorTextureAttachment(format);
    if (hasDepth) {frameBufferBuilder.addBasicDepthRenderBuffer();}
    if (hasStencil) {frameBufferBuilder.addBasicStencilRenderBuffer();}
    this.bufferBuilder = frameBufferBuilder;
    build();
  }

  /**
   * See {@link GLFrameBuffer#unbind()}
   */
  public static void unbind() {
    GLFrameBuffer.unbind();
  }

  @Override protected Texture createTexture(FrameBufferTextureAttachmentSpec attachmentSpec) {
    Texture result;
    GLOnlyTextureData data =
        new GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat,
                              attachmentSpec.format, attachmentSpec.type);
    result = new Texture(data);
    result.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    return result;
  }

  @Override protected void attachFrameBufferColorTexture(Texture texture) {
    Gdx.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D,
                                    texture.getTextureObjectHandle(), 0);
  }

  @Override protected void disposeColorTexture(Texture colorTexture) {
    colorTexture.dispose();
  }
}
