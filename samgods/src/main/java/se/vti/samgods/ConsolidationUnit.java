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

@JsonSerialize(using = ConsolidationUnit.Serializer.class)
@JsonDeserialize(using = ConsolidationUnit.Deserializer.class)
public class ConsolidationUnit {

	// -------------------- MEMBERS --------------------

	// TODO synchronize
	public final List<Id<Node>> nodeIds;
	public final SamgodsConstants.Commodity commodity;
	public final SamgodsConstants.TransportMode mode;
	public final Boolean isContainer;

	// TODO synchronize
	public List<List<Id<Link>>> linkIds = null;
	public Double length_m = null;
	public Boolean containsFerry = null;

//	public synchronized List<Id<Node>> getNodesCopy() {
//		return this.nodeIds.stream().toList();
//	}
//
//	public synchronized boolean isRouted() {
//		return (this.linkIds != null);
//	}
//
//	public synchronized List<List<Id<Link>>> getRoutesCopy() {
//		List<List<Id<Link>>> result = new ArrayList<>(this.linkIds.size());
//		for (List<Id<Link>> route : this.linkIds) {
//			result.add(route.stream().toList());
//		}
//		return result;
//	}

	// --------------------CONSTRUCTION --------------------

	private ConsolidationUnit(List<Id<Node>> nodes, SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer, List<List<Id<Link>>> linkIds) {
		this.nodeIds = nodes;
//		if (nodes == null) {
//			this.nodeIds = null;
//		} else {
//			this.nodeIds = Collections.synchronizedList(nodes);
//		}
		this.commodity = commodity;
		this.mode = mode;
		this.isContainer = isContainer;
		this.linkIds = linkIds;
//		if (linkIds == null) {
//			this.linkIds = null;
//		} else {
//			this.linkIds = Collections.synchronizedList(new ArrayList<>(linkIds.size()));
//			for (List<Id<Link>> linkIdSegment : linkIds) {
//				this.linkIds.add(Collections.synchronizedList(linkIdSegment));
//			}
//		}
	}

	public synchronized static List<ConsolidationUnit> createUnrouted(TransportEpisode episode) {
		if (episode.getLegs() == null) {
			return Collections.emptyList();
		} else {
			if (episode.getMode().equals(TransportMode.Rail) && (episode.getLegs().size() > 1)) {
				return episode
						.getLegs().stream().map(leg -> new ConsolidationUnit(extractNodes(Arrays.asList(leg)),
								episode.getCommodity(), episode.getMode(), episode.isContainer(), null))
						.collect(Collectors.toList());
			} else {
				return Arrays.asList(new ConsolidationUnit(extractNodes(episode.getLegs()), episode.getCommodity(),
						episode.getMode(), episode.isContainer(), null));
			}
		}
	}

	public synchronized ConsolidationUnit createRoutingEquivalentTemplate() {
		return new ConsolidationUnit(this.nodeIds, this.commodity, this.mode, this.isContainer, null);
	}

	// -------------------- INTERNALS --------------------

	private synchronized static List<Id<Node>> extractNodes(List<TransportLeg> legs) {
		final ArrayList<Id<Node>> nodes = new ArrayList<>(legs.size() + 1);
		legs.stream().map(l -> l.getOrigin()).forEach(n -> nodes.add(n));
		nodes.add(legs.get(legs.size() - 1).getDestination());
		return nodes;
	}

	private synchronized List<Object> createAsList() {
		return Arrays.asList(this.nodeIds, this.commodity, this.mode, this.isContainer, this.linkIds);
	}

	// -------------------- OVERRIDING Object --------------------

	@Override
	public synchronized int hashCode() {
		return this.createAsList().hashCode();
	}

	@Override
	public synchronized boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other instanceof ConsolidationUnit) {
			return this.createAsList().equals(((ConsolidationUnit) other).createAsList());
		} else {
			return false;
		}
	}

	@Override
	public synchronized String toString() {
		return this.createAsList().toString();
	}

	// -------------------- IMPLEMENTATION --------------------

	public synchronized void setRoutes(List<List<Link>> routes) {
		if (routes == null) {
			this.linkIds = null;
			this.length_m = null;
			this.containsFerry = null;
		} else {
			this.linkIds = new ArrayList<>(routes.size());
			this.length_m = 0.0;
			this.containsFerry = false;
			for (List<Link> route : routes) {
				this.linkIds.add(route.stream().map(l -> l.getId()).toList());
				this.length_m += route.stream().mapToDouble(l -> l.getLength()).sum();
				this.containsFerry = this.containsFerry || route.stream().anyMatch(l -> LinkAttributes.isFerrySynchronized(l));
			}
		}
	}

	public synchronized void computeNetworkCharacteristics(Network network) {
		this.length_m = this.linkIds.stream().flatMap(ll -> ll.stream())
				.mapToDouble(l -> network.getLinks().get(l).getLength()).sum();
		this.containsFerry = this.linkIds.stream().flatMap(ll -> ll.stream())
				.anyMatch(l -> LinkAttributes.isFerrySynchronized(network.getLinks().get(l)));
	}

//	public synchronized boolean isCompatible(FreightVehicleAttributes attrs) {
//		return (this.commodity == null || attrs.isCompatible(this.commodity))
//				&& (this.mode == null || this.mode.equals(attrs.mode))
//				&& (this.isContainer == null || this.isContainer.equals(attrs.isContainer));
//	}
//
//	public synchronized boolean isCompatible(VehicleType type) {
//		return this.isCompatible(FreightVehicleAttributes.getFreightAttributes(type));
//	}

	// -------------------- Json Serializer --------------------

	public static class Serializer extends JsonSerializer<ConsolidationUnit> {

		Serializer() {
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

	// -------------------- Json Deserializer --------------------

	public static class Deserializer extends JsonDeserializer<ConsolidationUnit> {

		public Deserializer() {
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

			ArrayNode routesArrayNode = (ArrayNode) node.get("routes");
			List<List<Id<Link>>> routes = new ArrayList<>(routesArrayNode.size());
			for (JsonNode routeNode : routesArrayNode) {
				ArrayNode routeArrayNode = (ArrayNode) routeNode;
				List<Id<Link>> route = new ArrayList<>(routeArrayNode.size());
				for (JsonNode linkNode : routeArrayNode) {
					final Id<Link> linkId = Id.createLinkId(Id.createLinkId(linkNode.asText()));
					route.add(linkId);
				}
				routes.add(route);
			}

			return new ConsolidationUnit(nodes, commodity, mode, isContainer, routes);
		}
	}

}