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
package se.vti.samgods.io;

import floetteroed.utilities.Tuple;
import se.vti.samgods.io.ChainChoiReader.TransportMode;
import se.vti.samgods.logistics.Location;
import se.vti.samgods.logistics.TransportLeg;

public class TransportLegImpl implements TransportLeg {

	private final Tuple<Location, Location> od;
	private final TransportMode mode;
	
	public TransportLegImpl(final Location origin, final Location destination, final TransportMode mode) {
		this.od = new Tuple<>(origin, destination);
		this.mode = mode;
	}

	@Override
	public Location getOrigin() {
		return this.od.getA();
	}

	@Override
	public Location getDestination() {
		return this.od.getB();
	}

	@Override
	public TransportMode getMode() {
		return this.mode;
	}

}
