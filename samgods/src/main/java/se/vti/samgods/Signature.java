/**
 * se.vti.samgods
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class Signature {

	private static abstract class ListRepresented {

		abstract List<Object> asList();

		@Override
		public int hashCode() {
			return this.asList().hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			} else if (other instanceof ListRepresented) {
				return this.asList().equals(((ListRepresented) other).asList());
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return this.asList().toString();
		}
	}

	public static class ConsolidationEpisode extends Episode {

		public final Boolean loadAtStart;
		public final Boolean unloadAtEnd;

		private ConsolidationEpisode(List<TransportLeg> legs, boolean loadAtStart, boolean unloadAtEnd) {
			super(legs.get(0).getCommodity(), legs.get(0).getMode(), legs.get(0).isContainer(),
					legs.get(0).containsFerry(),
					legs.stream().map(l -> l.getRouteIdsView()).collect(Collectors.toList()));
			this.loadAtStart = loadAtStart;
			this.unloadAtEnd = unloadAtEnd;
		}

		private ConsolidationEpisode(TransportLeg leg, boolean loadAtStart, boolean unloadAtEnd) {
			this(Arrays.asList(leg), loadAtStart, unloadAtEnd);
		}

		public static List<ConsolidationEpisode> create(TransportEpisode episode) {
			if (episode.getLegs() == null
					|| episode.getLegs().stream().mapToInt(l -> l.getRouteIdsView().size()).sum() == 0) {
				return Collections.emptyList();
			} else {
				if (episode.getMode().equals(TransportMode.Rail) && episode.getLegs().size() > 1) {
					// TODO What if loading / unloading happens during degenerate leg without links?
					return episode.getLegs().stream().filter(l -> l.getRouteIdsView().size() > 0)
							.map(l -> new ConsolidationEpisode(l, l == episode.getLegs().getFirst(),
									l == episode.getLegs().getLast()))
							.collect(Collectors.toList());
				} else {
					return Arrays.asList(new ConsolidationEpisode(episode.getLegs(), true, true));
				}
			}
		}

		@Override
		List<Object> asList() {
			List<Object> result = new ArrayList<>(super.asList());
			result.add(this.loadAtStart);
			result.add(this.unloadAtEnd);
			return result;
		}

		@Override
		public boolean isCompatible(TransportEpisode episode) {
			return create(episode).contains(this);
		}

	}

	@Deprecated
	public static class Episode extends ListRepresented {

		public final SamgodsConstants.Commodity commodity;
		public final SamgodsConstants.TransportMode mode;
		public final Boolean isContainer;
		public final Boolean containsFerry;
		public final List<List<Id<Link>>> linkIds;

		@Override
		List<Object> asList() {
			return Arrays.asList(this.commodity, this.mode, this.isContainer, this.containsFerry, this.linkIds);
		}

		public Episode(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
				Boolean containsFerry, List<List<Id<Link>>> linkIds) {
			this.commodity = commodity;
			this.mode = mode;
			this.isContainer = isContainer;
			this.containsFerry = containsFerry;
			this.linkIds = linkIds;
		}

		public Episode(TransportEpisode episode) {
			this(episode.getCommodity(), episode.getMode(), episode.isContainer(), episode.containsFerry(),
					episode.createLinkIds());
		}

		public boolean isCompatible(FreightVehicleAttributes attrs) {
			return (this.commodity == null || attrs.isCompatible(this.commodity))
					&& (this.mode == null || this.mode.equals(attrs.mode))
					&& (this.isContainer == null || this.isContainer.equals(attrs.isContainer))
					&& (this.containsFerry == null || !this.containsFerry || attrs.isFerryCompatible());
		}

		public boolean isCompatible(VehicleType type) {
			return this.isCompatible(FreightVehicleAttributes.getFreightAttributes(type));
		}

		public boolean isCompatible(TransportEpisode episode) {
			return (this.commodity == null || this.commodity.equals(episode.getCommodity()))
					&& (this.mode == null || this.mode.equals(episode.getMode()))
					&& (this.isContainer == null || this.isContainer.equals(episode.isContainer()))
					&& (this.containsFerry == null || this.containsFerry.equals(episode.containsFerry()))
					&& (this.linkIds == null || this.linkIds.equals(episode.createLinkIds()));
		}
	}
}
