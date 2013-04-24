package es.tid.graphlib.utils;

import org.apache.giraph.utils.IntPair;

/**
 * A pair of integers and the value on their edge
 */
public class IntPairVal extends IntPair {
  /** Value between two elements */
  private int value;

  /**
   * Constructor.
   * 
   * @param fst First element
   * @param snd Second element
   * @param val Edge Value
   */
  public IntPairVal(int fst, int snd, int val) {
    super(fst, snd);
    value = val;
  }

  /**
   * Get the value.
   * 
   * @return The value
   */
  public int getValue() {
    return value;
  }

  /**
   * Set the value between two elements.
   * 
   * @param value
   *          The value
   */
  public void setValue(int value) {
    this.value = value;
  }
}
