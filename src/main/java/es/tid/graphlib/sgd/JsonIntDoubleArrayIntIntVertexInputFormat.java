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
package es.tid.graphlib.sgd;

//import org.apache.giraph.examples.SimpleTriangleClosingVertex.IntArrayListWritable;
import org.apache.giraph.graph.DefaultEdge;
import org.apache.giraph.graph.Edge;
import org.apache.giraph.io.formats.TextVertexInputFormat;
import org.apache.giraph.vertex.Vertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

/**
  * VertexInputFormat that features: 
  * <code>int</code> vertex ID,
  * <code>double</code> vertex values,
  * <code>int</code> edge weights, and 
  * <code>int</code> message types,
  *  specified in JSON format.
  */

public class JsonIntDoubleArrayIntIntVertexInputFormat extends
  TextVertexInputFormat<IntWritable, DoubleArrayListWritable,
  IntWritable, IntWritable> {

  @Override
  public TextVertexReader createVertexReader(InputSplit split,
      TaskAttemptContext context) {
    return new JsonIntDoubleIntIntVertexReader();
  }

 /**
  * VertexReader that features <code>double</code> vertex
  * values and <code>int</code> out-edge weights. The
  * files should be in the following JSON format:
  * JSONArray(<vertex id>, 
  * 	JSONArray(<vertex value x>, <vertex value y>),
  * 	JSONArray(JSONArray(<dest vertex id>, <edge value>), ...))
  * 
  * Here is an example with vertex id 1, vertex values 4.3 and 2.1,
  * and two edges.
  * First edge has a destination vertex 2, edge value 2.1.
  * Second edge has a destination vertex 3, edge value 0.7.
  * [1,[4.3,2.1],[[2,2.1],[3,0.7]]]
  */
  class JsonIntDoubleIntIntVertexReader extends
    TextVertexReaderFromEachLineProcessedHandlingExceptions<JSONArray,
    JSONException> {
	  
    @Override
    protected JSONArray preprocessLine(Text line) throws JSONException {
      return new JSONArray(line.toString());
    }

  /*  protected IntWritable getId(JSONArray jsonVertex) throws
    JSONException, IOException {
    	JSONArray jsonIdArray = jsonVertex.getJSONArray(0);
    	IntArrayListWritable id = new IntArrayListWritable();
    	for (int i=0; i< jsonIdArray.length(); ++i){
    		id.add(new IntWritable(jsonIdArray.getInt(i)));
    	}
    	return id;
    }*/
    
    @Override
    protected IntWritable getId(JSONArray jsonVertex) throws 
    JSONException, IOException {
      return new IntWritable(jsonVertex.getInt(0));
    }
    
    /*
    @Override
    protected IntArrayListWritable getId(JSONArray jsonVertex) throws
    JSONException, IOException {
    	// Create a JSON array for the first field of the line
    	JSONArray jsonIdArray = jsonVertex.getJSONArray(0);
    	// Create an object 
    	IntArrayListWritable ids = new IntArrayListWritable();
    	for (int i=0; i < jsonIdArray.length(); ++i){
    		ids.add(new IntWritable(jsonIdArray.getInt(i)));
    	}
    	return ids;
    }*/
    
    protected DoubleArrayListWritable getValue(JSONArray jsonVertex) throws 
    JSONException, IOException {
    	// Create a JSON array for the second field of the line
    	JSONArray jsonValueArray = jsonVertex.getJSONArray(1);
    	// Create an object 
    	DoubleArrayListWritable values = new DoubleArrayListWritable();
    	for (int i=0; i < jsonValueArray.length(); ++i){
    		values.add(new DoubleWritable(jsonValueArray.getDouble(i)));
    	}
    	return values;
    }
   /* 
    @Override
    protected Iterable<Edge<IntArrayListWritable, IntWritable>> getEdges(
        JSONArray jsonVertex) throws JSONException, IOException {
    	// Create a JSON array for the third field of the line
    	JSONArray jsonEdgeArray = jsonVertex.getJSONArray(2);
    	// Create a List of objects
    	List<Edge<IntArrayListWritable, IntWritable>> edges =
    			Lists.newArrayListWithCapacity(jsonEdgeArray.length());
    	for (int i = 0; i < jsonEdgeArray.length(); ++i) {
    		// Create object
    		JSONArray jsonEdge = jsonEdgeArray.getJSONArray(i);
    		DefaultEdge<IntArrayListWritable, IntWritable> edge = 
    				new DefaultEdge<IntArrayListWritable, IntWritable>();
    		//edge.setTargetVertexId(new IntWritable(jsonEdge.getInt(0)));
//    		edge.setTargetVertexId(new IntWritable(jsonEdge.getJSONArray(0).getInt(0)));
    		edge.setTargetVertexId(new IntArrayListWritable(jsonEdge.getJSONObject(0).get()));
//    		edge.setTargetVertexId(new IntArrayListWritable(jsonEdge.));
    		edge.setValue(new IntWritable(jsonEdge.getInt(1)));
    		edges.add(edge);
    	}
      return edges;
    }*/

    @Override
    protected Iterable<Edge<IntWritable, IntWritable>> getEdges(
        JSONArray jsonVertex) throws JSONException, IOException {
    	// Create a JSON array for the third field of the line
    	JSONArray jsonEdgeArray = jsonVertex.getJSONArray(2);
    	// Create an object 
    	List<Edge<IntWritable, IntWritable>> edges =
    			Lists.newArrayListWithCapacity(jsonEdgeArray.length());
    	for (int i = 0; i < jsonEdgeArray.length(); ++i) {
    		JSONArray jsonEdge = jsonEdgeArray.getJSONArray(i);
    		DefaultEdge<IntWritable, IntWritable> edge = 
    				new DefaultEdge<IntWritable, IntWritable>();
    		edge.setTargetVertexId(new IntWritable(jsonEdge.getInt(0)));
    		edge.setValue(new IntWritable(jsonEdge.getInt(1)));
    		edges.add(edge);
    	}
      return edges;
    }
    
    @Override
    protected Vertex<IntWritable, DoubleArrayListWritable, IntWritable,
              IntWritable> handleException(Text line, JSONArray jsonVertex,
                  JSONException e) {
      throw new IllegalArgumentException(
          "Couldn't get vertex from line " + line, e);
    }

  }
}
