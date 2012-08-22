package it.processmining.clustering.hierarchical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Hierarchical Agglomerative Clusterer
 * See: http://en.wikipedia.org/wiki/Hierarchical_clustering
 * 
 * 
 * @author Alessandro Bruni <alessandro.bruni@gmail.com
 *
 */
public class HAClusterer {
	public class NodeNotFoundException extends Exception {
		private static final long serialVersionUID = 6706230474313414225L;
	}
	
	private List<HATreeNode> instances = new ArrayList<HATreeNode>();
	private HashMap<Integer, HATreeNode> nodesById = new HashMap<Integer, HATreeNode>();
	private int nextId = 0;
	private int mLength;
	private Distance dist;

	/**
	 * @return the number of dimensions 
	 */
	public int getLength() {
		return mLength;
	}

	/**
	 * Public constructor.
	 * 
	 * @param length the cardinality of the space vector (= size)
	 */
	public HAClusterer(int length, Distance distance) {
		mLength = length;
		this.dist = distance;
	}

	/**
	 * Adds an instance. Checks if the provided data conforms to the required length,
	 * builds and return a leaf node.
	 * 
	 * @param data
	 * @param name
	 * @return
	 */
	public HATreeNode addInstance(double[] data, String name) {
		assert (data.length == getLength());
		HATreeNode instance = new HATreeNode(nextId++, name, data);
		instances.add(instance);
		nodesById.put(instance.getId(), instance);
		return instance;
	}

	/**
	 * Clusters the instances
	 * 
	 * @return
	 */
	public HACluster cluster() {
		List<HATreeNode> nodes = new ArrayList<HATreeNode>(instances);
		double[][] distanceMatrix = new double[nextId + nextId - 1][nextId + nextId - 1];

		// Step 0: compute initial distances
		for (int i = 0; i < nextId; i++) {
			HATreeNode n1 = nodesById.get(i);
			for (int j = i + 1; j < nextId; j++) {
				HATreeNode n2 = nodesById.get(j);
				distanceMatrix[i][j] = dist.computeDistance(n1, n2);
				distanceMatrix[j][i] = distanceMatrix[i][j];
			}
		}
		
		// Step 1: build the hierarchical tree
		HATreeNode result = null;
		while (nodes.size() > 1) {
			double minDistance = Double.MAX_VALUE;
			HATreeNode sel1 = null, sel2 = null;

			// Find minimum distance nodes
			for (HATreeNode n1 : nodes) {
				for (HATreeNode n2 : nodes) {
					if (n1.getId() < n2.getId()) {
						double distance = distanceMatrix[n1.getId()][n2.getId()];
						if (distance < minDistance) {
							minDistance = distance;
							sel1 = n1;
							sel2 = n2;
						}
					}
				}
			}
			
			// Remove them and create their centroid
			nodes.remove(sel1);
			nodes.remove(sel2);
			result = createCentroid(sel1, sel2, minDistance);
			nodes.add(result);
			
			// Update distance matrix
			for (int i = 0; i < nextId; i++) {
				int j = result.getId();
				HATreeNode n1 = nodesById.get(i);
				distanceMatrix[i][j] = dist.computeDistance(n1, result);
				distanceMatrix[j][i] = distanceMatrix[i][j];
			}
		}
		return new HACluster(result, distanceMatrix, nodesById);
	}

	/**
	 * Creates a centroid for the two nodes
	 * 
	 * @param n1 the first node
	 * @param n2 the second node
	 * @param distance the computed distance between the two nodes
	 * @return their centroid
	 */
	private HATreeNode createCentroid(HATreeNode n1, HATreeNode n2, double distance) {
		double[] a1 = n1.getData();
		double[] a2 = n2.getData();

		double[] a3 = new double[getLength()];

		for (int i = 0; i < getLength(); i++) {
			a3[i] = (a1[i] + a2[i]) / 2;
		}

		HATreeNode inst = new HATreeNode(nextId++, "", n1, n2, distance, a3);
		nodesById.put(inst.getId(), inst);
		return inst;
	}
}