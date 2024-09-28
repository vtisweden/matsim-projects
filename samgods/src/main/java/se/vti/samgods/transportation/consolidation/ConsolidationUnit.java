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
package se.vti.samgods.transportation.consolidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

import floetteroed.utilities.Units;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.SamgodsLinkAttributes;

@JsonSerialize(using = ConsolidationUnit.Serializer.class)
@JsonDeserialize(using = ConsolidationUnit.Deserializer.class)
public class ConsolidationUnit {

	// -------------------- CONSTANTS --------------------

	// "Poison pill" for multithreaded routing.
	public static final ConsolidationUnit TERMINATE = new ConsolidationUnit(new ArrayList<>(0), null, null, null);

	// -------------------- MEMBERS --------------------

	public final CopyOnWriteArrayList<Id<Node>> nodeIds;
	public final SamgodsConstants.Commodity commodity;
	public final SamgodsConstants.TransportMode samgodsMode;
	public final Boolean isContainer;

	public CopyOnWriteArrayList<CopyOnWriteArrayList<Id<Link>>> linkIds = null;
	public Double length_km = null;
	public Boolean containsFerry = null;
	public Double domesticLength_km = null;

	// --------------------CONSTRUCTION --------------------

	private ConsolidationUnit(List<Id<Node>> nodes, SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer) {
		this.nodeIds = new CopyOnWriteArrayList<>(nodes);
		this.commodity = commodity;
		this.samgodsMode = mode;
		this.isContainer = isContainer;
	}

	public static List<ConsolidationUnit> createUnrouted(TransportEpisode episode) {
		if (episode.getLegODs() == null) {
			return Collections.emptyList();
		} else {
			if (episode.getMode().equals(TransportMode.Rail) && (episode.getLegODs().size() > 1)) {
				return episode
						.getLegODs().stream().map(od -> new ConsolidationUnit(extractNodes(Arrays.asList(od)),
								episode.getCommodity(), episode.getMode(), episode.isContainer()))
						.collect(Collectors.toList());
			} else {
				return Arrays.asList(new ConsolidationUnit(extractNodes(episode.getLegODs()), episode.getCommodity(),
						episode.getMode(), episode.isContainer()));
			}
		}
	}

	public ConsolidationUnit createRoutingEquivalentTemplate() {
		return new ConsolidationUnit(this.nodeIds, this.commodity, this.samgodsMode, this.isContainer);
	}

	// -------------------- INTERNALS --------------------

	private static List<Id<Node>> extractNodes(List<OD> legODs) {
		final ArrayList<Id<Node>> nodeIds = new ArrayList<>(legODs.size() + 1);
		legODs.stream().map(l -> l.origin).forEach(n -> nodeIds.add(n));
		nodeIds.add(legODs.get(legODs.size() - 1).destination);
		return nodeIds;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setRoutes(List<List<Link>> routes) {
		if (routes == null) {
			this.linkIds = null;
			this.length_km = null;
			this.containsFerry = null;
			this.domesticLength_km = null;
		} else {
			final List<CopyOnWriteArrayList<Id<Link>>> tmpLinkIds = new ArrayList<>(routes.size());
			double length_m = 0.0;
			double domesticLength_m = 0.0;
			this.containsFerry = false;
			for (List<Link> route : routes) {
				tmpLinkIds.add(new CopyOnWriteArrayList<>(route.stream().map(l -> l.getId()).toList()));
				length_m += route.stream().mapToDouble(l -> l.getLength()).sum();
				domesticLength_m += route.stream()
						.filter(l -> ((SamgodsLinkAttributes) l.getAttributes()
								.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).isDomestic)
						.mapToDouble(l -> l.getLength()).sum();
				this.containsFerry = this.containsFerry || route.stream().anyMatch(l -> ((SamgodsLinkAttributes) l
						.getAttributes().getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).samgodsMode.isFerry());
			}
			this.linkIds = new CopyOnWriteArrayList<CopyOnWriteArrayList<Id<Link>>>(tmpLinkIds);
			this.length_km = Units.KM_PER_M * length_m;
			this.domesticLength_km = Units.KM_PER_M * domesticLength_m;
		}
	}

	public void setRouteIds(List<List<Id<Link>>> routeIds) {
		this.length_km = null;
		this.containsFerry = null;
		this.domesticLength_km = null;
		if (routeIds == null) {
			this.linkIds = null;
		} else {
			final List<CopyOnWriteArrayList<Id<Link>>> tmpLinkIds = new ArrayList<>(routeIds.size());
			for (List<Id<Link>> route : routeIds) {
				tmpLinkIds.add(new CopyOnWriteArrayList<>(route));
			}
			this.linkIds = new CopyOnWriteArrayList<CopyOnWriteArrayList<Id<Link>>>(tmpLinkIds);
		}
	}

	public void computeNetworkCharacteristics(Network network) {
		this.length_km = Units.KM_PER_M * this.linkIds.stream().flatMap(ll -> ll.stream())
				.mapToDouble(l -> network.getLinks().get(l).getLength()).sum();
		this.domesticLength_km = Units.KM_PER_M
				* this.linkIds.stream().flatMap(ll -> ll.stream()).map(id -> network.getLinks().get(id))
						.filter(l -> ((SamgodsLinkAttributes) l.getAttributes()
								.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).isDomestic)
						.mapToDouble(l -> l.getLength()).sum();
		this.containsFerry = this.linkIds.stream().flatMap(ll -> ll.stream())
				.anyMatch(l -> ((SamgodsLinkAttributes) network.getLinks().get(l).getAttributes()
						.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).samgodsMode.isFerry());
	}

	// -------------------- OVERRIDING Object --------------------

	private List<Object> createAsList() {
		return Arrays.asList(this.nodeIds, this.commodity, this.samgodsMode, this.isContainer, this.linkIds);
	}

	@Override
	public int hashCode() {
		return this.createAsList().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other instanceof ConsolidationUnit) {
			return this.createAsList().equals(((ConsolidationUnit) other).createAsList());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.createAsList().toString();
	}

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
			gen.writeStringField("mode", consolidationUnit.samgodsMode.toString());
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

			final ConsolidationUnit result = new ConsolidationUnit(nodes, commodity, mode, isContainer);
			result.setRouteIds(routes);
			return result;
		}
	}
}