package base;

import org.graphstream.algorithm.ConnectedComponents;

import graph.GraphGenerator;

/**
 * The super class of all kinds of controllers.
 *
 * @author Linghui Luo
 *
 */
public abstract class Controller {
	protected GraphGenerator generator;
	protected boolean log;
	protected int numOfNodes;
	protected boolean showImplicit;
	public ConnectedComponents cc;
	//private boolean printOutput;
	//private final boolean debug = false;

	public Controller() {
		this.generator = new GraphGenerator();
		//this.printOutput = false;
	}

	public int getNumOfNodes() {
		return this.numOfNodes;
	}

	//	public void isConnected(String txt) {
	//		synchronized (this.graph) {
	//			if (this.printOutput) {
	//				final int n = this.cc.getConnectedComponentsCount();
	//				if (n > 1) {
	//					System.out.println(txt);
	//					System.out.printf("Not Connected: #connected components=%d.%n", n);
	//					this.printOutput = false;
	//				}
	//				if (this.debug) {
	//					System.out.printf("Debug: #connected components=%d.%n", n);
	//				}
	//			}
	//		}
	//	}

	/**
	 * This methods tells if a log should be saved
	 *
	 * @return
	 */
	public boolean saveLog() {
		return this.log;
	}

	//	/**
	//	 * this method is used if you want to print the number of connected
	//	 * components.
	//	 */
	//	public void setCcOutput() {
	//		this.cc = new ConnectedComponents();
	//		this.cc.init(this.graph);
	//		this.printOutput = true;
	//	}

	public boolean showImplicit() {
		return this.showImplicit;
	}
}
