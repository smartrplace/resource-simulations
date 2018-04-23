# resource-simulations

---
## Overview
This component provides an [OGEMA](http://ogema.org/) application that simulates resource values based on input time series in CSV format.

It requires Java 8 or higher.   

## Build
Prerequisites: git, Java and Maven installed, [OGEMA widgets](https://github.com/ogema/ogema-widgets) binaries available. 

1. Clone this repository
2. In a shell, navigate to the base folder and execute `mvn clean install`.

## Configuration
The repository only contains dummy time series as examples. New simulation time series can be added in two ways: either by means of an extension bundle (see example project), or by adding a folder "simTemplates" in the rundir. Below "simTemplates", use 1-level subfolders to structure your data, and make sure all files have ".csv"-endings. Example folder structure:
```
rundir
|
+---simTemplates
|    |
|    +---temperature
|    |    |---default.csv
|    |    |---outside.csv
|    |
|    +---power
|    |    |---default.csv
|    |    |---subphase1.csv
|    |    |---subphase2.csv
|    |    |---subphase3.csv
```
CSV files are comma-separated and conists of two columns: time (in ms) and value:
```
0;293.15
5000;293.46396
10000;293.77667
15000;294.0869
```
The timeseries will be repeated over and over again, when it is finished. 

To start a simulation, either use the provided GUI to create the configuration, or directly create a resource of type [ScheduledSimulationConfig](https://github.com/smartrplace/resource-simulations/blob/master/resource-simulation/src/main/java/org/smartrplace/sim/resource/config/ScheduledSimulationConfig.java), with a reference to the simulated resource in the `target` field, and two ids for the simulation type in `typePrimary` and `typeSecondary`. For instance, the types could be `temperature` and `outside`, for the example folder structure above. If the secondary type is `default`, it can be skipped.

## Run
Add the bundle `org.smartrplace.sim/resource-simulation/0.0.1-SNAPSHOT` to your OGEMA run configuration, plus any fragment bundles with simulation data.  
 
