package multiSkipGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JDialog;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.OutputPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.RendererType;
import org.graphstream.stream.file.FileSinkImages.Resolution;
import org.graphstream.stream.file.FileSinkImages.Resolutions;

import base.Controller;
import base.Direction;
import base.Protocol;
import base.Task.TaskType;
import graph.GeneratorNode;
import graph.GraphType;
import simo.ShowGraphFrame;

/**
 * 
 * @author Linghui Luo
 *
 */
public class MsController extends Controller {
	private JDialog parent;
	private static MsController instance;
	private final MsChannel channel;
	private ShowGraphFrame showGraphFrame;
	private HashMap<Integer, MsNode> nodes;
	private boolean showGraph;

	private Protocol protocol;
	private GraphType graphType;
	private int numOfSearches;
	private int numOfTests;
	private int degree;
	private int maxLevel;
	private int batchSize;
	private long startTime;

	private String outputFileName;
	boolean saveData;
	boolean makeFilm;
	private final String moviePath = "movies";
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
	boolean runToStable;

	public static MsController getInstance() {
		if (instance == null) {
			instance = new MsController();
		}
		return instance;
	}

	private MsController() {
		super();
		this.nodes = new HashMap<Integer, MsNode>();
		this.channel = MsChannel.getInstance();
		this.numOfNodes = 0;
		this.numOfSearches = 0;
		this.numOfTests = 0;
		this.graphType = GraphType.ScaleFree;
		this.degree = 0;
		this.maxLevel = 0;
		this.numOfTests = 0;
		this.saveData = true;
		this.runToStable = false;
	}

	public void simulateSingle(Protocol protocol, JDialog parent) {
		this.parent = parent;
		this.showGraph = true;
		switch (protocol) {
		case Naive:
			this.simulateNaive();
			break;
		case Alternative:
			this.simulateAlternative();
			break;
		default:
			this.simulateBoth();
			break;
		}
	}

	public void simulateMulti(Protocol protocol, int minNodes, int step, int maxNodes, boolean absolute, int searches, JDialog parent) {

		Thread simThread = new Thread() {
			@Override
			public void run() {

				MsController.this.parent = parent;
				MsController.this.showGraph = false;
				int currentNum = minNodes;
				while (currentNum <= maxNodes) {
					MsController.this.numOfNodes = currentNum;
					if (!absolute) {
						MsController.this.numOfSearches = searches * MsController.this.numOfNodes;
					} else {
						MsController.this.numOfSearches = searches;
					}
					MsController.this.setForChannel();
					switch (protocol) {
					case Naive:
						MsController.this.simulateNaive();
						break;
					case Alternative:
						MsController.this.simulateAlternative();
						break;
					default:
						MsController.this.simulateBoth();
						break;
					}
					currentNum += step;
				}
				System.out.println("All tests done!");
				System.exit(0);
			}
		};
		simThread.start();
	}

	public void simulateMultiToStable(Protocol protocol, int minNodes, int step, int maxNodes, int batchSize, JDialog parent) {

		this.parent = parent;
		this.showGraph = false;
		this.runToStable = true;
		this.batchSize = batchSize;
		Thread simThread = new Thread() {
			@Override
			public void run() {

				int currentNum = minNodes;
				while (currentNum <= maxNodes) {
					MsController.this.numOfNodes = currentNum;
					MsController.this.channel.setBatches(batchSize);
					MsController.this.setForChannel();
					switch (protocol) {
					case Naive:
						MsController.this.simulateNaive();
						break;
					case Alternative:
						MsController.this.simulateAlternative();
						break;
					default:
						MsController.this.simulateBoth();
						break;
					}
					currentNum += step;
				}
				System.out.println("All tests done!");
				System.exit(0);
			}
		};
		simThread.start();
	}

