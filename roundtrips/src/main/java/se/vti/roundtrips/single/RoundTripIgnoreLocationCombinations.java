/**
 * se.vti.skellefteaV2X
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
package se.vti.roundtrips.single;

import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class RoundTripIgnoreLocationCombinations<L> implements MHWeight<RoundTrip<L>> {

	private final int locationCnt;

	private double[] size;

	public RoundTripIgnoreLocationCombinations(int locationCnt, int maxLength) {
		this.locationCnt = locationCnt;
		this.size = new double[maxLength];
		for (int length = 1; length <= maxLength; length++) {
			this.size[length - 1] = this.size(length);
		}
	}

	private double size(int length) {
		if (length == 1) {
			return this.locationCnt;
		} else if (length == 2) {
			return this.locationCnt * (this.locationCnt - 1);
		} else if (length == 3) {
			return this.locationCnt * (this.locationCnt - 1) * (this.locationCnt - 2);
		} else {

			// first one is anything
			double size = this.locationCnt;

			// second one is anything different from first one
			double probaEqualsFirst = 0.0;
			size *= (this.locationCnt - 1.0);

			for (int i = 2; i < length - 1; i++) {
				// anything different from previous one
				size *= (this.locationCnt - 1.0);
				// only if previous not equals first one, we may uniformly select first one
				// again
				probaEqualsFirst = (1.0 - probaEqualsFirst) * 1.0 / (this.locationCnt - 1.0);
			}
			assert (probaEqualsFirst >= 0.0);
			assert (probaEqualsFirst <= 1.0);

			// number of options for last one depends on proba of previous one being equal
			// to first one
			size *= ((probaEqualsFirst * (this.locationCnt - 1.0)
					+ (1.0 - probaEqualsFirst) * (this.locationCnt - 2.0)));

			return size;
		}
	}

	@Override
	public double logWeight(RoundTrip<L> state) {
		return -Math.log(this.size[state.locationCnt() - 1]);
	}

	// checking against closed-form expression
	public static void main(String[] args) {

		{
			long cnt = 0;
			for (int i1 = 1; i1 <= 7; i1++) {
				for (int i2 = 1; i2 <= 7; i2++) {
					if (i2 != i1) {
						for (int i3 = 1; i3 <= 7; i3++) {
							if (i3 != i2) {
								for (int i4 = 1; i4 <= 7; i4++) {
									if (i4 != i3 && i4 != i1) {
										cnt++;
									}
								}
							}
						}
					}
				}
			}
			System.out.println(cnt);
		}

		{
			System.out.println(new RoundTripIgnoreLocationCombinations<>(7, 4).size(4));

		}

		for (int _N = 1; _N <= 10; _N++) {
			for (int _J = 4; _J <= _N; _J++) {
				RoundTripIgnoreLocationCombinations<Object> obj = new RoundTripIgnoreLocationCombinations<>(_N, _J);
				double size1 = obj.size(_J);

				double size2 = _N * Math.pow(_N - 1.0, _J - 2.0) ;
				double proba = 0.0;
				for (int l = 0; l <= _J - 4; l++) {
					proba += Math.pow(-1.0, l) / Math.pow(_N - 1.0, l + 1.0);
				}
				size2 *= (proba *(_N - 1.0) + (1.0 - proba) * (_N - 2.0));

				System.out.print(size2 / size1 + "\t");
			}
			System.out.println();
		}
	}

}
