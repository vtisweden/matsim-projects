/**
 * se.vti.atap.examples.minimalframework.parallel_links_ods
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
package se.vti.atap.examples.minimalframework.parallel_links.ods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.minimalframework.PlanInnovation;
import se.vti.atap.minimalframework.defaults.DoubleArrayWrapper;

/**
 * 
 * @author GunnarF
 *
 */
public class GreedyInnovation implements PlanInnovation<DoubleArrayWrapper, ODPair> {

	private final Network network;

	public GreedyInnovation(Network network) {
		this.network = network;
	}

	@Override
	public void assignInitialPlan(ODPair odPair) {
		double[] pathFlows_veh = new double[odPair.getNumberOfPaths()];
		pathFlows_veh[0] = odPair.demand_veh;
		odPair.setCurrentPlan(new Paths(pathFlows_veh));
	}

	@Override
	public void assignCandidatePlan(ODPair odPair, DoubleArrayWrapper travelTimes_s) {

		SingleODBeckmanApproximation approx = new SingleODBeckmanApproximation(odPair, travelTimes_s, network);

//		double[] s = new double[odPair.getNumberOfPaths()];
//		double[] c = new double[odPair.getNumberOfPaths()];
//		for (int h = 0; h < odPair.getNumberOfPaths(); h++) {
//			int ij = odPair.availableLinks[h];
//			double g = odPair.getCurrentPlan().flows_veh[h];
//			double v = travelTimes_s.data[ij];
//			s[h] = this.network.compute_dTravelTime_dFlow_s_veh(ij, this.network.computeFlow_veh(ij, v));
//			c[h] = v - s[h] * g;
//		}

		List<Integer> _H = new ArrayList<>(IntStream.range(0, odPair.getNumberOfPaths()).boxed().toList());
		Collections.sort(_H, new Comparator<>() {
			@Override
			public int compare(Integer h1, Integer h2) {
				return Double.compare(approx.c[h1], approx.c[h2]);
			}
		});

		List<Integer> _Hhat = new ArrayList<>(odPair.getNumberOfPaths());

		double d = odPair.demand_veh;
		double _B = 1.0 / (approx.s[0] * d);
		double _C = approx.c[0] / (approx.s[0] * d);
		double w = (1.0 + _C) / _B;
		_Hhat.add(0);

		int h = 1;
		while ((h < odPair.getNumberOfPaths()) && (approx.c[h] < w)) {
			_C += approx.c[h] / (approx.s[h] * d);
			_B += 1.0 / (approx.s[h] * d);
			w = (1.0 + _C) / _B;
			_Hhat.add(h);
			h++;
		}

		double[] f = new double[odPair.getNumberOfPaths()];
		for (int h2 : _Hhat) {
			f[h2] = (w - approx.c[h2]) / approx.s[h2];
		}

		double feasible_veh = 0.0;
		for (int h2 : _Hhat) {
			if (f[h2] >= 0) {
				feasible_veh += f[h2];
			} else {
				f[h2] = 0.0;
			}
		}
		if (feasible_veh < 1e-8) {
			throw new RuntimeException("no feasible flow");
		}
		for (int h2 : _Hhat) {
			f[h2] *= odPair.demand_veh / feasible_veh;
		}

		odPair.setCandidatePlan(new Paths(f));

//		double currentPotential = approx.compute(odPair.getCurrentPlan());
//		double candidatePotential = approx.compute(odPair.getCandidatePlan());
//		if (currentPotential < candidatePotential) {
//			System.out.print("!!!\t");
//		}
//		System.out.println("current = " + currentPotential + ", candidate = " + candidatePotential);

	}
}