	private void makeMovie(String fileName, int framesPerSecond, OutputType imageType, boolean cleanImages) throws IOException, InterruptedException {
		// @formatter:off
		int fileNr = 0;
		String outputFile = fileName;
		Path filePath = Paths.get(fileName);
		String imgExt = imageType.toString().toLowerCase();
		// find next free filename by adding a number to the file name
		while (Files.exists(Paths.get(moviePath, outputFile))) {
			String[] fileNameParts = filePath.getFileName().toString().split("\\.");
			fileNameParts[fileNameParts.length - 2] = fileNameParts[fileNameParts.length - 2] + fileNr++;
			outputFile = filePath.resolveSibling(String.join(".", fileNameParts)).toString();
		}

		String exepath = Paths.get("lib\\mencoder.exe").toAbsolutePath().toString();
		String[] command = new String[] { exepath, "\"mf://*." + imgExt + "\"", // file
																				// filter
				"-mf", "fps=" + framesPerSecond + ":type=" + imgExt, // frames
																		// per
																		// second
																		// and
																		// image
																		// type
				"-ovc", "lavc", // codec
				"-lavcopts", "\"vcodec=mpeg4:vqscale=2:vhq:v4mv:trell:autoaspect\"", // codec
																						// options
				"-o", outputFile, // output filename
				"-nosound", "-vf", "scale" }; // filter
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(new File(moviePath));
		Process proc = builder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
		proc.waitFor();

		if (cleanImages) {
			// delete image files
			Files.list(Paths.get(moviePath)).filter(path -> path.toString().endsWith("." + imgExt)).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		}
		// @formatter:on

	}

