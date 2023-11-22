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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author GunnarF
 *
 */
class TwoBlockStepSize {

	private List<Double> gapList = new ArrayList<>();

	TwoBlockStepSize() {
	}

	double getEta(boolean bound) {
		if (this.gapList.size() < 2) {
			return 1.0;
		} else {
			final int blockSize = (int) Math.ceil(0.5 * this.gapList.size());
			final double gap1 = this.gapList.subList(0, blockSize).stream().mapToDouble(g -> g).sum();
			final double gap2 = this.gapList.subList(this.gapList.size() - blockSize, this.gapList.size()).stream()
					.mapToDouble(g -> g).sum();
			double eta = (gap1 - gap2) / gap1;
			if (bound) {
				return Math.max(0.0, Math.min(1.0, eta));
			} else {
				return eta;
			}
		}
	}

	void update(final double gap) {
		this.gapList.add(gap);
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
