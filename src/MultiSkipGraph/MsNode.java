package MultiSkipGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;

import Base.Action;
import Base.Direction;
import Base.MessageTask;
import Base.Node;
import Base.Task.TaskType;

/**
 * MultilistNode is used for MultiSkipGraph Protocol.
 *
 * @author Linghui
 *
 */
public class MsNode extends Node {
	final Random random = new Random();
	private static final int MAX_DELAY = 10;
	private final MsChannel channel;
	protected LinkedBlockingQueue<MsTask> taskQueue;
	private final Timer timer;

	private final int maxLevel;
	// used for linear introduce
	private int introCountLeft;
	private int introCountRight;
	//used for level introduce
	private int introCountLevel;

	// Nodes are sorted in TreeMap according to the natural ordering of its IDs <nodeID,node>
	private final TreeMap<Integer, MsNode> leftNeighbors;
	private final TreeMap<Integer, MsNode> rightNeighbors;

	// Map for neighbors at each level.<level, node>
	private final TreeMap<Integer, MsNode> leftAtLevel;
	private final TreeMap<Integer, MsNode> rightAtLevel;

	//Map for neighbors with unknown levels <nodeID, node>
	private TreeMap<Integer, MsNode> leftUnknown;
	private TreeMap<Integer, MsNode> rightUnknown;
	// Map for neighbors stored in which level <nodeID,level>
	private final HashMap<Integer, Integer> leftLevelMap;
	private final HashMap<Integer, Integer> rightLevelMap;

	private int seq;
	// stores the current sequence number for each search(sid)
	private final HashMap<Integer, Integer> seqNums;
	// stores all the batches of search-message for each destID
	private final ConcurrentHashMap<Integer, ArrayList<MsMessage>> waitingFor;

	boolean naivAlgo;
	boolean slowGreedy;

	public MsNode(Integer id, Graph graph, int maxLevel) {
		this(id, graph, maxLevel, false);
	}

	public MsNode(Integer id, Graph graph, int maxLevel, boolean copy) {
		super(id, graph);
		this.maxLevel = maxLevel;
		if (graph != null) {
			this.setNodePosition();
		}
		this.showImplicit = MsController.getInstance().showImplicit();
		this.channel = MsChannel.getInstance();
		if (!copy) {
			this.channel.registerNode(this);
		}
		this.taskQueue = new LinkedBlockingQueue<MsTask>();
		this.timer = new Timer();
		this.introCountLeft = 0;
		this.introCountRight = 0;
		this.introCountLevel = 0;
		this.leftNeighbors = new TreeMap<Integer, MsNode>();
		this.rightNeighbors = new TreeMap<Integer, MsNode>();
		this.leftAtLevel = new TreeMap<Integer, MsNode>();
		this.rightAtLevel = new TreeMap<Integer, MsNode>();
		this.leftUnknown = new TreeMap<Integer, MsNode>();
		this.rightUnknown = new TreeMap<Integer, MsNode>();
		this.leftLevelMap = new HashMap<Integer, Integer>();
		this.rightLevelMap = new HashMap<Integer, Integer>();

		this.seq = 0;
		this.seqNums = new HashMap<Integer, Integer>();
		this.waitingFor = new ConcurrentHashMap<Integer, ArrayList<MsMessage>>();
	}

	protected MsNode copy(Graph newGraph) {
		MsNode clone = new MsNode(this.id, newGraph, this.maxLevel, true);
		return clone;
	}

	/**
	 * set node position
	 */
	private void setNodePosition() {
		// set node position on level 0
		final double x = this.id;
		double y = 0;
		this.graphNode.setAttribute("x", 3 * x);
		this.graphNode.setAttribute("y", y);
		// set node position on other levels
		for (int i = 1; i <= this.maxLevel + 1; i++) {
			String nodeAtLevel = this.id.toString() + ":" + i;
			org.graphstream.graph.Node node = null;
			node = this.addNodeToGraph(nodeAtLevel, i);
			String str = "Level edge from ";
			String from = "";
			if (i == 1) {
				from = this.id.toString();
			} else {
				from = this.id.toString() + ":" + (i - 1);
			}
			str += from + " to " + nodeAtLevel;
			if (i != this.maxLevel + 1 && this.graph.getEdge(str) == null) {
				final Edge edge = this.graph.addEdge(str, from, nodeAtLevel, true);
				edge.setAttribute("ui.style", "fill-color:grey; arrow-shape:none; stroke-mode:dots;");
			}
			node.setAttribute("x", 3 * x);
			int yshift = this.id % (int) Math.pow(2, i);
			y = 0;
			for (int j = 0; j < i; j++) {
				y = y + Math.pow(2, j) + 1;
			}

			if (i <= this.maxLevel) {
				node.setAttribute("y", y + yshift);
			} else {
				node.setAttribute("y", y + 2);
			}
		}
	}

	public int getDegree() {
		return this.leftNeighbors.size() + this.rightNeighbors.size();
	}

	protected int getNumOfUnknownEdges() {
		return this.leftUnknown.size() + this.rightUnknown.size();
	}

