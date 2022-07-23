package com.phonygames.pengine.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.framebuffer.PFrameBuffer;
import com.phonygames.pengine.graphics.framebuffer.PGLFrameBuffer;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.util.PBuilder;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class PRenderBuffer implements Disposable, PApplicationWindow.ResizeListener {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static PRenderBuffer activeBuffer = null;
  private static Texture testTexture = null;
  private final List<AttachmentSpec> attachmentSpecs = new ArrayList<>();
  @Getter
  @Accessors(fluent = true)
  private boolean active;
  @Getter
  @Accessors(fluent = true)
  private String fragmentLayout = "// LAYOUT\n";
  private PFrameBuffer frameBuffer, frameBufferPrev;
  @Getter
  @Accessors(fluent = true)
  private SizeMode sizeMode;
  @Getter
  @Accessors(fluent = true)
  private int staticWidth, staticHeight;
  @Getter
  @Accessors(fluent = true)
  private float windowScale = 1;

  private PRenderBuffer() {
  }

  @Override public void dispose() {
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

  public PShader getQuadShader(FileHandle frag) {
    return new PShader("", fragmentLayout(), PVertexAttributes.getPOSITION(),
                       Gdx.files.local("engine/shader/quad.vert.glsl"), frag);
  }

  public Texture texture() {
    createFrameBuffersIfNeeded();
    return frameBuffer.getTextureAttachments().first();
  }

  private void createFrameBuffersIfNeeded() {
    if (frameBuffer != null && frameBufferPrev != null) {
      return;
    }
    int desiredW = sizeMode == SizeMode.Static ? staticWidth : (int) (PApplicationWindow.getWidth() * windowScale);
    int desiredH = sizeMode == SizeMode.Static ? staticHeight : (int) (PApplicationWindow.getHeight() * windowScale);
    PGLFrameBuffer.PFrameBufferBuilder frameBufferBuilder = new PGLFrameBuffer.PFrameBufferBuilder(desiredW, desiredH);
    PGLFrameBuffer.PFrameBufferBuilder frameBufferBuilderPrev =
        new PGLFrameBuffer.PFrameBufferBuilder(desiredW, desiredH);
    for (int a = 0; a < attachmentSpecs.size(); a++) {
      attachmentSpecs.get(a).addAttachment(frameBufferBuilder);
      attachmentSpecs.get(a).addAttachment(frameBufferBuilderPrev);
    }
    frameBuffer = frameBufferBuilder.build();
    frameBufferPrev = frameBufferBuilderPrev.build();
  }

  public Texture texture(int index) {
    createFrameBuffersIfNeeded();
    return frameBuffer.getTextureAttachments().get(index);
  }

  public Texture texture(String id) {
    createFrameBuffersIfNeeded();
    for (int a = 0; a < attachmentSpecs.size(); a++) {
      val spec = attachmentSpecs.get(a);
      if (id.equals(spec.name)) {
        return frameBuffer.getTextureAttachments().get(a);
      }
    }
    return null;
  }

  public Texture getTexturePrev(String id) {
    createFrameBuffersIfNeeded();
    for (int a = 0; a < attachmentSpecs.size(); a++) {
      val spec = attachmentSpecs.get(a);
      if (id.equals(spec.name)) {
        return frameBufferPrev.getTextureAttachments().get(a);
      }
    }
    return null;
  }

  public int height() {
    return frameBuffer.getHeight();
  }

  public int numTextures() {
    createFrameBuffersIfNeeded();
    return frameBuffer.getTextureAttachments().size;
  }

  @Override public void onApplicationWindowResize(int width, int height) {
    if (sizeMode == SizeMode.WindowScale) {
      disposeInternal(); // Dispose the framebuffers to trigger a regen.
    }
  }

  public String getTextureName(int index) {
    return attachmentSpecs.get(index).name;
  }
  public PRenderBuffer renderQuad(PShader shader) {
    boolean wasActive = activeBuffer == this;
    if (!wasActive) {
      begin();
    }
    PMesh.FULLSCREEN_QUAD_MESH.getBackingMesh().render(shader.getShaderProgram(), GL20.GL_TRIANGLES);
    if (!wasActive) {
      end();
    }
    return this;
  }

  public void begin() {
    begin(true);
  }

  public void end() {
    active = false;
    frameBuffer.end();
    activeBuffer = null;
  }

  public void begin(boolean swapBuffers) {
    PAssert.isTrue(activeBuffer == null, "Another renderBuffer was already active!");
    createFrameBuffersIfNeeded();
    activeBuffer = this;
    if (PRenderContext.activeContext() != null) {
      PRenderContext.activeContext().setForRenderBuffer(this);
    }
    if (swapBuffers) {
      swapBuffers();
    }
    frameBuffer.begin();
    active = true;
  }

  private void swapBuffers() {
    PFrameBuffer temp = frameBuffer;
    frameBuffer = frameBufferPrev;
    frameBufferPrev = temp;
  }

  public int width() {
    return frameBuffer.getWidth();
  }

  public enum SizeMode {
    WindowScale, Static
  }

  protected static class AttachmentSpec {
    private final AttachmentType attachmentType;
    private final int internalFormat, format, type;
    private final boolean isGpuOnly, isDepth, isStencil;
    private final String name;

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
          builder.addColorTextureAttachment(internalFormat, format, type);
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

    enum AttachmentType {
      ColorBuffer, Float, DepthTexture, StencilTexture
    }
  }

  public static class Builder extends PBuilder {
    private PRenderBuffer renderBuffer = new PRenderBuffer();

    public Builder addDepthAttachment() {
      checkLock();
      renderBuffer.attachmentSpecs.add(
          new AttachmentSpec("depth", GL30.GL_DEPTH_COMPONENT32F, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT,
                             AttachmentSpec.AttachmentType.DepthTexture));
      return this;
    }

    public Builder addFloatAttachment(String name) {
      return addFloatAttachment(name, GL30.GL_RGBA16F, GL30.GL_RGBA);
    }

    public Builder addFloatAttachment(String name, int internalFormat) {
      return addFloatAttachment(name, internalFormat, GL30.GL_RGBA);
    }

    public Builder addFloatAttachment(String name, int internalFormat, int format) {
      checkLock();
      renderBuffer.fragmentLayout +=
          "layout(location = " + renderBuffer.attachmentSpecs.size() + ") out vec4 " + name + ";\n";
      renderBuffer.attachmentSpecs.add(
          new AttachmentSpec(name, internalFormat, format, GL30.GL_FLOAT, AttachmentSpec.AttachmentType.Float));
      return this;
    }

    public PRenderBuffer build() {
      lockBuilder();
      return renderBuffer;
    }

    public Builder setStaticSize(int width, int height) {
      checkLock();
      renderBuffer.sizeMode = SizeMode.Static;
      renderBuffer.staticWidth = width;
      renderBuffer.staticHeight = height;
      PApplicationWindow.removeResizeListener(renderBuffer);
      return this;
    }

    public Builder setWindowScale(float windowScale) {
      checkLock();
      renderBuffer.sizeMode = SizeMode.WindowScale;
      renderBuffer.windowScale = windowScale;
      PApplicationWindow.registerResizeListener(renderBuffer);
      return this;
    }
  }
}
