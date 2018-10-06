package Graph;

import java.util.ArrayList;

import org.graphstream.graph.Graph;

import MultiSkipGraph.MsController;
import MultiSkipGraph.MsNode;

/**
 * It is used to generate an undirected connected graph.
 *
 * @author Linghui Luo
 *
 */
public class GeneratorNode {
	private int color; // used by BFS
	private int degree;
	private final Integer id;
	private final ArrayList<Integer> neighborhood;

	public GeneratorNode(Integer id) {
		this.id = id;
		this.neighborhood = new ArrayList<Integer>();
		this.degree = 0;
		this.color = 0;// unvisited
	}

	protected void addNeighbor(Integer neighbor) {
		this.neighborhood.add(neighbor);
		this.increaseDegree();
	}

	/**
	 * this method converts this GeneratorNode to an MsNode
	 *
	 * @param graph
	 * @return
	 */
	protected MsNode convertToMsNode(Graph graph) {
		int maxLevel = MsController.getInstance().getMaxLevel();
		return new MsNode(this.id, graph, maxLevel);
	}

	protected int getColor() {
		return this.color;
	}

	protected int getDegree() {
		return this.degree;
	}

	protected int getId() {
		return this.id;// visited
	}

	protected ArrayList<Integer> getNeighborhood() {
		return this.neighborhood;
	}

	protected void increaseDegree() {
		this.degree++;
	}

	protected void setColor() {
		this.color = 1;
	}
};
