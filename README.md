# MultiSkipGraph - A Self-stabilizing Overlay Network that Maintains Monotonic Searchability

This is a simulator implemted for evaluating and demonstrating the MultiSkipGraph protocol and its extended version MultiSkipGraph*.

## How to use it?
-  You can 
    - run the executable jar multiSkipGraph.jar from the [release page](https://github.com/linghui2016/MultiSkipGraph/releases/download/IPDPS/multiSkipGraph.jar) or 
    - or import this project as Java Project into Eclipse. Simo.java is the main class to start the program, run it 
- You will see a window with two modes to be chosen:
  - Single-Test-Mode is for simulation with a fixed network size
    - In this mode the simulation will be visualized: 
      - nodes on even levels are blue 
      - nodes on odd levels are red 
      - nodes marked as unknown are gray
    - User can select if the test data will be saved 
            in your local working repository.
    - User can select if the simulation should be filmed and
      saved in folder "movies"(Number of Tests must be 1). 

  - Multi-Tests-Mode is for simulation with variable network size
    - Test data will be written into csv files
    - Minimum: the starting network size
    - Step value: network size increment
    - Maximum: the ending network size
    - For searches: the user can give
      - an absoulte number (terminate when all searches are processed and the network is staiblized)
      - a relative number to network size (terminate when all searches are processed and the network is staiblized)
      - a batch size (terminate when the network is stabilized)

