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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.VehicleType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.network.LinkAttributes;
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

	@JsonSerialize(using = ConsolidationUnitSerializer.class)
	@JsonDeserialize(using = ConsolidationUnitDeserializer.class)
	public static class ConsolidationUnit extends ListRepresented {

		public final List<Id<Node>> nodeIds;
		public final SamgodsConstants.Commodity commodity;
		public final SamgodsConstants.TransportMode mode;
		public final Boolean isContainer;

		public List<List<Id<Link>>> linkIds = null; // may be re-routed
		public List<List<Link>> links = null;
		public Boolean containsFerry = null; // depends on links

		public boolean isRouted() {
			return this.linkIds != null;
		}

		public boolean hasNetworkData() {
			return this.links != null && this.containsFerry != null;
		}

		public ConsolidationUnit(List<Id<Node>> nodes, SamgodsConstants.Commodity commodity,
				SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry,
				List<List<Id<Link>>> linkIds) {
			this.nodeIds = nodes;
			this.commodity = commodity;
			this.mode = mode;
			this.isContainer = isContainer;
			this.containsFerry = containsFerry;
			this.linkIds = linkIds;
		}

		public ConsolidationUnit(List<Id<Node>> nodes, SamgodsConstants.Commodity commodity,
				SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {
			this(nodes, commodity, mode, isContainer, containsFerry, null);
		}

		public ConsolidationUnit(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode,
				Boolean isContainer, Boolean containsFerry) {
			this(null, commodity, mode, isContainer, containsFerry, null);
		}

		public ConsolidationUnit(List<Id<Node>> nodes, SamgodsConstants.Commodity commodity,
				SamgodsConstants.TransportMode mode, Boolean isContainer, List<List<Id<Link>>> linkIds,
				Network network) {
			this.nodeIds = nodes;
			this.commodity = commodity;
			this.mode = mode;
			this.isContainer = isContainer;
			this.linkIds = linkIds;

			this.updateNetworkData(network);
		}

		public void setRoutes(List<List<Link>> routes) {
			this.linkIds = new ArrayList<>(routes.size());
			this.links = new ArrayList<>(routes.size());
			for (List<Link> route : routes) {
				if (route.size() == 0) {
					this.linkIds.add(new ArrayList<>(0));
					this.links.add(new ArrayList<>(0));
				} else {
					this.linkIds.add(route.stream().map(l -> l.getId()).collect(Collectors.toList()));
					this.links.add(route);
				}
			}
			this.containsFerry = this.links.stream().filter(list -> list != null).flatMap(list -> list.stream())
					.anyMatch(l -> LinkAttributes.isFerry(l));
		}

		public void updateNetworkData(Network network) {
			this.links = new ArrayList<>(this.linkIds.size());
			this.containsFerry = false;
			for (List<Id<Link>> routeIds : this.linkIds) {
				if (routeIds.size() == 0) {
					this.links.add(new ArrayList<>(0));
				} else {
					List<Link> route = routeIds.stream().map(id -> network.getLinks().get(id))
							.collect(Collectors.toList());
					this.links.add(route);
					this.containsFerry |= route.stream().anyMatch(l -> LinkAttributes.isFerry(l));
				}
			}
		}

		private static List<Id<Node>> extractNodes(List<TransportLeg> legs) {
			final ArrayList<Id<Node>> nodes = new ArrayList<>(legs.size() + 1);
			legs.stream().map(l -> l.getOrigin()).forEach(n -> nodes.add(n));
			nodes.add(legs.get(legs.size() - 1).getDestination());
			return nodes;
		}

		private static ConsolidationUnit createUnrouted(List<TransportLeg> legs) {
			return new ConsolidationUnit(extractNodes(legs), legs.get(0).getCommodity(), legs.get(0).getMode(),
					legs.get(0).isContainer(), null);
//			
//			final boolean noRoutes = legs.stream().allMatch(l -> l.getRouteIdsView() == null);
//			final boolean allRoutes = legs.stream().noneMatch(l -> l.getRouteIdsView() == null);
//
//			if (noRoutes) {
//				return new ConsolidationUnit(extractNodes(legs), legs.get(0).getCommodity(), legs.get(0).getMode(),
//						legs.get(0).isContainer(), legs.get(0).containsFerry());
//			} else if (allRoutes) {
//				return new ConsolidationUnit(extractNodes(legs), legs.get(0).getCommodity(), legs.get(0).getMode(),
//						legs.get(0).isContainer(),
//						legs.stream().map(l -> l.getRouteIdsView()).collect(Collectors.toList()), network);
//			} else {
//				throw new RuntimeException("Some routes have IDs, some not.");
//			}
		}

		private static ConsolidationUnit createUnrouted(TransportLeg leg) {
			return createUnrouted(Arrays.asList(leg));
		}

		public static List<ConsolidationUnit> createUnrouted(TransportEpisode episode) {
			if (episode.getLegs() == null) {
				return Collections.emptyList();
			} else {
				if (episode.getMode().equals(TransportMode.Rail) && episode.getLegs().size() > 1) {
					return episode.getLegs().stream().map(l -> createUnrouted(l)).collect(Collectors.toList());
				} else {
					return Arrays.asList(createUnrouted(episode.getLegs()));
				}
			}
		}

		public List<List<Link>> getLinks() {
			return this.links;
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

		@Override
		List<Object> asList() {
			return Arrays.asList(this.nodeIds, this.commodity, this.mode, this.isContainer, this.containsFerry,
					this.linkIds);
		}
	}

	public static class ConsolidationUnitDeserializer extends JsonDeserializer<ConsolidationUnit> {

		public ConsolidationUnitDeserializer() {
		}

		@Override
		public ConsolidationUnit deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			ObjectCodec codec = p.getCodec();
			JsonNode node = codec.readTree(p);

			List<Id<Node>> nodes = Arrays.stream(((TextNode) node.get("nodes")).asText().split(","))
					.map(n -> Id.createNodeId(n)).toList();
			Commodity commodity = Commodity.valueOf(((TextNode) node.get("commodity")).asText());
			TransportMode mode = TransportMode.valueOf(((TextNode) node.get("mode")).asText());
			boolean isContainer = Boolean.parseBoolean(((TextNode) node.get("isContainer")).asText());
			boolean containsFerry = Boolean.parseBoolean(((TextNode) node.get("containsFerry")).asText());

			List<List<Id<Link>>> linkIds = new ArrayList<>();
			ArrayNode routesNode = (ArrayNode) node.get("routes");

			for (JsonNode routeNode : routesNode) {
				JsonNode routeField = routeNode.get("route");
				if (routeField == null || routeField.isNull()) {
					linkIds.add(null);
				} else {
					String line = ((TextNode) routeField).asText();
					if ("".equals(line)) {
						linkIds.add(new ArrayList<>());
					} else {
						String[] idStrings = line.split(",");
						List<Id<Link>> routeLinks = List.of(idStrings).stream().map(idStr -> Id.createLinkId(idStr))
								.collect(Collectors.toList());
						linkIds.add(routeLinks);
					}
				}
			}

			return new ConsolidationUnit(nodes, commodity, mode, isContainer, containsFerry, linkIds);
		}
	}

	public static class ConsolidationUnitSerializer extends JsonSerializer<ConsolidationUnit> {

		ConsolidationUnitSerializer() {
		}

		@Override
		public void serialize(ConsolidationUnit consolidationUnit, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeStartObject();
			if (consolidationUnit.nodeIds.stream().anyMatch(id -> id.toString().contains(","))) {
				throw new RuntimeException("Link IDs must not contain \",\"");
			}
			gen.writeStringField("nodes",
					consolidationUnit.nodeIds.stream().map(n -> n.toString()).collect(Collectors.joining(",")));
			gen.writeStringField("commodity", consolidationUnit.commodity.toString());
			gen.writeStringField("mode", consolidationUnit.mode.toString());
			gen.writeStringField("isContainer", consolidationUnit.isContainer.toString());
			gen.writeStringField("containsFerry", consolidationUnit.containsFerry.toString());
			gen.writeFieldName("routes");
			gen.writeStartArray();
			for (List<Id<Link>> linkIds : consolidationUnit.linkIds) {
				gen.writeStartObject();
				if (linkIds.stream().anyMatch(id -> id.toString().contains(","))) {
					throw new RuntimeException("Link IDs must not contain \",\"");
				}
				gen.writeStringField("route",
						linkIds.stream().map(id -> id.toString()).collect(Collectors.joining(",")));
				gen.writeEndObject();
			}
			gen.writeEndArray();
			gen.writeEndObject();
		}

	}
}