	protected boolean hasStableNeighbor(int v, int level) {
		if (v < this.id) {
			MsNode current = this.leftAtLevel.get(level);
			if (current != null) {
				if (current.id != v) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} else {
			MsNode current = this.rightAtLevel.get(level);
			if (current != null) {
				if (current.id != v) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
	}

	/**
	 *
	 * This method computes the maximum id in the left/right neighbors which is
	 * smaller than vID.
	 *
	 * @param vID
	 * @param dir
	 * @return maximal id in the left/right neighbors which is smaller than v.id
	 */
	private int getLeftNearestNeighbor(int vID, Direction dir) {
		int max = Integer.MAX_VALUE;
		TreeMap<Integer, MsNode> list = new TreeMap<Integer, MsNode>();
		if (dir == Direction.LEFT) {
			synchronized (this.leftNeighbors) {
				list = new TreeMap<Integer, MsNode>(this.leftNeighbors.headMap(vID, false));
			}
		} else {
			synchronized (this.rightNeighbors) {
				list = new TreeMap<Integer, MsNode>(this.rightNeighbors.headMap(vID, false));
			}
		}
		if (list.isEmpty()) {
			max = -1;
		} else {
			max = list.lastKey();
		}
		return max;
	}

	/**
	 *
	 * This method computes the minimum id in the left/right neighbors which has
	 * bigger id than vID.
	 *
	 * @param vID
	 * @param dir
	 * @return minimal id in the left neighbors which is bigger than v.id
	 */
	private int getRightNearestNeighbor(int vID, Direction dir) {
		int min = Integer.MAX_VALUE;
		TreeMap<Integer, MsNode> list = new TreeMap<Integer, MsNode>();
		if (dir == Direction.LEFT) {
			synchronized (this.leftNeighbors) {
				list = new TreeMap<Integer, MsNode>(this.leftNeighbors.tailMap(vID, false));
			}
		} else {
			synchronized (this.rightNeighbors) {
				list = new TreeMap<Integer, MsNode>(this.rightNeighbors.tailMap(vID, false));
			}
		}
		if (list.isEmpty()) {
			min = -1;
		} else {
			min = list.firstKey();
		}
		return min;
	}

	protected TreeMap<Integer, MsNode> getNeighbors() {
		final TreeMap<Integer, MsNode> all = this.leftNeighbors;
		all.putAll(this.rightNeighbors);
		return all;
	}

	/**
	 * this method is called by channel whenever there is a message for this
	 * node. It is used to draw implicit edge if required
	 *
	 * @param message
	 */
	protected void informNode(MsMessage message) {
		switch (message.getAction()) {
		case INTRODUCE:
			this.addImplicitEdge(message.getNode().getID(), 0);
			break;
		case INTRO_LEVEL_NODE:
			this.addImplicitEdge(message.getNode().getID(), message.getLevel());
			break;
		case PROBE_LEVEL_INTRO:
			int level = 0;
			if (message.getLevel() >= 0) {
				level = message.getLevel();
			}
			this.addImplicitEdge(message.getSource().getID(), level);
			this.addImplicitEdge(message.getDest().getID(), level);
			break;
		default:
			break;
		}
	}

	protected void initiateNewSearch(int destID) {
		MsMessage message = new MsMessage(this.channel.generateMsgId(), Action.SEARCH, this, destID);
		if (!this.waitingFor.containsKey(destID)) {
			this.waitingFor.put(destID, new ArrayList<MsMessage>());
			this.seq = this.seq + 1;
			this.seqNums.put(destID, this.seq);
		}
		this.waitingFor.get(destID).add(message);
	}

	private void sendMsg(MsNode goal, MsMessage message) {
		int delay = this.random.nextInt(MAX_DELAY);
		this.timer.schedule(new MessageTask(goal, message), delay);
		//goal.informNode(message);
	}

	/**
	 * whenever there a message is received, we add implicit edge to the graph
	 *
	 * @param message
	 */
	public void receiveMessage(MsMessage message) {
		try {
			synchronized (this.taskQueue) {
				this.taskQueue.put(new MsTask(TaskType.MESSAGE, message));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processMessage(MsMessage message) {
		switch (message.getAction()) {
		case INTRODUCE:
			this.introduce(message.getNode());
			this.removeImplicitEdge(message.getNode().getID(), 0);
			break;
		case INTRO_LEVEL_NODE:
			if (this.naivAlgo) {
				this.introLevelNode(message.getNode(), message.getLevel());
			} else {
				this.alterIntroLevelNode(message.getNode(), message.getLevel());
			}
			this.removeImplicitEdge(message.getNode().getID(), message.getLevel());
			break;
		case PROBE_LEVEL_INTRO:
			int level = 0;
			if (message.getLevel() >= 0) {
				level = message.getLevel();
			}
			this.probeLevelIntro(message.getSource(), message.getDest(), message.getOLevel(), message.getLevel(), message.getDir());
			this.removeImplicitEdge(message.getSource().getID(), level);
			this.removeImplicitEdge(message.getDest().getID(), level);
			break;
		case PROBE_LEVEL_INTRO_SUCC:
			this.introLevelNode(message.getDest(), message.getOLevel());
			this.removeImplicitEdge(message.getDest().getID(), message.getOLevel());
			break;
		case SAFE_INTRODUCE:
			this.safeIntroduce(message.getNode(), message.getSource());
			break;
		case SAFE_DELETION:
			this.safeDeletion(message.getNode());
			break;
		case GREEDY_PROBE:
			this.greedyProbe(message.getSource(), message.getDestID(), message.getSeq(), message.getHops());
			break;
		case GREEDY_SUCC:
			this.greedySucc(message.getDest(), message.getDestID(), message.getSeq());
			break;
		case GENERIC_PROBE:
			this.genericProbe(message.getSource(), message.getDestID(), message.getNext(), message.getSeq(), message.getHops());
			break;
		case GENERIC_SUCC:
			this.genericSucc(message.getDest(), message.getDestID(), message.getSeq());
			break;
		case SLOW_GREEDY_PROBE:
			this.slowGreedyProbe(message.getSource(), message.getDestID(), message.getPrev(), message.getNext(), message.getSeq(), message.getHops());
			break;
		case SLOW_GREEDY_SUCC:
			this.slowGreedySucc(message.getDest(), message.getDestID(), message.getSeq());
		case PROBE_FAIL:
			this.probeFail(message.getDestID(), message.getSeq());
			break;
		default:
			break;
		}
		this.channel.incUsedMessages();
	}

	protected HashMap<Integer, Integer> getLeftLevelMap() {
		return this.leftLevelMap;
	}

	protected HashMap<Integer, Integer> getRightLevelMap() {
		return this.rightLevelMap;
	}

	protected LinkedBlockingQueue<MsTask> getTaskQueue() {
		return this.taskQueue;
	}

	/**
	 * Used for initial graph
	 *
	 * @param v
	 * @param level
	 * @param dir
	 */
	public void addLevelNeighbor(MsNode v, int level, Direction dir, boolean onlyAdd) {
		if (dir == Direction.LEFT) {
			if (!onlyAdd) {
				if (this.leftAtLevel.containsKey(level)) {
					MsNode current = this.leftAtLevel.get(level);
					this.removeLevelNeighbor(current, level, dir);
					this.addToUnknown(current);
				}
			}
			this.leftAtLevel.put(level, v);
			this.leftLevelMap.put(v.id, level);
			synchronized (this.leftNeighbors) {
				this.leftNeighbors.put(v.id, v);
			}
			this.addExplicitEdge(v.id, level);
		} else {
			if (!onlyAdd) {
				if (this.rightAtLevel.containsKey(level)) {
					MsNode current = this.rightAtLevel.get(level);
					this.removeLevelNeighbor(current, level, dir);
					this.addToUnknown(current);
				}
			}
			this.rightAtLevel.put(level, v);
			this.rightLevelMap.put(v.id, level);
			synchronized (this.rightNeighbors) {
				this.rightNeighbors.put(v.id, v);
			}
			this.addExplicitEdge(v.id, level);
		}
	}

	private void addToUnknown(MsNode v) {
		if (v.id < this.id) {
			if (!this.leftUnknown.containsKey(v.id)) {
				this.leftUnknown.put(v.id, v);
				synchronized (this.leftNeighbors) {
					this.leftNeighbors.put(v.id, v);
				}
				this.addExplicitEdge(v.id, maxLevel + 1);
			}
		} else {
			if (!this.rightUnknown.containsKey(v.id)) {
				this.rightUnknown.put(v.id, v);
				synchronized (this.rightNeighbors) {
					this.rightNeighbors.put(v.id, v);
				}
				this.addExplicitEdge(v.id, maxLevel + 1);
			}
		}
	}

	/**
	 * This method add an explicit edge at given level to graph
	 *
	 * @param neighbor
	 * @param level
	 * @param n
	 *            : number of nodes, used to set layout weight
	 */
	private void addExplicitEdge(Integer neighbor, int level) {
		if (graph != null) {
			if (neighbor != this.id) {
				synchronized (this.graph) {
					String s = "Explicit from " + this.id + " to " + neighbor + " at Level " + level;
					if (this.graph.getEdge(s) == null) {
						String from = this.id.toString();
						String to = neighbor.toString();
						if (level != 0) {
							from += ":" + level;
							to += ":" + level;
						}
						Edge edge = this.graph.addEdge(s, from, to, true);
						int j = -1;
						for (int i = 0; i <= this.maxLevel; i++) {
							if (Math.pow(2, i) == Math.abs(neighbor - this.id)) {
								j = i;
								break;
							}
						}
						if (level > this.maxLevel) {
							edge.setAttribute("ui.style", "size: 1px; fill-color:grey; arrow-size: 12px, 4px;");
						} else {
							if (Math.abs(neighbor - this.id) == 1 || j != -1) {
								if (level % 2 == 1) {
									edge.setAttribute("ui.style", "size: 2px; fill-color:red; arrow-size: 12px, 4px;");
								} else {
									edge.setAttribute("ui.style", "size: 2px; fill-color:blue; arrow-size: 12px, 4px;");
								}
							} else {
								edge.setAttribute("ui.style", "size: 1px; fill-color:grey; arrow-size: 12px, 4px;");
							}
						}
						edge.setAttribute("layout.weight", this.numOfNodes / 4);
					}
				}
			}
		}
	}

	/**
	 * This method add an implicite edge at given level to graph
	 *
	 * @param neighbor
	 * @param level
	 * @param n
	 *            : number of nodes, used to set layout weight
	 */
	private void addImplicitEdge(Integer neighbor, int level) {
		if (graph != null && this.showImplicit) {
			if (neighbor != this.id) {
				synchronized (this.graph) {
					String s = "Implicit from " + this.id + " to " + neighbor + " at Level " + level;
					if (this.graph.getEdge(s) == null) {
						String from = this.id.toString();
						String to = neighbor.toString();
						if (level != 0) {
							from += ":" + level;
							to += ":" + level;
						}
						Edge edge = this.graph.addEdge(s, from, to, true);
						if (level > this.maxLevel) {
							edge.setAttribute("ui.style", "size: 1px; fill-color:grey; arrow-size: 12px, 4px;stroke-mode:dots;");
						} else {
							if (level % 2 == 1) {
								edge.setAttribute("ui.style", "size: 1px; fill-color:red; arrow-size: 12px, 4px;stroke-mode:dots;");
							} else {
								edge.setAttribute("ui.style", "size: 1px; fill-color:blue; arrow-size: 12px, 4px;stroke-mode:dots;");
							}
						}
						edge.setAttribute("layout.weight", this.numOfNodes / 4);
					}
				}
			}
		}

	}

	private void removeLevelNeighbor(MsNode v, int level, Direction dir) {
		if (dir == Direction.LEFT) {
			this.leftAtLevel.remove(level);
			this.leftLevelMap.remove(v.id);
		} else {
			this.rightAtLevel.remove(level);
			this.rightLevelMap.remove(v.id);
		}
		this.removeExplicitEdge(v.id, level);
	}

	private void removeFromUnknown(MsNode v) {
		if (v.id < this.id) {
			if (this.leftUnknown.containsKey(v.id)) {
				this.leftUnknown.remove(v.id);
			}
		} else {
			if (this.rightUnknown.containsKey(v.id)) {
				this.rightUnknown.remove(v.id);
			}
		}
		this.removeExplicitEdge(v.id, maxLevel + 1);
	}

	/**
	 * This method remove an explicit edge at given level from graph
	 *
	 * @param neighbor
	 * @param level
	 */
	private void removeExplicitEdge(Integer neighbor, int level) {
		if (graph != null) {
			if (neighbor != this.id) {
				synchronized (this.graph) {
					String s = "Explicit from " + this.id + " to " + neighbor + " at Level " + level;
					if (this.graph.getEdge(s) != null) {
						this.graph.removeEdge(s);
					}

				}
			}
		}
	}

	/**
	 * This method remove an implicit edge at given level from graph
	 *
	 * @param neighbor
	 * @param level
	 */
	private void removeImplicitEdge(Integer neighbor, int level) {
		if (graph != null && this.showImplicit) {
			if (neighbor != this.id) {
				synchronized (this.graph) {
					String s = "Implicit from " + this.id + " to " + neighbor + " at Level " + level;
					if (this.graph.getEdge(s) != null) {
						this.graph.removeEdge(s);
					}

				}
			}
		}
	}

	private void genericSucc(MsNode dest, int destID, int seq) {
		this.sendMsg(this, new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, dest));
		if (seq >= this.seqNums.get(destID)) {
			if (this.waitingFor.containsKey(destID)) {
				for (int i = 0; i < this.waitingFor.get(destID).size(); i++) {
					this.sendMsg(dest, this.waitingFor.get(destID).get(i));
					this.channel.incProcessedSearchReqs(true, false);
				}
				this.waitingFor.get(destID).clear();
			}
		}
		this.introduce(dest);
	}

	private void genericProbe(MsNode source, int destID, TreeMap<Integer, MsNode> next, int seq, int hops) {
		TreeMap<Integer, MsNode> localNext = new TreeMap<Integer, MsNode>(next);
		if (source != this) {
			hops = hops + 1;
		}
		if (destID == this.id) {
			if (!localNext.isEmpty()) {
				for (MsNode u : localNext.values()) {
					this.introduce(u);
				}
			}
			this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.GENERIC_SUCC, destID, seq, this));
			this.channel.updateHops(hops);
		} else {
			if (destID < this.id) {
				if (localNext.containsKey(this.id)) {
					localNext.remove(this.id);
				}
				synchronized (this.leftNeighbors) {
					localNext.putAll(this.leftNeighbors.tailMap(destID, true));
				}
				if (localNext.isEmpty()) {
					this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.PROBE_FAIL, destID, seq));
					this.introduce(source);
				} else {
					MsNode u = localNext.lastEntry().getValue();
					if (!this.leftNeighbors.containsKey(u.id) && !this.rightNeighbors.containsKey(u.id)) {
						this.introduce(u);
					}
					this.sendMsg(u, new MsMessage(this.channel.generateMsgId(), Action.GENERIC_PROBE, source, destID, localNext, seq, hops));
				}
			} else {
				if (localNext.containsKey(this.id)) {
					localNext.remove(this.id);
				}
				synchronized (this.rightNeighbors) {
					localNext.putAll(this.rightNeighbors.headMap(destID, true));
				}
				if (localNext.isEmpty()) {
					this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.PROBE_FAIL, destID, seq));
					this.introduce(source);
				} else {
					MsNode u = localNext.firstEntry().getValue();
					if (!this.leftNeighbors.containsKey(u.id) && !this.rightNeighbors.containsKey(u.id)) {
						this.introduce(u);
					}
					this.sendMsg(u, new MsMessage(this.channel.generateMsgId(), Action.GENERIC_PROBE, source, destID, localNext, seq, hops));
				}
			}
		}
	}

	private void probeFail(int destID, int seq) {
		if (seq >= this.seqNums.get(destID)) {
			if (this.waitingFor.containsKey(destID)) {
				for (int i = 0; i < this.waitingFor.get(destID).size(); i++) {
					this.channel.incProcessedSearchReqs(false, true);
				}
				this.waitingFor.get(destID).clear();
			}
		}
	}

	private void slowGreedyProbe(MsNode source, int destID, TreeMap<Integer, MsNode> prev, TreeMap<Integer, MsNode> next, int seq, int hops) {
		TreeMap<Integer, MsNode> localNext = new TreeMap<Integer, MsNode>(next);
		if (source != this) {
			hops = hops + 1;
		}
		if (destID == this.id) {
			if (!localNext.isEmpty()) {
				for (MsNode u : localNext.values()) {
					this.introduce(u);
				}
			}
			this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.SLOW_GREEDY_SUCC, destID, seq, this));
			this.channel.updateHops(hops);
		} else {
			if (destID < this.id) {
				prev.put(this.id, this);
				synchronized (this.leftNeighbors) {
					localNext.putAll(this.leftNeighbors.tailMap(destID, true));
				}
				for (MsNode v : prev.values()) {
					if (localNext.containsKey(v.id)) {
						localNext.remove(v.id);
					}
				}
				if (localNext.isEmpty()) {
					this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.PROBE_FAIL, destID, seq));
					this.introduce(source);
				} else {
					MsNode u = localNext.firstEntry().getValue();
					if (!this.leftNeighbors.containsKey(u.id) && !this.rightNeighbors.containsKey(u.id)) {
						this.introduce(u);
					}
					this.sendMsg(u,
							new MsMessage(this.channel.generateMsgId(), Action.SLOW_GREEDY_PROBE, source, destID, prev, localNext, seq, hops));
				}
			} else {
				prev.put(this.id, this);
				synchronized (this.rightNeighbors) {
					localNext.putAll(this.rightNeighbors.headMap(destID, true));
				}
				for (MsNode v : prev.values()) {
					if (localNext.containsKey(v.id)) {
						localNext.remove(v.id);
					}
				}
				if (localNext.isEmpty()) {
					this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.PROBE_FAIL, destID, seq));
					this.introduce(source);
				} else {
					MsNode u = localNext.lastEntry().getValue();
					if (!this.leftNeighbors.containsKey(u.id) && !this.rightNeighbors.containsKey(u.id)) {
						this.introduce(u);
					}
					this.sendMsg(u,
							new MsMessage(this.channel.generateMsgId(), Action.SLOW_GREEDY_PROBE, source, destID, prev, localNext, seq, hops));
				}
			}
		}
	}

	private void slowGreedySucc(MsNode dest, int destID, int seq) {
		this.sendMsg(this, new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, dest));
		if (seq >= this.seqNums.get(destID)) {
			if (this.waitingFor.containsKey(destID)) {
				for (int i = 0; i < this.waitingFor.get(destID).size(); i++) {
					this.channel.incProcessedSearchReqs(true, true);
					this.sendMsg(dest, this.waitingFor.get(destID).get(i));
				}
				this.waitingFor.get(destID).clear();
			}
		}
		this.introduce(dest);
	}

	private void greedySucc(MsNode dest, int destID, int seq) {
		this.sendMsg(this, new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, dest));
		if (seq >= this.seqNums.get(destID)) {
			if (this.waitingFor.containsKey(destID)) {
				for (int i = 0; i < this.waitingFor.get(destID).size(); i++) {
					this.channel.incProcessedSearchReqs(true, true);
					this.sendMsg(dest, this.waitingFor.get(destID).get(i));
				}
				this.waitingFor.get(destID).clear();
			}
		}
		this.introduce(dest);
	}

	private void greedyProbe(MsNode source, int destID, int seq, int hops) {
		if (source != this) {
			hops = hops + 1;
		}
		if (destID == this.id) {
			this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.GREEDY_SUCC, destID, seq, this));
			this.channel.updateHops(hops);
		} else {
			if (destID < this.id) {
				if (this.leftNeighbors.isEmpty()) {
					this.introduce(source);
				} else {
					int next = -1;
					if (this.leftNeighbors.containsKey(destID)) {
						next = destID;
						this.sendMsg(this.leftNeighbors.get(next),
								new MsMessage(this.channel.generateMsgId(), Action.GREEDY_PROBE, source, destID, seq, hops));
					} else {
						next = this.getRightNearestNeighbor(destID, Direction.LEFT);
						if (next != -1) {
							this.sendMsg(this.leftNeighbors.get(next),
									new MsMessage(this.channel.generateMsgId(), Action.GREEDY_PROBE, source, destID, seq, hops));
						}
					}
				}
			} else {
				if (this.rightNeighbors.isEmpty()) {
					this.introduce(source);
				} else {
					int next = -1;
					if (this.rightNeighbors.containsKey(destID)) {
						next = destID;
						this.sendMsg(this.rightNeighbors.get(next),
								new MsMessage(this.channel.generateMsgId(), Action.GREEDY_PROBE, source, destID, seq, hops));
					} else {
						next = this.getLeftNearestNeighbor(destID, Direction.RIGHT);
						if (next != -1) {
							this.sendMsg(this.rightNeighbors.get(next),
									new MsMessage(this.channel.generateMsgId(), Action.GREEDY_PROBE, source, destID, seq, hops));
						}
					}
				}
			}
		}
	}

	/**
	 * used for alternative protocol
	 *
	 * @param source
	 * @param dest
	 * @param oLevel
	 * @param level
	 * @param dir
	 */
	private void probeLevelIntro(MsNode source, MsNode dest, int oLevel, int level, Direction dir) {
		//System.out.println(this.id + " " + source.id + " " + dest.id + " " + oLevel + " " + level + " " + dir);
		if (oLevel > 0) {
			if (this.id == dest.id && level == -1) {
				this.sendMsg(source, new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO_SUCC, dest, oLevel, dir));
			} else {
				if (dir == Direction.LEFT) {
					if (level > 0) {
						if (!this.leftAtLevel.containsKey(level - 1)) {
							this.introduce(source);
							this.introduce(dest);
						} else {
							MsNode next = this.leftAtLevel.get(level - 1);
							this.sendMsg(next,
									new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO, source, dest, oLevel, level - 1, dir));
						}
					}
					if (level == 0) {
						if (!this.leftAtLevel.containsKey(level)) {
							this.introduce(source);
							this.introduce(dest);
						} else {
							MsNode next = this.leftAtLevel.get(level);
							this.sendMsg(next,
									new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO, source, dest, oLevel, level - 1, dir));
						}
					}

				} else {
					if (level > 0) {
						if (!this.rightAtLevel.containsKey(level - 1)) {
							this.introduce(source);
							this.introduce(dest);
						} else {
							MsNode next = this.rightAtLevel.get(level - 1);
							this.sendMsg(next,
									new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO, source, dest, oLevel, level - 1, dir));
						}
					}
					if (level == 0) {
						if (!this.rightAtLevel.containsKey(level)) {
							this.introduce(source);
							this.introduce(dest);
						} else {
							MsNode next = this.rightAtLevel.get(level);
							this.sendMsg(next,
									new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO, source, dest, oLevel, level - 1, dir));
						}
					}
				}
			}
		} else {
			this.introduce(source);
			this.introduce(dest);
		}
	}

	/**
	 * used for alternative protocol
	 *
	 * @param v
	 * @param level
	 */
	private void alterIntroLevelNode(MsNode v, int i) {
		//System.out.println("a " + this.id + " " + v.id + " " + i);
		if (v.id != this.id) {
			if (i > 0) {
				boolean lowerLevelDone = true;
				if (v.id < this.id) {
					if (!this.leftNeighbors.isEmpty()) {
						for (int j = 0; j < i; j++) {
							if (!this.leftAtLevel.containsKey(j)) {
								lowerLevelDone = false;
								break;
							}
						}
					} else {
						lowerLevelDone = false;
					}
					if (!lowerLevelDone) {
						this.introduce(v);
					} else {
						MsNode next = this.leftAtLevel.get(i - 1);
						this.sendMsg(next, new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO, this, v, i, i - 1, Direction.LEFT));
					}
				} else {
					if (!this.rightNeighbors.isEmpty()) {
						for (int j = 0; j < i; j++) {
							if (!this.rightAtLevel.containsKey(j)) {
								lowerLevelDone = false;
								break;
							}
						}
					} else {
						lowerLevelDone = false;
					}
					if (!lowerLevelDone) {
						this.introduce(v);
					} else {
						MsNode next = this.rightAtLevel.get(i - 1);
						this.sendMsg(next, new MsMessage(this.channel.generateMsgId(), Action.PROBE_LEVEL_INTRO, this, v, i, i - 1, Direction.RIGHT));
					}
				}
			} else {
				this.introduce(v);
			}
		}
	}

	/**
	 * Used for naive(msg INTRO_LEVEL_NODE) & alternative(msg
	 * PROBE_LEVEL_INTRO_SUCC) protocol
	 *
	 * @param v
	 * @param i
	 */
	private void introLevelNode(MsNode v, int i) {
		//System.out.println(v.id + " " + i);
		if (v.id != this.id) {
			if (i > 0) {
				boolean lowerLevelDone = true;
				if (v.id < this.id) {
					if (!this.leftNeighbors.isEmpty()) {
						for (int j = 0; j < i; j++) {
							if (!this.leftAtLevel.containsKey(j)) {
								lowerLevelDone = false;
								break;
							}
						}
					} else {
						lowerLevelDone = false;
					}
					if (!lowerLevelDone) {
						this.introduce(v);
					} else {
						if (this.leftAtLevel.containsKey(i)) {
							MsNode current = this.leftAtLevel.get(i);
							if (v.id != current.id) {
								this.removeLevelNeighbor(current, i, Direction.LEFT);
								this.addToUnknown(current);
							}
						}
						if (!this.leftAtLevel.containsKey(i)) {
							if (this.leftNeighbors.containsKey(v.id)) {
								if (this.leftUnknown.containsKey(v.id)) {
									this.removeFromUnknown(v);
								} else {
									int level = this.leftLevelMap.get(v.id);
									this.removeLevelNeighbor(v, level, Direction.LEFT);
								}
							}
							this.addLevelNeighbor(v, i, Direction.LEFT, true);
						}
					}
				} else {
					if (!this.rightNeighbors.isEmpty()) {
						for (int j = 0; j < i; j++) {
							if (!this.rightAtLevel.containsKey(j)) {
								lowerLevelDone = false;
								break;
							}
						}
					} else {
						lowerLevelDone = false;
					}
					if (!lowerLevelDone) {
						this.introduce(v);
					} else {
						if (this.rightAtLevel.containsKey(i)) {
							MsNode current = this.rightAtLevel.get(i);
							if (v.id != current.id) {
								this.removeLevelNeighbor(current, i, Direction.RIGHT);
								this.addToUnknown(current);
							}
						}
						if (!this.rightAtLevel.containsKey(i)) {
							if (this.rightNeighbors.containsKey(v.id)) {
								if (this.rightUnknown.containsKey(v.id)) {
									this.removeFromUnknown(v);
								} else {
									int level = this.rightLevelMap.get(v.id);
									this.removeLevelNeighbor(v, level, Direction.RIGHT);
								}
							}
							this.addLevelNeighbor(v, i, Direction.RIGHT, true);
						}
					}
				}
			} else {
				this.introduce(v);
			}
		}

	}

	/**
	 * This method is called when the node processes a Introduce-message.
	 *
	 * @param v
	 */
	private void introduce(MsNode v) {
		if (v.id != this.id) {
			if (v.id < this.id) {
				if (!this.leftAtLevel.containsKey(0)) {
					this.removeFromUnknown(v);
					this.addLevelNeighbor(v, 0, Direction.LEFT, true);
				} else {
					MsNode maxLeft = this.leftAtLevel.get(0);
					if (v.id != maxLeft.id) {
						if (v.id > maxLeft.id) {
							this.removeLevelNeighbor(maxLeft, 0, Direction.LEFT);
							this.addToUnknown(maxLeft);
							this.removeFromUnknown(v);
							this.addLevelNeighbor(v, 0, Direction.LEFT, true);
						} else {
							int y1 = this.getRightNearestNeighbor(v.id, Direction.LEFT);
							if (y1 != -1) {
								this.sendMsg(this.leftNeighbors.get(y1), new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, v));
							}
							int y2 = this.getLeftNearestNeighbor(v.id, Direction.LEFT);
							if (y2 != -1) {
								this.sendMsg(this.leftNeighbors.get(y2), new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, v));
							}
						}
					}
				}

			} else {
				if (!this.rightAtLevel.containsKey(0)) {
					this.removeFromUnknown(v);
					this.addLevelNeighbor(v, 0, Direction.RIGHT, true);
				} else {
					MsNode min = this.rightAtLevel.get(0);
					if (v.id != min.id) {
						if (v.id < min.id) {
							this.removeLevelNeighbor(min, 0, Direction.RIGHT);
							this.addToUnknown(min);
							this.removeFromUnknown(v);
							this.addLevelNeighbor(v, 0, Direction.RIGHT, true);
						} else {
							int y1 = this.getRightNearestNeighbor(v.id, Direction.RIGHT);
							if (y1 != -1) {
								this.sendMsg(this.rightNeighbors.get(y1), new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, v));
							}
							int y2 = this.getLeftNearestNeighbor(v.id, Direction.RIGHT);
							if (y2 != -1) {
								this.sendMsg(this.rightNeighbors.get(y2), new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, v));
							}
						}
					}
				}

			}
		}

	}

	/**
	 * Level Introduce
	 */
	private void levelIntroduce() {
		if (this.maxLevel > 0) {
			if (this.introCountLevel == this.maxLevel) {
				this.introCountLevel = 0;
			}
			int level = this.introCountLevel;
			if (this.leftAtLevel.containsKey(level) && this.rightAtLevel.containsKey(level)) {
				MsNode left = this.leftAtLevel.get(level);
				MsNode right = this.rightAtLevel.get(level);
				this.sendMsg(left, new MsMessage(this.channel.generateMsgId(), Action.INTRO_LEVEL_NODE, level + 1, right));
				this.sendMsg(right, new MsMessage(this.channel.generateMsgId(), Action.INTRO_LEVEL_NODE, level + 1, left));
			}
			this.introCountLevel++;
		}
	}

	/**
	 * This method introduces neighbors linearly. Notice: each call of this
	 * method, it introduces only one pair sequentiell neighbors to each other
	 * on each side
	 */
	private void linearIntroduce() {
		if (!this.leftNeighbors.isEmpty())// linearly introduces the left neighbors
		{
			final int size = this.leftNeighbors.size();
			final Integer[] keys = this.leftNeighbors.keySet().toArray(new Integer[size]);
			if (size >= 2) {
				if (this.introCountLeft > size - 2) {
					this.introCountLeft = 0;
				}
				int i = this.introCountLeft;
				this.sendMsg(this.leftNeighbors.get(keys[i + 1]),
						new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, this.leftNeighbors.get(keys[i])));
				this.introCountLeft++;
			}
			// introduces this node itself to the nearest left neighbor
			if (size > 0) {
				this.sendMsg(this.leftNeighbors.get(keys[size - 1]), new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, this));
			}
		}
		if (!this.rightNeighbors.isEmpty())// linearly introduces the right neighbors
		{
			final int size = this.rightNeighbors.size();
			final Integer[] keys = this.rightNeighbors.keySet().toArray(new Integer[size]);
			if (size >= 2) {
				if (this.introCountRight > size - 2) {
					this.introCountRight = 0;
				}
				int i = this.introCountRight;
				this.sendMsg(this.rightNeighbors.get(keys[i + 1]),
						new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, this.rightNeighbors.get(keys[i])));
				this.introCountRight++;
			}
			// introduces this node itself to the nearest right neighbor
			if (size > 0) {
				this.sendMsg(this.rightNeighbors.get(keys[0]), new MsMessage(this.channel.generateMsgId(), Action.INTRODUCE, this));
			}
		}
	}

	private void sendHybridProbes() {
		if (!this.waitingFor.isEmpty()) {
			ArrayList<Integer> removed = new ArrayList<Integer>();
			for (ConcurrentHashMap.Entry<Integer, ArrayList<MsMessage>> entry : this.waitingFor.entrySet()) {
				int destID = entry.getKey();
				ArrayList<MsMessage> batch = entry.getValue();
				if (batch.size() > 0) {
					this.sendMsg(this, new MsMessage(this.channel.generateMsgId(), Action.GREEDY_PROBE, this, destID, this.seq, 0));
					TreeMap<Integer, MsNode> next = new TreeMap<Integer, MsNode>();
					next.put(this.id, this);
					this.sendMsg(this, new MsMessage(this.channel.generateMsgId(), Action.GENERIC_PROBE, this, destID, next, this.seq, 0));
				} else {
					removed.add(destID);// remove destID which the search is done
				}
			}
			for (Integer i : removed) {
				this.waitingFor.remove(i);
			}
		}
	}

	private void sendSlowGreedyProbes() {
		if (!this.waitingFor.isEmpty()) {
			ArrayList<Integer> removed = new ArrayList<Integer>();
			for (ConcurrentHashMap.Entry<Integer, ArrayList<MsMessage>> entry : this.waitingFor.entrySet()) {
				int destID = entry.getKey();
				ArrayList<MsMessage> batch = entry.getValue();
				if (batch.size() > 0) {
					TreeMap<Integer, MsNode> next = new TreeMap<Integer, MsNode>();
					next.put(this.id, this);
					this.sendMsg(this, new MsMessage(this.channel.generateMsgId(), Action.SLOW_GREEDY_PROBE, this, destID,
							new TreeMap<Integer, MsNode>(), next, this.seq, 0));
				} else {
					removed.add(destID);// remove destID which the search is done
				}
			}
			for (Integer i : removed) {
				this.waitingFor.remove(i);
			}
		}
	}

	private void safeDelegation() {
		for (MsNode v : this.leftUnknown.values()) {
			int right = this.getRightNearestNeighbor(v.id, Direction.LEFT);
			if (right != -1) {
				this.sendMsg(this.leftNeighbors.get(right), new MsMessage(this.channel.generateMsgId(), Action.SAFE_INTRODUCE, v, this));
			}
		}
		for (MsNode v : this.rightUnknown.values()) {
			int left = this.getLeftNearestNeighbor(v.id, Direction.RIGHT);
			if (left != -1) {
				this.sendMsg(this.rightNeighbors.get(left), new MsMessage(this.channel.generateMsgId(), Action.SAFE_INTRODUCE, v, this));
			}
		}
	}

	private void safeIntroduce(MsNode v, MsNode src) {
		//System.out.println("safeIntroduce " + this.id + " " + v.id + " " + src.id);
		if (src.id != this.id) {
			if (!this.leftNeighbors.containsKey(src.id) && !this.rightNeighbors.containsKey(src.id)) {
				this.introduce(src);
			}
		}
		if (v.id != this.id) {
			if (!this.leftNeighbors.containsKey(v.id) && this.rightNeighbors.containsKey(v.id)) {
				this.addToUnknown(v);
			}
			this.sendMsg(src, new MsMessage(this.channel.generateMsgId(), Action.SAFE_DELETION, v));
		}
	}

	private void safeDeletion(MsNode v) {
		//System.out.println("safeDeletion " + this.id + " " + v.id);
		if (v.id != this.id) {
			if (v.id < this.id) {
				if (this.leftUnknown.containsKey(v.id)) {
					this.removeFromUnknown(v);
					this.leftNeighbors.remove(v.id);
					this.removeExplicitEdge(v.id, maxLevel + 1);
				}
			} else {
				if (this.rightUnknown.containsKey(v.id)) {
					this.removeFromUnknown(v);
					this.rightNeighbors.remove(v.id);
					this.removeExplicitEdge(v.id, maxLevel + 1);
				}

			}
			this.introduce(v);
		}
	}

	private void checkUnknownSets() {
		if (!this.leftUnknown.isEmpty()) {
			for (MsNode v : this.leftNeighbors.values()) {
				if (this.leftLevelMap.containsKey(v.id)) {
					this.removeFromUnknown(v);
				}
			}
		}
		if (!this.rightUnknown.isEmpty()) {
			for (MsNode v : this.rightNeighbors.values()) {
				if (this.rightLevelMap.containsKey(v.id)) {
					this.removeFromUnknown(v);
				}
			}
		}
	}

	/**
	 * This method generates Probe-messages for all searching ids and introduces
	 * the neighbors linearly. It will be called periodically.
	 */
	private void timeout() {
		this.checkUnknownSets();
		if (this.naivAlgo) {
			this.naiveTimeout();
		} else {
			this.alterTimeout();
		}
	}

	private void naiveTimeout() {
		if (!this.channel.isLinearList()) {
			this.linearIntroduce();
		}
		this.levelIntroduce();
		this.sendHybridProbes();
		//this.sendSlowGreedyProbes();
	}

	private void alterTimeout() {
		this.safeDelegation();
		if (!this.channel.isLinearList()) {
			this.linearIntroduce();
		}
		this.levelIntroduce();
		//this.sendHybridProbes();
		this.sendSlowGreedyProbes();
	}

	private void reset() {
		for (int i = 0; i <= maxLevel; i++) {
			if (this.leftAtLevel.containsKey(i)) {
				MsNode current = this.leftAtLevel.get(i);
				this.removeLevelNeighbor(current, i, Direction.LEFT);
				this.addToUnknown(current);
			}
			if (this.rightAtLevel.containsKey(i)) {
				MsNode current = this.rightAtLevel.get(i);
				this.removeLevelNeighbor(current, i, Direction.RIGHT);
				this.addToUnknown(current);
			}
		}
		if (!this.leftNeighbors.isEmpty()) {
			MsNode max = this.leftNeighbors.lastEntry().getValue();
			this.removeFromUnknown(max);
			this.addLevelNeighbor(max, 0, Direction.LEFT, true);
		}
		if (!this.rightNeighbors.isEmpty()) {
			MsNode min = this.rightNeighbors.firstEntry().getValue();
			this.removeFromUnknown(min);
			this.addLevelNeighbor(min, 0, Direction.RIGHT, true);
		}
	}

	@Override
	public void run() {
		this.naivAlgo = this.channel.getNaiveAlgo();
		//this.slowGreedy = this.channel.getSlowGreedy();
		this.reset();
		while (!this.channel.getTerminate()) {
			try {
				if (!this.taskQueue.isEmpty()) {
					ArrayList<MsTask> tasks = new ArrayList<MsTask>();
					synchronized (this.taskQueue) {
						tasks.addAll(this.taskQueue);
						this.taskQueue.clear();
					}
					while (!tasks.isEmpty()) {
						final MsTask task = tasks.remove(0);
						List<MsTask> delete = new ArrayList<>();
						for (MsTask MsTask : tasks) {
							if (MsTask.getMessage().getAction() != Action.SEARCH && MsTask.getMessage().equals(task.getMessage())) {
								delete.add(MsTask);
							}
						}
						tasks.removeAll(delete);
						this.processMessage(task.getMessage());
					}
				}
				this.timeout();
				int t = 100 + random.nextInt(100);
				sleep(t);
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		this.timer.cancel();
	}

}
