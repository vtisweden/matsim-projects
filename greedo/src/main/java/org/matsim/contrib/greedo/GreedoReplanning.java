/**
 * org.matsim.contrib.emulation
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package org.matsim.contrib.greedo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emulation.EmulationEngine;
import org.matsim.contrib.greedo.shouldbeelsewhere.Hacks;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * @author Gunnar Flötteröd
 */
@Singleton
public final class GreedoReplanning implements PlansReplanning, ReplanningListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(GreedoReplanning.class);

	private final GreedoConfigGroup greedoConfig;

	private final Provider<EmulationEngine> emulationEngineProvider;

	private final MatsimServices services;

	private final StatisticsWriter<GreedoReplanning> statsWriter;

	private final AbstractReplannerSelector replannerSelector;

	private final Set<Id<Person>> personIds;

	private final Collection<? extends Person> persons;

	private final EmulationErrorAnalyzer emulationErrorAnalyzer;

	// -------------------- MEMBERS --------------------

	private Integer replanIteration = null;

	private Double gap = null;

	private final LinkedList<Map<String, LinkTravelTimeCopy>> listOfMode2travelTimes = new LinkedList<>();

	// TESTING

//	private SimplestEndogeneousReplanningRate simpleLambda;
//	private final RelativeAmbitionLevel ral = new RelativeAmbitionLevel();
//	private final TransformedObjFctEstimator objFctEst = new TransformedObjFctEstimator(0.95);

	private AmbitionGapSchedule ags = new AmbitionGapSchedule(10, -0.5, false);
//	private ThreeBlockStepSize stepSize = new ThreeBlockStepSize(1);
//	private TwoBlockStepSize twoBlockStepSize = new TwoBlockStepSize();	
//	private NagurneyStepSize nagurney = new NagurneyStepSize();

	private Double previousGapSum = null;
	private Double previousAnticipatedReductionSum = null;
//	private Double previousFilteredGapSum = null;
//	private Double previousAnticipatedFilteredReductionSum = null;
	private Double previousGeneralizedDistance = null;
	private Double previousOverrideEta = null;

	private Double previousMoverGapSum = null;

	private Set<Id<Person>> previousReplannerIds = null;

	private Double overrideEta = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	GreedoReplanning(final Provider<EmulationEngine> emulationEngineProvider, final MatsimServices services) {

		Hacks.append2file("regression.log", "deltaPsi\tPsi(lambda)\teta\tPsi\tt(lambda)\n");

		this.greedoConfig = ConfigUtils.addOrGetModule(services.getConfig(), GreedoConfigGroup.class);
		this.emulationEngineProvider = emulationEngineProvider;
		this.services = services;

		this.personIds = services.getScenario().getPopulation().getPersons().keySet();
		this.persons = services.getScenario().getPopulation().getPersons().values();

//		this.simpleLambda = new SimplestEndogeneousReplanningRate(0.95, this.personIds.size());

		this.replannerSelector = AbstractReplannerSelector.newReplannerSelector(this.greedoConfig);

		this.emulationErrorAnalyzer = new EmulationErrorAnalyzer();

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "GreedoReplanning.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanIteration";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.replanIteration.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MemorizedTravelTimes";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Integer.toString(data.listOfMode2travelTimes.size());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "TargetReplanningRate";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.replannerSelector.getTargetReplanningRate());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "RealizedReplanningRate";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.replannerSelector.getRealizedReplanningRate());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Gap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.gap.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MeanFilteredGap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.replannerSelector.getMeanFilteredGap());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MeanReplannerFilteredGap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.replannerSelector.getMeanReplannerFilteredGap());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "PopMeanEmulationError";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.emulationErrorAnalyzer.getMeanError());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "PopMeanAbsEmulationError";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.emulationErrorAnalyzer.getMeanAbsError());
			}
		});

		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "previousAnticipatedGapReductionSum";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.previousAnticipatedReductionSum);
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "previousGapSum";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.previousGapSum);
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "previousGeneralizedDistance";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.previousGeneralizedDistance);
			}
		});
