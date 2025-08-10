# ATAP - Agent-based Traffic Assignment Problem

** This document is under construction. Please come back later. **

This program implements a solution heuristic for the ATAP. It pushes the model system towards a Nash equilibrium (still without any guarantee that such an equilbrium exists or that it will be reached given that it exists). This document provides a detailed explanation of the method:

*G. Flötteröd (2025). A simulation heuristic for traveler- and vehicle-discrete dynamic traffic assignment. Technical report. Linköping University and Swedish National Road and Transport Research Institute.*

## Using the ATAP MATSim extension

The ATAP extension replaces MATSim's standard solver (a coevolutionary algorithm). Experience so far indicates that solutions computed with ATAP exhibit very little variability (i.e. high reproducibility) and have much lower equilibrium gaps than the standard solver.

### Accessing the code

Either clone the repository or configure [github packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages). 

Include the following Maven dependency in your pom.xml:

	<dependency>
		<groupId>se.vti.matsim-projects</groupId>
		<artifactId>atap</artifactId>
		<version>TODO</version>
	</dependency>

The MATSim extension lies in the se.vti.atap.matsim folder.

### Using the code

Minimal usage:

	ATAP atap = new ATAP();
	
	Config config = ConfigUtils.loadConfig(configFileName);		
	atap.configure(config);
		
	Scenario scenario = ScenarioUtils.loadScenario(config);		
	Controler controler = new Controler(scenario);
	atap.configure(controler);
	
	controler.run();
	
This will use a default configuration, which may work for simple scenarios. Two additional configuration steps may be necessary.

Add an atap module to your MATSim config file. Example:

	<module name="atap" >
		<param name="cheapStrategies" value="TimeAllocationMutator" />
		<param name="checkEmulatedAgentsCnt" value="0" />
		<param name="expensiveStrategies" value="ReRoute,TimeAllocationMutator_ReRoute,ChangeSingleTripMode,SubtourModeChoice,ChangeTripMode,ChangeLegMode,ChangeSingleLegMode,TripSubtourModeChoice" />
		<param name="initialStepSizeFactor" value="1.0" />
		<param name="kernelHalftime_s" value="300.0" />
		<param name="kernelThreshold" value="0.01" />
		<param name="maxMemory" value="1" />
		<param name="populationDistance" value="Kernel" />
		<param name="replannerIdentifier" value="UPPERBOUND_ATOMIC" />
		<param name="replanningRateIterationExponent" value="-0.5" />
		<param name="upperboundStepSize" value="Vanilla" />
		<param name="useFilteredTravelTimesForEmulation" value="false" />
		<param name="useFilteredTravelTimesForReplanning" value="false" />
		<param name="useLinearDistance" value="true" />
		<param name="useQuadraticDistance" value="true" />
	</module>

(Put explanations here, or in the code.)

ATAP needs to anticipate the scores of not yet executed plans. This functionality is provided by the emulation package, on which ATAP depends. Emulation is preconfigured for car as a congested network mode and for teleported modes. Other transport modes require to submit corresponding emulation functionality. For instance, the emulation of pt submodes is configured as follows:

	ATAP atap = new ATAP();
	
	atap.setEmulator(TransportMode.pt, ScheduleBasedTransitLegEmulator.class);
	atap.setEmulator("busPassenger", ScheduleBasedTransitLegEmulator.class);
	atap.setEmulator("subwayPassenger", ScheduleBasedTransitLegEmulator.class);
	atap.setEmulator("railPassenger", ScheduleBasedTransitLegEmulator.class);
	
	Config config = ConfigUtils.loadConfig(configFileName);		
	atap.configure(config);
		
	Scenario scenario = ScenarioUtils.loadScenario(config);		
	Controler controler = new Controler(scenario);
	atap.configure(controler);
	
	controler.run();
	
There is also (likely outdated) functionality for emulating roadpricing.

The package `se.vti.atap.matsim.examples.parallel_links` offers a ready-to-run example of using the ATAP assignment logic in MATSim. (No input files needed, all required data is created in-code.) 

## Exploring ATAP functionality without MATSim

The package `se.vti.atap.minimalframework` is meant for lightweight standalone experimentation with the algorithm. It depends neither on MATSim nor on any other code in this repository, meaning that it can be used by copy&paste into any other java project. There are two ways of using this package.

At the top-level of `se.vti.atap.minimalframework`, there are only interfaces and a single `Runner.java` class. The interfaces correspond to the terminology introduced in Flötteröd (2025). The `Runner.java` combines these interfaces in an ATAP assignment logic. This functions as a blueprint; to evaluate the model, one needs to specify a concrete agent representation, (dynamic) network loading, etc.

The top-level classes of `se.vti.atap.minimalframework.defaults` provides limited default implementations. The `se.vti.atap.minimalframework.defaults.planselection` package provides default implementations of all plan selection algorithms exlored in Flötteröd (2025). 

The package `se.vti.atap.minimalframework.defaults.planselection.proposed` contains an implementation of the "proposed method" of that article. The class `ProposedMethodWithLocalSearchPlanSelection.java` implements the prpoosed plan selection logic relying on (implementations of) the other interfaces and abstract classes in that package. The class  `AbstractApproximateNetworkConditions.java` is slightly more involved than a naive implementation such that it does not constitute a major computational bottleneck (it is evaluated many times).

Ready-to run examples are proided by the `ExampleRunner.java` class in the package `se.vti.atap.minimalframework.examples.parallel_links`. This package contains a complete instantiation of the "minimal framework", for both an "agent-based" and an "OD-flow-based" assignment problem.


