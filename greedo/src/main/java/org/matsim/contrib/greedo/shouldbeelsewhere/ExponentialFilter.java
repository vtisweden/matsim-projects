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
package org.matsim.contrib.greedo.shouldbeelsewhere;

public class ExponentialFilter {

	private final double inertia;

	private int k = -1;

	private double filteredValue = 0.0;

	public ExponentialFilter(final double inertia) {
		this.inertia = inertia;
	}

	public void addValue(final double value) {
		this.k++;
		this.filteredValue = (this.inertia * (1.0 - Math.pow(this.inertia, this.k)) * this.filteredValue
				+ (1 - this.inertia) * value) / (1.0 - Math.pow(this.inertia, this.k + 1.0));
	}

	public int getObservedValueCnt() {
		return (this.k + 1);
	}
	
	public double getFilteredValue() {
		return this.filteredValue;
	}
	
	public static void main(String[] args) {
		ExponentialFilter filter = new ExponentialFilter(0.95);
		double vanilla = 0.0;
		
		for (int k = 0; k < 100; k++) {
			double val = Math.exp(-0.1 * k);
			filter.addValue(val);
			if (k == 0) {
				vanilla = val;
			} else {
				vanilla = 0.95 * vanilla + 0.05 * val;
			}
			System.out.println(val + "\t" + vanilla + "\t" + filter.getFilteredValue());
		}
		
		
		
	}
	
}
