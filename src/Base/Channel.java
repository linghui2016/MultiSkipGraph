package Base;

/**
 * The super class of all kinds of channels.
 *
 * @author Linghui Luo
 *
 */
public abstract class Channel {
	private int msgCount; // current amount of generated messages

	/**
	 * generate an unique message id
	 */
	public synchronized int generateMsgId() {
		this.msgCount++;
		return this.msgCount;
	}

	/**
	 *
	 * @return current amount of generated messages
	 */
	public int getMsgCount() {
		return this.msgCount;
	}
}
