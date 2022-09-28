package com.phonygames.cybertag.world.grid.gen;

/** The parameters that a TileBuilding may be generated by. */
public class TileBuildingParameters {
  /** Door scores are penalized by this amount if the rooms are already directly connected by doors. */
  public float doorScorePenaltyForAlreadyDirectlyConnectedRooms = 3;
  /**
   * The weights for hallways based on the number of horizontal turns they take. The length of this array dictates the
   * maximum number of turns a hallway can include.
   */
  public float[] hallwayTurnWeights = new float[]{1, 1, 1, 1};
  /** The maximum (inclusive) number of doors that can separate two rooms before the penalty is scaled to 0. */
  public int maxRoomDoorSeparationForPenalty = 3;
  /**
   * Hallways are always generated if two rooms are completely disconnected. Otherwise, this is the minimum number of
   * doors that must separate two rooms before a hallway is allowed to connect them.
   */
  public int minConnectivityForHallways = 4;
//  public float hallwayPenaltyScaleForConnectedRooms = ;
  /**
   * The weights for hallway lengths. The length of this array dictates the
   * maximum length for a hallway.
   */
  public float[] hallwayLengthWeights = new float[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

  /** Returns a new HallwayParameters object for a new hallway. */
  public TileRoomParameters.HallwayParameters getHallwayParameters() {
    return new TileRoomParameters.HallwayParameters();
  }

  /** The number of tiles needed for a hallway to ramp up a single tile. */
  public int hallwayTilesPerSlopeUp = 2;
}
