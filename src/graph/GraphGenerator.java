package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import base.Action;
import base.Direction;
import multiSkipGraph.MsChannel;
import multiSkipGraph.MsController;
import multiSkipGraph.MsMessage;
import multiSkipGraph.MsNode;

/**
 * 
 * @author Linghui Luo
 *
 */
public class GraphGenerator {
	/**
	 * For each node we need at least one edge.
	 *
	 * Start with one node. In each iteration, create a new node and a new edge.
	 * The edge is to connect the new node with a random node from the previous
	 * node set.
	 *
	 * this algorithm needs O(n) and creates n nodes connected by n-1 edges.
	 */
	private HashMap<Integer, GeneratorNode> connectNodes(int numOfNodes, boolean withLimitForDegree, int maxDegree) {
		final HashMap<Integer, GeneratorNode> nodes = new HashMap<Integer, GeneratorNode>();
		for (int i = 0; i < numOfNodes; i++) {
			nodes.put(i, new GeneratorNode(i));
		}
		if (numOfNodes > 1) {
			final ArrayList<Integer> coherentGraph = new ArrayList<Integer>();// nodes
			// which
			// are
			// connected
			final ArrayList<Integer> restNodes = new ArrayList<Integer>();// nodes
			// which are
			// not
			// selected
			for (final Integer k : nodes.keySet()) {
				restNodes.add(k);
			}
			GeneratorNode current;
			GeneratorNode neighbor;
			final Random random = new Random();

			// At beginning we just have one node in the coherent subgraph
			final int randomIndex = random.nextInt(restNodes.size());
			current = nodes.get(randomIndex);
			restNodes.remove(current.getId());
			coherentGraph.add(current.getId());

			// generate edges to connect the rest nodes with the coherent
			// subgraph
			int currentIndex = 0;
			int neighborIndex = 0;
			boolean done = false;
			while (restNodes.size() > 0) {
				currentIndex = random.nextInt(coherentGraph.size());
				neighborIndex = random.nextInt(restNodes.size());
				current = nodes.get(coherentGraph.get(currentIndex));
				neighbor = nodes.get(restNodes.get(neighborIndex));
				if (random.nextBoolean())// the direction of the edge can be
											// current--->neighbor or otherwise
				{
					if (!withLimitForDegree || current.getDegree() < maxDegree) {
						nodes.get(current.getId()).addNeighbor(neighbor.getId());
						nodes.get(neighbor.getId()).increaseDegree();
						done = true;
					}

				} else {
					if (!withLimitForDegree || current.getDegree() < maxDegree) {
						nodes.get(neighbor.getId()).addNeighbor(current.getId());
						nodes.get(current.getId()).increaseDegree();
						done = true;
					}
				}
				if (done) {
					restNodes.remove(neighborIndex);
					coherentGraph.add(neighbor.getId());
					done = false;
				}
			}
		}
		return nodes;
	}

	/**
	 * this method converts a graph of type GeneratorNode to a graph of type
	 * MsNode
	 *
	 * @param nodes
	 * @return
	 */
	public HashMap<Integer, MsNode> convertToGraphWithMsNode(HashMap<Integer, GeneratorNode> nodes, Graph graph) {
		HashMap<Integer, MsNode> res = new HashMap<Integer, MsNode>();
		for (final GeneratorNode node : nodes.values()) {
			res.put(node.getId(), node.convertToMsNode(graph));
		}
		res = this.defineEdgesForMsNode(nodes, res);
		MsChannel.getInstance().setStartDegreeMap();
		return res;
	}

