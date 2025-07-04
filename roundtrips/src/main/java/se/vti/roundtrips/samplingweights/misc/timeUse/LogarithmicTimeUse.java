/**
 * se.vti.roundtrips.samplingweights.misc
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
package se.vti.roundtrips.samplingweights.misc.timeUse;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class LogarithmicTimeUse<N extends Node> {

	public static class Component {

		private final double targetDuration_h;
		private final double period_h;

		private List<Tuple<Double, Double>> openInterval_h;
		private double minEnBlockDurationAtLeastOnce_h;
		private double minEnBlockDurationEachTime_h;

		private boolean valid = false;
		private double effectiveDurationSum_h = 0;

		public Component(double targetDuration_h, double period_h) {
			this.targetDuration_h = targetDuration_h;
			this.period_h = period_h;
			this.openInterval_h = Arrays.asList(new Tuple<>(0.0, period_h));
			this.minEnBlockDurationAtLeastOnce_h = 0.0;
			this.minEnBlockDurationEachTime_h = 0.0;
		}

		public Component setOpeningTimes_h(double start_h, double end_h) {
			if (start_h < end_h) {
				this.openInterval_h = Arrays.asList(new Tuple<>(start_h, end_h));
			} else {
				// wraparound
				this.openInterval_h = Arrays.asList(new Tuple<>(0.0, end_h), new Tuple<>(start_h, this.period_h));				
			}
			return this;
		}

		public Component setMinEnBlockDurationAtLeastOnce_h(double dur_h) {
			this.minEnBlockDurationAtLeastOnce_h = dur_h;
			return this;
		}

		public Component setMinEnBlockDurationEachTime_h(double dur_h) {
			this.minEnBlockDurationEachTime_h = dur_h;
			return this;
		}

		private void resetEffectiveDuration_h() {
			this.valid = false;
			this.effectiveDurationSum_h = 0;
		}

		private void update(StayEpisode<?> stay) {
			double effectiveDuration_h = stay.overlap_h(this.openInterval_h, this.period_h);
			if (effectiveDuration_h >= this.minEnBlockDurationEachTime_h) {
				this.effectiveDurationSum_h += effectiveDuration_h;
				this.valid = this.valid || (effectiveDuration_h >= this.minEnBlockDurationAtLeastOnce_h);
			}
		}

		public double getEffectiveDuration_h() {
			return this.valid ? this.effectiveDurationSum_h : 0.0;
		}

	}

	private final double minDur_h = 0.001;

	private final Map<Tuple<N, Integer>, Component> nodeAndIndex2component = new LinkedHashMap<>();

	private final Set<Component> components = new LinkedHashSet<>();

	LogarithmicTimeUse() {
	}

	void assignComponent(Component component, N node, int index) {
		this.nodeAndIndex2component.put(new Tuple<>(node, index), component);
		this.components.add(component);
	}

	public void update(Iterable<RoundTrip<N>> roundTrips) {
		for (var component : this.components) {
			component.resetEffectiveDuration_h();
		}
		for (RoundTrip<N> roundTrip : roundTrips) {
			List<Episode> episodes = roundTrip.getEpisodes();
			for (int i = 0; i < episodes.size(); i += 2) {
				StayEpisode<?> stay = (StayEpisode<?>) episodes.get(i);
				Component component = this.nodeAndIndex2component
						.get(new Tuple<>(stay.getLocation(), roundTrip.getIndex()));
				if (component != null) {
					component.update(stay);
				}
			}
		}
	}

	double computeLogWeight(Iterable<RoundTrip<N>> roundTrips) {
		this.update(roundTrips);
		double result = 0.0;
		for (var component : this.components) {
			result += component.targetDuration_h
					* Math.log(Math.max(this.minDur_h, component.getEffectiveDuration_h()));
		}
		return result;
	}
}
