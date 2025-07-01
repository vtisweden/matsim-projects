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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripJsonIO {

	public static class Serializer extends JsonSerializer<RoundTrip<Node>> {
		@Override
		public void serialize(RoundTrip<Node> value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeStartObject();
			gen.writeFieldName("nodes");
			gen.writeStartArray();
			for (Node location : value.getLocationsView()) {
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

	public static class Deserializer extends JsonDeserializer<RoundTrip<Node>> {

		private final Scenario<Node> scenario;

		public Deserializer(Scenario<Node> scenario) {
			this.scenario = scenario;
		}

		@Override
		public RoundTrip<Node> deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			JsonNode node = p.getCodec().readTree(p);

			List<Node> locations = new ArrayList<>();
			JsonNode nodes = node.get("nodes");
			for (JsonNode n : nodes) {
				String name = n.asText();
				locations.add(scenario.getOrCreateLocationWithSameName(new Node(name)));
			}

			List<Integer> departures = new ArrayList<>();
			JsonNode dptBins = node.get("dptBins");
			for (JsonNode dpt : dptBins) {
				departures.add(dpt.asInt());
			}

			return new RoundTrip<>(locations, departures);
		}

	}

	public static void writeToFile(RoundTrip<Node> roundTrip, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		SimpleModule module = new SimpleModule();
		module.addSerializer((Class) RoundTrip.class, new Serializer());
		mapper.registerModule(module);
		mapper.writeValue(new File(fileName), roundTrip);
	}

	public static RoundTrip<Node> readFromFile(Scenario<Node> scenario, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(RoundTrip.class, new Deserializer(scenario));
		mapper.registerModule(module);
		ObjectReader reader = mapper.readerFor(RoundTrip.class);
		JsonParser parser = mapper.getFactory().createParser(new File(fileName));
		RoundTrip<Node> result = reader.readValue(parser);
		parser.close();
		return result;
	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		Scenario<Node> scenario = new Scenario<>();
		Node home = scenario.getOrCreateLocationWithSameName(new Node("home"));
		Node work = scenario.getOrCreateLocationWithSameName(new Node("work"));
		RoundTrip<Node> roundTrip = new RoundTrip<>(Arrays.asList(home, work), Arrays.asList(7, 18));
		writeToFile(roundTrip, "test.json");
		roundTrip = null;
		roundTrip = readFromFile(scenario, "test.json");
		System.out.println(roundTrip);
	}

}
