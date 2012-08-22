package it.processmining.clustering.hierarchical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Represents a hierarchical agglomerative cluster, with the distance matrix between
 * the tree nodes.
 * 
 * @author Alessandro Bruni <alessandro.bruni@gmail.com>
 *
 */
public class HACluster {

	private HATreeNode rootNode;
	private double[][] distanceMatrix;
	private HashMap<Integer, HATreeNode> instances;
	private int currentGroupId;
	
	/**
	 * Builds a hierarchical cluster, only used by HAClusterer
	 * 
	 * @param rootNode
	 * @param distanceMatrix
	 * @param instances
	 */
	HACluster(HATreeNode rootNode, double[][] distanceMatrix, HashMap<Integer, HATreeNode> instances) {
		this.rootNode = rootNode;
		this.distanceMatrix = distanceMatrix;
		this.instances = instances;
	}
	
	/**
	 * @return the root node of the hierarchy three
	 */
	public HATreeNode getRootNode() {
		return rootNode;
	}

	/**
	 * Returns the normalized distance between node i and node j
	 * 
	 * @param i the index of the first node
	 * @param j the index of the second node
	 * @return
	 */
	public double getNormalizedDistance(int i, int j) {
		return distanceMatrix[i][j] / rootNode.getMaxDistance();
	}
	
	/**
	 * Returns the node instance from its id
	 * 
	 * @param id the node id
	 * @return the node
	 */
	public HATreeNode getInstance(int id) {
		return instances.get(id);
	}
	
	/**
	 * Builds a cluster of nodes from the hierarchy tree.
	 * 
	 * @param threshold a value in [0,1]. Every subtree whose distance is above the threshold is split,
	 * while the rest is put together.
	 * @return a map between group ids and sets of instance nodes, which represents the cluster
	 */
	public HashMap<Integer, Collection<HATreeNode>> buildGroupsCutAt(double threshold) {
		HashMap<Integer, Collection<HATreeNode>> groupMap = new HashMap<Integer, Collection<HATreeNode>>();
		currentGroupId = 0;
		
		buildGroupsCutAtRec(rootNode, groupMap, threshold);
		
		return groupMap;
	}
	
	/**
	 * Recursive function that does the dirty job
	 * 
	 * @param node
	 * @param groupMap
	 * @param threshold
	 */
	private void buildGroupsCutAtRec(HATreeNode node, HashMap<Integer, Collection<HATreeNode>> groupMap, double threshold) {
		if (node.getMaxDistance() / rootNode.getMaxDistance() > threshold) {
			/* Just go down, splitting the left child's groups from the right child's groups */
			buildGroupsCutAtRec(node.getLeft(), groupMap, threshold);
			currentGroupId++;
			buildGroupsCutAtRec(node.getRight(), groupMap, threshold);
		} else {
			if (!node.isLeaf()) {
				/* Don't separate the subtrees, since we are underneath the threshold */
				buildGroupsCutAtRec(node.getLeft(), groupMap, threshold);
				buildGroupsCutAtRec(node.getRight(), groupMap, threshold);
			} else {
				/* We just encountered a leaf, so we add it to the current group */
				Collection<HATreeNode> nodes; 
				if (groupMap.containsKey(currentGroupId)) {
					nodes = groupMap.get(currentGroupId);
				} else {
					nodes = new ArrayList<HATreeNode>();
					groupMap.put(currentGroupId, nodes);
				}
				nodes.add(node);
			}
		}
	}
}
