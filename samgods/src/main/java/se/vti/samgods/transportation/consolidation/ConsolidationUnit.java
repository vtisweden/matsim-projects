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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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

import floetteroed.utilities.Units;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.fleet.FleetData;

@JsonSerialize(using = ConsolidationUnit.Serializer.class)
@JsonDeserialize(using = ConsolidationUnit.Deserializer.class)
public class ConsolidationUnit {

	// -------------------- CONSTANTS --------------------

	// "Poison pill" for multithreaded routing.
	public static final ConsolidationUnit TERMINATE = new ConsolidationUnit(null, null, null, null);

	// -------------------- MEMBERS --------------------

	public final OD od;
	public final SamgodsConstants.Commodity commodity;
	public final SamgodsConstants.TransportMode samgodsMode;
	public final Boolean isContainer;

	public CopyOnWriteArrayList<Id<Link>> linkIds = null;
	public Double length_km = null;
	public Double domesticLength_km = null;
	public Boolean containsFerry = null;

	public CopyOnWriteArraySet<VehicleType> linkCompatibleVehicleTypes = null;

	// --------------------CONSTRUCTION --------------------

	private ConsolidationUnit(OD od, SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode,
			Boolean isContainer) {
		this.od = od;
		this.commodity = commodity;
		this.samgodsMode = mode;
		this.isContainer = isContainer;
	}

	public static List<ConsolidationUnit> createUnrouted(TransportEpisode episode) {
		if (episode.getSegmentODs() == null) {
			return Collections.emptyList();
		} else {
			return episode.getSegmentODs().stream().map(
					od -> new ConsolidationUnit(od, episode.getCommodity(), episode.getMode(), episode.isContainer()))
					.collect(Collectors.toList());
		}
	}

	public ConsolidationUnit createRoutingEquivalentTemplate() {
		return new ConsolidationUnit(this.od, this.commodity, this.samgodsMode, this.isContainer);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setRoutes(List<Link> route, NetworkData networkData, FleetData fleetData) {
		if (route == null) {
			this.linkIds = null;
			this.length_km = null;
			this.domesticLength_km = null;
			this.containsFerry = null;
			this.linkCompatibleVehicleTypes = null;
		} else {
			this.linkIds = new CopyOnWriteArrayList<Id<Link>>(route.stream().map(l -> l.getId()).toList());
			this.computeNetworkCharacteristics(networkData, fleetData);
		}
	}

	// TODO compute network characteristics here
	public void setRouteIds(List<Id<Link>> routeIds) {
		this.length_km = null;
		this.domesticLength_km = null;
		this.containsFerry = null;
		this.linkCompatibleVehicleTypes = null;
		if (routeIds == null) {
			this.linkIds = null;
		} else {
			this.linkIds = new CopyOnWriteArrayList<>(routeIds);
		}
	}

	// TODO make private
	public void computeNetworkCharacteristics(NetworkData networkData, FleetData fleetData) {
		this.length_km = Units.KM_PER_M
				* this.linkIds.stream().mapToDouble(lid -> networkData.getLinks().get(lid).getLength()).sum();
		this.domesticLength_km = Units.KM_PER_M * this.linkIds.stream()
				.mapToDouble(lid -> networkData.getDomesticLinkIds().contains(lid)
						? networkData.getLinks().get(lid).getLength()
						: 0.0)
				.sum();
		this.containsFerry = this.linkIds.stream().anyMatch(lid -> networkData.getFerryLinkIds().contains(lid));
		this.linkCompatibleVehicleTypes = new CopyOnWriteArraySet<>(fleetData.computeLinkCompatibleVehicleTypes(this));
	}

	public List<? extends Link> allLinks(NetworkData networkData) {
		return this.linkIds.stream().map(lid -> networkData.getLinks().get(lid)).toList();
	}

	// -------------------- OVERRIDING Object --------------------

	private List<Object> createAsList() {
		return Arrays.asList(this.od, this.commodity, this.samgodsMode, this.isContainer, this.linkIds);
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
		final List<String> content = new LinkedList<>();
		content.add("commodity=" + this.commodity);
		content.add("isContainer=" + this.isContainer);
		content.add("mode=" + this.samgodsMode);
		content.add("containsFerry=" + this.containsFerry);
		content.add("od=" + this.od);
		content.add("length=" + this.length_km + "km");
		content.add("domesticLength=" + this.domesticLength_km + "km");
		content.add("numberOfRouteLinks=" + (this.linkIds != null ? this.linkIds.stream().count() : null));
		return this.getClass().getSimpleName() + "[" + content.stream().collect(Collectors.joining(",")) + "]";
	}

	// -------------------- Json Serializer --------------------

	public static class Serializer extends JsonSerializer<ConsolidationUnit> {

		Serializer() {
		}

		@Override
		public void serialize(ConsolidationUnit consolidationUnit, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeStartObject();
			gen.writeStringField("origin", consolidationUnit.od.origin.toString());
			gen.writeStringField("destination", consolidationUnit.od.destination.toString());
			gen.writeStringField("commodity", consolidationUnit.commodity.toString());
			gen.writeStringField("mode", consolidationUnit.samgodsMode.toString());
			gen.writeStringField("isContainer", consolidationUnit.isContainer.toString());
			gen.writeFieldName("links");
			gen.writeStartArray();
			for (Id<Link> linkId : consolidationUnit.linkIds) {
				gen.writeString(linkId.toString());
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

			Id<Node> origin = Id.createNodeId(((TextNode) node.get("origin")).asText());
			Id<Node> destination = Id.createNodeId(((TextNode) node.get("destination")).asText());
			OD od = new OD(origin, destination);
			Commodity commodity = Commodity.valueOf(((TextNode) node.get("commodity")).asText());
			TransportMode mode = TransportMode.valueOf(((TextNode) node.get("mode")).asText());
			boolean isContainer = Boolean.parseBoolean(((TextNode) node.get("isContainer")).asText());

			ArrayNode linksArrayNode = (ArrayNode) node.get("links");
			List<Id<Link>> route = new ArrayList<>(linksArrayNode.size());
			for (JsonNode linkNode : linksArrayNode) {
				final Id<Link> linkId = Id.createLinkId(linkNode.asText());
				route.add(linkId);
			}

			final ConsolidationUnit result = new ConsolidationUnit(od, commodity, mode, isContainer);
			result.setRouteIds(route);
			return result;
		}
	}
}