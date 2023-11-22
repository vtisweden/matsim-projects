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

import java.util.LinkedList;

/**
 * 
 * @author GunnarF
 *
 */
class NagurneyStepSize {

	private LinkedList<Double> totalGapList = new LinkedList<>();
	private LinkedList<Double> moverGapList = new LinkedList<>();
	private LinkedList<Double> stayerGapList = new LinkedList<>();
	private LinkedList<Double> lambdaList = new LinkedList<>();

	private LinkedList<Double> avgTotalGapList = new LinkedList<>();
	private LinkedList<Double> avgMoverGapList = new LinkedList<>();
	private LinkedList<Double> avgStayerGapList = new LinkedList<>();
	private LinkedList<Double> avgLambdaList = new LinkedList<>();

	private int k = 0;
	private int n = 1;
	private int nextSwitchK = 1;

	NagurneyStepSize() {
	}

//	private static double optN(double n, double psi_n, double psi_nm1, double psi_nm2) {
//		return (2.0 * n - 1.0) / 2.0 - (psi_n - psi_nm1) / (psi_n - 2.0 * psi_nm1 + psi_nm2);
//	}

	Double getLambda() {
		if (this.lambdaList.size() == 0) {
			return null;
		}
		return this.lambdaList.getFirst();
	}

	Double getAvgLambda() {
		if (this.avgLambdaList.size() == 0) {
			return null;
		}
		return this.avgLambdaList.getFirst();
	}

	Double getMoverGap() {
		if (this.moverGapList.size() == 0 || this.lambdaList.size() == 0) {
			return null;
		}
		return this.moverGapList.getFirst() / this.lambdaList.getFirst();
	}

	Double getAvgMoverGap() {
		if (this.avgMoverGapList.size() == 0 || this.avgLambdaList.size() == 0) {
			return null;
		}
		return this.avgMoverGapList.getFirst() / this.avgLambdaList.getFirst();
	}

	Double getStayerGap() {
		if (this.stayerGapList.size() == 0 || this.lambdaList.size() == 0) {
			return null;
		}
		return this.stayerGapList.getFirst() / (1.0 - this.lambdaList.getFirst());
	}

	Double getAvgStayerGap() {
		if (this.avgStayerGapList.size() == 0 || this.avgLambdaList.size() == 0) {
			return null;
		}
		return this.avgStayerGapList.getFirst() / (1.0 - this.avgLambdaList.getFirst());
	}

	double getEta(boolean bound) {
		if (this.avgTotalGapList.size() < 2) {
			return 1.0;
		} else {
			double eta = (this.avgTotalGapList.get(1) - this.avgTotalGapList.get(0)) * this.avgTotalGapList.getLast()
					/ (this.avgTotalGapList.get(1) * this.avgTotalGapList.get(1));
			if (bound) {
				return Math.max(0.0, Math.min(1.0, eta));
			} else {
				return eta;
			}
		}
	}

	void update(final double moverGap, final double stayerGap, final double lambda) {

		this.k++;
		this.totalGapList.addFirst(moverGap + stayerGap);
		this.moverGapList.addFirst(moverGap);
		this.stayerGapList.addFirst(stayerGap);
		this.lambdaList.addFirst(lambda);

		if (this.k == this.nextSwitchK) {

			this.avgTotalGapList.addFirst(this.totalGapList.stream().mapToDouble(v -> v).average().getAsDouble());
			this.avgMoverGapList.addFirst(this.moverGapList.stream().mapToDouble(v -> v).average().getAsDouble());
			this.avgStayerGapList.addFirst(this.stayerGapList.stream().mapToDouble(v -> v).average().getAsDouble());
			this.avgLambdaList.addFirst(this.lambdaList.stream().mapToDouble(v -> v).average().getAsDouble());

			this.n++;
			this.nextSwitchK = this.k + this.n;

			this.totalGapList = new LinkedList<>();
			this.moverGapList = new LinkedList<>();
			this.stayerGapList = new LinkedList<>();
			this.lambdaList = new LinkedList<>();
		}
	}

	public static void main(String[] args) {

		for (int r = 0; r < 100; r++) {
			double n = Math.random() * 100;
			double nOpt = n + Math.random() * 100;
			double a = Math.random();
			double b = Math.random();
			double psi_n = a * Math.pow(n - nOpt, 2.0) + b;
			double psi_nm1 = a * Math.pow(n - 1.0 - nOpt, 2.0) + b;
			double psi_nm2 = a * Math.pow(n - 2.0 - nOpt, 2.0) + b;

//			System.out.println((psi_n - psi_nm1) + "\t" + (-2.0 * a*nOpt + a * (2.0 * n - 1.0)));
//			System.out.println((psi_nm1 - psi_nm2) + "\t" + (-2.0*a*nOpt + a * (2.0 * n - 3.0)));
//			System.out.println(nOpt + "\t" + optN(n, psi_n, psi_nm1, psi_nm2));
//			System.out.println();

		}

	}

}
