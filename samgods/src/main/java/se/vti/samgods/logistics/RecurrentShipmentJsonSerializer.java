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
package se.vti.samgods.logistics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * 
 * @author GunnarF
 *
 */
public class RecurrentShipmentJsonSerializer extends JsonSerializer<RecurrentShipment> {

	@Override
	public void serialize(RecurrentShipment shipment, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {

		gen.writeStartObject();
		gen.writeStringField("commodity", shipment.getCommmodity().toString());
		gen.writeNumberField("amount_ton_yr", shipment.getSize_ton());
		gen.writeNumberField("frequency_1_yr", shipment.getFrequency_1_yr());
		gen.writeStringField("originNode", shipment.getTransportChain().getOriginNodeId().toString());
		gen.writeStringField("destinationNode", shipment.getTransportChain().getDestinationNodeId().toString());

		gen.writeFieldName("episodes");
		gen.writeStartArray();
		for (TransportEpisode episode : shipment.getTransportChain().getEpisodes()) {
			gen.writeStartObject();
			gen.writeStringField("loadingNode", episode.getLoadingNode().toString());
			gen.writeStringField("unloadingNode", episode.getUnloadingNode().toString());
			gen.writeStringField("mainTransportMode", episode.getMode().toString());

			gen.writeFieldName("legs");
			gen.writeStartArray();
			for (TransportLeg leg : episode.getLegs()) {
				gen.writeStartObject();
				gen.writeStringField("startNode", leg.getOD().origin.toString());
				gen.writeStringField("endNode", leg.getOD().destination.toString());
				gen.writeStringField("transportMode", leg.getMode().toString());
				gen.writeEndObject();
			}
			gen.writeEndArray();

			gen.writeEndObject();
		}
		gen.writeEndArray();

		gen.writeEndObject();
	}

	public static void writeToFile(List<RecurrentShipment> shipments, String fileName) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		try {
			mapper.writeValue(new File(fileName), shipments);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
