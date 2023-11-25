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
package se.vti.samgods.logistics;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;

public class TransportLeg {

	private final OD od;

	private final SamgodsConstants.TransportMode mode;

	private List<Node> route;

	public TransportLeg(OD od, TransportMode mode) {
		this.od = od;
		this.mode = mode;
	}

	public TransportLeg(Id<Node> origin, Id<Node> destination, TransportMode mode) {
		this(new OD(origin, destination), mode);
	}

	public Id<Node> getOrigin() {
		return this.od.origin;
	}

	public Id<Node> getDestination() {
		return this.od.destination;
	}

	public OD getOD() {
		return this.od;
	}

	public SamgodsConstants.TransportMode getMode() {
		return this.mode;
	}

	public void setRoute(final List<Node> route) {
		this.route = route;
	}

	public List<Node> getRoute() {
		return this.route;
	}

}
