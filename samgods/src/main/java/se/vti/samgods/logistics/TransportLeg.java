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

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportLeg {

	private final OD od;

	private final TransportMode mode;

	private final char samgodsMode;

	private List<Link> route = null;
	private Double length_m = null;
	private Double duration_s = null;

	public TransportLeg(OD od, TransportMode mode, char samgodsMode) {
		this.od = od;
		this.mode = mode;
		this.samgodsMode = samgodsMode;
	}

	public TransportLeg(Id<Node> origin, Id<Node> destination, TransportMode mode, char samgodsMode) {
		this(new OD(origin, destination), mode, samgodsMode);
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

	public char getSamgodsMode() {
		return this.samgodsMode;
	}

	public void setRoute(final List<Link> route) {
		this.route = Collections.unmodifiableList(route);
		this.length_m = 0.0;
		this.duration_s = 0.0;
		for (Link link : route) {
			this.length_m += link.getLength();
			this.duration_s += link.getLength() / link.getFreespeed();
		}
	}

	public List<Link> getRouteView() {
		return this.route;
	}

	public double getLength_m() {
		return this.length_m;
	}

	public double getDuration_s() {
		return this.duration_s;
	}

}
