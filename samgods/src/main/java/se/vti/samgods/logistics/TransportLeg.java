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
import java.util.stream.Collectors;

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

	// defining
	private final OD od;
	private final TransportMode mode;
	private List<Id<Link>> route = null;

	// derived
//	private Double length_m = null;
//	private Double duration_s = null;

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

	public void setRoute(final List<Link> route) {
		this.route = Collections.unmodifiableList(route.stream().map(l -> l.getId()).collect(Collectors.toList()));
//		this.length_m = 0.0;
//		this.duration_s = 0.0;
//		for (Link link : route) {
//			this.length_m += link.getLength();
//			this.duration_s += link.getLength() / link.getFreespeed();
//		}
	}

	public List<Id<Link>> getRouteView() {
		return this.route;
	}

//	public Double getLength_m() {
//		return this.length_m;
//	}
//
//	public Double getDuration_s() {
//		return this.duration_s;
//	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public int hashCode() {
		int code = this.od.hashCode() + 31 * this.mode.hashCode();
		if (this.route != null) {
			code = this.route.hashCode() + 31 * code;
		}
		return code;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof TransportLeg)) {
			return false;
		}
		final TransportLeg otherLeg = (TransportLeg) other;
		return this.od.equals(otherLeg.od) && (this.mode.equals(otherLeg.mode) && this.route.equals(otherLeg.route));
	}

}