	/**
	 * this method defines the edges in the generated graph either it is
	 * explicit or implicit.
	 *
	 * @param nodes
	 * @param res
	 * @return
	 */
	private HashMap<Integer, MsNode> defineEdgesForMsNode(HashMap<Integer, GeneratorNode> nodes, HashMap<Integer, MsNode> res) {
		final Random random = new Random();
		int maxLevel = MsController.getInstance().getMaxLevel();
		for (final GeneratorNode node : nodes.values()) {
			// set the edge explicit or implicit
			final ArrayList<Integer> doneList = new ArrayList<Integer>();
			/**
			 * store the neighbor which the edges to it has been defined.
			 */
			for (final Integer it : node.getNeighborhood()) {
				if (!doneList.contains(it)) {
					int level = random.nextInt(maxLevel + 1);
					if (random.nextBoolean())// explicit edge
					{
						if (it < node.getId())// explicit edge
						{
							res.get(node.getId()).addLevelNeighbor(res.get(it), level, Direction.LEFT, false);
						} else {
							res.get(node.getId()).addLevelNeighbor(res.get(it), level, Direction.RIGHT, false);
						}
					} else {
						if (random.nextBoolean()) {
							res.get(node.getId())
									.receiveMessage(new MsMessage(MsChannel.getInstance().generateMsgId(), Action.INTRODUCE, res.get(it)));
						} else {
							res.get(node.getId()).receiveMessage(
									new MsMessage(MsChannel.getInstance().generateMsgId(), Action.INTRO_LEVEL_NODE, level, res.get(it)));
						}
					}
					doneList.add(it);
				}
			}
		}
		return res;

	}

	/**
	 * this method generates a connected graph with the constraint of maximal
	 * degree.
	 *
	 * @param numOfNodes
	 * @param numOfLeavingNodes
	 * @param maxDegree
	 * @return
	 */
	public HashMap<Integer, GeneratorNode> generateGraphWithMG(int numOfNodes, int numOfLeavingNodes, int maxDegree) {
		final HashMap<Integer, GeneratorNode> nodes = this.connectNodes(numOfNodes, true, maxDegree);
		// for each node generate a random degree d from 1 to maxDegree and
		// connect node with n nodes
		final Random random = new Random();
		GeneratorNode neighbor;
		for (final GeneratorNode node : nodes.values()) {
			int d = 1;
			while (true)// generate a random degree which should greater than 0,
						// because each node is at least connected with another
						// node
			{
				final int temp = random.nextInt(maxDegree + 1);
				if (temp > 0) {
					d = temp;
					break;
				}
			}
			int retries = 0;
			while (node.getDegree() < d && retries++ < 1000) {
				do {
					neighbor = nodes.get(random.nextInt(numOfNodes));
				} while (node.getId() == neighbor.getId());// we exclude the
															// edge which
															// points the node
															// itself
				if (random.nextBoolean()) {
					/**
					 * node-->neighbor can have maximal 2 edges, one explicit
					 * one implicit
					 *
					 */
					if (Collections.frequency(node.getNeighborhood(), neighbor.getId()) < 2

							&& neighbor.getDegree() < maxDegree)// edge
																																// node-->neighbor
					{
						nodes.get(node.getId()).addNeighbor(neighbor.getId());
						nodes.get(neighbor.getId()).increaseDegree();
					}
				} else {
					if (Collections.frequency(neighbor.getNeighborhood(), node.getId()) < 2// neighbor-->node
							// can
							// have
							// maximal
							// 2
							// edges,
							// one
							// explicit
							// one
							// implicit
							&& neighbor.getDegree() < maxDegree)// edge
					// neighbor--->node
					{
						nodes.get(neighbor.getId()).addNeighbor(node.getId());
						nodes.get(node.getId()).increaseDegree();
						retries = 0;
					}
				}
			}
		}
		return nodes;
	}

	/**
	 * This method convert a scale-free graph generated by the
	 * BarasiAlbertGenerator to a graph of type GeneratorNode.
	 *
	 * @param numOfNodes
	 * @param numOfLeavingNodes
	 * @param degree
	 * @return
	 */
	public HashMap<Integer, GeneratorNode> generateScaleFreeGraph(int numOfNodes, int numOfLeavingNodes, int degree) {
		final HashMap<Integer, GeneratorNode> nodes = new HashMap<Integer, GeneratorNode>();
		final Graph singleGraph = new SingleGraph("Barabasi-Albert");
		final Generator gen = new BarabasiAlbertGenerator(degree);
		gen.addSink(singleGraph);
		gen.begin();
		while (singleGraph.getNodeCount() < numOfNodes) {
			gen.nextEvents();
		}
		gen.end();
		for (final org.graphstream.graph.Node node : singleGraph.getEachNode())// Initialize
																				// nodes
		{
			final int id = Integer.parseInt(node.getId());
			final GeneratorNode v = new GeneratorNode(id);
			nodes.put(id, v);
		}
		for (final org.graphstream.graph.Node node : singleGraph.getEachNode())// add
																				// neighbors
																				// to
																				// each
																				// node
		{
			final int id = Integer.parseInt(node.getId());
			final Iterator<org.graphstream.graph.Node> it = node.getNeighborNodeIterator();
			while (it.hasNext()) {
				final org.graphstream.graph.Node neighbor = it.next();
				final int neighborID = Integer.parseInt(neighbor.getId());
				nodes.get(id).addNeighbor(neighborID);
			}
		}
		singleGraph.clear();
		return nodes;
	}

