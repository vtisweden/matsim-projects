/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.external.rail;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;

public class ChainAndAnnualShipmentJsonSerializer extends JsonSerializer<ChainAndShipmentSize> {

		@Override
		public void serialize(ChainAndShipmentSize chainAndShipment, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {

			gen.writeStartObject();
			gen.writeStringField("commodity", chainAndShipment.transportChain.getCommodity().toString());
			gen.writeNumberField("amount_ton_yr", chainAndShipment.annualShipment.getTotalAmount_ton());
			gen.writeStringField("originNode", chainAndShipment.transportChain.getOD().origin.toString());
			gen.writeStringField("destinationNode", chainAndShipment.transportChain.getOD().toString());

			gen.writeFieldName("episodes");
			gen.writeStartArray();
			for (TransportEpisode episode : chainAndShipment.transportChain.getEpisodes()) {
				gen.writeStartObject();
				gen.writeStringField("loadingNode", episode.getLoadingNodeId().toString());
				gen.writeStringField("unloadingNode", episode.getUnloadingNodeId().toString());
				gen.writeStringField("mainTransportMode", episode.getMode().toString());
				gen.writeEndObject();
			}
			gen.writeEndArray();

			gen.writeEndObject();
		}

//		public static void writeToFile(List<AnnualShipment> shipments, String fileName) {
//			ObjectMapper mapper = new ObjectMapper();
//			mapper.enable(SerializationFeature.INDENT_OUTPUT);
//			mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
//			try {
//				mapper.writeValue(new File(fileName), shipments);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}