/**
 * se.vti.samgods.transportation.trajectories
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
package se.vti.samgods.external.ntmcalc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;

import floetteroed.utilities.Units;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.network.SamgodsLinkAttributes;

/**
 * 
 * @author GunnarF
 *
 */

public class VehicleEpisode2NTMCalcSerializer extends JsonSerializer<VehicleEpisode> {

	private final Network network;

	VehicleEpisode2NTMCalcSerializer(Network network) {
		this.network = network;
	}

	@Override
	public void serialize(VehicleEpisode vehicleEpisode, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {

		final String globalMode = vehicleEpisode.getTransportEpisode().getMode().toString();

		gen.writeStartObject();

//		gen.writeNullField("episodeId");
		gen.writeStringField("vehicleId", vehicleEpisode.getVehicle().getId().toString());
		gen.writeStringField("vehicleType", vehicleEpisode.getVehicle().getType().getId().toString());
		gen.writeNumberField("load_ton", vehicleEpisode.getLoad_ton());

		gen.writeFieldName("links");
		gen.writeStartArray();
		for (TransportLeg leg : vehicleEpisode.getTransportEpisode().getLegs()) {
			for (Id<Link> linkId : leg.getRouteView()) {
				final Link link = this.network.getLinks().get(linkId);
				gen.writeStartObject();
				gen.writeStringField("linkId", linkId.toString());
				gen.writeNumberField("length_m", link.getLength());
				gen.writeNumberField("maxSpeed_km_h", Math.round(SamgodsLinkAttributes.getSpeed1_km_h(link)));
				if (link.getAllowedModes().size() != 1) {
					throw new RuntimeException(
							"Link " + linkId + " has not exactly one mode: " + link.getAllowedModes());
				}
				final String samgodsMode = SamgodsLinkAttributes.getSamgodsMode(link).toString();
				gen.writeStringField("mode", samgodsMode);
				if (SamgodsLinkAttributes.isFerry(link)) {
					gen.writeNumberField("vesselDWT", 5678.9); // TODO!!!
				}
				gen.writeEndObject();
			}
		}
		gen.writeEndArray();

		gen.writeEndObject();
	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {

		System.out.println("STARTED ...");

		Network network = NetworkUtils.createNetwork();
		Node node = NetworkUtils.createAndAddNode(network, Id.createNodeId("node"), new Coord());
		Link link1 = NetworkUtils.createAndAddLink(network, Id.createLinkId(1), node, node, 100, 0, 0, 0, "", "");
		Link link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId(2), node, node, 200, 0, 0, 0, "", "");
		link1.setAllowedModes(new LinkedHashSet<>(Arrays.asList(SamgodsConstants.TransportMode.Road.toString())));
		link2.setAllowedModes(new LinkedHashSet<>(Arrays.asList(SamgodsConstants.TransportMode.Ferry.toString())));

		link1.setFreespeed(Units.M_S_PER_KM_H * 60.0);
		link2.setFreespeed(Units.M_S_PER_KM_H * 100.0);

		TransportLeg leg = new TransportLeg(new OD(node.getId(), node.getId()), SamgodsConstants.TransportMode.Road);
		leg.setRoute(Arrays.asList(link1, link2));

		Vehicle veh = VehicleUtils.createVehicle(Id.createVehicleId("veh1"),
				VehicleUtils.createVehicleType(Id.create("vehType", VehicleType.class)));

		TransportEpisode episode = new TransportEpisode(SamgodsConstants.TransportMode.Road,
				SamgodsConstants.Commodity.AGRICULTURE, false);
		episode.addLeg(leg);

		VehicleEpisode vehicleEpisode = new VehicleEpisode(veh, 123.45, episode);

		List<VehicleEpisode> episodes = new ArrayList<>();
		episodes.add(vehicleEpisode);
		episodes.add(vehicleEpisode);

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

		mapper.writeValue(new File("episode.json"), episodes);

		System.out.println("... DONE");
	}
}
