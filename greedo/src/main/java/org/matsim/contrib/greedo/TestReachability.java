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

import java.util.Arrays;

public class TestReachability {

	static double[] distr(double sigma, int _K) {
		double[] p = new double[_K];
		p[0] = 1.0;
		for (int k = 0; k < _K; k++) {
//			for (double pVal : p) {
//				System.out.print(MathHelpers.round(pVal, 2) + "\t");
//			}
//			System.out.println();			
			double[] pNew = Arrays.copyOf(p, p.length);
			for (int k2 = 0; k2 < _K; k2++) {
				for (int fwdLag = 1; k2 + fwdLag < _K; fwdLag++) {
					double flux = p[k2] * (Math.pow(sigma, fwdLag) - Math.pow(sigma, fwdLag + 1));
					pNew[k2] -= flux;
					pNew[k2 + fwdLag] += flux;
				}
			}
			p = pNew;
		}
		return p;
	}

	static double successProba(double sigma, double[] distr, double targetSize) {
		double successProba = 0.0;
		for (int i = 0; i < distr.length; i++) {
			double size = Math.pow(sigma, i);
			successProba += distr[i] * Math.min(1.0, targetSize / size);
		}
		return successProba;
	}

	static double expectedSize(double sigma, double[] distr) {
		double size = 0.0;
		for (int i = 0; i < distr.length; i++) {
			size += distr[i] * Math.pow(sigma, i);
		}
		return size;
	}

	public static void main(String[] args) {

		final int _K = 100;
		final double targetSize = 1e-2;

		for (double sigma = 0.01; sigma <= 1.001; sigma += 0.01) {
			System.out.println(sigma + "\t" + Math.log(successProba(sigma, distr(sigma, _K), targetSize)));
		}
	}
}
