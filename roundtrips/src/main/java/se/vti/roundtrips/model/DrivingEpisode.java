/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.model;

import se.vti.roundtrips.single.Location;

/**
 * 
 * @author GunnarF
 *
 */
public final class DrivingEpisode<L extends Location> extends Episode {

	private final L origin;
	private final L destination;

	public DrivingEpisode(L origin, L destination) {
		this.origin = origin;
		this.destination = destination;
	}

	public L getOrigin() {
		return origin;
	}

	public L getDestination() {
		return destination;
	}

	@Override
	public String toString() {
		return super.toString() + ",od(" + this.origin + "," + this.destination + ")";
	}
}
