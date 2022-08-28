package com.phonygames.pengine.graphics.sdf;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PString;
import com.phonygames.pengine.util.PStringUtils;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class PSDFSheet {
  public static final String FILE_EXTENSION = "psdf";
  public static final String IMAGE_SRC = "imageSource";
  public static final String SHEET_NAME = "sheetName";
  private String name;
  private final PStringMap<Symbol> symbols = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PTexture texture = new PTexture();

  private PSDFSheet() {
  }

  public Symbol get(String sdfKey) {
    return symbols.get(sdfKey);
  }

  public static PSDFSheet blank(String name) {
    PSDFSheet sheet = new PSDFSheet();
    sheet.name = name;
    return sheet;
  }

  public static PSDFSheet fromFileHandle(FileHandle fileHandle) {
    String[] r = PStringUtils.splitByLine(fileHandle.readString());
    PSDFSheet sheet = new PSDFSheet();
    for (int a = 0; a < r.length; a++) {
      final String line = r[a];
      String[] split = line.split("\\|");
      if (split.length == 0 || line.startsWith("#")) {
        continue;
      }
      String type = split[0];
      switch (type) {
        case SHEET_NAME:
          sheet.name = split[1];
          break;
        case IMAGE_SRC:
          Texture t = new Texture(fileHandle.parent().child(split[1]));
          t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
          sheet.texture.set(t);
          break;
        case Symbol.SYMBOL:
          Symbol symbol = Symbol.fromString(line);
          sheet.registerSymbol(symbol);
          break;
      }
    }
    return sheet;
  }

  public PSDFSheet registerSymbol(Symbol symbol) {
    symbols.put(symbol.id, symbol);
    symbol.sheet = this;
    return this;
  }

  protected boolean writePSDFFileHandle(FileHandle fileHandle, String imageSource) {
    PAssert.isTrue(fileHandle.extension().equals(PSDFSheet.FILE_EXTENSION));
    PString outData = PString.obtain();
    outData.append("#PSDF").appendBr();
    outData.append(SHEET_NAME).append("|").append(name).appendBr();
    outData.append(IMAGE_SRC).append("|").append(imageSource).appendBr();
    try (val it = symbols.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        if (e.v() == null) {continue;}
        outData.append(e.v().toString()).appendBr();
      }
    }
    fileHandle.writeString(outData.toString(), false);
    outData.free();
    return true;
  }

  public enum Channel {
    R, G, B, A;

    public static Channel fromIndex(int index) {
      switch (index) {
        case 0:
          return Channel.R;
        case 1:
          return Channel.G;
        case 2:
          return Channel.B;
        case 3:
        default:
          return Channel.A;
      }
    }

    public int index() {
      switch (this) {
        case A:
          return 3;
        case B:
          return 2;
        case G:
          return 1;
        case R:
        default:
          return 0;
      }
    }

    public PVec4 value() {
      switch (this) {
        case A:
          return PVec4.W;
        case B:
          return PVec4.Z;
        case G:
          return PVec4.Y;
        case R:
        default:
          return PVec4.X;
      }
    }
  }

  @Builder public static class Symbol {
    public static final String SYMBOL = "Symbol";
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    protected Channel channel;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    protected String id;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    protected float scale;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** Inlcudes padding on both sides. */ protected int sheetHeight;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    protected int sheetPadding;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** Inlcudes padding on both sides. */ protected int sheetWidth;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    protected int sheetX;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    protected int sheetY;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PSDFSheet sheet;

    public static Symbol fromString(String s) {
      String[] split = s.split("\\|");
      Symbol.SymbolBuilder builder = Symbol.builder();
      builder.id(split[1]);
      builder.channel(Channel.fromIndex(Integer.parseInt(split[2])));
      builder.sheetX(Integer.parseInt(split[3]));
      builder.sheetY(Integer.parseInt(split[4]));
      builder.sheetWidth(Integer.parseInt(split[5]));
      builder.sheetHeight(Integer.parseInt(split[6]));
      builder.sheetPadding(Integer.parseInt(split[7]));
      builder.scale(Float.parseFloat(split[8]));
      return builder.build();
    }

    public int sheetRightX() {
      return sheetWidth + sheetX;
    }

    public int sheetTopY() {
      return sheetHeight + sheetY;
    }

    public String toString() {
      PString s = PString.obtain();
      s.append(SYMBOL).append("|");
      s.append(id).append("|");
      s.append(channel.index()).append("|");
      s.append(sheetX).append("|");
      s.append(sheetY).append("|");
      s.append(sheetWidth).append("|");
      s.append(sheetHeight).append("|");
      s.append(sheetPadding).append("|");
      s.append(scale).append("|");
      return s.toString();
    }
  }
}
