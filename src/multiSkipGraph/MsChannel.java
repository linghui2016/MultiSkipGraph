package multiSkipGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import base.Channel;
import base.Pair;

/**
 * 
 * @author Linghui Luo
 *
 */
public class MsChannel extends Channel {
	private static MsChannel instance;

	public static MsChannel getInstance() {
		if (instance == null) {
			instance = new MsChannel();
		}
		return instance;
	}

	private MsChannel() {
		super();
		this.reset(false);
	}

	private static final int period = 100;
	private Random random = new Random();
	private boolean terminate;
	private boolean stable;
	private boolean linearList;
	private boolean searchDone;
	private boolean naiveAlgo;
	private boolean slowGreedy = false;

	private TreeMap<Integer, MsNode> msNodeMap;
	private ArrayList<Pair<Integer, Integer>> searches; // searches: <sourceID, destID>
	private ArrayList<Integer> batches; // size of batches
	private ArrayList<Pair<Integer, Integer>> lastSearches;// searches which generated in last simulation
	private ArrayList<Integer> lastBatches;//size of batches which generated in last simulation

	private int numOfNodes;
	private int numOfSearchReqs;

	private Timer timer; // timer to send messages

	// Metrics
	private HashMap<Integer, Integer> startDegreeMap; // outgoing degree of each node by initialization
	private HashMap<Integer, Integer> endDegreeMap; // store the outgoing degree of each node by termination;

	private int maxDegreeGrowth;
	private int minDegreeGrowth;
	private int medianDegreeGrowth;
	private double avgDegreeGrowth;

	private int maxExtraEdges;
	private int minExtraEdges;
	private int medianExtraEdges;
	private double avgExtraEdges;

	private int maxDist;
	private int medianDist;
	private double avgDist;

	private Integer processedSearchReqs;
	private int usedMessages;

	private TreeSet<Integer> hopsSet;
	private int probeCount;
	private int sumOfHops;
	private int maxHops;
	private int medianHops;
	private double avgHops;
	private int minHops;

	private long endTime;
	private long timeForStable;
	private int maxLevel;

	private int succeedGreedy;
	private int succeedGeneric;
	private int failedProbe;
	private int distributedSearches;

	private boolean terminateToStable;
	private int batchSize;

	private void resetMetrics() {
		this.maxDegreeGrowth = Integer.MIN_VALUE;
		this.minDegreeGrowth = Integer.MAX_VALUE;
		this.medianDegreeGrowth = 0;
		this.avgDegreeGrowth = 0;

		this.maxExtraEdges = Integer.MIN_VALUE;
		this.minExtraEdges = Integer.MAX_VALUE;
		this.medianExtraEdges = 0;
		this.avgExtraEdges = 0;

		this.maxDist = Integer.MIN_VALUE;
		this.medianDist = 0;
		this.avgDist = 0;

		this.processedSearchReqs = 0;
		this.usedMessages = 0;

		this.hopsSet = new TreeSet<Integer>();
		this.probeCount = 0;
		this.sumOfHops = 0;
		this.maxHops = Integer.MIN_VALUE;
		this.minHops = Integer.MAX_VALUE;
		this.medianHops = 0;
		this.avgHops = 0;

		this.endTime = 0;
		this.timeForStable = 0;

		this.succeedGreedy = 0;
		this.succeedGeneric = 0;
		this.failedProbe = 0;
		this.distributedSearches = 0;
	}

	protected void reset(boolean useLastSearches) {
		this.terminate = false;
		this.stable = false;
		this.linearList = false;
		this.searchDone = false;

		this.msNodeMap = new TreeMap<Integer, MsNode>();
		this.searches = new ArrayList<Pair<Integer, Integer>>();
		this.batches = new ArrayList<Integer>();
		if (!useLastSearches) {
			this.lastSearches = new ArrayList<Pair<Integer, Integer>>();
			this.lastBatches = new ArrayList<Integer>();
		}

		this.startDegreeMap = new HashMap<Integer, Integer>();
		this.endDegreeMap = new HashMap<Integer, Integer>();

		this.resetMetrics();

		if (this.timer != null) {
			this.timer.cancel();
		}
		this.timer = new Timer();
	}

