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
package es.tid.graphlib.als;

import org.apache.giraph.io.formats.TextVertexOutputFormat;
import org.apache.giraph.vertex.Vertex;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import es.tid.graphlib.utils.DoubleArrayListHashMapWritable;

import java.io.IOException;

/**
 * Simple text-based {@link org.apache.giraph.io.EdgeInputFormat} for
 * graphs with int ids.
 *
 * Each line consists of: vertex id, vertex value and option edge value
 */
public class TextIntDoubleArrayHashMapVertexOutputFormat extends
    TextVertexOutputFormat<IntWritable, DoubleArrayListHashMapWritable,
    IntWritable> {
	
	/** Specify the output delimiter */
	public static final String LINE_TOKENIZE_VALUE = "output.delimiter";
	/** Default output delimiter */
	public static final String LINE_TOKENIZE_VALUE_DEFAULT = "   ";

	public TextVertexWriter
    createVertexWriter(TaskAttemptContext context) {
		return new TextIntDoubleArrayVertexWriter();
	}
	
	 protected class TextIntDoubleArrayVertexWriter extends TextVertexWriterToEachLine {
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
		    (Vertex<IntWritable, DoubleArrayListHashMapWritable, IntWritable, ?> vertex)
		      throws IOException {
		    	
		    	boolean flagError = getContext().getConfiguration().getBoolean("als.printerr", false);
		    	boolean flagUpdates = getContext().getConfiguration().getBoolean("als.printupdates", false);
		    	String type="";
		    	if (((Als)vertex).isItem()==true) {
		        	type = "item";
		    	}
		        else {
		        	type = "user";
		        }
		    	String id = vertex.getId().toString();
		        String value = vertex.getValue().getLatentVector().toString();
		        String error = null;
		        String updates = null;
		        Text line = new Text(type + delimiter + id + delimiter + value);
		        if (flagError) {
		        	try{
		        		error = Double.toString((Math.abs(((Als)vertex).halt_factor)));
		        	} catch (Exception exc) {
		        		exc.printStackTrace();
		        	}
		        	line.append(delimiter.getBytes(),0,delimiter.length());
		        	line.append(error.getBytes(), 0, error.length());
		        }
		        if (flagUpdates){
		        	try{
		        		updates = Integer.toString(((Als)vertex).getUpdates()); //.toString();
		        	} catch (Exception exc) {
		        		exc.printStackTrace();
		        	}
		        	line.append(delimiter.getBytes(),0,delimiter.length());
		        	line.append(updates.getBytes(), 0, updates.length());
		        }
				return new Text(line);
		    }
	 }
}