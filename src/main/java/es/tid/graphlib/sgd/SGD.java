package es.tid.graphlib.sgd;

import org.apache.giraph.Algorithm;
import org.apache.giraph.graph.DefaultEdge;
import org.apache.giraph.graph.Edge;
import org.apache.giraph.vertex.EdgeListVertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.log4j.Logger;
import es.tid.graphlib.examples.MessageWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates the Pregel Stochastic Gradient Descent (SGD) implementation.
 */
@Algorithm(
    name = "Stochastic Gradient Descent (SGD)",
    description = "Minimizes the error in users preferences predictions"
)

public class SGD extends EdgeListVertex<IntWritable, DoubleArrayListWritable, 
IntWritable, MessageWrapper>{
	/** The convergence tolerance */
	static double INIT=0.5;
	/** SGD vector size **/
	static int SGD_VECTOR_SIZE=2;
	/** Regularization parameter */
	static double LAMBDA= 0.005;
	/** Learning rate */
	static double GAMMA=0.01;
	/** Number of supersteps */
	static double ITERATIONS=5;
	/** Max rating */
	static double MAX=5;
	/** Min rating */
	static double MIN=0;
	/** Error */    
	public double e;
	/** Observed Value - Rating */
	private double observed;
	/** Type of vertex
	 * 0 for user, 1 for item */
	private boolean item=false;
	
	/** Class logger */
	private static final Logger LOG =
			Logger.getLogger(SGD.class);

	public void compute(Iterable<MessageWrapper> messages) {
		/** Value of Vertex */
		DoubleArrayListWritable value = new DoubleArrayListWritable();

		/** Array List with errors for printing in the last superstep */
		//ArrayList<Double> errors = new ArrayList<Double>();
		HashMap<Integer,Double> errmap = new HashMap<Integer,Double>();
				
		System.out.println("*******  Vertex: "+getId()+", superstep:"+getSuperstep());
		//System.out.println("*******  Vertex: "+getSourceVertexId()+", superstep:"+getSuperstep());

		/** First Superstep for users */
		if (getSuperstep()==0) {
			for (int i=0; i<SGD_VECTOR_SIZE; i++) {
				value.add(new DoubleWritable(INIT));
			}
			setValue(value);
		}
		/** First Superstep for items */
		if (getSuperstep()==1) {		
			
			for (int i=0; i<SGD_VECTOR_SIZE; i++) {
				value.add(new DoubleWritable(INIT));
			}
			setValue(value);
			item=true;
		}
		
		System.out.println("item:" + item);
		/*** For each message */
		for (MessageWrapper message : messages) {
			/*** Debugging */
			if (LOG.isDebugEnabled()) {
				LOG.debug("Vertex " + getId() + " predicts for item " +
						message.getSourceId().get());
			}
			System.out.println("-I am vertex " + getId() + 
					" and received from " + message.getSourceId().get());

			/** Start receiving message from the second superstep */
			if (getSuperstep()==1) {							
				// Save its rating given from the user
				observed = message.getMessage().get(message.getMessage().size()-1).get();
				IntWritable sourceId = message.getSourceId();
				DefaultEdge<IntWritable, IntWritable> edge = new DefaultEdge<IntWritable, IntWritable>();
				edge.setTargetVertexId(sourceId);
				edge.setValue(new IntWritable((int) observed));
				System.out.println("   Adding edge:"+edge);
				addEdge(edge);
				// Remove the last value from message - it's there for the 1st round
				message.getMessage().remove(message.getMessage().size()-1);				
			}
			/*** Calculate error */
			observed = (double)getEdgeValue(message.getSourceId()).get();
			e = getError(observed, getValue(), message.getMessage());
			System.out.println("ERROR = " + e);
			/** user_vector = vertex_vector + 
			 * 2*GAMMA*(real_value - 
			 * dot_product(vertex_vector,other_vertex_vector))*other_vertex_vector + 
			 * LAMBDA * vertex_vector */
			System.out.println("BEFORE:vertex_vector=" + getValue().get(0).get() + "," + getValue().get(1).get()); 
			setValue(dotAddition(dotAddition(getValue(), 
					numMatrixProduct((double) (2*GAMMA*e), message.getMessage())),
					numMatrixProduct((double) LAMBDA, getValue())));
			System.out.println("AFTER:vertex_vector=" + getValue().get(0).get() + "," + getValue().get(1).get()); 
			e = getError((double)getEdgeValue(message.getSourceId()).get(), 
					getValue(), message.getMessage());
			System.out.println("ERROR = " + e);
			if (getSuperstep() == ITERATIONS-2 && item==false 
					|| getSuperstep() == ITERATIONS-1 && item==false) {
				errmap.put(new Integer(getEdgeValue(message.getSourceId()).get()), e);
			}
		} // End of for each message
		System.out.print("I am vertex " + getId() + " and sent to ");
		
		if (getSuperstep()<ITERATIONS){
			/** Send to all neighbors a message*/
			for (Edge<IntWritable, IntWritable> edge : getEdges()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Vertex " + getId() + " sent a message to " +
							edge.getTargetVertexId());
				}
				/** Create a message and wrap together the source id and the message */
				MessageWrapper message = new MessageWrapper();
				message.setSourceId(getId());
				message.setMessage(getValue());
				
				if (getSuperstep()==0) {
					message.getMessage().add(
							new DoubleWritable(getEdgeValue(edge.getTargetVertexId()).get()));
				}
					
				sendMessage(edge.getTargetVertexId(), message);
				System.out.print(edge.getTargetVertexId() + " ");
			}				
			System.out.println();
			if (getSuperstep() == ITERATIONS-2 && item==false 
					|| getSuperstep() == ITERATIONS-1 && item==false) {
				for (Map.Entry<Integer, Double> entry : errmap.entrySet()) {
				    System.out.println("------ Error for item " + entry.getKey() + ": " + entry.getValue() + " -------");
				}
			}
		}
		else {
			System.out.println("nowhere! Time to sleep!");
		}
		voteToHalt();
	}//EofCompute

	/*** Calculate the error: e=observed-predicted */
	public double getError(double observed, DoubleArrayListWritable ma, DoubleArrayListWritable mb){
		/*** Predicted value */
		double predicted = dotProduct(ma,mb);
		if (predicted > MAX)	predicted = MAX;
		if (predicted < MIN)	predicted = MIN;
		return java.lang.Math.abs(observed-predicted);
	}
	public double  getError(){
		return e;
	}
	/*** Calculate the dot product of 2 vectors: vector1*vector2 */
	public double dotProduct(DoubleArrayListWritable ma, DoubleArrayListWritable mb){
		double result = ma.get(0).get() * mb.get(0).get() + ma.get(1).get() * mb.get(1).get();
		return result;
	}
	
	/*** Calculate the dot addition of 2 vectors: vector1+vector2 */
	public DoubleArrayListWritable dotAddition(
			DoubleArrayListWritable ma, 
			DoubleArrayListWritable mb){
		DoubleArrayListWritable result = new DoubleArrayListWritable();
		result.add(new DoubleWritable(ma.get(0).get() + mb.get(0).get()));
		result.add(new DoubleWritable(ma.get(1).get() + mb.get(1).get()));
		return result;
	}
	
	/*** Calculate the product num*matirx */
	public DoubleArrayListWritable numMatrixProduct(double num, DoubleArrayListWritable matrix){
		DoubleArrayListWritable result = new DoubleArrayListWritable();
		result.add(new DoubleWritable(num * matrix.get(0).get()));
		result.add(new DoubleWritable(num * matrix.get(1).get()));
		return result;
	}
}
