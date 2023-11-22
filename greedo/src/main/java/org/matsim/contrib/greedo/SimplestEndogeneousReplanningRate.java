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
class SimplestEndogeneousReplanningRate {

	private final Regression moverRegr;
	private final Regression stayerRegr;

	private final double populationSize;

	SimplestEndogeneousReplanningRate(final double inertia, final double populationSize) {
		this.moverRegr = new Regression(inertia, 2);
		this.stayerRegr = new Regression(inertia, 2);
		this.populationSize = populationSize;
	}

	void update(double previousLambda, double moverGapSum, double stayerGapSum) {
		previousLambda = Math.max(0.01, Math.min(previousLambda, 0.99));
		moverGapSum /= this.populationSize;
		stayerGapSum /= this.populationSize;
		final Vector x = new Vector(previousLambda, 1.0);
		this.moverRegr.update(x, moverGapSum / previousLambda);
		this.stayerRegr.update(x, stayerGapSum / (1.0 - previousLambda));
	}

	double getAMove() {
		return this.moverRegr.getCoefficients().get(0);
	}

	double getBMove() {
		return this.moverRegr.getCoefficients().get(1);
	}

	double getAStay() {
		return this.stayerRegr.getCoefficients().get(0);
	}

	double getBStay() {
		return this.stayerRegr.getCoefficients().get(1);
	}

	Double getLambda() {
		return (getBStay() - getBMove() - getAStay()) / (getAMove() - getAStay());
	}

}
