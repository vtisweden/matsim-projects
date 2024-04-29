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
package se.vti.samgods.transportation.ntmcalc;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportLeg;

/**
 * 
 * @author GunnarF
 *
 */
public class Leg2NTMCalcSerializer extends JsonSerializer<TransportLeg> {

	private final JsonSerializer<List<Link>> linkListSerializer = new JsonSerializer<>() {

		@Override
		public void serialize(List<Link> linkList, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeFieldName("links");
			gen.writeStartArray();
			for (Link link : linkList) {
				gen.writeStartObject();
				gen.writeStringField("link", link.getId().toString());
				gen.writeNumberField("length_m", link.getLength());

				gen.writeFieldName("modes");
				gen.writeStartArray();
				for(String mode : link.getAllowedModes()) {
					gen.writeString(mode);
				}
				gen.writeEndArray();
				
				//				gen.writeArrayFieldStart("modes");
//				serializers.defaultSerializeValue(link.getAllowedModes().stream().toArray(), gen);

				
				gen.writeEndObject();

			}
			gen.writeEndArray();
		}
		
	};
	
	public Leg2NTMCalcSerializer() {
	}

	@Override
	public void serialize(TransportLeg leg, JsonGenerator gen, SerializerProvider serializers) throws IOException {

		gen.writeStartObject(); 

		gen.writeNullField("legId");
		gen.writeStringField("originNode", leg.getOrigin().toString());
		gen.writeStringField("destinationNode", leg.getDestination().toString());
		gen.writeStringField("mode", leg.getMode().toString());

		this.linkListSerializer.serialize(leg.getRouteView(), gen, serializers);

		gen.writeEndObject(); 
	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {

		System.out.println("STARTED ...");

		Network network = NetworkUtils.createNetwork();
		Node node = NetworkUtils.createAndAddNode(network, Id.createNodeId("node"), new Coord());
		Link link1 = NetworkUtils.createAndAddLink(network, Id.createLinkId(1), node, node, 100, 0, 0, 0, "", "");
		Link link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId(2), node, node, 200, 0, 0, 0, "", "");
		link2.setAllowedModes(new LinkedHashSet<>(Arrays.asList("car", "ferry")));
		
		TransportLeg leg = new TransportLeg(new OD(node.getId(), node.getId()), SamgodsConstants.TransportMode.Road,
				'?');
		leg.setRoute(Arrays.asList(link1, link2));

		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File("transportLeg.json"), leg);

		System.out.println("... DONE");
	}

}
