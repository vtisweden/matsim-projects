/**
 * se.vti.samgods.models
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
package se.vti.samgods.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationSimulationDemo {

	public static void main(String[] args) {

		Random rnd = new Random();

		// time structure
		int _SERINT = 30;

		// supply
		double _VEHCAP = 20.0;

		// demand
		double _INDTON = 120.0;
		double _FREQ = 1.00 / 60.0;
		double _PrACTIVE = 1.0 - Math.exp(-_FREQ * _SERINT);
		double _ACTDEM = _INDTON * _FREQ * _SERINT / _PrACTIVE;

		int serviceIntervalCnt = 365 / _SERINT + 1;
		double meanRequestInterval_days = 1.0 / _FREQ;

		List<ArrayList<Double>> requestDaysOverIntervals = IntStream.range(0, serviceIntervalCnt).boxed()
				.map(i -> new ArrayList<Double>()).toList();
		for (double d = meanRequestInterval_days * rnd.nextExponential(); d < 365; d += meanRequestInterval_days
				* rnd.nextExponential()) {
			int interval = (int) (d / _SERINT);
			requestDaysOverIntervals.get(interval).add(d - interval * _SERINT);
		}

		System.out.println("day\tcumulativeDemand[ton]\tcumulativeSupply[ton]\tpending[ton]\tefficiency");
		double cumulativeDemand_ton = 0.0;
		double cumulativeSupply_ton = 0.0;
		for (int serviceInterval = 0; serviceInterval < serviceIntervalCnt; serviceInterval++) {
			List<Double> requestDays = requestDaysOverIntervals.get(serviceInterval);

			double[] demands = new double[_SERINT];
			for (Double requestDay : requestDays) {
				demands[requestDay.intValue()] += _INDTON;
			}

			double[] supplies = new double[_SERINT];
			double _SERVICES = 1.0
					* (Math.max(0, cumulativeDemand_ton - cumulativeSupply_ton) + Arrays.stream(demands).sum())
					/ _VEHCAP;
			double pickupInterval_days = _SERINT / _SERVICES;
			for (int service = 0; service < _SERVICES; service++) {
				int roundedDay = (int) (pickupInterval_days * service);
				supplies[roundedDay] += _VEHCAP;				
			}
			
			for (int day = 0; day < _SERINT; day++) {
				cumulativeDemand_ton += demands[day];
				cumulativeSupply_ton += supplies[day];
				System.out.println((serviceInterval * _SERINT + day) + "\t" + cumulativeDemand_ton + "\t"
						+ cumulativeSupply_ton + "\t" + Math.max(0, cumulativeDemand_ton - cumulativeSupply_ton) + "\t"
						+ Math.min(1, cumulativeDemand_ton / cumulativeSupply_ton));
			}

		}
	}

}
