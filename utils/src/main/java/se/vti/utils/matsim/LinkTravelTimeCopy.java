/**
 * se.vti.utils
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
package se.vti.utils.matsim;

import static java.lang.Math.ceil;
import static java.lang.Math.min;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.DynamicData;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkTravelTimeCopy implements TravelTime {

	@SuppressWarnings("serial")
	private static class ModifiableDynamicData<K extends Object> extends DynamicData<K> {

		ModifiableDynamicData(int startTime_s, int binSize_s, int binCnt) {
			super(startTime_s, binSize_s, binCnt);
		}

		ModifiableDynamicData(final ModifiableDynamicData<K> parent) {
			super(parent.getStartTime_s(), parent.getBinSize_s(), parent.getBinCnt());
			this.add(parent, 1.0); // ensures deep copy of double[] data holding arrays
		}

		void multiply(final double factor) {
			for (double[] series : this.data.values()) {
				for (int i = 0; i < series.length; i++) {
					series[i] *= factor;
				}
			}
		}

		void add(final ModifiableDynamicData<K> other, final double otherFactor) {
			for (Map.Entry<K, double[]> otherEntry : other.data.entrySet()) {
				final double[] otherSeries = otherEntry.getValue();
				final double[] mySeries = this.getNonNullDataArray(otherEntry.getKey());
				for (int i = 0; i < mySeries.length; i++) {
					mySeries[i] += otherFactor * otherSeries[i];
				}
			}
		}
	}
	
	private final ModifiableDynamicData<Id<Link>> data_s;

	public LinkTravelTimeCopy(final TravelTime travelTimes, final Config config, final Network network) {

		final int binSize_s = config.travelTimeCalculator().getTraveltimeBinSize();
		final int binCnt = (int) ceil(((double) config.travelTimeCalculator().getMaxTime()) / binSize_s);

		this.data_s = new ModifiableDynamicData<Id<Link>>(0, binSize_s, binCnt);

		for (Link link : network.getLinks().values()) {
			for (int bin = 0; bin < binCnt; bin++) {
				this.data_s.put(link.getId(), bin,
						travelTimes.getLinkTravelTime(link, (bin + 0.5) * binSize_s, null, null));
			}
		}
	}

	public LinkTravelTimeCopy(final LinkTravelTimeCopy parent) {
		this.data_s = new ModifiableDynamicData<>(parent.data_s);
	}

	@Override
	public synchronized double getLinkTravelTime(Link link, double time_s, Person person, Vehicle vehicle) {
		final int bin = min(this.data_s.getBinCnt() - 1, (int) (time_s / this.data_s.getBinSize_s()));
		return this.data_s.getBinValue(link.getId(), bin);
	}

	public void multiply(final double factor) {
		this.data_s.multiply(factor);
	}

	public void add(final LinkTravelTimeCopy other, final double otherFactor) {
		this.data_s.add(other.data_s, otherFactor);
	}

	public static LinkTravelTimeCopy newWeightedSum(final List<LinkTravelTimeCopy> addends,
			final List<Double> weights) {
		assert(addends.size() == weights.size());
		final LinkTravelTimeCopy result = new LinkTravelTimeCopy(addends.get(0));
		result.multiply(weights.get(0));
		for (int i = 1; i < addends.size(); i++) {
			result.add(addends.get(i), weights.get(i));
		}
		return result;
	}
}