	private void runNaive() {
		if (this.saveData) {
			this.outputFileName = "Naive_" + "N=" + numOfNodes + "_S=" + numOfSearches + "_D=" + sdf.format(new Date()) + ".csv";
		}
		OutputPolicy outputPolicy = OutputPolicy.BY_EDGE_ADDED_REMOVED;
		OutputType type = OutputType.PNG;
		Resolution resolution = Resolutions.HD720;
		FileSinkImages fsi = null;
		if (this.makeFilm) {
			fsi = new FileSinkImages("prefix", type, resolution, outputPolicy);
			fsi.setRenderer(RendererType.SCALA);
		}
		for (int i = 1; i <= numOfTests; i++) {
			Graph graph = null;
			if (showGraph) {
				graph = new MultiGraph("Naive Test " + i + "/" + numOfTests + ": #nodes=" + numOfNodes + " #searches=" + numOfSearches);
			}
			if (this.makeFilm) {
				graph.addSink(fsi);
			}
			try {
				if (showGraph) { // for the graphic animation
					showGraphFrame = new ShowGraphFrame(MsController.getInstance().parent, 0);
					showGraphFrame.setGraph(graph);
				}
				HashMap<Integer, GeneratorNode> generated = new HashMap<Integer, GeneratorNode>();
				switch (graphType) {
				case ScaleFree:
					generated = generator.generateScaleFreeGraph(numOfNodes, 0, degree);
					break;
				case RandomAvgDegree:
					generated = generator.generatorGraphWithAG(numOfNodes, 0, degree);
					break;
				default:
					generated = generator.generatorRandomGraph(numOfNodes, 0);
					break;
				}
				nodes = generator.convertToGraphWithMsNode(generated, graph);
				if (this.makeFilm) {
					fsi.begin(moviePath + "/film");
				}
				boolean naiveAlgo = true;
				channel.setToRun(naiveAlgo);
				startTime = System.currentTimeMillis();
				for (MsNode node : nodes.values()) {
					node.start();
				}
				try {
					for (MsNode node : nodes.values()) {
						node.join();
					}
				} catch (Exception e) {
				}
				if (this.makeFilm) {
					fsi.end();
					this.makeMovie("movie.avi", 15, type, true);
				}
			} catch (IOException | InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// algo terminated
			this.channel.doAfterTermination();
			if (saveData) {
				this.saveToFile(i);
			}
			channel.reset(false);
			if (showGraph) {
				if (i != numOfTests) {
					graph.clear();
					showGraphFrame.dispose();
				}
			}
		}
	}

	private void runAlternative() {
		OutputPolicy outputPolicy = OutputPolicy.BY_EDGE_ADDED_REMOVED;
		OutputType type = OutputType.PNG;
		Resolution resolution = Resolutions.HD720;
		FileSinkImages fsi = null;
		if (this.saveData) {
			this.outputFileName = "Alter_" + "N=" + numOfNodes + "_S=" + numOfSearches + "_D=" + sdf.format(new Date()) + ".csv";
		}
		for (int i = 1; i <= numOfTests; i++) {
			Graph graph = null;
			if (showGraph) {
				graph = new MultiGraph("Alternative Test " + i + "/" + numOfTests + ": #nodes=" + numOfNodes + " #searches=" + numOfSearches);
			}
			if (showGraph) {// for the graphic animation
				showGraphFrame = new ShowGraphFrame(parent, 0);
				showGraphFrame.setGraph(graph);
			}
			if (this.makeFilm) {
				fsi = new FileSinkImages("prefix", type, resolution, outputPolicy);
				fsi.setRenderer(RendererType.SCALA);
				graph.addSink(fsi);
			}
			HashMap<Integer, GeneratorNode> generated = new HashMap<Integer, GeneratorNode>();
			switch (graphType) {
			case ScaleFree:
				generated = generator.generateScaleFreeGraph(numOfNodes, 0, degree);
				break;
			case RandomAvgDegree:
				generated = generator.generatorGraphWithAG(numOfNodes, 0, degree);
				break;
			default:
				generated = generator.generatorRandomGraph(numOfNodes, 0);
				break;
			}
			nodes = generator.convertToGraphWithMsNode(generated, graph);
			boolean naiveAlgo = false;
			if (this.makeFilm) {
				try {
					fsi.begin(moviePath + "/film");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			channel.setToRun(naiveAlgo);
			startTime = System.currentTimeMillis();
			for (MsNode node : nodes.values()) {
				node.start();
			}
			try {
				for (MsNode node : nodes.values()) {
					node.join();
				}
			} catch (Exception e) {
			}
			// algo terminated
			this.channel.doAfterTermination();
			if (saveData) {
				this.saveToFile(i);
			}
			if (this.makeFilm) {
				try {
					fsi.end();
					this.makeMovie("movie_alternative.avi", 15, type, true);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			channel.reset(false);
			if (showGraph) {
				if (i != numOfTests) {
					graph.clear();
					showGraphFrame.dispose();
				}
			}
		}
	}

	private void runBoth() {
		String name = "N=" + numOfNodes + "_S=" + numOfSearches + "_D=" + sdf.format(new Date()) + ".csv";
		OutputPolicy outputPolicy = OutputPolicy.BY_EDGE_ADDED_REMOVED;
		OutputType type = OutputType.PNG;
		Resolution resolution = Resolutions.HD720;
		FileSinkImages fsiNaive = null;
		FileSinkImages fsiAlt = null;

		for (int i = 1; i <= numOfTests; i++) {
			System.out.println("Test " + i);
			if (this.saveData) {
				this.outputFileName = "Both_Naive_" + name;
			}
			Graph firstGraph = null;
			if (showGraph || this.makeFilm) {
				firstGraph = new MultiGraph("Naive Test " + i + "/" + numOfTests + ": #nodes=" + numOfNodes + " #searches=" + numOfSearches);
			}

			if (this.makeFilm) {
				fsiNaive = new FileSinkImages("prefix", type, resolution, outputPolicy);
				fsiNaive.setRenderer(RendererType.SCALA);
				fsiAlt = new FileSinkImages("prefix", type, resolution, outputPolicy);
				fsiAlt.setRenderer(RendererType.SCALA);
				firstGraph.addSink(fsiNaive);
			}

			if (showGraph) {
				showGraphFrame = new ShowGraphFrame(parent, 1);
				showGraphFrame.setGraph(firstGraph);
			}
			// generate a random graph
			HashMap<Integer, GeneratorNode> generated = new HashMap<Integer, GeneratorNode>();
			switch (graphType) {
			case ScaleFree:
				generated = generator.generateScaleFreeGraph(numOfNodes, 0, degree);
				break;
			case RandomAvgDegree:
				generated = generator.generatorGraphWithAG(numOfNodes, 0, degree);
				break;
			default:
				generated = generator.generatorRandomGraph(numOfNodes, 0);
				break;
			}
			nodes = generator.convertToGraphWithMsNode(generated, firstGraph);
			// deep copy the random graph
			Graph secondGraph = null;
			if (showGraph || this.makeFilm) {
				secondGraph = new MultiGraph("Alternative Test: " + i + "/" + numOfTests + ": #nodes=" + numOfNodes + " #searches=" + numOfSearches);
			}
			if (makeFilm) {
				secondGraph.addSink(fsiAlt);
			}
			HashMap<Integer, MsNode> copyNodes = MsController.copyGraph(nodes, secondGraph);
			// run naive algo
			protocol = Protocol.Naive;
			boolean naiveAlgo = true;
			if (this.makeFilm) {
				try {
					fsiNaive.begin(moviePath + "/film");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			channel.setToRun(naiveAlgo);
			startTime = System.currentTimeMillis();
			for (MsNode node : nodes.values()) {
				node.start();
			}
			try {
				for (MsNode node : nodes.values()) {
					node.join();
				}
			} catch (Exception e) {
			}
			this.channel.doAfterTermination();
			if (saveData) {
				this.saveToFile(i);
				this.outputFileName = "Both_Alter_" + name;
			}
			if (this.makeFilm) {
				try {
					fsiNaive.end();
					this.makeMovie("movie_naive.avi", 15, type, true);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// run alternative algo
			ShowGraphFrame secondFrame = null;
			if (showGraph) {
				secondFrame = new ShowGraphFrame(parent, 2);
				secondFrame.setGraph(secondGraph);
			}
			nodes = copyNodes;
			boolean useLastSearches = true;
			channel.reset(useLastSearches);
			naiveAlgo = false;
			channel.setToRun(naiveAlgo);
			for (MsNode node : copyNodes.values()) {
				MsChannel.getInstance().registerNode(node);
			}
			protocol = Protocol.Alternative;
			if (this.makeFilm) {
				try {
					fsiAlt.begin(moviePath + "/film");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			startTime = System.currentTimeMillis();
			for (MsNode node : nodes.values()) {
				node.start();
			}
			try {
				for (MsNode node : nodes.values()) {
					node.join();
				}
			} catch (Exception e) {
			}

			this.channel.doAfterTermination();
			if (saveData) {
				this.saveToFile(i);
			}
			if (this.makeFilm) {
				try {
					fsiAlt.end();
					this.makeMovie("movie_alternative.avi", 15, type, true);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			channel.reset(false);
			if (showGraph) {
				if (i != numOfTests) {
					firstGraph.clear();
					secondGraph.clear();
					showGraphFrame.dispose();
					secondFrame.dispose();
				}
			}
		}
	}

	private void simulateNaive() {
		if (showGraph) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					MsController.this.runNaive();
				}
			});
			thread.start();
		} else {
			this.runNaive();
		}
	}

	private void simulateAlternative() {
		if (showGraph) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					MsController.this.runAlternative();
				}
			});
			thread.start();
		} else {
			this.runAlternative();
		}
	}

	private void simulateBoth() {
		if (showGraph) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					MsController.this.runBoth();
				}
			});
			thread.start();
		} else {
			this.runBoth();
		}
	}

	public static HashMap<Integer, MsNode> copyGraph(HashMap<Integer, MsNode> nodes, Graph newGraph) {
		HashMap<Integer, MsNode> copy = new HashMap<Integer, MsNode>();
		// copy the nodes
		for (MsNode node : nodes.values()) {
			copy.put(node.getID(), node.copy(newGraph));
		}
		// copy explicit and implicit edges
		for (MsNode node : nodes.values()) {
			// copy left neighbors
			for (Entry<Integer, Integer> neighbor : node.getLeftLevelMap().entrySet()) {
				copy.get(node.getID()).addLevelNeighbor(copy.get(neighbor.getKey()), neighbor.getValue(), Direction.LEFT, false);
			}
			// copy right neighbors
			for (Entry<Integer, Integer> neighbor : node.getRightLevelMap().entrySet()) {
				copy.get(node.getID()).addLevelNeighbor(copy.get(neighbor.getKey()), neighbor.getValue(), Direction.RIGHT, false);
			}
			// copy messages
			for (MsTask task : node.getTaskQueue()) {
				if (task.getTaskType() == TaskType.MESSAGE) {
					MsMessage msg = task.getMessage();
					MsMessage copyMsg = msg.copy();
					if (msg.getSource() != null) {
						copyMsg.setSource(copy.get(msg.getSource().getID()));
					}
					if (msg.getDest() != null) {
						copyMsg.setDest(copy.get(msg.getDest().getID()));
					}
					if (msg.getNode() != null) {
						copyMsg.setNode(copy.get(msg.getNode().getID()));
					}
					if (msg.getNext() != null) {
						TreeMap<Integer, MsNode> copyNext = new TreeMap<Integer, MsNode>();
						for (Entry<Integer, MsNode> v : msg.getNext().entrySet()) {
							copyNext.put(v.getKey(), copy.get(v.getKey()));
						}
						copyMsg.setNext(copyNext);
					}
					copy.get(node.getID()).receiveMessage(copyMsg);
				}
			}
		}
		return copy;
	}

	public int getMaxLevel() {
		return this.maxLevel;
	}

	protected long getStartTime() {
		return this.startTime;
	}

	public void setProperties(Protocol protocol, int numOfNodes, int numOfSearches, GraphType graphType, int degree, int numOfTests, boolean saveData,
			boolean makeFilm) {
		this.protocol = protocol;
		this.numOfNodes = numOfNodes;
		this.numOfSearches = numOfSearches;
		this.graphType = graphType;
		this.degree = degree;
		this.numOfTests = numOfTests;
		this.saveData = saveData;
		this.makeFilm = makeFilm;
		this.setForChannel();
	}

	private void setForChannel() {
		this.maxLevel = (int) Math.floor(Math.log(this.numOfNodes - 1) / Math.log(2));
		this.channel.setNumOfNodes(numOfNodes);
		this.channel.setNumOfSearchReqs(numOfSearches);
	}

	public String getProtocol() {
		return this.protocol.toString();
	}

	public int getTestNo() {
		return this.numOfTests;
	}

	private void saveToFile(int testNo) {
		StringBuilder output = new StringBuilder();
		if (!Files.exists(Paths.get(outputFileName))) {
			if (this.runToStable || this.numOfSearches > 0) {
				if (this.runToStable) {
					output.append("Batch_Size;" + this.batchSize + "\n");
				}
				if (this.protocol == Protocol.Naive) {
					output.append(
							"Test#;Protocol;#Nodes;Time;#Messages;#Searches;#Greedy_Succ;#Generic_Succ;#Failed_Probe;Max_Hops;Median_Hops;Avg_Hops;Min_Hops;\n");
				} else {
					output.append(
							"Test#;Protocol;#Nodes;Time;#Messages;#Searches;#Slow_Greedy_Succ;#Failed_Probe;Max_Hops;Median_Hops;Avg_Hops;Min_Hops;\n");
				}
			} else {
				if (this.protocol == Protocol.Naive) {
					output.append(
							"Test#;Protocol;#Nodes;Time;#Messages;Max_Degree_Growth;Median_Degree_Growth;Avg_Degree_Growth;Min_Degree_Growth;Max_Dist;Median_Dist;Avg_Dist;Max_Extra_Edges;Median_Extra_Edges; Avg_Extra_Edges;Min_Extra_Edges\n");
				} else {
					output.append(
							"Test#;Protocol;#Nodes;Time;#Messages;Max_Degree_Growth;Median_Degree_Growth;Avg_Degree_Growth;Min_Degree_Growth;Max_Dist;Median_Dist;Avg_Dist\n");
				}
			}
		}
		output.append(testNo);
		output.append(";");
		output.append(MsController.getInstance().getProtocol());
		output.append(";");
		output.append(this.numOfNodes);
		if (this.runToStable || this.numOfSearches > 0) {
			output.append(";");
			output.append(channel.getEndTime() - this.getStartTime());
			output.append(";");
			output.append(channel.getUsedMessages());
			output.append(";");
			output.append(channel.getProcessedSearchReqs());
			output.append(";");
			output.append(channel.getSucceedGreedy());
			output.append(";");
			if (this.protocol == Protocol.Naive) {
				output.append(channel.getSucceedGeneric());
				output.append(";");
			}
			output.append(channel.getFailedProbe());
			output.append(";");
			output.append(channel.getMaxHops());
			output.append(";");
			output.append(channel.getMedianHops());
			output.append(";");
			String avgHops = channel.getAvgHops() + "";
			output.append(avgHops.replace(".", ","));// stoopid excel
			output.append(";");
			output.append(channel.getMinHops());
			output.append("\n");
		} else {
			output.append(";");
			output.append(channel.getEndTime() - this.getStartTime());
			output.append(";");
			output.append(channel.getUsedMessages());
			output.append(";");
			output.append(channel.getMaxDegreeGrowth());
			output.append(";");
			output.append(channel.getMedianDegreeGrowth());
			output.append(";");
			String avgInc = channel.getAvgDegreeGrowth() + "";
			output.append(avgInc.replace(".", ","));// stoopid excel
			output.append(";");
			output.append(channel.getMinDegreeGrowth());
			output.append(";");
			output.append(channel.getMaxDist());
			output.append(";");
			output.append(channel.getMedianDist());
			output.append(";");
			String avgDist = channel.getAvgDist() + "";
			output.append(avgDist.replace(".", ","));// stoopid excel
			if (this.protocol == Protocol.Naive) {
				output.append(";");
				output.append(channel.getMaxExtraEdges());
				output.append(";");
				output.append(channel.getMedianExtraEdges());
				output.append(";");
				String avgEdges = channel.getAvgExtraEdges() + "";
				output.append(avgEdges.replace(".", ","));// stoopid excel
				output.append(";");
				output.append(channel.getMinExtraEdges());
			}
			output.append("\n");
		}
		try {
			Files.write(Paths.get(outputFileName), output.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// exception handling left as an exercise for the reader
		}
	}

	public boolean getShowGraph() {
		return this.showGraph;
	}
}
