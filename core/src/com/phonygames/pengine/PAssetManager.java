package com.phonygames.pengine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.phonygames.pengine.file.PFileHandleUtils;

import java.util.List;

public class PAssetManager {
  public static class Directories {
    public static final String textureAtlas = "textureAtlas";
  }

  private static final AssetManager assetManager = new AssetManager();

  public static void init() {
    List<FileHandle> textureAtlasFiles = PFileHandleUtils.allFilesRecursive(Gdx.files.internal(Directories.textureAtlas), "atlas");
    for (FileHandle f : textureAtlasFiles) {
      assetManager.load(f.path(), TextureAtlas.class);
    }
  }
}
