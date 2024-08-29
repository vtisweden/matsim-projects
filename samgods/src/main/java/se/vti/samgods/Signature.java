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
import java.util.Set;
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
 * Signatures are shared among episodes, hence synchronized throughout.
 * 
 * @author GunnarF
 *
 */
public class Signature {

	private static abstract class ListRepresented {

		abstract List<Object> asList();

		@Override
		public synchronized int hashCode() {
			return this.asList().hashCode();
		}

		@Override
		public synchronized boolean equals(Object other) {
			if (this == other) {
				return true;
			} else if (other instanceof ListRepresented) {
				return this.asList().equals(((ListRepresented) other).asList());
			} else {
				return false;
			}
		}

		@Override
		public synchronized String toString() {
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

		public List<List<Id<Link>>> linkIds = null;
		public List<List<Link>> links = null;
		private Set<Id<Link>> ferryLinkIds = null;

		// CONSTRUCTION

		public ConsolidationUnit(List<Id<Node>> nodes, SamgodsConstants.Commodity commodity,
				SamgodsConstants.TransportMode mode, Boolean isContainer, List<List<Id<Link>>> linkIds) {
			this.nodeIds = nodes;
			this.commodity = commodity;
			this.mode = mode;
			this.isContainer = isContainer;
			this.linkIds = linkIds;
		}

		public synchronized static List<ConsolidationUnit> createUnrouted(TransportEpisode episode) {
			if (episode.getLegs() == null) {
				return Collections.emptyList();
			} else {
				if (episode.getMode().equals(TransportMode.Rail) && episode.getLegs().size() > 1) {
					return episode.getLegs().stream().map(leg -> Arrays.asList(leg))
							.map(legs -> new ConsolidationUnit(extractNodes(legs), episode.getCommodity(),
									episode.getMode(), episode.isContainer(), null))
							.collect(Collectors.toList());
				} else {
					return Arrays.asList(new ConsolidationUnit(extractNodes(episode.getLegs()), episode.getCommodity(),
							episode.getMode(), episode.isContainer(), null));
				}
			}
		}

		public synchronized static ConsolidationUnit createVehicleCompatibilityTemplate(
				SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
				Boolean containsFerry) {
			return new ConsolidationUnit(null, commodity, mode, isContainer, null);
		}

		public synchronized ConsolidationUnit createRoutingEquivalentCopy() {
			return new ConsolidationUnit(this.nodeIds, this.commodity, this.mode, this.isContainer, null);
		}

		// IMPLEMENTATION

		public synchronized Double computeLength_m() {
			if (this.hasNetworkReferences()) {
				return this.links.stream().flatMap(ll -> ll.stream()).mapToDouble(l -> l.getLength()).sum();
			} else {
				return null;
			}
		}

		public synchronized boolean isRouted() {
			return this.linkIds != null;
		}

		public synchronized boolean hasNetworkReferences() {
			return this.links != null;
		}

		public synchronized void setRoutes(List<List<Link>> routes) {
			this.linkIds = new ArrayList<>(routes.size());
			this.links = new ArrayList<>(routes.size());
			for (List<Link> route : routes) {
				this.linkIds.add(route.stream().map(l -> l.getId()).collect(Collectors.toList()));
				this.links.add(route);
			}
			this.ferryLinkIds = Collections.synchronizedSet(this.links.stream().flatMap(list -> list.stream())
					.filter(l -> LinkAttributes.isFerry(l)).map(l -> l.getId()).collect(Collectors.toSet()));
		}

		public synchronized void updateNetworkReferences(Network network) {
			this.links = new ArrayList<>(this.linkIds.size());
			for (List<Id<Link>> routeIds : this.linkIds) {
				List<Link> routeRefs = routeIds.stream().map(id -> network.getLinks().get(id))
						.collect(Collectors.toList());
				this.links.add(routeRefs);
			}
			this.ferryLinkIds = Collections.synchronizedSet(this.links.stream().flatMap(list -> list.stream())
					.filter(l -> LinkAttributes.isFerry(l)).map(l -> l.getId()).collect(Collectors.toSet()));
		}

		private synchronized static List<Id<Node>> extractNodes(List<TransportLeg> legs) {
			final ArrayList<Id<Node>> nodes = new ArrayList<>(legs.size() + 1);
			legs.stream().map(l -> l.getOrigin()).forEach(n -> nodes.add(n));
			nodes.add(legs.get(legs.size() - 1).getDestination());
			return nodes;
		}

		public synchronized List<List<Link>> getLinks() {
			return this.links;
		}

		public synchronized Boolean isFerry(Id<Link> linkId) {
			if (this.ferryLinkIds == null) {
				return null;
			} else {
				return this.ferryLinkIds.contains(linkId);
			}
		}

		public synchronized Boolean containsFerry() {
			if (this.ferryLinkIds == null) {
				return null;
			} else {
				return (this.ferryLinkIds.size() > 0);
			}
		}

		public synchronized boolean isCompatible(FreightVehicleAttributes attrs) {
			return (this.commodity == null || attrs.isCompatible(this.commodity))
					&& (this.mode == null || this.mode.equals(attrs.mode))
					&& (this.isContainer == null || this.isContainer.equals(attrs.isContainer))
					&& (this.containsFerry() == null || !this.containsFerry() || attrs.isFerryCompatible());
		}

		public synchronized boolean isCompatible(VehicleType type) {
			return this.isCompatible(FreightVehicleAttributes.getFreightAttributes(type));
		}

		@Override
		synchronized List<Object> asList() {
			return Arrays.asList(this.nodeIds, this.commodity, this.mode, this.isContainer, this.containsFerry(),
					this.linkIds);
		}
	}

