package com.phonygames.pengine.graphics.color;

import com.phonygames.pengine.math.PVec4;

public class PColor {
  public static final PVec4 WHITE = PVec4.obtain().set(1, 1, 1, 1).lockWriting();
  public static final PVec4 BLACK = PVec4.obtain().set(0, 0, 0, 1).lockWriting();
  public static final PVec4 RED = PVec4.obtain().set(1, 0, 0, 1).lockWriting();
  public static final PVec4 GREEN = PVec4.obtain().set(0, 1, 0, 1).lockWriting();
  public static final PVec4 BLUE = PVec4.obtain().set(0, 0, 1, 1).lockWriting();
  public static final PVec4 CYAN = PVec4.obtain().set(0, 1, 1, 1).lockWriting();
  public static final PVec4 MAGENTA = PVec4.obtain().set(1, 0, 1, 1).lockWriting();
  public static final PVec4 YELLOW = PVec4.obtain().set(1, 1, 0, 1).lockWriting();
}
