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
import java.util.Random;

/**
 * 
 * @author GunnarF
 *
 */
class ThreeBlockStepSize {

	private final double initialIterationToEtaExponent = -0.5;

	private final double iterationToBlockSizeExponent = 0.5;

	private final int minBlockSize;

	private List<Double> gapList = new ArrayList<>();

	ThreeBlockStepSize(final int minBlockSize) {
		this.minBlockSize = minBlockSize;
	}

	private Double lastEta = null;

	Double getLastEta() {
		return this.lastEta;
	}
	
	/**
	 * @param iteration First iteration shall be zero.
	 * @param bound
	 * @return
	 */
	double getEta(final int iteration, final boolean bound) {

		final int blockSize = (int) (Math.pow((iteration + 1) / 3.0, this.iterationToBlockSizeExponent));

//		System.out.println("block size = " + blockSize);

		if (blockSize < this.minBlockSize) {

			this.lastEta = Math.pow(iteration + 1, this.initialIterationToEtaExponent);
			return this.lastEta;

		} else {

			final double gapA = this.gapList
					.subList(this.gapList.size() - 2 * blockSize, this.gapList.size() - blockSize).stream()
					.mapToDouble(g -> g).sum();
			final double gapB = this.gapList.subList(this.gapList.size() - blockSize, this.gapList.size()).stream()
					.mapToDouble(g -> g).sum();

//			System.out.println("gapA = " + gapA + ", gapB = " + gapB);

			double deltaEta = (gapA - gapB) / gapA - this.lastEta;
			deltaEta = Math.signum(deltaEta) * Math.min(Math.abs(deltaEta), this.lastEta / iteration);
			this.lastEta += deltaEta;

			if (bound) {
				return Math.max(0.0, Math.min(1.0, this.lastEta));
			} else {
				return this.lastEta;
			}
		}
	}

	void update(final double gap) {
		this.gapList.add(gap);
	}

	public static void main(String[] args) {

		Random rnd = new Random();
		double sigmaFact = 0.1;

		double a = 1.0;
		double b = 2.0;

		ThreeBlockStepSize atbss = new ThreeBlockStepSize(3);

		for (int k = 0; k < 1000; k++) {
//			final double eta = 1.0 / (k+1);
			double gap = 1.0 / Math.pow(k + 1.0, 1);
			gap += rnd.nextGaussian(0, sigmaFact * gap);
			gap = Math.max(0.0, gap);

			atbss.update(gap);
			System.out.println(gap + "\t" + atbss.getEta(k, false));
		}

	}

}
