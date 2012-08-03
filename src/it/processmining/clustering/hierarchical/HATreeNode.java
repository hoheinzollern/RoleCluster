package it.processmining.clustering.hierarchical;

public class HATreeNode {
	private int id;
	private HATreeNode parent;
	private HATreeNode left;
	private HATreeNode right;
	private double distance;
	private double maxDistance;
	private double[] data;
	private String name;
	private int size;

	public HATreeNode(int id, String name, HATreeNode left, HATreeNode right, double distance, double[] data) {
		this.id = id;
		this.name = name;
		this.left = left;
		this.right = right;
		this.distance = distance;
		this.maxDistance = distance;
		this.data = data;

		this.size = 1;
		
		if (!isLeaf()) {
			left.parent = this;
			right.parent = this;
			this.size += left.size;
			this.size += right.size;
			this.maxDistance += Math.max(left.maxDistance, right.maxDistance);
		}
	}
	
	public HATreeNode(int id, String name, double[] data) {
		this(id, name, null, null, 0, data);
	}

	/**
	 * @return the node id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the node name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the parent node
	 */
	@SuppressWarnings("unused")
	private HATreeNode getParent() {
		return parent;
	}

	/**
	 * @return the left child
	 */
	public HATreeNode getLeft() {
		return left;
	}

	/**
	 * @return the right child
	 */
	public HATreeNode getRight() {
		return right;
	}

	/**
	 * @return the distance between its children
	 */
	public double getDistance() {
		return distance;
	}

	/**
	 * @return the maximum distance to a leaf
	 */
	public double getMaxDistance() {
		return maxDistance;
	}

	/**
	 * Depending whether the node is a real instance or a computed centroid,
	 * returns its position in the space.
	 * 
	 * @return the spatial position of this node
	 */
	public double[] getData() {
		return data;
	}

	/**
	 * @return the size of the whole tree
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return true if it has no childs
	 */
	public boolean isLeaf() {
		return left == null && right == null;
	}
}
