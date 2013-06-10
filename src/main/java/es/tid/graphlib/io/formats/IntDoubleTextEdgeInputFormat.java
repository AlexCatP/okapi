/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package es.tid.graphlib.io.formats;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.giraph.io.EdgeReader;
import org.apache.giraph.io.formats.IntNullTextEdgeInputFormat;
import org.apache.giraph.io.formats.TextEdgeInputFormat;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import es.tid.graphlib.utils.IntPairDoubleVal;

/**
 * Simple text-based {@link org.apache.giraph.io.EdgeInputFormat} for
 * weighted graphs with int ids int values.
 *
 * Each line consists of: source_vertex, target_vertex
 */
public class IntDoubleTextEdgeInputFormat extends
    TextEdgeInputFormat<IntWritable, DoubleWritable> {
  /** Splitter for endpoints */
  private static final Pattern SEPARATOR = Pattern.compile("[\t ]");

  @Override
  public EdgeReader<IntWritable, DoubleWritable> createEdgeReader(
      InputSplit split, TaskAttemptContext context) throws IOException {
    return new IntDoubleTextEdgeReader();
  }

  /**
   * {@link org.apache.giraph.io.EdgeReader} associated with
   * {@link IntNullTextEdgeInputFormat}.
   */
  public class IntDoubleTextEdgeReader extends
      TextEdgeReaderFromEachLineProcessed<IntPairDoubleVal> {
    @Override
    protected IntPairDoubleVal preprocessLine(Text line) throws IOException {
      String[] tokens = SEPARATOR.split(line.toString());
      return new IntPairDoubleVal(Integer.valueOf(tokens[0]),
          Integer.valueOf(tokens[1]), Double.valueOf(tokens[2]));
    }

    @Override
    protected IntWritable getSourceVertexId(IntPairDoubleVal endpoints)
      throws IOException {
      return new IntWritable(endpoints.getFirst());
    }

    @Override
    protected IntWritable getTargetVertexId(IntPairDoubleVal endpoints)
      throws IOException {
      return new IntWritable(endpoints.getSecond());
    }

    @Override
    protected DoubleWritable getValue(IntPairDoubleVal endpoints) throws IOException {
      return new DoubleWritable(endpoints.getValue());
    }
  }
}