	protected void registerNode(MsNode node) {
		if (!this.msNodeMap.containsKey(node.getID())) {
			this.msNodeMap.put(node.getID(), node);
		}
		if (!this.startDegreeMap.containsKey(node.getID())) {
			this.startDegreeMap.put(node.getID(), 0);
		}
		if (!this.endDegreeMap.containsKey(node.getID())) {
			this.endDegreeMap.put(node.getID(), 0);
		}
	}

	public void deliverMsg(MsNode goal, MsMessage message) {
		if (this.msNodeMap.containsKey(goal.getID())) {
			this.msNodeMap.get(goal.getID()).receiveMessage(message);
		}
	}

	protected synchronized void incUsedMessages() {
		this.usedMessages++;
	}

	protected synchronized void incProcessedSearchReqs(boolean success, boolean greedy) {
		this.processedSearchReqs++;
		if (success) {
			if (greedy) {
				this.succeedGreedy++;
			} else {
				this.succeedGeneric++;
			}
		} else {
			this.failedProbe++;
		}
	}

	/**
	 * this method generates a given number of search requests randomly or use
	 * the searches from last tests. It will be called before nodes starting to
	 * run.
	 */
	protected void generateSearchReqs() {
		if (!this.lastSearches.isEmpty()) {
			this.searches = new ArrayList<Pair<Integer, Integer>>(this.lastSearches);
		} else {
			int generatedSearchReqs = 0;
			while (generatedSearchReqs < this.numOfSearchReqs) {
				int sourceID = this.random.nextInt(this.numOfNodes);
				int destID = this.random.nextInt(this.numOfNodes);
				if (sourceID != destID) {
					this.searches.add(new Pair<Integer, Integer>(sourceID, destID));
					generatedSearchReqs++;
				}
			}
		}
		this.lastSearches = new ArrayList<Pair<Integer, Integer>>(this.searches);
		this.generatedBatches();
		this.lastBatches = new ArrayList<Integer>(this.batches);
		TimerTask distributeSearches = new TimerTask() {
			@Override
			public void run() {
				if (!MsChannel.this.batches.isEmpty()) {
					int batch = MsChannel.this.batches.get(0);
					MsChannel.this.batches.remove(0);
					MsChannel.this.distributeSearchReq(batch);
				}
			}
		};
		this.timer.schedule(distributeSearches, 0, period);
	}

	private void generateSearchReqsToStable() {
		if (!this.lastSearches.isEmpty()) {
			this.searches = new ArrayList<Pair<Integer, Integer>>(this.lastSearches);
		}
		TimerTask distributeSearchesToStable = new TimerTask() {
			@Override
			public void run() {
				if (MsChannel.this.searches.isEmpty()) {
					int generatedSearchReqs = 0;
					while (generatedSearchReqs < MsChannel.this.batchSize) {
						int sourceID = MsChannel.this.random.nextInt(MsChannel.this.numOfNodes);
						int destID = MsChannel.this.random.nextInt(MsChannel.this.numOfNodes);
						if (sourceID != destID) {
							MsChannel.this.searches.add(new Pair<Integer, Integer>(sourceID, destID));
							MsChannel.this.lastSearches.add(new Pair<Integer, Integer>(sourceID, destID));
							generatedSearchReqs++;
						}
					}
				}
				MsChannel.this.distributeSearchReq(MsChannel.this.batchSize);
			}
		};
		this.timer.schedule(distributeSearchesToStable, 0, period);
	}

	protected void generatedBatches() {
		if (!this.lastBatches.isEmpty()) {
			this.batches = new ArrayList<Integer>(this.lastBatches);
		} else {
			int size = 5;
			int i = 0;
			while (i < this.numOfSearchReqs) {
				int remain = this.numOfSearchReqs - i;
				int batch = random.nextInt(10);
				if (remain > 5) {
					if (batch < 5) {
						batch = size;
					}

				} else {
					batch = remain;
				}
				this.batches.add(batch);
				i += batch;
			}
		}
	}

