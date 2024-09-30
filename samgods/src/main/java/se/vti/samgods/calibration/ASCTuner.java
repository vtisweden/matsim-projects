/**
 * se.vti.samgods.calibration
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
package se.vti.samgods.calibration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 
 * @author GunnarF
 *
 */
public class ASCTuner<A> {

	private final ConcurrentMap<A, Double> alternative2asc = new ConcurrentHashMap<>();

	private final Map<A, Double> alternative2lnTargetProba = new LinkedHashMap<>();

	private A referenceAlternative = null;

	public ASCTuner() {
	}

	public void setTargetProba(A alternative, Double proba) {
		if (this.referenceAlternative == null) {
			this.referenceAlternative = alternative;
		}
		this.alternative2lnTargetProba.put(alternative, Math.log(proba));
		this.alternative2asc.put(alternative, 0.0);
	}

	public void update(Function<A, Double> alternative2realized) {
		final double lnT0 = this.alternative2lnTargetProba.get(this.referenceAlternative);
		final double lnP0 = Math.log(Math.max(1e-3, alternative2realized.apply(this.referenceAlternative)));
		for (Map.Entry<A, Double> e : this.alternative2lnTargetProba.entrySet()) {
			final A alternative = e.getKey();
			final double lnT = e.getValue();
			final double lnP = Math.log(Math.max(1e-3, alternative2realized.apply(alternative)));
			final double deltaASC = (lnT - lnP) - (lnT0 - lnP0);
			this.alternative2asc.compute(alternative, (alt, asc) -> asc + deltaASC);
		}

		final double maxASC = this.alternative2asc.values().stream().mapToDouble(asc -> asc).max().getAsDouble();
		this.alternative2asc.entrySet().stream().forEach(e -> e.setValue(e.getValue() - maxASC));
	}

	public ConcurrentMap<A, Double> getAlternative2asc() {
		return this.alternative2asc;
	}

//	public Double getTarget(A alternative) {
//		if (this.alternative2lnTargetProba.containsKey(alternative)) {
//			return Math.exp(this.alternative2lnTargetProba.get(alternative));
//		} else {
//			return null;
//		}
//	}

}
