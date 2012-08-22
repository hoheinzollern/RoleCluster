package it.processmining.clustering.hierarchical;

/**
 * Euclidean distance
 * 
 * @author Alessandro Bruni <alessandro.bruni@gmail.com>
 *
 */
public class EuclideanDistance implements Distance {
	
	private static EuclideanDistance instance = new EuclideanDistance();

	public double computeDistance(HATreeNode n1, HATreeNode n2) {
		double[] a1 = n1.getData();
		double[] a2 = n2.getData();
		double sum = 0;

		for (int i = 0; i < a1.length; i++) {
			sum += (a1[i] - a2[i]) * (a1[i] - a2[i]);
		}

		return Math.sqrt(sum);
	}
	
	private EuclideanDistance() {}
	
	public static EuclideanDistance create() {
		return instance;
	}

}
