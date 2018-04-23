# resource-simulations

---
## Overview
This component provides an ([OGEMA](http://ogema.org/)) application that simulates resource values based on input time series in CSV format.

It requires Java 8 or higher.   

## Build
Prerequisites: git, Java and Maven installed, ([OGEMA widgets](https://github.com/ogema/ogema-widgets)) binaries available. 

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

## Run
Add the bundle `org.smartrplace.sim/resource-simulation/0.0.1-SNAPSHOT` to your OGEMA run configuration, plus any fragment bundles with simulation data.  
 