	//	/**
	//	 * this method generates a connected graph with constraint of average
	//	 * degree.
	//	 *
	//	 *
	//	 * @param numOfNodes
	//	 * @param numOfLeavingNodes
	//	 * @param avgDegree
	//	 * @return
	//	 */
	//	public HashMap<Integer, GeneratorNode> generatorGraphWithAG(int numOfNodes, int numOfLeavingNodes, int avgDegree) {
	//		final HashMap<Integer, GeneratorNode> nodes = new HashMap<Integer, GeneratorNode>();
	//		final Graph singleGraph = new SingleGraph("Random");
	//		final Generator gen = new RandomGenerator(avgDegree);
	//		ConnectedComponents cc = new ConnectedComponents();
	//		while (true) {
	//			gen.addSink(singleGraph);
	//			gen.begin();
	//			while (singleGraph.getNodeCount() < numOfNodes) {
	//				gen.nextEvents();
	//			}
	//			gen.end();
	//			cc.init(singleGraph);
	//			if (cc.getConnectedComponentsCount() != 1) {
	//				singleGraph.clear();
	//			} else {
	//				break;
	//			}
	//		}
	//		for (final org.graphstream.graph.Node node : singleGraph.getEachNode())// Initialize
	//																				// nodes
	//		{
	//			final int id = Integer.parseInt(node.getId());
	//			final GeneratorNode v = new GeneratorNode(id);
	//			nodes.put(id, v);
	//		}
	//		for (final org.graphstream.graph.Node node : singleGraph.getEachNode())// add
	//																				// neighbors
	//																				// to
	//																				// each
	//																				// node
	//		{
	//			final int id = Integer.parseInt(node.getId());
	//			final Iterator<org.graphstream.graph.Node> it = node.getNeighborNodeIterator();
	//			while (it.hasNext()) {
	//				final org.graphstream.graph.Node neighbor = it.next();
	//				final int neighborID = Integer.parseInt(neighbor.getId());
	//				nodes.get(id).addNeighbor(neighborID);
	//			}
	//		}
	//		singleGraph.clear();
	//		return nodes;
	//	}

	/**
	 * this method generates a connected graph with constraint of average
	 * degree.
	 *
	 *
	 * @param graph
	 * @param numOfNodes
	 * @param numOfLeavingNodes
	 * @param avgDegree
	 * @return
	 */
	public HashMap<Integer, GeneratorNode> generatorGraphWithAG(int numOfNodes, int numOfLeavingNodes, int avgDegree) {
		HashMap<Integer, GeneratorNode> nodes = this.connectNodes(numOfNodes, false, numOfNodes - 1);
		int sumOfEdges = Math.round(avgDegree * numOfNodes / 2.0f);
		sumOfEdges -= numOfNodes - 1;// N nodes need N-1 edges so that the graph
										// is connected
										// generates the rest edges
		GeneratorNode current;
		GeneratorNode neighbor;
		Random random = new Random();
		while (sumOfEdges != 0) {
			do {
				current = nodes.get(random.nextInt(numOfNodes));
				neighbor = nodes.get(random.nextInt(numOfNodes));
			} while (current.getId() == neighbor.getId());// we exclude the edge
															// which
															// points the node
															// itself
			if (random.nextBoolean()) {
				if (Collections.frequency(current.getNeighborhood(), neighbor.getId()) < 2) {
					nodes.get(current.getId()).addNeighbor(neighbor.getId());
					nodes.get(neighbor.getId()).increaseDegree();
					sumOfEdges--;
				} else {
					if (Collections.frequency(neighbor.getNeighborhood(), current.getId()) < 2) {
						nodes.get(neighbor.getId()).addNeighbor(current.getId());
						nodes.get(current.getId()).increaseDegree();
						sumOfEdges--;
					}
				}
			} else {
				if (Collections.frequency(neighbor.getNeighborhood(), current.getId()) < 2) {
					nodes.get(neighbor.getId()).addNeighbor(current.getId());
					nodes.get(current.getId()).increaseDegree();
					sumOfEdges--;
				} else {
					if (Collections.frequency(current.getNeighborhood(), neighbor.getId()) < 2) {
						nodes.get(current.getId()).addNeighbor(neighbor.getId());
						nodes.get(neighbor.getId()).increaseDegree();
						sumOfEdges--;
					}
				}
			}
		}
		return nodes;
	}

