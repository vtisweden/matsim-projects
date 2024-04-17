
package instances.vienna;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.Tuple;
import se.vti.od2roundtrips.model.MultiRoundTripWithOD;
import se.vti.od2roundtrips.model.OD2RoundtripsScenario;
import se.vti.od2roundtrips.model.SimpleStatsLogger;
import se.vti.od2roundtrips.model.SingleToMultiComponent;
import se.vti.od2roundtrips.model.TAZ;
import se.vti.od2roundtrips.targets.AtHomeOverNightTarget;
import se.vti.od2roundtrips.targets.AtMainActivityTarget;
import se.vti.od2roundtrips.targets.HomeLocationTarget;
import se.vti.od2roundtrips.targets.ODTarget;
import se.vti.od2roundtrips.targets.PopulationGrouping;
import se.vti.od2roundtrips.targets.TargetLogger;
import se.vti.roundtrips.model.DefaultDrivingSimulator;
import se.vti.roundtrips.model.DefaultParkingSimulator;
import se.vti.roundtrips.model.Simulator;
import se.vti.roundtrips.model.VehicleState;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.preferences.AllDayTimeConstraintPreference;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.preferences.StrategyRealizationConsistency;
import se.vti.roundtrips.preferences.UniformOverLocationCount;
import se.vti.roundtrips.single.PossibleTransitions;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripDepartureProposal;
import se.vti.roundtrips.single.RoundTripLocationProposal;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

public class ViennaMultiRunner {

	public static void main(String[] args) throws IOException {

		// Spezifiziere den Dateinamen

		String outputFileName = "output/districts_multi_output.txt";

		// Erstelle einen FileOutputStream für die Datei

		FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);

		PrintStream consolePrintStream = System.out;

		PrintStream filePrintStream = new PrintStream(fileOutputStream);

		PrintStream combinedPrintStream = new PrintStream(new TeeOutputStream(consolePrintStream, filePrintStream));

		System.setOut(combinedPrintStream);

		// Creating the scenario

		OD2RoundtripsScenario scenario = new OD2RoundtripsScenario();

		scenario.setMaxParkingEpisodes(4);

		scenario.setTimeBinCnt(24);

		// Load skim matrices and feed into scenario

		for (Map.Entry<Tuple<String, String>, Double> entry : MatrixDataReader.read("./input/districts_skim_km.csv")
				.entrySet()) {
			TAZ from = scenario.getOrCreateAndAddLocation(entry.getKey().getA());
			TAZ to = scenario.getOrCreateAndAddLocation(entry.getKey().getB());
			scenario.setDistance_km(from, to, entry.getValue());
		}

		for (Map.Entry<Tuple<String, String>, Double> entry : MatrixDataReader.read("./input/districts_skim_h.csv")
				.entrySet()) {
			TAZ from = scenario.getOrCreateAndAddLocation(entry.getKey().getA());
			TAZ to = scenario.getOrCreateAndAddLocation(entry.getKey().getB());
			scenario.setTime_h(from, to, entry.getValue());
		}

		// Create sampling preferences

		final Preferences<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> allPreferences = new Preferences<>();

		final List<TargetLogger> targetLoggers = new ArrayList<>();

		int roundTripCnt = 1000; // Change number of RT inside one set-of-RT here

		// Consistency preferences

		allPreferences.addComponent(new SingleToMultiComponent(new UniformOverLocationCount<>(scenario)));

		allPreferences.addComponent(new SingleToMultiComponent(new AllDayTimeConstraintPreference<>()));

		allPreferences.addComponent(new SingleToMultiComponent(new StrategyRealizationConsistency<>(scenario)));

		// OD reproduction preference

		ODTarget odTarget = new ODTarget();

		for (Map.Entry<Tuple<String, String>, Double> entry : MatrixDataReader.read("./input/districts_od.csv")
				.entrySet()) {
			TAZ from = scenario.getOrCreateAndAddLocation(entry.getKey().getA());
			TAZ to = scenario.getOrCreateAndAddLocation(entry.getKey().getB());
			odTarget.setODEntry(from, to, entry.getValue());
		}

		allPreferences.addComponent(odTarget, 10);
		targetLoggers.add(new TargetLogger(1000, odTarget, "odTarget.log"));

		// home location preferences and population group segmentation

		Map<String, Map<TAZ, Double>> group2home2target = new LinkedHashMap<>();
		for (Map.Entry<Tuple<String, String>, Double> entry : MatrixDataReader
				.read("./input/districts_home_locations.csv").entrySet()) {
			String group = entry.getKey().getA();
			Map<TAZ, Double> home2target = group2home2target.computeIfAbsent(group, g -> new LinkedHashMap<>());
			TAZ home = scenario.getLocation(entry.getKey().getB());
			double target = entry.getValue();
			home2target.put(home, target);
		}

		double populationSize = 0.0; // needed further below
		PopulationGrouping grouping = new PopulationGrouping(roundTripCnt);
		for (Map.Entry<String, Map<TAZ, Double>> entry : group2home2target.entrySet()) {
			double groupSize = entry.getValue().values().stream().mapToDouble(c -> c).sum();
			grouping.addGroup(entry.getKey(), groupSize);
			populationSize += groupSize;
		}

