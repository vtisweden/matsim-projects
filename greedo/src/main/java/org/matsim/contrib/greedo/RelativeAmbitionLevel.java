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

import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author GunnarF
 *
 */
class RelativeAmbitionLevel {

	private final Regression regr;

	private Double referenceGap = null;
	private Double referenceDistance = null;

	RelativeAmbitionLevel(final double inertia) {
		this.regr = new Regression(inertia, 3);
	}

	void update(double currentGap, double previousGap, double previousAnticipatedReduction, double previousFilteredGap,
			double previousGeneralizedDistance) {
		if (this.referenceGap == null) {
			this.referenceGap = 0.1 * previousGap;
			this.referenceDistance = 0.1 * previousGeneralizedDistance;
		}
		currentGap /= this.referenceGap;
		previousGap /= this.referenceGap;
		previousFilteredGap /= this.referenceGap;
		previousAnticipatedReduction /= this.referenceGap;
		previousGeneralizedDistance /= this.referenceDistance;

		final Vector regrX = new Vector(previousAnticipatedReduction / previousGeneralizedDistance,
				previousFilteredGap / previousGeneralizedDistance, 1.0);
		this.regr.update(regrX, currentGap - previousGap);
	}

	double getAlpha() {
		return this.regr.getCoefficients().get(0);
	}

	double getBeta() {
		return this.regr.getCoefficients().get(1);
	}

	Double getEta() {
		if (this.referenceGap == null) {
			return null;
		} else {
			return -(this.getBeta() / this.getAlpha());
		}
	}
}
