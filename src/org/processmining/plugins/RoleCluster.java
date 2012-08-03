package org.processmining.plugins;

import it.processmining.clustering.hierarchical.HATreeNode;



public class RoleCluster {

	HATreeNode root;
	
	public RoleCluster(HATreeNode root) {
		this.root = root;
	}
	
	public HATreeNode getRoot() {
		return root;
	}
	
}