	/**
	 * this method generates a random connected graph.
	 *
	 * @param numOfNodes
	 * @param numOfLeavingNodes
	 * @return
	 */
	public HashMap<Integer, GeneratorNode> generatorRandomGraph(int numOfNodes, int numOfLeavingNodes) {
		final HashMap<Integer, GeneratorNode> nodes = this.connectNodes(numOfNodes, false, numOfNodes - 1);
		final Random random = new Random();
		/**
		 * 2N*(N-1) is max edges, N-1 edges are essential so that the graph is
		 * connected
		 */
		final int temp = 2 * numOfNodes * (numOfNodes - 1) - (numOfNodes - 1);
		int restOfEdges = random.nextInt(temp);
		// generates the rest edges
		GeneratorNode current;
		GeneratorNode neighbor;
		while (restOfEdges != 0) {
			do {
				current = nodes.get(random.nextInt(numOfNodes));
				neighbor = nodes.get(random.nextInt(numOfNodes));
			} while (current.getId() == neighbor.getId());// we exclude the edge
															// which
															// points the node
															// itself
			if (random.nextBoolean()) {
				if (Collections.frequency(current.getNeighborhood(), neighbor.getId()) < 2) {
					nodes.get(current.getId()).addNeighbor(neighbor.getId());
					nodes.get(neighbor.getId()).increaseDegree();
					restOfEdges--;
				} else {
					if (Collections.frequency(neighbor.getNeighborhood(), current.getId()) < 2) {
						nodes.get(neighbor.getId()).addNeighbor(current.getId());
						nodes.get(current.getId()).increaseDegree();
						restOfEdges--;
					}
				}
			} else {
				if (Collections.frequency(neighbor.getNeighborhood(), current.getId()) < 2) {
					nodes.get(neighbor.getId()).addNeighbor(current.getId());
					nodes.get(current.getId()).increaseDegree();
					restOfEdges--;
				} else {
					if (Collections.frequency(current.getNeighborhood(), neighbor.getId()) < 2) {
						nodes.get(current.getId()).addNeighbor(neighbor.getId());
						nodes.get(neighbor.getId()).increaseDegree();
						restOfEdges--;
					}
				}
			}
		}
		return nodes;
	}

	//	/**
	//	 * This method uses BFS to check if the graph is connected
	//	 *
	//	 * @param map
	//	 * @return
	//	 */
	//	private boolean isConnected(HashMap<Integer, GeneratorNode> map) {
	//		boolean connected = true;
	//		HashMap<Integer, GeneratorNode> visited = new HashMap<Integer, GeneratorNode>();
	//		Map.Entry<Integer, GeneratorNode> entry = map.entrySet().iterator().next();
	//		LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>();
	//		try {
	//			queue.put(entry.getKey());
	//			visited.put(entry.getKey(), entry.getValue());
	//			while (!queue.isEmpty()) {
	//				int u = queue.take();
	//				GeneratorNode node = map.get(u);
	//				for (int id : node.getNeighborhood()) {
	//					GeneratorNode adj = map.get(id);
	//					if (adj.getColor() == 0) {
	//						adj.setColor();
	//						visited.put(id, adj);
	//						if (!queue.contains(adj)) {
	//							queue.put(adj.getId());
	//						}
	//					}
	//				}
	//			}
	//		} catch (InterruptedException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		if (visited.size() != visited.size()) {
	//			connected = false;
	//		}
	//		return connected;
	//	}
}
