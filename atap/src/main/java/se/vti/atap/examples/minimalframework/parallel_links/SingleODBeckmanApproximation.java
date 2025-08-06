/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.examples.minimalframework.parallel_links;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import se.vti.atap.examples.minimalframework.parallel_links.ods.ODPair;
import se.vti.atap.examples.minimalframework.parallel_links.ods.Paths;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleODBeckmanApproximation {

	private final List<Integer> _H;

	private final double[] s;
	private final double[] c;
	private final double[] g;
	private final double[] v;

	private final double[] f;

	public SingleODBeckmanApproximation(ODPair odPair, NetworkConditionsImpl initialNetworkConditions) {
		this._H = new ArrayList<>(odPair.getNumberOfPaths());
		for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
			if (odPair.getCurrentPlan().pathFlows_veh[path] > 0) {
				this._H.add(path);
			}
		}
		this.s = new double[odPair.getNumberOfPaths()];
		this.c = new double[odPair.getNumberOfPaths()];
		this.g = new double[odPair.getNumberOfPaths()];
		this.v = new double[odPair.getNumberOfPaths()];
		this.f = new double[odPair.getNumberOfPaths()];
		this.updateInternally(odPair, initialNetworkConditions);
	}

	public void updateAfterNetworkLoading(ODPair odPair, NetworkConditionsImpl networkConditions) {
		int bestPath = odPair.computeBestPath(networkConditions);
		if (!_H.contains(bestPath)) {
			this._H.add(bestPath);
		}
		this.updateInternally(odPair, networkConditions);
	}

	private void updateInternally(ODPair odPair, NetworkConditionsImpl networkConditions) {
		for (int h = 0; h < odPair.getNumberOfPaths(); h++) {
			int ij = odPair.availableLinks[h];
			this.g[h] = odPair.getCurrentPlan().pathFlows_veh[h];
			this.v[h] = networkConditions.linkTravelTimes_s[ij];
			this.s[h] = networkConditions.dLinkTravelTimes_dLinkFlows_s_veh[ij];
			this.c[h] = this.v[h] - this.s[h] * this.g[h];
		}

		Collections.sort(this._H, new Comparator<>() {
			@Override
			public int compare(Integer h1, Integer h2) {
				return Double.compare(c[h1], c[h2]);
			}
		});

		double d = odPair.demand_veh;

		List<Integer> _Hhat = new ArrayList<>(this._H.size());
		Iterator<Integer> hIterator = this._H.iterator();

		double _B = 0;
		double _C = 0;
		double w;

		int h = hIterator.next();
		do {
			_B += 1.0 / (this.s[h] * d);
			_C += this.c[h] / (this.s[h] * d);
			w = (1.0 + _C) / _B;

			_Hhat.add(h);

			if (hIterator.hasNext()) {
				h = hIterator.next();
			} else {
				h = -1;
			}
		} while ((h >= 0) && (this.c[h] < w));

		this._H.clear();
		this._H.addAll(_Hhat);

		Arrays.fill(this.f, 0.0);
		for (int h2 : _Hhat) {
			this.f[h2] = (w - this.c[h2]) / this.s[h2];
		}

		double feasible_veh = 0.0;
		for (int h2 : _Hhat) {
			if (this.f[h2] >= 0) {
				feasible_veh += this.f[h2];
			} else {
				this.f[h2] = 0.0;
			}
		}
		if (feasible_veh < 1e-8) {
			throw new RuntimeException("no feasible flow");
		}
		if (Math.abs(feasible_veh - odPair.demand_veh) > 1e-8) {
			for (int h2 : _Hhat) {
				this.f[h2] *= odPair.demand_veh / feasible_veh;
			}
		}
	}

	public double[] createBestResponsePathFlows() {
		return Arrays.copyOf(this.f, this.f.length);
	}

	public double computeBeckmanFunctionValue(Paths paths) {
		double result = 0.0;
		for (int h = 0; h < paths.getNumberOfPaths(); h++) {
			double f = paths.pathFlows_veh[h];
			if (f > 0) {
				result += (this.v[h] - this.s[h] * this.g[h]) * f + 0.5 * this.s[h] * f * f;
			}
		}
		return result;
	}

}
