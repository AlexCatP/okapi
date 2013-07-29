/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package es.tid.graphlib.partitioning;

import java.io.IOException;

import org.apache.giraph.edge.Edge;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.formats.TextVertexOutputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * Simple text-based {@link org.apache.giraph.io.EdgeInputFormat} for graphs
 * with int ids.
 *
 * Each line consists of: vertex id, vertex value and option edge value
 */
public class PartitioningVertexOutputFormat extends
  TextVertexOutputFormat<IntWritable, IntWritable,
  IntWritable> {
  /** Specify the output delimiter */
  public static final String LINE_TOKENIZE_VALUE = "output.delimiter";
  /** Default output delimiter */
  public static final String LINE_TOKENIZE_VALUE_DEFAULT = "   ";
  /**
   * Create Vertex Writer
   *
   * @param context Context
   * @return new object TextIntIntVertexWriter
   */
  public TextVertexWriter
  createVertexWriter(TaskAttemptContext context) {
    return new TextIntIntVertexWriter();
  }

  /** Class TextIntIntVertexWriter */
  protected class TextIntIntVertexWriter
      extends TextVertexWriterToEachLine {
    /** Saved delimiter */
    private String delimiter;

    @Override
    public void initialize(TaskAttemptContext context)
      throws IOException, InterruptedException {
      super.initialize(context);
      Configuration conf = context.getConfiguration();
      delimiter = conf
        .get(LINE_TOKENIZE_VALUE, LINE_TOKENIZE_VALUE_DEFAULT);
    }

    @Override
    protected Text convertVertexToLine
    (Vertex<IntWritable, IntWritable, IntWritable, ?> vertex)
      throws IOException {
      String id = vertex.getId().toString();
      String value = vertex.getValue().toString();
      int migrations = ((Partitioning) vertex).getMigrationsCounter();
      int initValue = ((Partitioning) vertex).getInitialPartition();
      int localEdges = 0;
      for (Edge<IntWritable, IntWritable> edge : vertex.getEdges()) {
        if (edge.getValue().get() == vertex.getValue().get()) {
          localEdges++;
        }
      }
      Text line = new Text("id:" + delimiter + id + delimiter +
        "initVal:" + delimiter + initValue + delimiter +
        "finVal:" + delimiter + value + delimiter +
        "migrations:" + delimiter + migrations + delimiter +
        "localEdges / totalEdges:" + delimiter + localEdges + " / " +
        vertex.getNumEdges());
      return new Text(line);
    }
  }
}
