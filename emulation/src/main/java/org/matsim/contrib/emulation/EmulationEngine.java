/**
 * org.matsim.contrib.emulation
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
 * Partially based on code by Sebastian Hörl.
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
package org.matsim.contrib.emulation;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emulation.emulators.PlanEmulator;
import org.matsim.contrib.emulation.handlers.EmulationHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Carved out of IERReplanning.
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 * 
 */
public class EmulationEngine {

	// -------------------- CONSTANTS --------------------

	private final static Logger logger = Logger.getLogger(EmulationEngine.class);

	private final StrategyManager strategyManager;
	private final Scenario scenario;
	private final Provider<ReplanningContext> replanningContextProvider;
	private final Provider<PlanEmulator> planEmulatorProvider;
	private final EmulationConfigGroup ierConfig;
	private final Provider<Set<EmulationHandler>> emulationHandlerProvider;
	private final Provider<ScoringFunctionFactory> scoringFunctionFactoryProvider;

	// -------------------- MEMBERS --------------------

	private boolean overwriteTravelTimes = false;

//	private boolean sampleTravelTimePerAgent = false;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public EmulationEngine(StrategyManager strategyManager, Scenario scenario,
			Provider<ReplanningContext> replanningContextProvider, Provider<PlanEmulator> planEmulatorProvider,
			Config config, Provider<Set<EmulationHandler>> emulationHandlerProvider,
			Provider<ScoringFunctionFactory> scoringFunctionFactoryProvider) {
		this.strategyManager = strategyManager;
		this.scenario = scenario;
		this.replanningContextProvider = replanningContextProvider;
		this.planEmulatorProvider = planEmulatorProvider;
		this.ierConfig = ConfigUtils.addOrGetModule(config, EmulationConfigGroup.class);
		this.emulationHandlerProvider = emulationHandlerProvider;
		this.scoringFunctionFactoryProvider = scoringFunctionFactoryProvider;
	}

	// -------------------- SETTERS / GETTERS --------------------

	public void setOverwriteTravelTimes(final boolean overwriteTravelTimes) {
		this.overwriteTravelTimes = overwriteTravelTimes;
	}

	public boolean getOverwiteTravelTimes() {
		return this.overwriteTravelTimes;
	}

//	public void setSampleTravelTimePerAgent(final boolean sampleTravelTimePerAgent) {
//		this.sampleTravelTimePerAgent = sampleTravelTimePerAgent;
//	}
//
//	public boolean getSampleTravelTimePerAgent() {
//		return sampleTravelTimePerAgent;
//	}

	// -------------------- STATIC UTILITIES --------------------

	public static void selectBestPlans(final Population population) {
		final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
		for (Person person : population.getPersons().values()) {
			person.setSelectedPlan(bestPlanSelector.selectPlan(person));
		}
	}

	public static void removeUnselectedPlans(final Population population) {
		for (Person person : population.getPersons().values()) {
			PersonUtils.removeUnselectedPlans(person);
		}
	}

	public static void ensureOnePlanPerPersonInScenario(final Scenario scenario, final boolean selectBestPlan) {
		if (selectBestPlan) {
			selectBestPlans(scenario.getPopulation());
		}
		removeUnselectedPlans(scenario.getPopulation());
	}

	// -------------------- IMPLEMENTATION --------------------

	public static Map<Person, Map<String, ? extends TravelTime>> createPerson2mode2travelTime(
			final Collection<? extends Person> persons, Map<String, ? extends TravelTime> mode2travelTime) {
		final Map<Person, Map<String, ? extends TravelTime>> result = new ConcurrentHashMap<>(persons.size());
		for (Person person : persons) {
			result.put(person, mode2travelTime);
		}
		return result;
	}

	public static Map<Person, Integer> createPerson2ttIndex(final Collection<? extends Person> persons,
			final int indexCnt, final boolean samplePerPerson) {
		final Map<Person, Integer> result = new LinkedHashMap<>(persons.size());
		int index = MatsimRandom.getRandom().nextInt(indexCnt);
		for (Person person : persons) {
			if (samplePerPerson) {
				index = MatsimRandom.getRandom().nextInt(indexCnt);
			}
			result.put(person, index);
		}
		return result;
	}

	public static Map<Person, Map<String, ? extends TravelTime>> createPerson2mode2travelTime(
			final Map<Person, Integer> person2index, final List<Map<String, ? extends TravelTime>> allMode2travelTime) {
		final Map<Person, Map<String, ? extends TravelTime>> result = new ConcurrentHashMap<>(person2index.size());
		for (Map.Entry<Person, Integer> e : person2index.entrySet()) {
			result.put(e.getKey(), allMode2travelTime.get(e.getValue()));
		}
		return result;
	}

	public static Map<Person, Map<String, ? extends TravelTime>> createPerson2mode2travelTime(
			final Collection<? extends Person> persons,
			final List<Map<String, ? extends TravelTime>> allMode2travelTime, final boolean samplePerPerson) {
//		final Map<Person, Map<String, ? extends TravelTime>> result = new ConcurrentHashMap<>(persons.size());
//		int index = MatsimRandom.getRandom().nextInt(allMode2travelTime.size());
//		for (Person person : persons) {
//			if (samplePerPerson) {
//				index = MatsimRandom.getRandom().nextInt(allMode2travelTime.size());
//			}
//			result.put(person, allMode2travelTime.get(index));
//		}
//		return result;
		final Map<Person, Integer> person2ttIndex = createPerson2ttIndex(persons, allMode2travelTime.size(),
				samplePerPerson);
		return createPerson2mode2travelTime(person2ttIndex, allMode2travelTime);
	}

	public void replan(final int matsimIteration,
			final Map<Person, Map<String, ? extends TravelTime>> person2mode2travelTime) {

		final ReplanningContext replanningContext = this.replanningContextProvider.get();

		removeUnselectedPlans(this.scenario.getPopulation());
		this.emulate(matsimIteration, person2mode2travelTime, null);

		for (int i = 0; i < this.ierConfig.getIterationsPerCycle(); i++) {

			logger.info(
					String.format("Started replanning iteration %d/%d", i + 1, this.ierConfig.getIterationsPerCycle()));

			logger.info("[[Suppressing logging while running StrategyManager.]]");
			final Level originalLogLevel = Logger.getLogger("org.matsim").getLevel();
			Logger.getLogger("org.matsim").setLevel(Level.ERROR);

			this.strategyManager.run(this.scenario.getPopulation(), replanningContext);

			Logger.getLogger("org.matsim").setLevel(originalLogLevel);

			this.emulate(matsimIteration, person2mode2travelTime, null);
			selectBestPlans(this.scenario.getPopulation());
			removeUnselectedPlans(this.scenario.getPopulation());

			logger.info(String.format("Finished replanning iteration %d/%d", i + 1,
					this.ierConfig.getIterationsPerCycle()));
		}
	}

