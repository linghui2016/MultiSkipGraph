package Base;

import java.util.TimerTask;

import MultiSkipGraph.MsChannel;
import MultiSkipGraph.MsMessage;
import MultiSkipGraph.MsNode;

/**
 * MessageTask is used by the sendMsg method in respective Channel. For a new kind of node. we should extend a constructor
 * for it and add modifications in the method run()
 *
 * @author Linghui Luo
 *
 */
public class MessageTask extends TimerTask {

	private final MsNode goal;
	private final MsMessage message;

	public MessageTask(MsNode goal, MsMessage message) {
		this.goal = goal;
		this.message = message;
	}

	@Override
	public void run() {
		if (this.goal != null) {
			MsChannel.getInstance().deliverMsg(this.goal, this.message);
		}
	}
}
