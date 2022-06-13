package com.phonygames.pengine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.phonygames.pengine.file.PFileHandleUtils;
import com.phonygames.pengine.graphics.model.PGltf;

import net.mgsx.gltf.loaders.glb.GLBAssetLoader;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.List;

import lombok.Getter;

public class PAssetManager {
  @Getter
  private static final AssetManager assetManager = new AssetManager();

  public static void init() {
    assetManager.setLoader(SceneAsset.class, ".glb", new GLBAssetLoader());
    assetManager.setLoader(SceneAsset.class, ".gltf", new GLTFAssetLoader());
    List<FileHandle> textureAtlasFiles = PFileHandleUtils.allFilesRecursive(Gdx.files.local(""), "atlas");
    List<FileHandle> gltfFiles = PFileHandleUtils.allFilesRecursive(Gdx.files.local(""), "glb|gltf");
    for (FileHandle f : textureAtlasFiles) {
      assetManager.load(f.path(), TextureAtlas.class);
    }
    for (FileHandle f : gltfFiles) {
      assetManager.load(f.path(), SceneAsset.class);
    }
  }

  public static boolean isLoaded(String id) {
    return assetManager.isLoaded(id);
  }

  public static void preFrameUpdate() {
    PGltf.triggerLoads();
    assetManager.update();
  }

  public static SceneAsset sceneAsset(String id) {
    return assetManager.get(id, SceneAsset.class);
  }

  public static class Directories {}
}
