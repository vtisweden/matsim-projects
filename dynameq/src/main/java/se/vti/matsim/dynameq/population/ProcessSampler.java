/**
 * se.vti.matsim.dynameq
 * 
 * Copyright (C) 2023 by Filip Kristofferson (VTI) and Gunnar Flötteröd (VTI, LiU).
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
package se.vti.matsim.dynameq.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.PoissonDistribution;

/**
 * Linearly interpolates between (time, rate) tuples where rates represent
 * number of events (e.g. vehicle departures) per unit time. Draws from (a
 * Poisson process with trend given by) the linearly interpolated rate function
 * and returns a List of event times.
 *
 * @author Gunnar Flötteröd
 */

public class ProcessSampler {

	private static final double EPS = 1e-8;

	private final List<Double> times = new ArrayList<>();
	private final List<Double> rates = new ArrayList<>();

	private final Random rnd;

	/**
	 * Main constructor.
	 * 
	 * @param rnd - random generator
	 */
	public ProcessSampler(final Random rnd) {
		this.rnd = rnd;
	}

	/**
	 * This method adds a an event rate for a specific time.
	 * 
	 * @param time - the time when the event rate is valid
	 * @param rate - the event rate
	 */
	public void add(double time, double rate) {
		if ((!this.times.isEmpty()) && (time < this.times.get(this.times.size() - 1) + EPS)) {
			throw new IllegalArgumentException("Subsequent times must be at least " + EPS + " units apart.");
		}
		if (rate < 0.0) {
			throw new IllegalArgumentException("Rates must be nonnegative.");
		}
		this.times.add(time);
		this.rates.add(rate);
	}

	/**
	 * This method gets a list of event times based on the event rates.
	 * 
	 * @return list with event times
	 */
	public List<Double> sample() {
		List<Double> result = new ArrayList<>();
		final double maxRate = this.rates.stream().mapToDouble(r -> r).max().getAsDouble();

		if (maxRate > EPS) {
			final double t0 = this.times.get(0);
			final double te = this.times.get(this.times.size() - 1) - EPS;

			// Generates a list of proposed event times.
			final List<Double> proposalTimes = generateProposalTimes(maxRate, te, t0);

			// Draws a set of event times from the list of proposed times.
			int i = 0;
			for (double proposalTime : proposalTimes) {
				while (!(this.times.get(i) <= proposalTime && this.times.get(i + 1) > proposalTime)) {
					i++;
				}
				final double slope = (this.rates.get(i + 1) - this.rates.get(i))
						/ (this.times.get(i + 1) - this.times.get(i));
				final double rate = this.rates.get(i) + slope * (proposalTime - this.times.get(i));
				if (this.rnd.nextDouble() < rate / maxRate) {
					result.add(proposalTime);
				}
			}

		}

		// Returns the list of event times.
		return result;
	}

	private List<Double> generateProposalTimes(double maxRate, double te, double t0) {

		PoissonDistribution poissonDistr = new PoissonDistribution(maxRate * (te - t0));
		poissonDistr.reseedRandomGenerator(this.rnd.nextLong());
		List<Double> proposalTimes = this.rnd.doubles(poissonDistr.sample()).map(u -> t0 + u * (te - t0)).boxed()
				.collect(Collectors.toList());
		Collections.sort(proposalTimes);

		return proposalTimes;
	}

	public static void main(String[] args) {
		ProcessSampler ps = new ProcessSampler(new Random());
		ps.add(0, 5.0); // at 00:00, there depart on average 5 vehicles per time unit (here, hour)
		ps.add(6, 40.0); // at 06:00, there depart on average 40 vehicles
		ps.add(7, 50.0); // ...
		ps.add(8, 30.0);
		ps.add(13, 10.0);
		ps.add(15, 40.0);
		ps.add(17, 20.0);
		ps.add(22, 10.0);
		ps.add(24, 5.0); // at 24:00, the departure rate is again the same as at 00:00 (wrap-around)
		int cnt = 0;
		for (Double time : ps.sample()) {
			System.out.println(time + "\t" + (++cnt));
		}
		System.out.println(ps.times.get(ps.times.size() - 1) + "\t" + cnt);
	}
}
