/**
 * se.vti.samgods
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
package se.vti.samgods.consolidation.road;

import se.vti.samgods.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class Shipment {

	private final Commodity commodity;

	private final double tons;

	private final double probability;

	public Shipment(final Commodity commodity, final double tons, double probability) {
		this.commodity = commodity;
		this.tons = tons;
		this.probability = probability;
	}

	public double getWeight_ton() {
		return this.tons;
	}

	public double getProbability() {
		return this.probability;
	}

	public Commodity getType() {
		return this.commodity;
	}

	@Override
	public String toString() {
		return this.commodity + "(weight=" + this.tons + "ton,proba=" + this.probability + ")";
	}

}
