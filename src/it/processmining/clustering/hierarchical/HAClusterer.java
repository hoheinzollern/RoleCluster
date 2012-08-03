package it.processmining.clustering.hierarchical;

import java.util.ArrayList;
import java.util.List;

public class HAClusterer {
	private List<HATreeNode> instances = new ArrayList<HATreeNode>();
	private int nextId = 0;
	private int mLength;

	public int getLength() {
		return mLength;
	}

	public HAClusterer(int length) {
		mLength = length;
	}

	public HATreeNode addInstance(double[] data, String name) {
		assert (data.length == getLength());
		HATreeNode instance = new HATreeNode(nextId++, name, data);
		instances.add(instance);
		return instance;
	}

	public HATreeNode cluster() {
		List<HATreeNode> nodes = new ArrayList<HATreeNode>(instances);

		HATreeNode result = null;
		while (nodes.size() > 1) {
			double minDistance = Double.MAX_VALUE;
			HATreeNode sel1 = null, sel2 = null;
			for (HATreeNode n1 : nodes) {
				for (HATreeNode n2 : nodes) {
					if (n1.getId() < n2.getId()) {
						double distance = computeDistance(n1, n2);
						if (distance < minDistance) {
							minDistance = distance;
							sel1 = n1;
							sel2 = n2;
						}
					}
				}
			}
			nodes.remove(sel1);
			nodes.remove(sel2);
			result = createCentroid(sel1, sel2, minDistance);
			nodes.add(result);
		}
		return result;
	}

	private HATreeNode createCentroid(HATreeNode sel1, HATreeNode sel2, double distance) {
		double[] a1 = sel1.getData();
		double[] a2 = sel2.getData();

		double[] a3 = new double[getLength()];

		for (int i = 0; i < getLength(); i++) {
			a3[i] = (a1[i] + a2[i]) / 2;
		}

		HATreeNode inst = new HATreeNode(nextId++, "", sel1, sel2, distance, a3);
		return inst;
	}

	private double computeDistance(HATreeNode n1, HATreeNode n2) {
		double[] a1 = n1.getData();
		double[] a2 = n2.getData();
		double sum = 0;

		for (int i = 0; i < getLength(); i++) {
			sum += (a1[i] - a2[i]) * (a1[i] - a2[i]);
		}

		return Math.sqrt(sum);
	}
}