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

import org.apache.log4j.Logger;

import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;

class TransformedObjFctEstimator {

	private final double eps = 1e-6;
	
	private final Regression regr;

	TransformedObjFctEstimator(final double inertia) {
		this.regr = new Regression(inertia, 2);
	}

	void update(double _Q, double eta) {
		
		if (_Q < this.eps) {
			Logger.getLogger(this.getClass()).warn("Q = " + _Q);
			return;
		}
		
		eta = Math.max(this.eps, Math.min(1.0 - this.eps, eta));
		final Vector regrX = new Vector(Math.log(eta / (1.0 - eta)), 1.0);
		this.regr.update(regrX, Math.log(_Q / (1.0 - eta)));
	}

	double getEtaOpt() {
		return this.regr.getCoefficients().get(0);
	}
	
}
