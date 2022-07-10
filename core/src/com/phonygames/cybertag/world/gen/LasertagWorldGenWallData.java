package com.phonygames.cybertag.world.gen;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PArrayUtils;
import com.phonygames.pengine.util.PList;

public class LasertagWorldGenWallData {
    protected final int baseX, baseY, baseZ;
    protected final PList<Float> cornerHeightsH = new PList<>();
    // Always traverse in the +x or +z direction.
    protected final PList<Float> cornerHeightsL = new PList<>();
    protected final PList<LasertagWorldGenRoom> opposingRooms = new PList<>();
    protected final LasertagWorldGenRoom room;
    protected final Side side;
    protected final PList<TileType> tileTypes = new PList<>();
    protected final PList<Integer> tileXs = new PList<>();
    protected final PList<Integer> tileZs = new PList<>();

    protected LasertagWorldGenWallData(LasertagWorldGenRoom room, Side side, int baseX, int baseY, int baseZ) {
      this.room = room;
      this.side = side;
      this.baseX = baseX;
      this.baseY = baseY;
      this.baseZ = baseZ;
    }

    /**
     * Fills the data buffers.
     */
    protected void processFromTiles() {
      // Fill the data buffers with some default data and determine its length.
      PAssert.isTrue(tileTypes.isEmpty());
      for (int a = 0; ; a++) {
        int x = baseX;
        int y = baseY;
        int z = baseZ;
        LasertagWorldGenRoom opposingRoom = null;
        switch (side) {
          case Xl: // Moving in the +z direction.
            z += a;
            opposingRoom = room.building.roomAtTile(x - 1, y, z);
            break;
          case Xh: // Moving in the +z direction.
            z += a;
            opposingRoom = room.building.roomAtTile(x + 1, y, z);
            break;
          case Zl: // Moving in the +x direction.
            x += a;
            opposingRoom = room.building.roomAtTile(x, y, z - 1);
            break;
          case Zh: // Moving in the +x direction.
            x += a;
            opposingRoom = room.building.roomAtTile(x, y, z + 1);
            break;
        }
        if (room.building.roomAtTile(x, y, z) != room || opposingRoom == room) {
          break;
        }
        tileTypes.add(TileType.NORMAL); // Put in some default values for now.
        cornerHeightsL.add(1f);
        cornerHeightsH.add(1f);
        tileXs.add(x);
        tileZs.add(z);
        opposingRooms.add(opposingRoom);
      }
      // Now that we've gotten filled in the data with default data, do extra processing.
      int tilesTilDoorFrame = -1;
      boolean doorFrameIsSingleTileWide = false;
      int remainingDoorFrameSize = 0;
      boolean doorFrameDataNeedsUpdating = true;
      for (int a = 0; a < length(); a++) {
        // Determine if we need / the style of doorframe we need.
        LasertagWorldGenRoom opposingRoom = opposingRooms.get(a);
        if (opposingRoom == null) {
          tilesTilDoorFrame = -1; // You can't build a doorframe into the void!
          doorFrameDataNeedsUpdating = true;
          doorFrameIsSingleTileWide = false;
        } else if (doorFrameDataNeedsUpdating) {
          // Figure out if the other room already generated a doorframe, and if so, copy it.
          LasertagWorldGenWallData opposingWallData = opposingDataAtTileIndex(a);
          if (opposingWallData != null) {
            // If there is an opposing wall, see if it has a doorframe connected to this wall.
            int doorframeIndexStart = -1, doorframeIndexEnd = -1;
            for (int b = a; b < length(); b++) {
              int indexForOtherWallData =
                  opposingWallData.indexForOpposingTilePosition(tileXs.get(b), baseY, tileZs.get(b));
              if (indexForOtherWallData >= opposingWallData.length() || indexForOtherWallData == -1) {
                break;
              }
              TileType tileTypeAtIndexFromOtherWallData = opposingWallData.tileTypes.get(indexForOtherWallData);
              if (tileTypeAtIndexFromOtherWallData == TileType.DOORFRAME && doorframeIndexStart == -1) {
                doorframeIndexStart = b;
              }
              if (tileTypeAtIndexFromOtherWallData != TileType.DOORFRAME && doorframeIndexStart != -1 &&
                  doorframeIndexEnd == -1) {
                doorframeIndexEnd = b - 1;
              }
            }
            if (doorframeIndexEnd == -1 && doorframeIndexStart != -1) {
              doorframeIndexEnd = doorframeIndexStart;
            }
            // If a doorframe is connected to this wall, doorframeIndexEnd will not be -1.
            if (doorframeIndexEnd != -1) {
              tilesTilDoorFrame = doorframeIndexStart - a;
              remainingDoorFrameSize = doorframeIndexEnd - doorframeIndexStart + 1;
              if (remainingDoorFrameSize == 1) {doorFrameIsSingleTileWide = true;}
            }
          } else {
            // If there is no opposing wall, we get to decide whether or not and where to build a doorframe.
            int potentialDoorframeLength = 0;
            for (int b = a; b < length(); b++) {
              if (opposingRooms.get(b) == opposingRoom) {
                potentialDoorframeLength++;
              } else {
                break;
              }
            }
            if (potentialDoorframeLength > 0) {
              final float[] doorFrameSizeWeights = new float[]{.5f, .2f, .1f};
              remainingDoorFrameSize =
                  Math.min(potentialDoorframeLength, PArrayUtils.randomIndexWithWeights(doorFrameSizeWeights) + 1);
              if (remainingDoorFrameSize == 1) {doorFrameIsSingleTileWide = true;}
              tilesTilDoorFrame = MathUtils.random(potentialDoorframeLength - remainingDoorFrameSize);
            }
          }
          doorFrameDataNeedsUpdating = false;
        }
        // Generate a doorframe using the tilesTilDoorFrame, remainingDoorFrameSize, and doorFrameIsSingleTileWide vars.
        if (tilesTilDoorFrame == 0) {
          if (doorFrameIsSingleTileWide) {
            tileTypes.set(a, TileType.DOORFRAME);
            tilesTilDoorFrame = -1;
          } else {
            if (remainingDoorFrameSize > 0) {
              remainingDoorFrameSize--;
              // TODO: support multiple tile wide doorframes, and set the appropriate tyletype here.
              if (remainingDoorFrameSize == 0) {
                tilesTilDoorFrame = -1;
              }
            }
          }
        } else {
          tilesTilDoorFrame--;
        }
      }
    }

