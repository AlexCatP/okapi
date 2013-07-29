package es.tid.graphlib.cf.sgd;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

import es.tid.graphlib.cf.CfVertexValue;
import es.tid.graphlib.utils.DoubleArrayListWritable;

/**
 * A Writable implementation for 2 elements
 * First element: sourceValue
 * Second element: neighValues
 */

public class SgdVertexValue
extends CfVertexValue
implements Writable {
  /** Neighbors Values */
  private HashMap<IntWritable, DoubleArrayListWritable> neighValues;

  /** Constructor */
  public SgdVertexValue() {
    super();
    neighValues = new HashMap<IntWritable, DoubleArrayListWritable>();
  }

  /**
   * Write bytes
   *
   * @param output Output
   */
  public void write(DataOutput output) throws IOException {
    super.write(output);
    output.writeInt(getSize());
    for (IntWritable key : neighValues.keySet()) {
      key.write(output);
      neighValues.get(key).write(output);
    }
  }

  /**
   * Read bytes
   *
   * @param input Input
   */
  public void readFields(DataInput input) throws IOException {
    super.readFields(input);

    neighValues = new HashMap<IntWritable, DoubleArrayListWritable>();
    int size = input.readInt();
    for (int i = 0; i < size; i++) {
      IntWritable key = new IntWritable();
      key.readFields(input);
      DoubleArrayListWritable value = new DoubleArrayListWritable();
      value.readFields(input);
      neighValues.put(key, value);
    }
  }

  /**
   * Add a vertex latent value to the HashMap.
   *
   * @param neighId Key of the pair
   * @param neighVal Value of the pair
   */
  public void setNeighborValue(IntWritable neighId,
    DoubleArrayListWritable neighVal) {
    neighValues.put(neighId, neighVal);
  }

  /**
   * Get vertex neighbors latent vector values
   *
   * @param id Neighbor's Id
   * @return Neighbor Latent Value
   */
  public DoubleArrayListWritable getNeighValue(IntWritable id) {
    return neighValues.get(id);
  }

  /**
   * Get the neighbors vertices values (data stored with vertex)
   *
   * @return Neighbors values
   */
  public HashMap<IntWritable, DoubleArrayListWritable> getAllNeighValue() {
    return neighValues;
  }

  /**
   * Get number of neighbors latent vectors in this list.
   *
   * @return Number of neighbors latent vectors in the list
   */
  public int getSize() {
    return neighValues.size();
  }

  /**
   * Check if the list is empty.
   *
   * @return True iff there are no pairs in the list
   */
  public boolean isEmpty() {
    return getSize() == 0;
  }
}
