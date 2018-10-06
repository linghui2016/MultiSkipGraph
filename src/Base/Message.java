package Base;

/**
 * The super class all kinds of messages. It should contains three elements: msgID, action and the delay to be sent.
 *
 * @author Linghui Luo
 *
 */
public class Message {
	protected Action action;
	protected int msgID;

	protected Message(Action action, int msgID) {
		this.action = action;
		this.msgID = msgID;
	}

	public Action getAction() {
		return this.action;
	}

	public int getMsgID() {
		return this.msgID;
	}
}
