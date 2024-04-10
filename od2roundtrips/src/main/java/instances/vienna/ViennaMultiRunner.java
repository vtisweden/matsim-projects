
package instances.vienna;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.od2roundtrips.model.MultiRoundTripWithOD;
import se.vti.od2roundtrips.model.OD2RoundtripsScenario;
import se.vti.od2roundtrips.model.PopulationGrouping;
import se.vti.od2roundtrips.model.SimpleStatsLogger;
import se.vti.od2roundtrips.model.SingleToMultiComponent;
import se.vti.od2roundtrips.model.TAZ;
import se.vti.od2roundtrips.targets.HomeLocationTarget;
import se.vti.od2roundtrips.targets.ODTarget;
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

		try {

			FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);

			PrintStream consolePrintStream = System.out;

			PrintStream filePrintStream = new PrintStream(fileOutputStream);

			PrintStream combinedPrintStream = new PrintStream(new TeeOutputStream(consolePrintStream, filePrintStream));

			System.setOut(combinedPrintStream);

			// Creating the scenario

			OD2RoundtripsScenario scenario = new OD2RoundtripsScenario();

			// Reads in district names from .csv and safes to namesOfDistrics

			DistrictNamesFromCsvToList districtNamesFromCsvToList = new DistrictNamesFromCsvToList();

			List<String> namesOfDistricts = districtNamesFromCsvToList.readCSV("input/districts_skim_km.csv", 24);

			System.out.println("List namesOfDistrics: " + namesOfDistricts);

			// Create each district once; the scenario will memorize it by name

			for (int i = 0; i < namesOfDistricts.size(); i++) {

				String name = namesOfDistricts.get(i);

				scenario.createAndAddLocation(name);

			}

			// Reads in distance between districts [km] from .csv and safes to
			// distanceBetweenDistrictsKm

			ValuesFromCsvToList distanceFromCsvToList = new ValuesFromCsvToList();

			List<List<String>> distanceBetweenDistrictsKm = distanceFromCsvToList.readCSV("input/districts_skim_km.csv",
					24);

			System.out.println(distanceBetweenDistrictsKm);

			// System.out.println(distanceBetweenDistrictsKm.size());

			// Double Loop that adds distance [km] to scenario

			for (int i = 0; i < distanceBetweenDistrictsKm.size(); i++) {

				List<String> row = distanceBetweenDistrictsKm.get(i);

				// System.out.println(row.size());

				String origin = namesOfDistricts.get(i);

				System.out.println("Current row in distanceBetweenDistrictsKm: " + row);

				System.out.println();

				for (int j = 1; j < row.size(); j++) {

					String destination = namesOfDistricts.get(j - 1);

					double dist_km = Double.parseDouble(row.get(j));

					scenario.setSymmetricDistance_km(origin, destination, dist_km);

					System.out.println("Origin: " + origin);

					System.out.println("Destination: " + destination);

					System.out.println("Distance: " + dist_km);

					System.out.println();

				}

			}

			// Reads in travel-time between districts [h] from .csv and safes to
			// distanceBetweenDistrictsH

			ValuesFromCsvToList hFromCsvToList = new ValuesFromCsvToList();

			List<List<String>> distanceBetweenDistrictsH = hFromCsvToList.readCSV("input/districts_skim_h.csv", 24);

			// System.out.println(distanceBetweenDistrictsMin);

			// Double Loop that adds travel-time [h] to scenario

			for (int i = 0; i < distanceBetweenDistrictsH.size(); i++) {

				List<String> row = distanceBetweenDistrictsH.get(i);

				// System.out.println(row.size());

				String origin = namesOfDistricts.get(i);

				System.out.println("Current row in distanceBetweenDistrictsH: " + row);

				System.out.println();

				for (int j = 1; j < row.size(); j++) {

					String destination = namesOfDistricts.get(j - 1);

					double dist_h = Double.parseDouble(row.get(j));

					scenario.setSymmetricTime_h(origin, destination, dist_h);

					System.out.println("Origin: " + origin);

					System.out.println("Destination: " + destination);

					System.out.println("Travel Time: " + dist_h);

					System.out.println();

				}

			}

			scenario.setMaxParkingEpisodes(4);

			scenario.setTimeBinCnt(24);

			final Preferences<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> allPreferences = new Preferences<>();

			final List<TargetLogger> targetLoggers = new ArrayList<>();

			// Consistency preferences

			allPreferences.addComponent(new SingleToMultiComponent(new UniformOverLocationCount<>(scenario)));

			allPreferences.addComponent(new SingleToMultiComponent(new AllDayTimeConstraintPreference<>()));

			allPreferences.addComponent(new SingleToMultiComponent(new StrategyRealizationConsistency<>(scenario)));

			// Reads in number-of-trips (OD) from .csv and safes to odMatrice

			ValuesFromCsvToList numberOfTripsFromCsvToList = new ValuesFromCsvToList();

			List<List<String>> odMatrice = numberOfTripsFromCsvToList.readCSV("input/districts_od.csv", 24);

			// System.out.println(odMatrice);

			// Double Loop for ODPreference

			ODTarget odTarget = new ODTarget();

			for (int i = 0; i < odMatrice.size(); i++) {

				List<String> row = odMatrice.get(i);

				// System.out.println(row.size());

				String origin = namesOfDistricts.get(i);

				System.out.println("Current row in odMatrice: " + row);

				System.out.println();

				for (int j = 1; j < row.size(); j++) {

					String destination = namesOfDistricts.get(j - 1);

					double numberOfTrips = Double.parseDouble(row.get(j));

					odTarget.setODEntry(scenario.getLocation(origin), scenario.getLocation(destination), numberOfTrips);

					System.out.println("Origin: " + origin);

					System.out.println("Destination: " + destination);

					System.out.println("Number of Trips: " + numberOfTrips);

					System.out.println();

				}

			}

			allPreferences.addComponent(odTarget, 10);
			targetLoggers.add(new TargetLogger(1000, odTarget, "odTarget.log"));

			// parse home location file

			Map<String, Map<TAZ, Double>> group2zone2target = new LinkedHashMap<>();

			AbstractTabularFileHandlerWithHeaderLine homeHandler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startDataRow(final String[] row) {
					Map<TAZ, Double> zone2target = new LinkedHashMap<>();
					group2zone2target.put(row[0], zone2target);
					for (Map.Entry<String, Integer> e : this.label2index.entrySet()) {
						if (e.getValue() > 0 && row[e.getValue()].length() > 0) {
							zone2target.put(scenario.getLocation(e.getKey()), Double.parseDouble(row[e.getValue()]));
						}
					}
				}
			};

			TabularFileParser parser = new TabularFileParser();
			parser.setDelimiterTags(new String[] { "," });
			parser.setOmitEmptyColumns(false);
			parser.parse("./input/districts_home_locations.csv", homeHandler);

			// create population segmentation

			int roundTripCnt = 1000; // Change number of RT inside one set-of-RT here

			PopulationGrouping grouping = new PopulationGrouping(roundTripCnt);

			for (Map.Entry<String, Map<TAZ, Double>> entry : group2zone2target.entrySet()) {
				grouping.addGroup(entry.getKey(), entry.getValue().values().stream().mapToDouble(c -> c).sum());
			}

			// create home preferences

			for (Map.Entry<String, Map<TAZ, Double>> group2xEntry : group2zone2target.entrySet()) {
				HomeLocationTarget target = new HomeLocationTarget();
				for (Map.Entry<TAZ, Double> zone2targetEntry : group2xEntry.getValue().entrySet()) {
					target.setTarget(zone2targetEntry.getKey(), zone2targetEntry.getValue());
				}
				target.setFilter(grouping.createFilter(group2xEntry.getKey()));
				allPreferences.addComponent(target, 10.0);
				targetLoggers.add(new TargetLogger(1000, target, "homeTarget_" + group2xEntry.getKey() + ".log"));
			}

			// Default physical simulator

			Simulator<TAZ, VehicleState, RoundTrip<TAZ>> simulator = new Simulator<>(scenario,
					() -> new VehicleState());

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

			MHAlgorithm<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> algo = new MHAlgorithm<>(proposalMulti,
					allPreferences,

					new Random());

			algo.addStateProcessor(new SimpleStatsLogger(scenario, 1000));

			for (TargetLogger targetLogger : targetLoggers) {
				algo.addStateProcessor(targetLogger);
			}
			
			final MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> initialStateMulti = new MultiRoundTripWithOD<>(
					roundTripCnt);

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

		} catch (FileNotFoundException e) {

			e.printStackTrace();

		}

	}

}