    public final int length() {
      return tileTypes.size();
    }

    private @Nullable LasertagWorldGenWallData opposingDataAtTileIndex(int index) {
      PAssert.isTrue(index >= 0 && index < length());
      int x = xForIndex(index);
      int y = baseY;
      int z = zForIndex(index);
      LasertagWorldGenRoom opposingRoom = opposingRooms.get(index);
      if (opposingRoom == null) {
        return null;
      }
      // Loop through the LasertagWorldGenWallData of the opposing room to find the LasertagWorldGenWallData that matches the necessary description.
//      for (LasertagWorldGenWallData data : opposingRoom.wallData) {
//        switch (side) {
//          case Xh:
//            if (data.side == Side.Xl && data.indexForTilePosition(x + 1, y, z) != -1) {return data;}
//            break;
//          case Xl:
//            if (data.side == Side.Xh && data.indexForTilePosition(x - 1, y, z) != -1) {return data;}
//            break;
//          case Zh:
//            if (data.side == Side.Zl && data.indexForTilePosition(x, y, z + 1) != -1) {return data;}
//            break;
//          case Zl:
//            if (data.side == Side.Zh && data.indexForTilePosition(x, y, z - 1) != -1) {return data;}
//            break;
//        }
//      }
      return null;
    }

    private int indexForOpposingTilePosition(int x, int y, int z) {
      if (y != this.baseY) {
        return -1;
      }
      switch (side) {
        case Xl: // Moving in the +z direction.
          return x != (baseX - 1) ? -1 : z - baseZ;
        case Xh: // Moving in the +z direction.
          return x != (baseX + 1) ? -1 : z - baseZ;
        case Zl: // Moving in the +x direction.
          return z != (baseZ - 1) ? -1 : x - baseX;
        case Zh: // Moving in the +x direction.
          return z != (baseZ + 1) ? -1 : x - baseX;
        default:
          PAssert.fail("WTF");
          return -1;
      }
    }

    private int xForIndex(int index) {
      switch (side) {
        case Xl:
        case Xh:
          // Moving in the +z direction.
          return baseX;
        case Zl:
        case Zh:
          return baseX + index;
        default:
          PAssert.fail("WTF");
          return -1;
      }
    }

    private int zForIndex(int index) {
      switch (side) {
        case Xl:
        case Xh:
          // Moving in the +z direction.
          return baseZ + index;
        case Zl:
        case Zh:
          return baseZ;
        default:
          PAssert.fail("WTF");
          return -1;
      }
    }

    private int indexForTilePosition(int x, int y, int z) {
      if (y != this.baseY) {
        return -1;
      }
      switch (side) {
        case Xl:
        case Xh:
          // Moving in the +z direction.
          return x != baseX ? -1 : z - baseZ;
        case Zl:
        case Zh:
          // Moving in the +x direction.
          return z != baseZ ? -1 : x - baseX;
        default:
          PAssert.fail("WTF");
          return -1;
      }
    }

    enum Side {
      Xl, Xh, Zl, Zh
    }

    enum TileType {
      NORMAL, WINDOW, DOORFRAME
    }
  }