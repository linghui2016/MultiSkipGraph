package MultiSkipGraph;

import  Base.Task;

/**
 * 
 * @author Linghui Luo
 *
 */
public class MsTask extends Task
{
	MsMessage message;
	
	public MsTask(TaskType taskType)
	{
		super(taskType);
	}
	
	public MsTask(TaskType taskType, MsMessage message)
	{
		super(taskType);
		this.message = message;
	}
	
	protected MsMessage getMessage()
	{
		return this.message;
	}
}
