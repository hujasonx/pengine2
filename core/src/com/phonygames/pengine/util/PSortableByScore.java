package com.phonygames.pengine.util;

import com.phonygames.pengine.util.collection.PList;

public interface PSortableByScore<T> extends Comparable<PSortableByScore<T>> {
  @Override public default int compareTo(PSortableByScore<T> other) {
    float score = score();
    float otherScore = other.score();
    if (score > otherScore) {return +1;}
    if (score < otherScore) {return -1;}
    return 0;
  }
  float score();

  static <T extends PSortableByScore<T>> T highestScorerIn(PList<T> ts) {
    if (ts.isEmpty()) {
       return null;
    }
    T bestT = ts.get(0);
    float bestScore = bestT.score();
    for (int a = 1; a < ts.size(); a++) {
      float newScore = ts.get(a).score();
      if (newScore > bestScore) {
        bestScore = newScore;
        bestT = ts.get(a);
      }
    }
    return bestT;
  }
}