		for (Map.Entry<String, Map<TAZ, Double>> group2xEntry : group2home2target.entrySet()) {
			HomeLocationTarget target = new HomeLocationTarget();
			for (Map.Entry<TAZ, Double> home2targetEntry : group2xEntry.getValue().entrySet()) {
				target.setTarget(home2targetEntry.getKey(), home2targetEntry.getValue());
			}
			target.setFilter(grouping.createFilter(group2xEntry.getKey()));
			allPreferences.addComponent(target, 10.0);
			targetLoggers.add(new TargetLogger(1000, target, "homeTarget_" + group2xEntry.getKey() + ".log"));
		}

		// Preference for staying at home

		/*-
		 * Parameters
		 * 1. minimal duration actually spent at home
		 * 2. length of interval during which at-home is considered
		 * 3. end time of interval during which at-home is considered
		 * 
		 * So 10,12,7 means: Spend at least 10 hours at home between 19:00 and 7:00.
		 */

		AtHomeOverNightTarget atHomeOverNightTarget = new AtHomeOverNightTarget(10.0, 12.0, 7.0, populationSize);
		allPreferences.addComponent(atHomeOverNightTarget, 10.0);
		targetLoggers.add(new TargetLogger(1000, atHomeOverNightTarget, "atHomeOverNight.log"));

		// main activity location preference

		Map<String, Map<TAZ, Double>> group2main2target = new LinkedHashMap<>();
		// Replace table, this currently uses home locations as main activity locations.
		for (Map.Entry<Tuple<String, String>, Double> entry : MatrixDataReader
				.read("./input/districts_home_locations.csv").entrySet()) {
			String group = entry.getKey().getA();
			Map<TAZ, Double> main2target = group2main2target.computeIfAbsent(group, g -> new LinkedHashMap<>());
			TAZ main = scenario.getLocation(entry.getKey().getB());
			double target = entry.getValue();
			main2target.put(main, target);
		}

		for (Map.Entry<String, Map<TAZ, Double>> group2xEntry : group2main2target.entrySet()) {
			// everybody is active for 9hrs between during the 12h interval ending at 19:00.
			AtMainActivityTarget target = new AtMainActivityTarget(9.0, 12.0, 19.0);
			for (Map.Entry<TAZ, Double> main2targetEntry : group2xEntry.getValue().entrySet()) {
				target.setTarget(main2targetEntry.getKey(), main2targetEntry.getValue());
			}
			target.setFilter(grouping.createFilter(group2xEntry.getKey()));
			allPreferences.addComponent(target, 10.0);
			targetLoggers.add(new TargetLogger(1000, target, "longOutOfHomeTarget_" + group2xEntry.getKey() + ".log"));
		}

		// Default physical simulator

		Simulator<TAZ, VehicleState, RoundTrip<TAZ>> simulator = new Simulator<>(scenario, () -> new VehicleState());

		simulator.setDrivingSimulator(new DefaultDrivingSimulator<>(scenario, () -> new VehicleState()));

		simulator.setParkingSimulator(new DefaultParkingSimulator<>(scenario, () -> new VehicleState()));

		// Create MH algorithm

		double locationProposalWeight = 0.5;

		double departureProposalWeight = 0.5;

		RoundTripProposal<RoundTrip<TAZ>> proposal = new RoundTripProposal<>(simulator, scenario.getRandom());

		proposal.addProposal(new RoundTripLocationProposal<RoundTrip<TAZ>, TAZ>(scenario,

				(state, scen) -> new PossibleTransitions<TAZ>(state, scen)), locationProposalWeight);

		proposal.addProposal(new RoundTripDepartureProposal<>(scenario), departureProposalWeight);

		MultiRoundTripProposal<RoundTrip<TAZ>, MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> proposalMulti = new MultiRoundTripProposal<>(

				scenario.getRandom(),

				proposal);

		MHAlgorithm<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> algo = new MHAlgorithm<>(proposalMulti, allPreferences,

				new Random());

		algo.addStateProcessor(new SimpleStatsLogger(scenario, 1000));

		for (TargetLogger targetLogger : targetLoggers) {
			algo.addStateProcessor(targetLogger);
		}

		final MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> initialStateMulti = new MultiRoundTripWithOD<>(roundTripCnt);

		Random rnd = new Random();

		for (int i = 0; i < initialStateMulti.size(); i++) {

			RoundTrip<TAZ> initialStateSingle = null;

			do {

				TAZ loc1 = scenario.getLocationsView().get(rnd.nextInt(scenario.getLocationsView().size()));

				TAZ loc2 = scenario.getLocationsView().get(rnd.nextInt(scenario.getLocationsView().size()));

				int time1 = rnd.nextInt(scenario.getBinCnt());

				int time2 = rnd.nextInt(scenario.getBinCnt());

				if (!loc1.equals(loc2) && (time1 != time2)) {

					initialStateSingle = new RoundTrip<TAZ>(Arrays.asList(loc1, loc2),

							Arrays.asList(Math.min(time1, time2), Math.max(time1, time2)));

				}

			} while (initialStateSingle == null);

			initialStateSingle.setEpisodes(simulator.simulate(initialStateSingle));

			initialStateMulti.setRoundTrip(i, initialStateSingle);

		}

		algo.setInitialState(initialStateMulti);

		algo.setMsgInterval(1000);

		// Run MH algorithm

		algo.run(100 * 1000);

		// Close file output stream

		fileOutputStream.close();

		// Close file print stream

		combinedPrintStream.close();

		filePrintStream.close();
	}

}
