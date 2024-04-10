/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package od2roundtrips.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class MultiRoundTripWithOD<L, R extends RoundTrip<L>> extends MultiRoundTrip<R> {

	private final Map<Tuple<L, L>, Integer> odMatrix = new LinkedHashMap<>();

	private int singleTripCnt = 0;

	private Double error = null;
	
	public MultiRoundTripWithOD(int size) {
		super(size);
	}
	
	public void setODReproductionError(double error) {
		this.error = error;
	}
	
	public Double getODReproductionError() {
		return this.error;
	}

	@Override
	public void setRoundTrip(int i, R newRoundTrip) {
		this.setRoundTrip(i, newRoundTrip, true);
	}

	private void setRoundTrip(int i, R newRoundTrip, boolean updateOd) {

		if (updateOd) {

			if ((newRoundTrip != null) && (newRoundTrip.locationCnt() > 1)) {
				this.singleTripCnt += newRoundTrip.locationCnt();
				for (int l = 0; l < newRoundTrip.locationCnt(); l++) {
					Tuple<L, L> od = new Tuple<>(newRoundTrip.getLocation(l), newRoundTrip.getSuccessorLocation(l));
					this.odMatrix.compute(od, (od2, v) -> v == null ? 1 : v + 1);
				}
			}

			final R oldRoundTrip = this.getRoundTrip(i);
			if ((oldRoundTrip != null) && (oldRoundTrip.locationCnt() > 1)) {
				this.singleTripCnt -= oldRoundTrip.locationCnt();
				for (int l = 0; l < oldRoundTrip.locationCnt(); l++) {
					Tuple<L, L> od = new Tuple<>(oldRoundTrip.getLocation(l), oldRoundTrip.getSuccessorLocation(l));
					int oldVal = this.odMatrix.get(od);
					if (oldVal > 1) {
						this.odMatrix.put(od, oldVal - 1);
					} else {
						this.odMatrix.remove(od);
					}
				}
			}
		}

		super.setRoundTrip(i, newRoundTrip);
	}

	public Map<Tuple<L, L>, Integer> getODView() {
		return Collections.unmodifiableMap(this.odMatrix);
	}

	public int getSingleTripCnt() {
		return this.singleTripCnt;
	}

	@Override
	public MultiRoundTripWithOD<L, R> clone() {
		MultiRoundTripWithOD<L, R> result = new MultiRoundTripWithOD<L, R>(this.size());
		for (int i = 0; i < this.size(); i++) {
			result.setRoundTrip(i, (R) this.getRoundTrip(i).clone(), false);
		}
		result.odMatrix.putAll(this.odMatrix);
		result.singleTripCnt = this.singleTripCnt;
		result.error = this.error;
		return result;
	}

}
