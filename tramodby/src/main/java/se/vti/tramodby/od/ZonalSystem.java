/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.tramodby.od;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * This class handles the zonal system information.
 * 
 */
public class ZonalSystem {

	/**
	 * This class stores data of a specific zone.
	 * 
	 *
	 */
	public class Zone {
		private final Id<Zone> id;
		private final String name;
		private final Set<Id<Link>> linkIds = Collections.synchronizedSet(new LinkedHashSet<>());

		private Zone(final Id<Zone> id, final String name) {
			this.id = id;
			this.name = name;
		}

		/**
		 * This method gets the id of the zone.
		 * 
		 * @return zonal id
		 */
		public Id<Zone> getId() {
			return this.id;
		}

		/**
		 * This method gets the ids of all links contained within the zone.
		 * 
		 * @return set of link ids
		 */
		public Set<Id<Link>> getLinkIds() {
			return this.linkIds;
		}
	}

	private final Map<Id<Zone>, Zone> id2zone = Collections.synchronizedMap(new LinkedHashMap<>());

	private final Map<Id<Link>, Id<Zone>> linkId2zoneId = Collections.synchronizedMap(new LinkedHashMap<>());

	/**
	 * This method adds a new link id to a zone.
	 * 
	 * @param linkId   - the id of the link
	 * @param zoneId   - the zone id
	 * @param zoneName - the zone name
	 */
	public void add(final Id<Link> linkId, final Id<Zone> zoneId, final String zoneName) {
		final Zone zone = this.id2zone.computeIfAbsent(zoneId, id -> new Zone(id, zoneName));
		if (!zoneName.equals(zone.name)) {
			throw new IllegalArgumentException("There exists already a zone with identifier " + zoneId
					+ ", but its name (" + zone.name + ") does not match the newly given name (" + zoneName + ")");
		}
		zone.linkIds.add(linkId);
		this.linkId2zoneId.put(linkId, zoneId);
	}

	/**
	 * This method gets all zones.
	 * 
	 * @return set of zones
	 */
	public Map<Id<Zone>, Zone> getAllZones() {
		return this.id2zone;
	}

	/**
	 * This method gets the number of zones.
	 * 
	 * @return number of zones
	 */
	public int zoneCnt() {
		return this.id2zone.size();
	}

	/**
	 * This method gets the number of links.
	 * 
	 * @return number of links
	 */
	public int linkCnt() {
		return this.linkId2zoneId.size();
	}

	/**
	 * This method checks the link and zone data are consistent.
	 * 
	 * @return consistency
	 */
	public boolean consistent() {
		Set<Id<Link>> collectedLinkIds = new LinkedHashSet<>();
		for (Zone zone : this.id2zone.values()) {
			collectedLinkIds.addAll(zone.linkIds);
		}
		return collectedLinkIds.equals(this.linkId2zoneId.keySet());
	}

}
