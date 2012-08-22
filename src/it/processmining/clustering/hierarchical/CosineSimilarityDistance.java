package it.processmining.clustering.hierarchical;

/**
 * Cosine similarity distance metrics
 * See: http://en.wikipedia.org/wiki/Cosine_similarity
 * 
 * @author Alessandro Bruni <alessandro.bruni@gmail.com>
 *
 */
public class CosineSimilarityDistance implements Distance {

	private static CosineSimilarityDistance instance = new CosineSimilarityDistance();

	public double computeDistance(HATreeNode n1, HATreeNode n2) {
		double[] a1 = n1.getData();
		double[] a2 = n2.getData();
		double dot = 0;
		double m1 = 0;
		double m2 = 0;

		for (int i = 0; i < a1.length; i++) {
			dot += a1[i] * a2[i];
			m1 += a1[i] * a1[i];
			m2 += a2[i] * a2[i];
		}
		
		return dot / (Math.sqrt(m1) * Math.sqrt(m2));
	}
	
	private CosineSimilarityDistance() {}
	
	public static CosineSimilarityDistance create() {
		return instance;
	}

}
