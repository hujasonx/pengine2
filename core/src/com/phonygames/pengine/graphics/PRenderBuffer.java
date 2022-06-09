package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.framebuffer.PFrameBuffer;
import com.phonygames.pengine.graphics.framebuffer.PGLFrameBuffer;
import com.phonygames.pengine.util.PBuilder;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class PRenderBuffer implements Disposable, PApplicationWindow.ResizeListener {
  @Getter
  private static PRenderBuffer activeBuffer = null;

  private PRenderBuffer() {

  }

  public enum SizeMode {
    WindowScale, Static
  }

  private final List<AttachmentSpec> attachmentSpecs = new ArrayList<>();

  @Getter
  private SizeMode sizeMode;
  @Getter
  private float windowScale = 1;
  @Getter
  private int staticWidth, staticHeight;
  @Getter
  private boolean active;

  private PFrameBuffer frameBuffer, frameBufferPrev;

  private static Texture testTexture = null;

  public int width() {
    return frameBuffer.getWidth();
  }

  public int height() {
    return frameBuffer.getHeight();
  }

  public Texture getTexture() {
    createFrameBuffersIfNeeded();
    return frameBuffer.getTextureAttachments().first();

//    if (testTexture == null) {
//      testTexture = new Texture(Gdx.files.internal("badlogic.jpg"));
//    }
//    return testTexture;
  }

  public void begin() {
    begin(true);
  }

  public void begin(boolean swapBuffers) {
    PAssert.isTrue(activeBuffer == null, "Another renderBuffer was already active!");
    createFrameBuffersIfNeeded();
    activeBuffer = this;

    if (swapBuffers) {
      swapBuffers();
    }

    frameBuffer.begin();
    active = true;
  }

  public void end() {
    active = false;
    frameBuffer.end();
    activeBuffer = null;
  }

  private void swapBuffers() {
    PFrameBuffer temp = frameBuffer;
    frameBuffer = frameBufferPrev;
    frameBufferPrev = temp;
  }

  private void createFrameBuffersIfNeeded() {
    if (frameBuffer != null && frameBufferPrev != null) {
      return;
    }

    int desiredW = sizeMode == SizeMode.Static ? staticWidth : (int) (PApplicationWindow.getWidth() * windowScale);
    int desiredH = sizeMode == SizeMode.Static ? staticHeight : (int) (PApplicationWindow.getHeight() * windowScale);
    PGLFrameBuffer.PFrameBufferBuilder frameBufferBuilder = new PGLFrameBuffer.PFrameBufferBuilder(desiredW, desiredH);
    PGLFrameBuffer.PFrameBufferBuilder frameBufferBuilderPrev = new PGLFrameBuffer.PFrameBufferBuilder(desiredW, desiredH);
    for (int a = 0; a < attachmentSpecs.size(); a++) {
      attachmentSpecs.get(a).addAttachment(frameBufferBuilder);
      attachmentSpecs.get(a).addAttachment(frameBufferBuilderPrev);
    }

    frameBuffer = frameBufferBuilder.build();
    frameBufferPrev = frameBufferBuilderPrev.build();
  }

  public static class Builder extends PBuilder {
    private PRenderBuffer renderBuffer = new PRenderBuffer();

    public Builder setWindowScale(float windowScale) {
      checkLock();
      renderBuffer.sizeMode = SizeMode.WindowScale;
      renderBuffer.windowScale = windowScale;
      PApplicationWindow.registerResizeListener(renderBuffer);
      return this;
    }

    public Builder setStaticSize(int width, int height) {
      checkLock();
      renderBuffer.sizeMode = SizeMode.Static;
      renderBuffer.staticWidth = width;
      renderBuffer.staticHeight = height;
      PApplicationWindow.removeResizeListener(renderBuffer);
      return this;
    }

    public Builder addFloatAttachment(String name, int internalFormat, int format) {
      checkLock();
      renderBuffer.attachmentSpecs.add(new AttachmentSpec(name, internalFormat, format, GL30.GL_FLOAT, AttachmentSpec.AttachmentType.Float));
      return this;
    }

    public Builder addDepthAttachment() {
      checkLock();
      renderBuffer.attachmentSpecs.add(new AttachmentSpec("depth", GL30.GL_DEPTH_COMPONENT32F, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, AttachmentSpec.AttachmentType.DepthTexture));
      return this;
    }

    public PRenderBuffer build() {
      lockBuilder();
      return renderBuffer;
    }
  }

  protected static class AttachmentSpec {
    private final String name;
    private final int internalFormat, format, type;
    private final boolean isGpuOnly, isDepth, isStencil;
    private final AttachmentType attachmentType;

    enum AttachmentType {
      ColorBuffer,
      Float,
      DepthTexture,
      StencilTexture
    }

    public AttachmentSpec(String name, int internalFormat, int format, AttachmentType attachmentType) {
      this(name, internalFormat, format, 0, attachmentType);
      PAssert.isTrue(attachmentType == AttachmentType.Float, "Non-float must use the other constructor");
    }

    private AttachmentSpec(String name, int internalFormat, int format, int type, AttachmentType attachmentType) {
      this.name = name;
      this.internalFormat = internalFormat;
      this.format = format;
      this.type = type;
      this.attachmentType = attachmentType;
      this.isGpuOnly = true;
      this.isDepth = attachmentType == AttachmentType.DepthTexture;
      this.isStencil = attachmentType == AttachmentType.StencilTexture;
    }

    private PGLFrameBuffer.PFrameBufferBuilder addAttachment(PGLFrameBuffer.PFrameBufferBuilder builder) {
      switch (attachmentType) {
        case ColorBuffer:
          builder.addColorTextureAttachment(internalFormat, format, type, isGpuOnly);
          break;
        case Float:
          builder.addFloatAttachment(internalFormat, format);
          break;
        case DepthTexture:
          builder.addDepthTextureAttachment(internalFormat, type);
          break;
        case StencilTexture:
          builder.addStencilTextureAttachment(internalFormat, type);
          break;
      }
      return builder;
    }
  }

  @Override
  public void dispose() {
    disposeInternal();
  }

  private void disposeInternal() {
    if (frameBuffer != null) {
      frameBuffer.dispose();
      frameBuffer = null;
    }

    if (frameBufferPrev != null) {
      frameBufferPrev.dispose();
      frameBufferPrev = null;
    }
  }

  @Override
  public void onApplicationWindowResize(int width, int height) {
    if (sizeMode == SizeMode.WindowScale) {
      disposeInternal(); // Dispose the framebuffers to trigger a regen.
    }
  }
}
