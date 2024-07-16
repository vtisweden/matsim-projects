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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.network.LinkAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportLeg {

	// -------------------- MEMBERS --------------------

	private final OD od;

	private TransportEpisode parent;

	// derived
	private List<Id<Link>> routeIds = null;
	private Boolean containsFerry = null;

	// -------------------- CONSTRUCTION --------------------

	public TransportLeg(OD od) {
		this.od = od;
	}

	public TransportLeg(Id<Node> origin, Id<Node> destination) {
		this(new OD(origin, destination));
	}

	// -------------------- IMPLEMENTATION --------------------

	/* package */ void setParent(TransportEpisode parent) {
		this.parent = parent;
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

	public SamgodsConstants.Commodity getCommodity() {
		if (this.parent == null) {
			return null;
		} else {
			return this.parent.getCommodity();
		}
	}

	public Boolean isContainer() {
		if (this.parent == null) {
			return null;
		} else {
			return this.parent.isContainer();
		}
	}

	public SamgodsConstants.TransportMode getMode() {
		if (this.parent == null) {
			return null;
		} else {
			return this.parent.getMode();
		}
	}

	public Boolean containsFerry() {
		return this.containsFerry;
	}

	public void setRoute(final List<Link> route) {
		if (route == null) {
			this.routeIds = null;
			this.containsFerry = null;
		} else {
			this.routeIds = Collections
					.unmodifiableList(route.stream().map(l -> l.getId()).collect(Collectors.toList()));
			this.containsFerry = route.stream().anyMatch(l -> LinkAttributes.getMode(l).isFerry());
		}
	}

	public List<Id<Link>> getRouteIdsView() {
		return this.routeIds;
	}

	public boolean isRouted() {
		return (this.routeIds != null);
	}

	// -------------------- OVERRIDING OF Object --------------------

//	@Override
//	public int hashCode() {
//		return this.asList().hashCode();
//	}
//
//	@Override
//	public boolean equals(Object other) {
//		if (this == other) {
//			return true;
//		} else if (!(other instanceof TransportLeg)) {
//			return false;
//		} else {
//			return this.asList().equals(((TransportLeg) other).asList());
//		}
//	}
}
