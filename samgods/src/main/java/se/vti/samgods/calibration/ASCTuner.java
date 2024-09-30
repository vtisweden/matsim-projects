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

	private final double minProba = 1e-3;

	private final double targetTotal;

	private final double eta;
	
	private final ConcurrentMap<A, Double> alternative2asc = new ConcurrentHashMap<>();

	private final Map<A, Double> alternative2target = new LinkedHashMap<>();

//	private A referenceAlternative = null;

	public ASCTuner(double targetTotal, double eta) {
		this.targetTotal = targetTotal;
		this.eta = eta;
	}

	public void setTarget(A alternative, Double target) {
//		if (this.referenceAlternative == null) {
//			this.referenceAlternative = alternative;
//		}
		this.alternative2target.put(alternative, target);
		this.alternative2asc.put(alternative, 0.0);
	}

	public void update(Function<A, Double> alternative2realized) {
		final double realizedTotal = this.alternative2target.keySet().stream()
				.mapToDouble(a -> alternative2realized.apply(a)).sum();

		final double lnT0;
		final double lnP0;
		if (realizedTotal <= this.targetTotal) {
			lnT0 = Math.log(this.minProba);
			lnP0 = Math.log(Math.max(this.minProba, this.targetTotal - realizedTotal));
		} else {
			lnT0 = Math.log(Math.max(this.minProba, realizedTotal - this.targetTotal));
			lnP0 = Math.log(this.minProba);
		}

//		final double lnT0 = this.alternative2lnTarget.get(this.referenceAlternative);
//		final double lnP0 = Math.log(Math.max(1e-3, alternative2realized.apply(this.referenceAlternative)));
//		final double lnT0 = Math.log(this.relativeTolerance * this.targetTotal);
//		final double lnP0 = Math.log(Math.max(1e-3, alternative2realized.apply(this.referenceAlternative)));

//		final Map<A, Double> alternative2DeltaASC = new LinkedHashMap<>();
		for (Map.Entry<A, Double> e : this.alternative2target.entrySet()) {
			final A alternative = e.getKey();
			final double lnT = Math.log(Math.max(this.minProba, e.getValue()));
			final double lnP = Math.log(Math.max(this.minProba, alternative2realized.apply(alternative)));
			final double deltaASC = (lnT - lnP) - (lnT0 - lnP0);
			this.alternative2asc.compute(alternative, (alt, asc) -> asc + this.eta * deltaASC);
//			alternative2DeltaASC.put(alternative, deltaASC);
		}
		
//		final double maxDeltaASC = alternative2DeltaASC.values().stream().mapToDouble(asc -> Math.abs(asc)).max()
//				.getAsDouble();
//		final double sumDeltaASC = alternative2DeltaASC.values().stream().mapToDouble(asc -> asc).max()
//				.getAsDouble();
//		if (maxDeltaASC - minDeltaASC > Math.log(2)) {
//			final double fact = Math.log(2) / maxDeltaASC;
//			alternative2DeltaASC.entrySet().stream().forEach(e -> e.setValue(fact * e.getValue()));
//		}
//		alternative2DeltaASC.entrySet().stream()
//				.forEach(e -> this.alternative2asc.compute(e.getKey(), (alt, asc) -> asc + e.getValue()));

//		final double maxASC = this.alternative2asc.values().stream().mapToDouble(asc -> asc).max().getAsDouble();
//		this.alternative2asc.entrySet().stream().forEach(e -> e.setValue(e.getValue() - maxASC));
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
