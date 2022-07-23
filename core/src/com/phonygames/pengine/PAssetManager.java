package com.phonygames.pengine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.phonygames.pengine.file.PFileHandleUtils;
import com.phonygames.pengine.graphics.model.PGLBAssetLoader;
import com.phonygames.pengine.graphics.model.PModel;

import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.List;

import lombok.Getter;

public class PAssetManager {
  @Getter
  private static final AssetManager assetManager = new AssetManager();

  public static void dispose() {
    assetManager.dispose();
  }

  public static void init() {
    assetManager.setLoader(PModel.class, ".glb", new PGLBAssetLoader());
    List<FileHandle> textureAtlasFiles = PFileHandleUtils.allFilesRecursive(Gdx.files.local(""), "atlas");
    List<FileHandle> gltfFiles = PFileHandleUtils.allFilesRecursive(Gdx.files.local(""), "glb|gltf");
    for (FileHandle f : textureAtlasFiles) {
      assetManager.load(f.path(), TextureAtlas.class);
    }
    for (FileHandle f : gltfFiles) {
      assetManager.load(f.path(), PModel.class);
    }
  }

  public static boolean isLoaded(String id) {
    return assetManager.isLoaded(id);
  }

  public static PModel model(String filename, boolean blockIfUnloaded) {
    if (assetManager.isLoaded(filename)) {
      return assetManager.get(filename);
    }
    if (blockIfUnloaded) {
      return assetManager.finishLoadingAsset(filename);
    }
    return null;
  }

  public static TextureRegion textureRegion(String filename, String regionName, boolean blockIfUnloaded) {
    if (!assetManager.isLoaded(filename) && !blockIfUnloaded) {
      return null;
    }
    TextureAtlas atlas = null;
    if (assetManager.isLoaded(filename)) {
      atlas= assetManager.get(filename);
    }
    if (blockIfUnloaded) {
      atlas= assetManager.finishLoadingAsset(filename);
    }
    return atlas.findRegion(regionName);
  }

  public static void preFrameUpdate() {
    assetManager.update();
  }

  public static class Directories {}
}
