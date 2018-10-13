# MultiSkipGraph - A Self-stabilizing Overlay Network that Maintains Monotonic Searchability

This is a simulator implemented in Java for evaluating and demonstrating the MultiSkipGraph protocol and its extended version MultiSkipGraph*.

## Watch a demo
In the visualization, nodes on even levels are blue, on odd levels are red and marked as "unknown" are gray at the top. 
- Click to see a demo of simulating a self-stabilizing overlay network with 9 nodes using the MultiSkipGraph protocol.
[![multiskipgraph](https://img.youtube.com/vi/S8yd7fApSfk/0.jpg)](http://www.youtube.com/watch?v=S8yd7fApSfk)
- Click to see a demo of simulating a self-stabilizing overlay network with 9 nodes using the MultiSkipGraph* protocol.
[![multiskipgraphstar](https://img.youtube.com/vi/keOdsxxjWwU/0.jpg)](http://www.youtube.com/watch?v=keOdsxxjWwU)
## How to run it?
-  You can 
    - run the executable jar [multiSkipGraph.jar](https://github.com/linghui2016/MultiSkipGraph/releases/download/IPDPS/multiSkipGraph.jar) from the [release page](https://github.com/linghui2016/MultiSkipGraph/releases) or 
    - or import this project as Java Project into Eclipse. Simo.java is the main class to start the program, run it 
- You will see a window with two modes to be chosen:
  - ``Single-Test-Mode`` is for simulation with a fixed network size. 
    - In this mode the simulation will be visualized: 
      - nodes on even levels are blue 
      - nodes on odd levels are red 
      - nodes marked as unknown are gray (at the top)
    - User can select if the test data will be saved 
            in your local working repository.
    - User can select if the simulation should be filmed and
      saved in folder "movies"(Number of Tests must be 1). 

  - ``Multi-Tests-Mode`` is for simulation with variable network size
    - Test data will be written into csv files
    - Minimum: the starting network size
    - Step value: network size increment
    - Maximum: the ending network size
    - For searches: the user can give
      - an absoulte number (terminate when all searches are processed and the network is staiblized)
      - a relative number to network size (terminate when all searches are processed and the network is staiblized)
      - a batch size (terminate when the network is stabilized)
 - Click the ``Single-Test-Mode`` button, choose ``Both for comparision``, and then ``Simulate`` button to run a simulation by the default setting. 
 - You will see the simulation running first the MultiSkipGraph protocol and then the MultiSkipGraph protocol*.
 
## Contact
Please e-mail linghui.luo@upb.de if you have any questions.  

