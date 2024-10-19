/**
 * se.vti.roundtrips.single
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
package se.vti.roundtrips.single;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripJsonIO {
	
	public static class Serializer extends JsonSerializer<RoundTrip<? extends Location>> {
		@Override
		public void serialize(RoundTrip<? extends Location> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartObject();
			gen.writeFieldName("nodes");
			gen.writeStartArray();
			for (Location location : value.getLocationsView()) {
				gen.writeString(location.getName());
			}
			gen.writeEndArray();
			gen.writeFieldName("dptBins");
			gen.writeStartArray();
			for (Integer departure : value.getDeparturesView()) {
				gen.writeNumber(departure);
			}
			gen.writeEndArray();
			gen.writeEndObject();
		}
	}

	public static void writeToFile(RoundTrip<? extends Location> roundTrip, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		SimpleModule module = new SimpleModule();
		module.addSerializer((Class) RoundTrip.class, new Serializer());
		mapper.registerModule(module);
		mapper.writeValue(new File(fileName), roundTrip);
	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		RoundTrip<? extends Location> roundTrip = new RoundTrip<>(
				Arrays.asList(new Location("home"), new Location("work")), Arrays.asList(7, 18));
		writeToFile(roundTrip, "test.json");
		System.out.println("DONE");
	}

}