//		this.statsWriter.addSearchStatistic(new Statistic<>() {
//			@Override
//			public String label() {
//				return "moverGap";
//			}
//
//			@Override
//			public String value(GreedoReplanning data) {
//				return Statistic.toString(data.nagurney.getMoverGap());
//			}
//		});
//		this.statsWriter.addSearchStatistic(new Statistic<>() {
//			@Override
//			public String label() {
//				return "avgMoverGap";
//			}
//
//			@Override
//			public String value(GreedoReplanning data) {
//				return Statistic.toString(data.nagurney.getAvgMoverGap());
//			}
//		});
//
//		this.statsWriter.addSearchStatistic(new Statistic<>() {
//			@Override
//			public String label() {
//				return "stayerGap";
//			}
//
//			@Override
//			public String value(GreedoReplanning data) {
//				return Statistic.toString(data.nagurney.getStayerGap());
//			}
//		});
//		this.statsWriter.addSearchStatistic(new Statistic<>() {
//			@Override
//			public String label() {
//				return "avgStayerGap";
//			}
//
//			@Override
//			public String value(GreedoReplanning data) {
//				return Statistic.toString(data.nagurney.getAvgStayerGap());
//			}
//		});
//
//		this.statsWriter.addSearchStatistic(new Statistic<>() {
//			@Override
//			public String label() {
//				return "lambda";
//			}
//
//			@Override
//			public String value(GreedoReplanning data) {
//				return Statistic.toString(data.nagurney.getLambda());
//			}
//		});
//		this.statsWriter.addSearchStatistic(new Statistic<>() {
//			@Override
//			public String label() {
//				return "avgLambda";
//			}
//
//			@Override
//			public String value(GreedoReplanning data) {
//				return Statistic.toString(data.nagurney.getAvgLambda());
//			}
//		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "rawEta";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Statistic.toString(data.ags.getEta(data.replanIteration, false));
			}
		});
	}

	// -------------------- INTERNALS --------------------

	private void emulate(final MatsimServices services, final Map<String, ? extends TravelTime> mode2travelTimes,
			EventHandler eventHandler, boolean overwritePlanTimes) {
		final EmulationEngine emulationEngine = this.emulationEngineProvider.get();
		emulationEngine.setOverwriteTravelTimes(overwritePlanTimes);
		emulationEngine.emulate(services.getIterationNumber(), mode2travelTimes, eventHandler);
	}

	private void emulateAgainstAllTravelTimes(final List<Map<Id<Person>, Double>> personId2scorePerReplication,
			final EventHandler eventsHandlerForMostRecentTT, final boolean overwritePlanTimesFromMostRecentTT,
			final List<Map<String, LinkTravelTimeCopy>> mode2travelTimesForEmulation) {

		for (Map<String, LinkTravelTimeCopy> mode2travelTimes : mode2travelTimesForEmulation) {

			// Important! Iteration order is such that most recent travel times are
			// processed first. This has implications for overriding (right) plan times.
			final boolean mostRecentTravelTimes = (mode2travelTimes == mode2travelTimesForEmulation.get(0));

			this.emulate(this.services, mode2travelTimes, mostRecentTravelTimes ? eventsHandlerForMostRecentTT : null,
					mostRecentTravelTimes && overwritePlanTimesFromMostRecentTT);

			final Map<Id<Person>, Double> scores = new LinkedHashMap<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				scores.put(person.getId(), person.getSelectedPlan().getScore());
			}
			personId2scorePerReplication.add(scores);
		}
	}

	private Map<String, LinkTravelTimeCopy> newFilteredTravelTimes() {
		final Map<String, LinkTravelTimeCopy> result = new LinkedHashMap<>();

		for (String mode : this.listOfMode2travelTimes.getFirst().keySet()) {
			final List<LinkTravelTimeCopy> travelTimes = new ArrayList<>(this.listOfMode2travelTimes.size());
			final List<Double> weights = new ArrayList<>(this.listOfMode2travelTimes.size());
			for (Map<String, LinkTravelTimeCopy> mode2travelTime : this.listOfMode2travelTimes) {
				travelTimes.add(mode2travelTime.get(mode));
				weights.add(1.0 / this.listOfMode2travelTimes.size());
			}
			result.put(mode, LinkTravelTimeCopy.newWeightedSum(travelTimes, weights));
		}

		return result;
	}

	// -------------------- AFTER MOBSIM LISTENER --------------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {

		while (this.listOfMode2travelTimes.size() >= this.greedoConfig.getMaxMemory()) {
			this.listOfMode2travelTimes.removeLast();
		}

		final Map<String, TravelTime> realizedMode2travelTime = event.getServices().getInjector()
				.getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() {
				}));

		final Map<String, LinkTravelTimeCopy> newMode2travelTime = new LinkedHashMap<>(realizedMode2travelTime.size());
		for (Map.Entry<String, TravelTime> realizedEntry : realizedMode2travelTime.entrySet()) {
			newMode2travelTime.put(realizedEntry.getKey(), new LinkTravelTimeCopy(realizedEntry.getValue(),
					event.getServices().getConfig(), event.getServices().getScenario().getNetwork()));
		}
		this.listOfMode2travelTimes.addFirst(newMode2travelTime);

	}

	// -------------------- REPLANNING LISTENER --------------------

	@Override
	public void notifyReplanning(final ReplanningEvent event) {

		if (this.replanIteration == null) {
			this.replanIteration = 0;
		} else {
			this.replanIteration++;
		}

		/*
		 * (0) Extract (filtered) travel times for different purposes: population
		 * distance, replanning, emulation.
		 */

		final Map<String, LinkTravelTimeCopy> mode2filteredTravelTimes;
		if (this.listOfMode2travelTimes.size() == 1) {
			mode2filteredTravelTimes = this.listOfMode2travelTimes.getFirst();
		} else {
			// TODO speed up with a recursive formulation
			mode2filteredTravelTimes = this.newFilteredTravelTimes();
		}

		final Map<String, LinkTravelTimeCopy> mode2travelTimesForReplanning;
		if (this.greedoConfig.getUseFilteredTravelTimesForReplanning()) {
			mode2travelTimesForReplanning = mode2filteredTravelTimes;
		} else {
			mode2travelTimesForReplanning = this.listOfMode2travelTimes.getFirst();
		}

		final List<Map<String, LinkTravelTimeCopy>> mode2travelTimesForEmulation;
		if (this.greedoConfig.getUseFilteredTravelTimesForEmulation()) {
			mode2travelTimesForEmulation = Collections.singletonList(mode2filteredTravelTimes);
		} else {
			mode2travelTimesForEmulation = this.listOfMode2travelTimes;
		}

		/*
		 * (1) Extract old plans and compute new plans. Evaluate both old and new plans.
		 */

		EmulationEngine.ensureOnePlanPerPersonInScenario(this.services.getScenario(), false);

		this.emulationErrorAnalyzer.setSimulatedScores(this.services.getScenario().getPopulation());
		final EventsChecker emulatedEventsChecker;
		if (this.greedoConfig.getCheckEmulatedAgentsCnt() > 0) {
			emulatedEventsChecker = new EventsChecker("observedPersons.txt", false);
		} else {
			emulatedEventsChecker = null;
		}

		final List<Map<Id<Person>, Double>> personId2oldScoreOverReplications = new ArrayList<>(
				mode2travelTimesForEmulation.size());
		this.emulateAgainstAllTravelTimes(personId2oldScoreOverReplications, emulatedEventsChecker, true,
				mode2travelTimesForEmulation);
		Plans oldPlans = new Plans(this.services.getScenario().getPopulation());

		this.emulationErrorAnalyzer.setEmulatedScores(personId2oldScoreOverReplications.get(0));
		if (emulatedEventsChecker != null) {
			emulatedEventsChecker.writeReport("emulatedEventsReport." + (event.getIteration() - 1) + ".txt");
		}

		final List<Map<Id<Person>, Double>> personId2newScoreOverReplications = new ArrayList<>(
				mode2travelTimesForEmulation.size());
		final EmulationEngine replanningEngine = this.emulationEngineProvider.get();
		replanningEngine.setOverwriteTravelTimes(true);
		replanningEngine.replan(event.getIteration(), mode2travelTimesForReplanning);
		this.emulateAgainstAllTravelTimes(personId2newScoreOverReplications, null, true, mode2travelTimesForEmulation);
		final Plans newPlans = new Plans(event.getServices().getScenario().getPopulation());

		/*
		 * (2) Compute intermediate statistics.
		 */

//		if (this.previousReplannerIds != null) {
//			final double previousLambda = ((double) this.previousReplannerIds.size()) / this.personIds.size();
//			final double moverGapSum = personId2newScoreOverReplications.get(0).entrySet().stream()
//					.filter(e -> this.previousReplannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum()
//					- personId2oldScoreOverReplications.get(0).entrySet().stream()
//							.filter(e -> this.previousReplannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue())
//							.sum();
//			final double stayerGapSum = personId2newScoreOverReplications.get(0).entrySet().stream()
//					.filter(e -> !this.previousReplannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum()
//					- personId2oldScoreOverReplications.get(0).entrySet().stream()
//							.filter(e -> !this.previousReplannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue())
//							.sum();
//			this.simpleLambda.update(previousLambda, moverGapSum, stayerGapSum);
//		}

		this.gap = personId2newScoreOverReplications.get(0).values().stream().mapToDouble(s -> s).average()
				.getAsDouble()
				- personId2oldScoreOverReplications.get(0).values().stream().mapToDouble(s -> s).average()
						.getAsDouble();
		
		this.ags.registerGap(this.gap);

		final int lagCnt = personId2newScoreOverReplications.size();
		final double lagWeight = 1.0 / lagCnt;

		final Map<Id<Person>, Double> personId2FilteredGap = new LinkedHashMap<>(this.personIds.size());
//		final Map<Id<Person>, Double> personId2filteredOldScore = new LinkedHashMap<>(this.personIds.size());
		for (Id<Person> personId : personIds) {
			double filteredGap = 0.0;
//			double filteredOldScore = 0.0;
			for (int lag = 0; lag < lagCnt; lag++) {
				filteredGap += lagWeight * (personId2newScoreOverReplications.get(lag).get(personId)
						- personId2oldScoreOverReplications.get(lag).get(personId));
//				filteredOldScore += lagWeight * personId2oldScoreOverReplications.get(lag).get(personId);
			}
			personId2FilteredGap.put(personId, filteredGap);
//			personId2filteredOldScore.put(personId, filteredOldScore);
		}

		final double gapSum = this.gap * this.personIds.size();
		final double filteredGapSum = personId2FilteredGap.values().stream().mapToDouble(g -> g).sum();

		/*
		 * (3) Identify re-planners.
		 */

//		if (this.replanIteration < 30) {
//			this.overrideEta = 1.0 / Math.sqrt(1 + this.replanIteration);
//		} else {
//			this.overrideEta += 0.01 * this.ral.getAlpha() * this.previousGeneralizedDistance;
//		}
//		double setEta = this.overrideEta * (1.0 + 0.1 * (2.0 * MatsimRandom.getRandom().nextDouble() - 1.0));
//		double setEta = this.overrideEta;
//		setEta = Math.max(0.01, Math.min(0.99, setEta));

		final AbstractPopulationDistance popDist = AbstractPopulationDistance.newPopulationDistance(oldPlans, newPlans,
				this.services.getScenario(), mode2filteredTravelTimes);
		this.replannerSelector.setDistanceToReplannedPopulation(popDist);

//		UpperBoundReplannerSelector ubSel = (UpperBoundReplannerSelector) this.replannerSelector;
//		if (ubSel.theta == null || this.previousReplannerIds == null) {
//			ubSel.theta = 0.0;
//			ubSel._Theta = 0.0;
//		} else {
//			final double innoWeight = 0.05;
//			ubSel.theta = (1.0 - innoWeight) * ubSel.theta
//					+ innoWeight * this.previousMoverGapSum / this.previousReplannerIds.size();
//			ubSel._Theta = (1.0 - innoWeight) * ubSel._Theta + innoWeight * this.previousMoverGapSum;
//		}

//		((UpperBoundReplannerSelector) this.replannerSelector).setOverrideEta(this.nagurney.getEta(true));
		((UpperBoundReplannerSelector) this.replannerSelector).setOverrideEta(this.ags.getEta(this.replanIteration, true));
		
		final Set<Id<Person>> replannerIds = this.replannerSelector.selectReplanners(personId2FilteredGap,
				this.replanIteration);

		for (Person person : this.persons) {
			if (replannerIds.contains(person.getId())) {
				newPlans.set(person);
			} else {
				oldPlans.set(person);
			}
		}

//		this.previousMoverGapSum = personId2newScoreOverReplications.get(0).entrySet().stream()
//				.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum()
//				- personId2oldScoreOverReplications.get(0).entrySet().stream()
//						.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();

//		{
//			final double moverGapSum = personId2newScoreOverReplications.get(0).entrySet().stream()
//					.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum()
//					- personId2oldScoreOverReplications.get(0).entrySet().stream()
//							.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
//			final double stayerGapSum = personId2newScoreOverReplications.get(0).entrySet().stream()
//					.filter(e -> !replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum()
//					- personId2oldScoreOverReplications.get(0).entrySet().stream()
//							.filter(e -> !replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
//			this.nagurney.update(moverGapSum, stayerGapSum, this.replannerSelector.getRealizedReplanningRate());
//		}
		
//		this.stepSize.update(this.gap);

		this.previousReplannerIds = replannerIds;

//		if (this.previousGeneralizedDistance != null) {
//			Hacks.append2file("regression.log",
//					(gapSum - this.previousGapSum) + "\t" + this.previousAnticipatedReductionSum + "\t"
//							+ this.previousOverrideEta + "\t" + this.previousGapSum + "\t"
//							+ this.previousGeneralizedDistance + "\n");
//		}
//
//		if (this.previousGeneralizedDistance != null) { // set not before the *2nd* iteration
//			this.ral.update(gapSum, this.previousGapSum, this.previousAnticipatedReductionSum, this.previousGapSum,
//					this.previousGeneralizedDistance, this.previousOverrideEta);
//		}

		/*
		 * (4) Postprocess.
		 */

//		this.previousFilteredGapSum = filteredGapSum;
//		this.previousAnticipatedFilteredReductionSum = this.replannerSelector.getMeanReplannerFilteredGap()
//				* replannerIds.size();

		// TODO attention relies on overrideEta being a number!
		this.previousOverrideEta = ((UpperBoundReplannerSelector) this.replannerSelector).getOverrideEta();

		this.previousGapSum = gapSum;
		this.previousAnticipatedReductionSum = personId2newScoreOverReplications.get(0).entrySet().stream()
				.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum()
				- personId2oldScoreOverReplications.get(0).entrySet().stream()
						.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
		this.previousGeneralizedDistance = this.replannerSelector.getMoveSize();

		this.emulationErrorAnalyzer.update(this.services.getScenario().getPopulation());
		this.statsWriter.writeToFile(this);
	}

}
