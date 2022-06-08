package com.phonygames.pengine.util;

import java.util.List;

public class PCollectionUtils {
  /**
   * Checks equality by calling .equals() on the list elements.
   *
   * @param a
   * @param b
   * @return If a and b are the same.
   */
  public static boolean equals(List a, List b) {
    if (a.size() != b.size()) {
      return false;
    }

    for (int i = 0; i < a.size(); i++) {
      Object ao = a.get(i);
      Object bo = b.get(i);
      if (ao == null && bo == null) {
        continue;
      }

      if (ao != null && bo != null) {
        if (!ao.equals(bo)) {
          return false;
        }
      } else {
        // One of them is null but not the other.
        return false;
      }
    }
    return true;
  }
}