	public static class ConsolidationUnitSerializer extends JsonSerializer<ConsolidationUnit> {

		ConsolidationUnitSerializer() {
		}

		@Override
		public void serialize(ConsolidationUnit consolidationUnit, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			if (consolidationUnit.nodeIds.stream().anyMatch(id -> id.toString().contains(","))) {
				throw new RuntimeException("Link IDs must not contain \",\"");
			}
			gen.writeStartObject();
			gen.writeFieldName("nodes");
			gen.writeStartArray();
			for (Id<Node> nodeId : consolidationUnit.nodeIds) {
				gen.writeString(nodeId.toString());
			}
			gen.writeEndArray();

			gen.writeStringField("commodity", consolidationUnit.commodity.toString());
			gen.writeStringField("mode", consolidationUnit.mode.toString());
			gen.writeStringField("isContainer", consolidationUnit.isContainer.toString());
			gen.writeStringField("containsFerry", consolidationUnit.containsFerry().toString());
			gen.writeFieldName("routes");
			gen.writeStartArray();
			for (List<Id<Link>> linkIds : consolidationUnit.linkIds) {
				if (linkIds.stream().anyMatch(id -> id.toString().contains(","))) {
					throw new RuntimeException("Link IDs must not contain \",\"");
				}
				gen.writeStartArray();
				for (Id<Link> linkId : linkIds) {
					gen.writeString(linkId.toString());
				}
				gen.writeEndArray();
			}
			gen.writeEndArray();
			gen.writeEndObject();
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

			ArrayNode nodesNode = (ArrayNode) node.get("nodes");
			List<Id<Node>> nodes = new ArrayList<>(nodesNode.size());
			for (JsonNode nodeNode : nodesNode) {
				nodes.add(Id.createNodeId(nodeNode.asText()));
			}

			Commodity commodity = Commodity.valueOf(((TextNode) node.get("commodity")).asText());
			TransportMode mode = TransportMode.valueOf(((TextNode) node.get("mode")).asText());
			boolean isContainer = Boolean.parseBoolean(((TextNode) node.get("isContainer")).asText());
			boolean containsFerry = Boolean.parseBoolean(((TextNode) node.get("containsFerry")).asText());

			ArrayNode routesArrayNode = (ArrayNode) node.get("routes");
			List<List<Id<Link>>> routes = new ArrayList<>(routesArrayNode.size());
			for (JsonNode routeNode : routesArrayNode) {
				ArrayNode routeArrayNode = (ArrayNode) routeNode;
				List<Id<Link>> route = new ArrayList<>(routeArrayNode.size());
				for (JsonNode linkNode : routeArrayNode) {
					route.add(Id.createLinkId(linkNode.asText()));
				}
				routes.add(route);
			}

//			ArrayNode routesNode = (ArrayNode) node.get("routes");
//			for (JsonNode routeNode : routesNode) {
//				JsonNode routeField = routeNode.get("route");
//				if (routeField == null || routeField.isNull()) {
//					linkIds.add(null);
//				} else {
//					String line = ((TextNode) routeField).asText();
//					if ("".equals(line)) {
//						linkIds.add(new ArrayList<>());
//					} else {
//						String[] idStrings = line.split(",");
//						List<Id<Link>> routeLinks = List.of(idStrings).stream().map(idStr -> Id.createLinkId(idStr))
//								.collect(Collectors.toList());
//						linkIds.add(routeLinks);
//					}
//				}
//			}

			return new ConsolidationUnit(nodes, commodity, mode, isContainer, routes);
		}
	}
}
