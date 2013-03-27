package es.tid.graphlib.sgd;

import org.apache.giraph.Algorithm;
import org.apache.giraph.aggregators.DoubleSumAggregator;
import org.apache.giraph.graph.DefaultEdge;
import org.apache.giraph.graph.Edge;
import org.apache.giraph.master.DefaultMasterCompute;
import org.apache.giraph.vertex.EdgeListVertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import es.tid.graphlib.examples.MessageWrapper;
import es.tid.graphlib.examples.SimpleMasterComputeVertex;

import java.lang.Math;

/**
 * Demonstrates the Pregel Stochastic Gradient Descent (SGD) implementation.
 */
@Algorithm(
		name = "Stochastic Gradient Descent (SGD)",
		description = "Minimizes the error in users preferences predictions"
		)

public class SgdGeneral extends EdgeListVertex<IntWritable, DoubleArrayListWritable, 
IntWritable, MessageWrapper>{
	/** SGD vector size **/
	static int SGD_VECTOR_SIZE=2;
	/** Regularization parameter */
	static double LAMBDA= 0.005;
	/** Learning rate */
	static double GAMMA=0.01;
	/** Number of supersteps */
	static double ITERATIONS=10;
	/** Convergence Tolerance */
	static double TOLERANCE = 0.3;
	/** Max rating */
	static double MAX=5;
	/** Min rating */
	static double MIN=0;
	/** Decimals */
	static int DECIMALS=4;
	/** Error */    
	public double err;
	/** Observed Value - Rating */
	private double observed;
	/** Type of vertex
	 * 0 for user, 1 for item */
	private boolean item=false;
	/** RMSD Error */
	private double rmsdErr=0d;
	/** Factor Error: it may be RMSD or L2NORM on initial&final vector  */
	public double err_factor=0d;
	/** Initial vector value to be used for the L2Norm case */
	DoubleArrayListWritable initialValue = new DoubleArrayListWritable();
	/** Aggregators to get values from the workers to the master */
	public static final String RMSD_AGG = "rmsd.aggregator";
	
	public void compute(Iterable<MessageWrapper> messages) {
		/** Counter of messages received - different from getNumEdges() 
		 * because a neighbor may not send a message */
		int msgCounter = 0;

		/** Flag for checking if parameter for RMSD aggregator received */
		boolean rmsdFlag = getContext().getConfiguration().getBoolean("sgd.aggregate", false);
		/** Flag for checking which termination factor to use: basic, rmsd, l2norm */
		String factorFlag = getContext().getConfiguration().get("sgd.factor", "basic");
		
		/** If it's the first round for users (superstep: 0) or 
		 *  If it's the first round for items (superstep: 1)
		 */
		if (getSuperstep() < 2){ 
			initLatentVector();
		}
		/** First Superstep for items */
		if (getSuperstep() == 1) {		
			item=true;
		}
		/*System.out.println("*******  Vertex: "+getId()+", superstep:"+getSuperstep()+", item:" + item + 
				", [" + getValue().get(0).get() + "," + getValue().get(1).get() + "]"); 
		 */
		rmsdErr=0d;
		/*** For each message */
		for (MessageWrapper message : messages) {
			msgCounter++;
/*			System.out.println("  [RECEIVE] from " + message.getSourceId().get()
					+ " [" + message.getMessage().get(0) + "," + message.getMessage().get(1) + "]");
*/			DefaultEdge<IntWritable, IntWritable> edge = new DefaultEdge<IntWritable, IntWritable>();

			/** If first superstep for items --> store the rating given from user */
			if (getSuperstep()==1) {							
				observed = message.getMessage().get(message.getMessage().size()-1).get();
				//System.out.println("observed: " + observed);
				IntWritable sourceId = message.getSourceId();
				edge.setTargetVertexId(sourceId);
				edge.setValue(new IntWritable((int) observed));
				//System.out.println("   Adding edge:" + edge);
				addEdge(edge);
				// Remove the last value from message - it's there for the 1st round
				message.getMessage().remove(message.getMessage().size()-1);				
			}
			/*** Calculate error */
			observed = (double)getEdgeValue(message.getSourceId()).get();
			err = getError(getValue(), message.getMessage(), observed);
			/** user_vector = vertex_vector + 
			 * 2*GAMMA*(real_value - 
			 * dot_product(vertex_vector,other_vertex_vector))*other_vertex_vector + 
			 * LAMBDA * vertex_vector */
			//System.out.println("BEFORE: error = " + err + " vertex_vector= " + getValue());
			runSgdAlgorithm(message.getMessage());
			/*setValue(dotAddition(getValue(),
					numMatrixProduct((double) -GAMMA,
							(dotAddition(numMatrixProduct((double) err,message.getMessage()),
									numMatrixProduct((double) LAMBDA, getValue())))))); */
			err = getError(getValue(), message.getMessage(),observed);
			//System.out.println("AFTER: error = " + err + " vertex_vector = " + getValue());
			/* If termination flag is set to RMSD or RMSD aggregator is true */
			if (factorFlag.equals("rmsd") || rmsdFlag){
				rmsdErr+= Math.pow(err, 2);
				//System.out.println("rmsdErr: " + rmsdErr);
			}
		} // End of for each message
		
		// If basic factor specified
		if (factorFlag.equals("basic")){
			err_factor=TOLERANCE+1;
		}
		// If RMSD aggregator flag is true
		if (rmsdFlag){
			this.aggregate(RMSD_AGG, new DoubleWritable(rmsdErr));
		}
		if (factorFlag.equals("rmsd")){
			err_factor = getRMSD(msgCounter);
			//System.out.println("myRMSD: " + err_factor + ", numEdges: " + msgCounter);
		}
		// If termination factor is set to L2NOrm
		if (factorFlag.equals("l2norm")){
			err_factor = getL2Norm(initialValue, getValue());
			/*System.out.println("NormVector: sqrt((initial[0]-final[0])^2 + (initial[1]-final[1])^2): " 
					+ err_factor);*/
		}
		if (getSuperstep()==0 || (err_factor > TOLERANCE && getSuperstep()<ITERATIONS)){
			sendMsgs();
		}
		// err_factor is used in the OutputFormat file. --> To print the error
		if (factorFlag.equals("basic")){
			err_factor=err;
		}
		voteToHalt();
	}//EofCompute

	/*** Initialize Vertex Latent Vector */
	public void initLatentVector(){		
		for (int i=0; i<SGD_VECTOR_SIZE; i++) {
			getValue().add(new DoubleWritable(((double)(getId().get()+i) % 100d)/100d));
		}
		/** For L2Norm */
		initialValue = getValue();
	}
	
	/*** Modify Vertex Latent Vector based on SGD equation */
	public void runSgdAlgorithm(DoubleArrayListWritable vvertex){
		/** user_vector = vertex_vector + 
		 * 2*GAMMA*(real_value - 
		 * dot_product(vertex_vector,other_vertex_vector))*other_vertex_vector + 
		 * LAMBDA * vertex_vector */
		DoubleArrayListWritable la, ra, ga, val = new DoubleArrayListWritable();
		la = numMatrixProduct((double) LAMBDA, getValue());
		ra = numMatrixProduct((double) err,vvertex);
		ga = numMatrixProduct((double) -GAMMA, (dotAddition(ra, la)));
		val = dotAddition(getValue(), ga);
		//System.out.print("Latent Vector: " + val);
		keepXdecimals(val, DECIMALS);
		//System.out.println(" , 4 decimals: " + val);
		setValue(val);
	}
	
	/*** Decimal Precision of latent vector values */
	public void keepXdecimals(DoubleArrayListWritable value, int x){
		double num=1;
		for (int i=0; i<x; i++){
			num*=10;
		}
		for (int i=0; i<value.size(); i++){
			value.set(i, new DoubleWritable((double)(Math.round(value.get(i).get() * num) / num)));
		}
	}
	
	/*** Send messages to neighbours */
	public void sendMsgs(){
		/** Send to all neighbors a message*/
		for (Edge<IntWritable, IntWritable> edge : getEdges()) {
			/** Create a message and wrap together the source id and the message */
			MessageWrapper message = new MessageWrapper();
			message.setSourceId(getId());
			message.setMessage(getValue());
			if (getSuperstep()==0) {
				message.getMessage().add(new DoubleWritable(edge.getValue().get()));
			}
			sendMessage(edge.getTargetVertexId(), message);
/*			System.out.println("  [SEND] to " + edge.getTargetVertexId() + 
					" (rating: " + edge.getValue() + ")" +
					" [" + getValue().get(0) + "," + getValue().get(1) + "]");
*/			 // End of for each edge
		}
	}
	/*** Calculate the RMSD on the errors calculated by the current vertex */
	public double getRMSD(int msgCounter){
		return Math.sqrt(rmsdErr/msgCounter);
	}
	/*** Calculate the RMSD on the errors calculated by the current vertex */
	public double getL2Norm(DoubleArrayListWritable valOld, DoubleArrayListWritable valNew){
		double result=0;
		for (int i=0; i<valOld.size(); i++){
			result += Math.pow((valOld.get(i).get() - valNew.get(i).get()),2);
		}
		//System.out.println("L2norm: " + result);
		return Math.sqrt(result);
	}
	/*** Calculate the error: e=observed-predicted */
	public double getError(DoubleArrayListWritable ma, DoubleArrayListWritable mb, double observed){
		/*** Predicted value */
		double predicted = dotProduct(ma,mb);
		predicted = Math.min(predicted, MAX);
		predicted = Math.max(predicted, MIN);
		return predicted-observed;
	}

	/*** Calculate the dot product of 2 vectors: vector1*vector2 */
	public double dotProduct(DoubleArrayListWritable ma, DoubleArrayListWritable mb){
		double result = 0d;
		for (int i=0; i<SGD_VECTOR_SIZE; i++){
			result += (ma.get(i).get() * mb.get(i).get());
		}
		return result;
	}

	/*** Calculate the dot addition of 2 vectors: vector1+vector2 */
	public DoubleArrayListWritable dotAddition(
			DoubleArrayListWritable ma, 
			DoubleArrayListWritable mb){
		DoubleArrayListWritable result = new DoubleArrayListWritable();
		for (int i=0; i<SGD_VECTOR_SIZE; i++){
			result.add(new DoubleWritable(ma.get(i).get() + mb.get(i).get()));
		}
		return result;
	}

	/*** Calculate the product num*matirx */
	public DoubleArrayListWritable numMatrixProduct(double num, DoubleArrayListWritable matrix){
		DoubleArrayListWritable result = new DoubleArrayListWritable();
		for (int i=0; i<SGD_VECTOR_SIZE; i++){
			result.add(new DoubleWritable(num * matrix.get(i).get()));
		}
		return result;
	}
	
	/**
	 * MasterCompute used with {@link SimpleMasterComputeVertex}.
	 */
	public static class MasterCompute
	extends DefaultMasterCompute {
		@Override
		public void compute() {
			double numRatings=0;
			double totalRMSD=0;
			if (getSuperstep()>1){
				// In superstep=1 only half edges are created (users to items)
				if (getSuperstep()==2)
					numRatings = getTotalNumEdges();
				else
					numRatings = getTotalNumEdges()/2;

				totalRMSD = Math.sqrt(((DoubleWritable)getAggregatedValue(RMSD_AGG)).get()/numRatings);
					
				System.out.println("Superstep: " + getSuperstep() + ", [Aggregator] Added Values: " + getAggregatedValue(RMSD_AGG)
						+ " / " + numRatings
						+ " = " + ((DoubleWritable)getAggregatedValue(RMSD_AGG)).get()/numRatings
						+ " --> sqrt(): " + totalRMSD);
					
				getAggregatedValue(RMSD_AGG);
				if (totalRMSD < TOLERANCE){
					System.out.println("HALT!");
					haltComputation();
				}
			}
		} // Eof Compute()

		@Override
		public void initialize() throws InstantiationException,
		IllegalAccessException {
				registerAggregator(RMSD_AGG, DoubleSumAggregator.class);
		}
	}
}
