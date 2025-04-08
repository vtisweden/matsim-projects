/**
 * org.matsim.contrib.greedo
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

/**
 * 
 * @author GunnarF
 *
 */
class GapAnalyzer {

	// -------------------- CONSTANTS --------------------

	final int percentileStep; // Between zero and one hundred.

	// -------------------- MEMBERS --------------------

	private Map<Id<Person>, Double> person2score = null;

	private Double minScore = null;
	private Double meanScore = null;
	private Double maxScore = null;

	private Double meanAbsoluteGap = null;
	private List<Double> absoluteGapPercentiles = null;

//	private Double meanRelativeGap = null;
//	private List<Double> relativeGapPercentiles = null;

	// -------------------- CONSTRUCTION --------------------

	GapAnalyzer(int percentileStep) {
		if (100 % percentileStep != 0) {
			throw new RuntimeException("100 must be an integer multiple of percentileStep.");
		}
		this.percentileStep = percentileStep;
	}

	// -------------------- INTERNALS --------------------

	private List<Double> generatePercentiles(List<Id<Person>> sortedPersonIds, Map<Id<Person>, Double> person2value) {
		final List<Double> result = new ArrayList<>(100 / this.percentileStep + 1);
		for (int percentile = 0; percentile <= 100; percentile += this.percentileStep) {
			final double fractionalIndex = (percentile / 100.0) * (sortedPersonIds.size() - 1.0);
			final int lowerIndex = (int) fractionalIndex;
			if ((lowerIndex < sortedPersonIds.size() - 1) && (fractionalIndex - lowerIndex >= 1e-8)) {
				final double higherWeight = fractionalIndex - lowerIndex;
				result.add((1.0 - higherWeight) * person2value.get(sortedPersonIds.get(lowerIndex))
						+ higherWeight * person2value.get(sortedPersonIds.get(lowerIndex + 1)));
			} else {
				result.add(person2value.get(sortedPersonIds.get(lowerIndex)));
			}
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	static String createPercentileHeader(int percentileStep, Function<Integer, String> percentileToColumnHeader) {
		final StringBuffer result = new StringBuffer();
		for (int percentile = 0; percentile <= 100; percentile += percentileStep) {
			result.append(percentileToColumnHeader.apply(percentile));
			if (percentile < 100) {
				result.append("\t");
			}
		}
		return result.toString();
	}

	void registerPlansBeforeReplanning(final Population population) {
		this.person2score = population.getPersons().values().stream()
				.collect(Collectors.toMap(p -> p.getId(), p -> p.getSelectedPlan().getScore()));
		this.minScore = this.person2score.values().stream().mapToDouble(s -> s).min().getAsDouble();
		this.meanScore = this.person2score.values().stream().mapToDouble(s -> s).average().getAsDouble();
		this.maxScore = this.person2score.values().stream().mapToDouble(s -> s).max().getAsDouble();
//		Hacks.append2file("allScores.txt", this.person2score.values().stream().mapToDouble(s -> s).sorted().boxed()
//				.map(s -> Double.toString(s)).collect(Collectors.joining("\t")) + "\n");
	}

	void registerPlansAfterReplanning(final Population population) {

		final Map<Id<Person>, Double> personId2absoluteGap = population.getPersons().values().stream()
				.collect(Collectors.toMap(p -> p.getId(),
						p -> p.getSelectedPlan().getScore() - this.person2score.get(p.getId())));
		this.meanAbsoluteGap = personId2absoluteGap.values().stream().mapToDouble(ag -> ag).average().getAsDouble();
		final List<Id<Person>> sortedPersonIds = new ArrayList<>(personId2absoluteGap.keySet().stream().toList());
		Collections.sort(sortedPersonIds, new Comparator<>() {
			@Override
			public int compare(Id<Person> personId1, Id<Person> personId2) {
				return Double.compare(personId2absoluteGap.get(personId1), personId2absoluteGap.get(personId2));
			}
		});
		this.absoluteGapPercentiles = this.generatePercentiles(sortedPersonIds, personId2absoluteGap);

//		final Map<Id<Person>, Double> personId2relativeGap = population.getPersons().values().stream()
//				.collect(Collectors.toMap(p -> p.getId(),
//						p -> (p.getSelectedPlan().getScore() - this.person2score.get(p.getId()))
//								/ Math.max(1e-8, Math.abs(this.person2score.get(p.getId())))));
//		this.meanRelativeGap = personId2relativeGap.values().stream().mapToDouble(rg -> rg).average().getAsDouble();
//		Collections.sort(sortedPersonIds, new Comparator<>() {
//			@Override
//			public int compare(Id<Person> personId1, Id<Person> personId2) {
//				return Double.compare(personId2relativeGap.get(personId1), personId2relativeGap.get(personId2));
//			}
//		});
//		this.relativeGapPercentiles = this.generatePercentiles(sortedPersonIds, personId2relativeGap);
	}

	Double getMinScore() {
		return this.minScore;
	}

	Double getMeanScore() {
		return this.meanScore;
	}

	Double getMaxScore() {
		return this.maxScore;
	}

	Double getMeanAbsoluteGap() {
		return this.meanAbsoluteGap;
	}

	String getAbsolutePercentiles() {
		if (this.absoluteGapPercentiles != null) {
			return this.absoluteGapPercentiles.stream().map(p -> Double.toString(p)).collect(Collectors.joining("\t"));
		} else {
			return createPercentileHeader(this.percentileStep, p -> "");
		}
	}

//	String getRelativePercentiles() {
//		if (this.relativeGapPercentiles != null) {
//			return this.relativeGapPercentiles.stream().map(p -> Double.toString(p)).collect(Collectors.joining("\t"));
//		} else {
//			return createPercentileHeader(this.percentileStep, p -> "");
//		}
//	}

}
