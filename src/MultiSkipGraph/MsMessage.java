package MultiSkipGraph;

import java.util.TreeMap;

import Base.Action;
import Base.Direction;
import Base.Message;

/**
 * 
 * @author Linghui Luo
 *
 */
public class MsMessage extends Message {
	private int destID;
	private int seq;
	private int hops;
	private int oLevel;
	private int level;
	private Direction dir;

	private MsNode source;
	private MsNode dest;
	private MsNode node;
	private TreeMap<Integer, MsNode> next;
	private TreeMap<Integer, MsNode> prev;

	public MsMessage(int msgID, Action action) {
		super(action, msgID);
	}

	/**
	 * Greedy_Search message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param source
	 * @param destID
	 * @param seq
	 */
	public MsMessage(int msgID, Action action, MsNode source, int destID, int seq, int hops) {
		super(action, msgID);
		this.source = source;
		this.destID = destID;
		this.seq = seq;
		this.hops = hops;
	}

	/**
	 * Slow_Greedy_Search message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param source
	 * @param destID
	 * @param prev
	 * @param next
	 * @param seq
	 * @param hops
	 */
	public MsMessage(int msgID, Action action, MsNode source, int destID, TreeMap<Integer, MsNode> prev, TreeMap<Integer, MsNode> next, int seq,
			int hops) {
		super(action, msgID);
		this.source = source;
		this.destID = destID;
		this.prev = prev;
		this.next = next;
		this.seq = seq;
		this.hops = hops;
	}

	/**
	 * Generic_Search message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param source
	 * @param destID
	 * @param seq
	 */
	public MsMessage(int msgID, Action action, MsNode source, int destID, TreeMap<Integer, MsNode> next, int seq, int hops) {
		super(action, msgID);
		this.source = source;
		this.destID = destID;
		this.next = next;
		this.seq = seq;
		this.hops = hops;
	}

	/**
	 * Introduce or Safe_Deletion message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param node
	 */
	public MsMessage(int msgID, Action action, MsNode node) {
		super(action, msgID);
		this.node = node;
	}

	/**
	 * Intro_Level_Node message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param level
	 * @param node
	 * @param delay
	 */
	public MsMessage(int msgID, Action action, int level, MsNode node) {
		super(action, msgID);
		this.node = node;
		this.level = level;
	}

	/**
	 * Search_Success message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param destID
	 * @param seq
	 * @param dest
	 */
	public MsMessage(int msgID, Action action, int destID, int seq, MsNode dest) {
		super(action, msgID);
		this.dest = dest;
		this.destID = destID;
		this.seq = seq;
	}

	/**
	 * Search_Fail message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param destID
	 * @param seq
	 */
	public MsMessage(int msgID, Action action, int destID, int seq) {
		super(action, msgID);
		this.destID = destID;
		this.seq = seq;
	}

	/**
	 * Search message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param sourceNode
	 * @param destID
	 */
	public MsMessage(int msgID, Action action, MsNode source, int destID) {
		super(action, msgID);
		this.source = source;
		this.destID = destID;
	}

	/**
	 * PROBE_LEVEL_INTRO message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param source
	 * @param dest
	 * @param level
	 */
	public MsMessage(int msgID, Action action, MsNode source, MsNode dest, int oLevel, int level, Direction dir) {
		super(action, msgID);
		this.source = source;
		this.dest = dest;
		this.oLevel = oLevel;
		this.level = level;
		this.dir = dir;
	}

	/**
	 * PROBE_LEVEL_INTRO_SUCC message for MultiSkipGraph protocol
	 *
	 * @param msgID
	 * @param action
	 * @param dest
	 * @param oLevel
	 * @param dir
	 */
	public MsMessage(int msgID, Action action, MsNode dest, int oLevel, Direction dir) {
		super(action, msgID);
		this.dest = dest;
		this.oLevel = oLevel;
		this.dir = dir;
	}

