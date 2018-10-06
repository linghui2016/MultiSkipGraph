package base;

import java.util.concurrent.ConcurrentHashMap;

import org.graphstream.graph.Graph;

import multiSkipGraph.MsController;

/**
 * The super class of all kinds of nodes.
 *
 * @author Linghui Luo
 *
 */
public abstract class Node extends Thread {
	protected Graph graph;
	protected org.graphstream.graph.Node graphNode;
	protected Integer id;
	protected ConcurrentHashMap<Integer, Integer> messagesCheck;
	protected boolean showImplicit = false;
	protected int numOfNodes;

	public Node(Integer id, Graph graph) {
		this.id = id;
		this.graph = graph;
		this.messagesCheck = new ConcurrentHashMap<Integer, Integer>();
		this.numOfNodes = MsController.getInstance().getNumOfNodes();
		this.addNodeToGraph(this.id.toString());
	}

	protected org.graphstream.graph.Node addNodeToGraph(String name) {
		if (this.graph != null) {
			if (this.graph != null) {
				final org.graphstream.graph.Node node = this.graph.addNode(name);
				node.setAttribute("ui.style", "size: 30px; fill-color: lightblue; stroke-mode: plain; stroke-color: black;text-size:12;");
				node.setAttribute("ui.label", this.id);

				if (name.equals(this.id.toString())) {
					this.graphNode = node;
				}
				return node;
			}
		}
		return null;
	}

	protected org.graphstream.graph.Node addNodeToGraph(String name, int level) {
		if (this.graph != null) {
			if (this.graph != null) {
				final org.graphstream.graph.Node node = this.graph.addNode(name);
				if (level > MsController.getInstance().getMaxLevel()) {
					node.setAttribute("ui.style", "size: 30px; fill-color: grey; stroke-mode: plain; stroke-color: black;");
				} else {
					int left = (int) (this.id - Math.pow(2, level));
					int right = (int) (this.id + Math.pow(2, level));
					if (left >= 0 || right < this.numOfNodes) {
						if (level % 2 == 0) {
							node.setAttribute("ui.style", "size: 30px; fill-color: lightblue; stroke-mode: plain; stroke-color: black;text-size:12;");
						} else {
							node.setAttribute("ui.style", "size: 30px; fill-color: pink; stroke-mode: plain; stroke-color: black;text-size:12;");
						}
					} else {
						node.setAttribute("ui.style", "size: 30px; fill-color: grey; stroke-mode: plain; stroke-color: black;text-size:12;");
					}
				}
				node.setAttribute("ui.label", this.id);
				if (name.equals(this.id.toString())) {
					this.graphNode = node;
				}
				return node;
			}
		}
		return null;
	}

	public Graph getGraph() {
		return this.graph;
	}

	public Integer getID() {
		return this.id;
	}

}
