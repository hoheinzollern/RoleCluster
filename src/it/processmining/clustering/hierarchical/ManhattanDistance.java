package it.processmining.clustering.hierarchical;

/**
 * Manhattan distance 
 * 
 * @author Alessandro Bruni <alessandro.bruni@gmail.com>
 *
 */
public class ManhattanDistance implements Distance {

	private static ManhattanDistance instance = new ManhattanDistance();

	public double computeDistance(HATreeNode n1, HATreeNode n2) {
		double[] a1 = n1.getData();
		double[] a2 = n2.getData();
		double sum = 0;

		for (int i = 0; i < a1.length; i++) {
			sum += Math.abs(a1[i] - a2[i]);
		}

		return sum;
	}
	
	private ManhattanDistance() {}
	
	public static ManhattanDistance create() {
		return instance;
	}

}