	/**
	 * SAFE_INTRODUCE message
	 *
	 * @param msgID
	 * @param action
	 * @param node
	 * @param source
	 */
	public MsMessage(int msgID, Action action, MsNode node, MsNode source) {
		super(action, msgID);
		this.node = node;
		this.source = source;
	}

	protected void setSource(MsNode source) {
		this.source = source;
	}

	protected void setDest(MsNode dest) {
		this.dest = dest;
	}

	protected void setNode(MsNode node) {
		this.node = node;
	}

	protected void setNext(TreeMap<Integer, MsNode> next) {
		this.next = next;
	}

	protected MsNode getSource() {
		return this.source;
	}

	protected int getDestID() {
		return this.destID;
	}

	protected int getSeq() {
		return this.seq;
	}

	protected MsNode getDest() {
		return this.dest;
	}

	protected MsNode getNode() {
		return this.node;
	}

	protected int getHops() {
		return this.hops;
	}

	protected int getLevel() {
		return this.level;
	}

	protected int getOLevel() {
		return this.oLevel;
	}

	protected Direction getDir() {
		return this.dir;
	}

	protected TreeMap<Integer, MsNode> getNext() {
		return this.next;
	}

	protected TreeMap<Integer, MsNode> getPrev() {
		return this.prev;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.destID;
		result = prime * result + this.seq;
		result = prime * result + this.hops;
		result = prime * result + this.oLevel;
		result = prime * result + this.level;
		result = prime * result + ((this.dir == null) ? 0 : this.dir.hashCode());
		result = prime * result + ((this.source == null) ? 0 : this.source.hashCode());
		result = prime * result + ((this.dest == null) ? 0 : this.dest.hashCode());
		result = prime * result + ((this.node == null) ? 0 : this.node.getID().hashCode());
		result = prime * result + ((this.next == null) ? 0 : this.next.hashCode());
		result = prime * result + ((this.prev == null) ? 0 : this.prev.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		MsMessage other = (MsMessage) obj;
		// compare destID
		if (this.destID != other.destID) {
			return false;
		}
		// compare seq
		if (this.seq != other.seq) {
			return false;
		}
		// compare hops
		if (this.hops != other.hops) {
			return false;
		}
		// compare oLevel
		if (this.oLevel != other.oLevel) {
			return false;
		}
		// compare level
		if (this.level != other.level) {
			return false;
		}
		// compare dir
		if (this.dir != other.dir) {
			return false;
		}
		// compare source
		if (this.source == null) {
			if (other.source != null) {
				return false;
			}
		} else {
			if (other.source == null) {
				return false;
			} else if (!this.source.getID().equals(other.source.getID())) {
				return false;
			}
		}

		// compare dest
		if (this.dest == null) {
			if (other.dest != null) {
				return false;
			}
		} else {
			if (other.dest == null) {
				return false;
			} else if (!this.dest.getID().equals(other.dest.getID())) {
				return false;
			}
		}
		// compare node
		if (this.node == null) {
			if (other.node != null) {
				return false;
			}
		} else {
			if (other.node == null) {
				return false;
			} else if (!this.node.getID().equals(other.node.getID())) {
				return false;
			}
		}
		// compare next
		if (this.next == null) {
			if (other.next != null) {
				return false;
			}
		} else {
			if (other.next == null) {
				return false;
			} else {
				for (MsNode v : this.next.values()) {
					if (!other.next.containsKey(v.getID())) {
						return false;
					}
				}
			}
		}
		// compare prev
		if (this.prev == null) {
			if (other.prev != null) {
				return false;
			}
		} else {
			if (other.prev == null) {
				return false;
			} else {
				for (MsNode v : this.prev.values()) {
					if (!other.prev.containsKey(v.getID())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	protected MsMessage copy() {
		MsMessage copy = new MsMessage(this.msgID, this.action);
		copy.destID = this.destID;
		copy.seq = this.seq;
		copy.hops = this.hops;
		copy.oLevel = this.oLevel;
		copy.level = this.level;
		copy.dir = this.dir;
		return copy;
	}
}