//	public void emulate(int iteration, 
//			final Map<Person, Map<String, ? extends TravelTime>> person2mode2travelTime,
//			EventHandler eventsHandler) {
//		this.emulate(this.scenario.getPopulation().getPersons().values(), iteration, 
//				person2mode2travelTime, 
//				eventsHandler);
//	}

	public void emulate(int iteration, final Map<Person, Map<String, ? extends TravelTime>> person2mode2travelTime,
			final EventHandler eventsHandler) {

		Iterator<? extends Person> personIterator = person2mode2travelTime.keySet().iterator();
		List<Thread> threads = new LinkedList<>();

		long totalNumberOfPersons = person2mode2travelTime.size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);
		AtomicBoolean finished = new AtomicBoolean(false);

		logger.info("[[Suppressing logging while emulating.]]");
		final Level originalLogLevel = Logger.getLogger("org.matsim").getLevel();
		Logger.getLogger("org.matsim").setLevel(Level.ERROR); // WARN);

		// Here we set up all the runner threads and start them
		for (int i = 0; i < this.scenario.getConfig().global().getNumberOfThreads(); i++) {
			Thread thread = new Thread(() -> {

				final PlanEmulator planEmulator;
				synchronized (this.planEmulatorProvider) {
					planEmulator = this.planEmulatorProvider.get();
				}

				final Set<Person> personsToScore = new LinkedHashSet<>();
				Map<Id<Person>, Person> batch = new LinkedHashMap<>();

				do {
					batch.clear();

					// Here we create our batch
					synchronized (personIterator) {
						while (personIterator.hasNext() && batch.size() < this.ierConfig.getBatchSize()) {
							final Person person = personIterator.next();
							batch.put(person.getId(), person);
							personsToScore.add(person);
						}
					}

					final EventsManager eventsManager = EventsUtils.createEventsManager();
					if (eventsHandler != null) {
						eventsManager.addHandler(eventsHandler);
					}
					eventsManager.initProcessing();

					final EventsToScore events2score;
					synchronized (this.scoringFunctionFactoryProvider) {
						events2score = EventsToScore.createWithoutScoreUpdating(this.scenario,
								this.scoringFunctionFactoryProvider.get(), eventsManager);
					}
					events2score.beginIteration(iteration,
							this.scenario.getConfig().controler().getLastIteration() == iteration);

					// Emulate batch.
					for (Person person : batch.values()) {
						planEmulator.emulate(person, person.getSelectedPlan(),
								// mode2travelTime,
								person2mode2travelTime.get(person), eventsHandler, this.emulationHandlerProvider,
								iteration, eventsManager, events2score, this.overwriteTravelTimes);
					}

					events2score.finish();
					for (Person person : batch.values()) {
						person.getSelectedPlan().setScore(events2score.getAgentScore(person.getId()));
					}

					processedNumberOfPersons.addAndGet(batch.size());
				} while (batch.size() > 0);
			});

			threads.add(thread);
			thread.start();
		}

		// We want one additional thread to track progress and output some information
		Thread progressThread = new Thread(() -> {
			long currentProcessedNumberOfPersons = 0;
			long lastProcessedNumberOfPersons = -1;

			while (!finished.get()) {
				try {
					currentProcessedNumberOfPersons = processedNumberOfPersons.get();

					if (currentProcessedNumberOfPersons > lastProcessedNumberOfPersons) {
						logger.info(String.format("Emulating... %d / %d (%.2f%%)", currentProcessedNumberOfPersons,
								totalNumberOfPersons, 100.0 * currentProcessedNumberOfPersons / totalNumberOfPersons));
					}

					lastProcessedNumberOfPersons = currentProcessedNumberOfPersons;

					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});

		progressThread.start();

		try {

			// Wait for all the runners to finish
			for (Thread thread : threads) {
				thread.join();
			}

			// Wait for the progress thread to finish
			finished.set(true);
			progressThread.join();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		logger.info("Emulation finished.");

		Logger.getLogger("org.matsim").setLevel(originalLogLevel);

	}
}
