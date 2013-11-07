package ml.grafos.okapi.common.jblas;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.apache.hadoop.io.Writable;
import org.jblas.FloatMatrix;

public class FloatMatrixWritable extends FloatMatrix implements Writable {

  public FloatMatrixWritable() {
    super();
  }
  
  public FloatMatrixWritable(int rows, int columns, float... array) {
    super(rows, columns, array);
  }
  
  @Override
  public void readFields(DataInput input) throws IOException {
    int length = input.readInt();
    this.rows = input.readInt();
    this.columns = input.readInt();
    byte[] array = new byte[length]; 
    input.readFully(array);
    this.data = toFloatArray(array);
    this.length = data.length;
  }

  @Override
  public void write(DataOutput output) throws IOException {
    byte[] array = toByteArray(data);
    output.writeInt(array.length);
    output.writeInt(rows);
    output.writeInt(columns);
    output.write(array);
  }

  public byte[] toByteArray(float[] floatArray) {
    byte byteArray[] = new byte[floatArray.length*4]; 
    ByteBuffer byteBuf = ByteBuffer.wrap(byteArray); 
    FloatBuffer floatBuf = byteBuf.asFloatBuffer(); 
    floatBuf.put (floatArray); 
    return byteArray; 
  }

  public float[] toFloatArray(byte[] byteArray) {
    float floatArray[] = new float[byteArray.length/4]; 
    ByteBuffer byteBuf = ByteBuffer.wrap(byteArray); 
    FloatBuffer floatBuf = byteBuf.asFloatBuffer(); 
    floatBuf.get (floatArray); 
    return floatArray; 
  }

}