	/**
	 * Distribute a batch of Search message
	 *
	 * @param batchSize
	 */
	protected void distributeSearchReq(int batchSize) {
		if (this.msNodeMap.size() == this.numOfNodes) {
			for (int i = 0; i < batchSize; i++) {
				if (!this.searches.isEmpty()) {
					int sourceID = this.searches.get(0).getFirst();
					int destID = this.searches.get(0).getSecond();
					this.msNodeMap.get(sourceID).initiateNewSearch(destID);
					this.distributedSearches++;
					this.searches.remove(0);
				} else {
					break;
				}
			}
		}
	}

	protected void setNumOfNodes(int n) {
		this.numOfNodes = n;
	}

	protected void setNumOfSearchReqs(int n) {
		this.numOfSearchReqs = n;
	}

	protected void setToRun(boolean naive) {
		this.naiveAlgo = naive;
		if (!this.terminateToStable) {
			if (this.numOfSearchReqs != 0) {
				this.generateSearchReqs();
			}
		} else {
			if (this.batchSize != 0) {
				this.generateSearchReqsToStable();
			}
		}
		this.maxLevel = MsController.getInstance().getMaxLevel();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				MsChannel.getInstance().terminate = MsChannel.this.terminate();
			}
		};
		this.timer.schedule(timerTask, 0, 200);
	}

	protected boolean getNaiveAlgo() {
		return this.naiveAlgo;
	}

	protected boolean getSlowGreedy() {
		return this.slowGreedy;
	}

	/**
	 * dijkstra algorithm
	 *
	 * @param start
	 *            : the id of start node for dijkstra
	 * @return maximal distance from start to other nodes
	 */
	public int dijkstra(Integer start, TreeSet<Integer> dists) {
		int maxDist = 0;
		HashMap<Integer, Integer> distance = new HashMap<Integer, Integer>();// (id,distance)
		ArrayList<Integer> unSetteledNodes = new ArrayList<Integer>();// stores id of each unsettled node
		for (MsNode v : this.msNodeMap.values()) {
			if (v.getID() != start) {
				distance.put(v.getID(), Integer.MAX_VALUE);
			} else {
				distance.put(start, 0);
			}
			unSetteledNodes.add(v.getID());
		}
		while (!unSetteledNodes.isEmpty()) {
			Integer minID = start;
			int minDist = Integer.MAX_VALUE;
			for (Integer node : unSetteledNodes) {
				if (distance.get(node) < minDist) {
					minDist = distance.get(node);
					minID = node;
				}
			}
			unSetteledNodes.remove(minID);
			MsNode u = this.msNodeMap.get(minID);
			if (u != null) {
				ArrayList<MsNode> list = new ArrayList<MsNode>(u.getNeighbors().values());
				for (MsNode v : list) {
					if (distance.get(v.getID()) > distance.get(u.getID()) + 1) {
						distance.put(v.getID(), distance.get(u.getID()) + 1);
						if (distance.get(v.getID()) > maxDist) {
							maxDist = distance.get(v.getID());
						}
						dists.add(distance.get(v.getID()));
					}
				}
			}
		}
		int sumDist = 0;
		for (Integer dist : distance.values()) {
			sumDist += dist;

		}
		this.avgDist += (double) sumDist / distance.size();
		return maxDist;
	}

	public void calculateDist() {
		int max = 0;
		TreeSet<Integer> dists = new TreeSet<Integer>();
		for (MsNode node : this.msNodeMap.values()) {
			int dist = this.dijkstra(node.getID(), dists);
			if (dist > max) {
				max = dist;
			}
		}
		int middle = dists.size() / 2;
		Iterator<Integer> it = dists.iterator();
		int i = 0;
		while (it.hasNext() && i < middle) {
			it.next();
			i++;
		}
		this.medianDist = it.next();
		this.maxDist = max;
		this.avgDist /= this.msNodeMap.size();
	}

	public int getMedianDist() {
		return this.medianDist;
	}

	public double getAvgDist() {
		this.avgDist = Math.round(avgDist * 100) / 100.0;
		return this.avgDist;
	}

	public int getMaxDist() {
		return this.maxDist;
	}

	/**
	 * This method is used to calculate the hops of a probe message.
	 *
	 * @param hops
	 */
	public void updateHops(int hops) {
		this.hopsSet.add(hops);
		this.probeCount++;
		this.sumOfHops += hops;
		if (hops > this.maxHops) {
			this.maxHops = hops;
		}
		if (hops < this.minHops) {
			this.minHops = hops;
		}
	}

	public int getMaxHops() {
		if (this.probeCount == 0) {
			return 0;
		}
		return this.maxHops;
	}

	public int getMedianHops() {
		if (this.probeCount == 0) {
			return 0;
		}
		int middle = this.hopsSet.size() / 2;
		Iterator<Integer> it = this.hopsSet.iterator();
		int i = 0;
		while (it.hasNext() && i < middle) {
			it.next();
			i++;
		}
		this.medianHops = it.next();
		return this.medianHops;
	}

	public double getAvgHops() {
		if (this.probeCount == 0) {
			return 0;
		}
		double avgHops = (double) this.sumOfHops / (double) this.probeCount;
		this.avgHops = Math.floor(avgHops * 100) / 100;
		return this.avgHops;
	}

	public int getMinHops() {
		if (this.probeCount == 0) {
			return 0;
		}
		return this.minHops;
	}

	public void setStartDegreeMap() {
		for (final MsNode node : this.msNodeMap.values()) {
			this.startDegreeMap.put(node.getID(), node.getDegree());
		}
	}

	public void setEndDegreeMap() {
		for (final MsNode node : this.msNodeMap.values()) {
			this.endDegreeMap.put(node.getID(), node.getDegree());
		}
	}

	public void calculateDegreeGrowth() {
		this.setEndDegreeMap();
		TreeSet<Integer> diffs = new TreeSet<Integer>();
		int sumDiff = 0;
		for (final Integer node : this.endDegreeMap.keySet()) {
			final int diff = this.endDegreeMap.get(node) - this.startDegreeMap.get(node);
			if (diff > this.maxDegreeGrowth) {
				this.maxDegreeGrowth = diff;
			}
			if (diff < this.minDegreeGrowth) {
				this.minDegreeGrowth = diff;
			}
			sumDiff += diff;
			diffs.add(diff);
		}
		int middle = diffs.size() / 2;
		Iterator<Integer> it = diffs.iterator();
		int i = 0;
		while (it.hasNext() && i < middle) {
			it.next();
			i++;
		}
		this.medianDegreeGrowth = it.next();
		this.avgDegreeGrowth = (double) sumDiff / (double) this.endDegreeMap.size();
	}

	public int getMaxDegreeGrowth() {
		if (this.maxDegreeGrowth == Integer.MIN_VALUE) {
			System.out.println("max. degreeInc is not set");
			return 0;
		}
		return this.maxDegreeGrowth;
	}

	public int getMedianDegreeGrowth() {
		return this.medianDegreeGrowth;
	}

	public double getAvgDegreeGrowth() {
		this.avgDegreeGrowth = Math.floor(this.avgDegreeGrowth * 100) / 100;
		return this.avgDegreeGrowth;
	}

	public int getMinDegreeGrowth() {
		if (this.minDegreeGrowth == Integer.MAX_VALUE) {
			System.out.println("min. degreeInc is not set");
			return 0;
		}
		return this.minDegreeGrowth;
	}

	void calculateExtraEdges() {
		TreeSet<Integer> edgesMap = new TreeSet<Integer>();
		int sum = 0;
		for (MsNode v : this.msNodeMap.values()) {
			int unknownEdges = v.getNumOfUnknownEdges();
			edgesMap.add(unknownEdges);
			sum += unknownEdges;
			if (unknownEdges > this.maxExtraEdges) {
				this.maxExtraEdges = unknownEdges;
			}
			if (unknownEdges < this.minExtraEdges) {
				this.minExtraEdges = unknownEdges;
			}
		}
		int middle = edgesMap.size() / 2;
		Iterator<Integer> it = edgesMap.iterator();
		int i = 0;
		while (it.hasNext() && i < middle) {
			it.next();
			i++;
		}
		this.medianExtraEdges = it.next();
		this.avgExtraEdges = (double) sum / this.numOfNodes;
	}

	public int getMaxExtraEdges() {
		return this.maxExtraEdges;
	}

	public int getMedianExtraEdges() {
		return this.medianExtraEdges;
	}

	public double getAvgExtraEdges() {
		return this.avgExtraEdges;
	}

	public int getMinExtraEdges() {
		return this.minExtraEdges;
	}

	public int getUsedMessages() {
		return this.usedMessages;
	}

	public long getEndTime() {
		return this.endTime;
	}

	public long getTimeForStable() {
		return this.timeForStable;
	}

	public int getNumOfNodes() {
		return this.numOfNodes;
	}

	public boolean getStable() {
		return this.stable;
	}

	protected boolean isStable() {
		if (!this.stable) {
			if (this.msNodeMap.size() == this.numOfNodes) {
				for (int i = 0; i <= this.maxLevel; i++) {
					for (MsNode current : this.msNodeMap.values()) {
						int sdist = (int) Math.pow(2, i);
						int left = current.getID() - sdist;
						int right = current.getID() + sdist;
						if (left >= 0) {
							if (!current.hasStableNeighbor(left, i)) {
								return false;
							}
						}
						if (right <= this.numOfNodes - 1) {
							if (!current.hasStableNeighbor(right, i)) {
								return false;
							}
						}
					}
					if (i == 0) {
						if (!this.linearList) {
							this.linearList = true;
						}
					}
				}
				if (!this.naiveAlgo) {
					for (MsNode current : this.msNodeMap.values()) {
						if (current.getNumOfUnknownEdges() != 0) {
							//System.out.println(current.getID() + ": Unknown set not empty" + current.getNumOfUnknownEdges());
							return false;
						}
					}
				}
				this.stable = true;
				this.timeForStable = System.currentTimeMillis();
			}
		}
		return this.stable;
	}

	protected boolean isLinearList() {
		return this.linearList;
	}

	public boolean getTerminate() {
		return this.terminate;
	}

	protected void doAfterTermination() {
		if (this.terminate) {
			this.calculateDegreeGrowth();
			this.calculateDist();
			if (this.naiveAlgo) {
				this.calculateExtraEdges();
			}
			if (this.naiveAlgo) {
				System.out.println("Naive Algo" + " #nodes=" + this.numOfNodes + " -- Time for stabilizing: "
						+ (this.timeForStable - MsController.getInstance().getStartTime()) + " ms.");

			} else {
				System.out.println("Alternative Algo" + " #nodes=" + this.numOfNodes + " -- Time for stabilizing: "
						+ (this.timeForStable - MsController.getInstance().getStartTime()) + " ms.");
			}
			//			if (this.distributedSearches > 0) {
			//				System.out.println("Distributed Searches: " + this.distributedSearches);
			//				System.out.println("Processed Searches: " + this.processedSearchReqs);
			//				if (this.naiveAlgo) {
			//					System.out.println("Greedy Succeed: " + this.succeedGreedy);
			//					System.out.println("Generic Succeed: " + this.succeedGeneric);
			//				} else {
			//					System.out.println("Slow Greedy Succeed: " + this.succeedGreedy);
			//				}
			//				System.out.println("Failed Probe: " + this.failedProbe);
			//
			//			}
			System.out.println();
		}
	}

	protected int getProcessedSearchReqs() {
		return this.processedSearchReqs;
	}

	protected int getDistributedSearches() {
		return this.distributedSearches;
	}

	protected int getSucceedGreedy() {
		return this.succeedGreedy;
	}

	protected int getSucceedGeneric() {
		return this.succeedGeneric;
	}

	protected int getFailedProbe() {
		return this.failedProbe;
	}

	public boolean terminate() {
		if (!this.terminate) {
			boolean localTerminate = false;
			if (this.terminateToStable) {
				localTerminate = this.isStable();
			} else {
				if (!this.searchDone) {
					this.searchDone = (this.processedSearchReqs == this.numOfSearchReqs);
				}
				if (!this.stable) {
					this.stable = this.isStable();
				}
				localTerminate = this.searchDone && this.stable;
			}
			if (localTerminate) {
				if (this.endTime == 0) {
					if (this.numOfSearchReqs == 0) {
						this.endTime = this.timeForStable;
					} else {
						this.endTime = System.currentTimeMillis();
					}
				}
			}
			return localTerminate;
		} else {
			return this.terminate;
		}
	}

	public void setBatches(int batchSize) {
		this.batchSize = batchSize;
		this.terminateToStable = true;
	}
}
