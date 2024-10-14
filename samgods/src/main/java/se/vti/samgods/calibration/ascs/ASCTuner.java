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
package se.vti.samgods.calibration.ascs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 
 * @author GunnarF
 *
 */
public class ASCTuner<A> {

	// -------------------- CONSTANTS --------------------

	private final double relativeSlack = 0.1;

	private final double minProba = 1e-3;

	private final Double targetTotal;

	private final double updateStepSize;

	// -------------------- MEMBERS --------------------

	private final Map<A, Double> alternative2asc = new LinkedHashMap<>();

	private final Map<A, Double> alternative2target = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ASCTuner(Double targetTotal, double updateStepSize) {
		this.targetTotal = targetTotal;
		this.updateStepSize = updateStepSize;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setTarget(A alternative, Double target) {
		this.alternative2target.put(alternative, target);
		this.alternative2asc.put(alternative, 0.0);
	}

	public Map<A, Double> getAlternative2asc() {
		return this.alternative2asc;
	}

	public void update(Function<A, Double> alternative2realized) {
		final double realizedTotal = this.alternative2target.keySet().stream()
				.mapToDouble(a -> alternative2realized.apply(a)).sum();
		final double tmpTargetTotal = (this.targetTotal == null ? realizedTotal : this.targetTotal);
		final double lnT0;
		final double lnP0;

		if (realizedTotal <= tmpTargetTotal) {
			final double targetSlack = this.relativeSlack * tmpTargetTotal;
			final double realSlack = (tmpTargetTotal + targetSlack) - realizedTotal;
			lnT0 = Math.log(Math.max(this.minProba, targetSlack));
			lnP0 = Math.log(Math.max(this.minProba, realSlack));
		} else {
			final double realSlack = this.relativeSlack * realizedTotal;
			final double targetSlack = (realizedTotal + realSlack) - tmpTargetTotal;
			lnT0 = Math.log(Math.max(this.minProba, targetSlack));
			lnP0 = Math.log(Math.max(this.minProba, realSlack));
		}

		for (Map.Entry<A, Double> e : this.alternative2target.entrySet()) {
			final A alternative = e.getKey();
			final double lnT = Math.log(Math.max(this.minProba, e.getValue()));
			final double lnP = Math.log(Math.max(this.minProba, alternative2realized.apply(alternative)));
			final double deltaASC = (lnT - lnP) - (lnT0 - lnP0);
			this.alternative2asc.compute(alternative, (alt, asc) -> asc + this.updateStepSize * deltaASC);
		}
	}
}
