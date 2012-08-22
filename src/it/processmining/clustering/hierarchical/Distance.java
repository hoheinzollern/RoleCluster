package it.processmining.clustering.hierarchical;

/**
 * Distance interface: computes distance between two nodes.
 * 
 * @author Alessandro Bruni <alessandro.bruni@gmail.com>
 */
public interface Distance {

	/**
	 * Computes the distance between two points in space
	 * 
	 * @param n1 the first point
	 * @param n2 the second point
	 * @return the distance value
	 */
	public double computeDistance(HATreeNode n1, HATreeNode n2);
	
}
