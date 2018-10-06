package Base;

/**
 * There are two kinds of task for a node: either the node handles a message or executes timeout.
 * 
 * @author Linghui Luo
 * 
 */
public class Task
{
	public enum TaskType
	{
		MESSAGE, TIMEOUT
	}
	
	private TaskType	taskType;
	
	public Task(TaskType taskType)
	{
		this.taskType = taskType;
	}
	
	public TaskType getTaskType()
	{
		return this.taskType;
	}
}
