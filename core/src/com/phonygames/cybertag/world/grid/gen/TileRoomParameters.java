package com.phonygames.cybertag.world.grid.gen;

/** The parameters that a TileRoom may be generated by. */
public abstract class TileRoomParameters {
  /** The maximum number of tiles in a horizontal edge. */
  public int maximumEdgeSize = 10;
  /** The minimum number of tiles in a horizontal edge. */
  public int minimumEdgeSize = 5;
  /** The weights for the room height in tiles. */
  public float[] heightWeights = new float[] {0.5f, .4f, .3f, .2f, .1f};
  /** The minimum horizontal thickness of any given portion of the room. */
  public int minimumRoomThickness = 3;
  /** The ratio of floor tiles that must have full height (no obstructions from other rooms. */
  public float minimumFullHeightTilesRatio = .25f;
  /** The ratio of tiles that must be in the room compared to the constructed bounds. */
  public float minimumTilesRatio = .5f;
  /** Whether or not the room should have an open ceiling. */
  public boolean openCeiling = false;

  public static class Standard extends TileRoomParameters {

  }

//  public void
}
