/**
 * se.vti.samgods.calibration.ascs
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
package se.vti.samgods.calibration.ascs;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class ASCs {

	// -------------------- MEMBERS --------------------

	private final Map<VehicleType, Double> vehicleType2ASC;

	private final Map<TransportMode, Double> mode2ASC;

	private final Map<Commodity, Double> railCommodity2ASC;

	// -------------------- CONSTRUCTION --------------------

	public ASCs() {
		this(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
	}

	public ASCs(Map<VehicleType, Double> vehicleType2ASC, Map<TransportMode, Double> mode2ASC,
			Map<Commodity, Double> railCommodity2ASC) {
		this.vehicleType2ASC = vehicleType2ASC;
		this.mode2ASC = mode2ASC;
		this.railCommodity2ASC = railCommodity2ASC;
	}

	// -------------------- IMPLEMENTATION --------------------

	public Map<VehicleType, Double> getVehicleTyp2ASC() {
		return this.vehicleType2ASC;
	}

	public Map<TransportMode, Double> getMode2ASC() {
		return this.mode2ASC;
	}

	public Map<Commodity, Double> getRailCommodity2ASC() {
		return this.railCommodity2ASC;
	}

	// -------------------- JSON (DE)SERIALIZATION --------------------

	static class Serializer extends JsonSerializer<ASCs> {

		@Override
		public void serialize(ASCs ascs, JsonGenerator jsonGenerator, SerializerProvider serializers)
				throws IOException {
			jsonGenerator.writeStartObject();

			jsonGenerator.writeFieldName("vehicleType2ASC");
			jsonGenerator.writeStartObject();
			for (Map.Entry<VehicleType, Double> entry : ascs.vehicleType2ASC.entrySet()) {
				jsonGenerator.writeObjectField(entry.getKey().getId().toString(), entry.getValue());
			}
			jsonGenerator.writeEndObject();

			jsonGenerator.writeFieldName("mode2ASC");
			jsonGenerator.writeStartObject();
			for (Map.Entry<TransportMode, Double> entry : ascs.mode2ASC.entrySet()) {
				jsonGenerator.writeNumberField(entry.getKey().toString(), entry.getValue());
			}
			jsonGenerator.writeEndObject();

			jsonGenerator.writeFieldName("railCommodity2ASC");
			jsonGenerator.writeStartObject();
			for (Map.Entry<Commodity, Double> entry : ascs.railCommodity2ASC.entrySet()) {
				jsonGenerator.writeNumberField(entry.getKey().toString(), entry.getValue());
			}
			jsonGenerator.writeEndObject();

			jsonGenerator.writeEndObject();
		}
	}

	public void writeToFile(String fileName) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(ASCs.class, new Serializer());
		mapper.registerModule(module);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(new File(fileName), this);
	}

	static class Deserializer extends JsonDeserializer<ASCs> {

		private final Vehicles vehicles;

		Deserializer(Vehicles vehicles) {
			this.vehicles = vehicles;
		}

		@Override
		public ASCs deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
			JsonNode node = jsonParser.getCodec().readTree(jsonParser);

			ASCs result = new ASCs();

			JsonNode vehicleTypeNode = node.get("vehicleType2ASC");
			if (vehicleTypeNode != null) {
				vehicleTypeNode.fields().forEachRemaining(entry -> {
					Id<VehicleType> id = Id.create(entry.getKey(), VehicleType.class);
					result.vehicleType2ASC.put(this.vehicles.getVehicleTypes().get(id), entry.getValue().asDouble());
				});
			}

			JsonNode modeNode = node.get("mode2ASC");
			if (modeNode != null) {
				modeNode.fields().forEachRemaining(entry -> {
					TransportMode mode = TransportMode.valueOf(entry.getKey());
					result.mode2ASC.put(mode, entry.getValue().asDouble());
				});
			}

			JsonNode commodityNode = node.get("railCommodity2ASC");
			if (commodityNode != null) {
				commodityNode.fields().forEachRemaining(entry -> {
					Commodity commodity = Commodity.valueOf(entry.getKey());
					result.railCommodity2ASC.put(commodity, entry.getValue().asDouble());
				});
			}

			return result;
		}
	}

	public static ASCs createFromFile(String fileName, Vehicles vehicles) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ASCs.class, new Deserializer(vehicles));
		mapper.registerModule(module);
		return mapper.readValue(new File(fileName), ASCs.class);

	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		ASCs asc = new ASCs();

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		Id<VehicleType> smallTruckId = Id.create("smalltruck", VehicleType.class);
		vehicles.addVehicleType(VehicleUtils.createVehicleType(smallTruckId));

		asc.vehicleType2ASC.put(vehicles.getVehicleTypes().get(smallTruckId), 12.34);
		asc.mode2ASC.put(TransportMode.Rail, 6789.);
		asc.railCommodity2ASC.put(Commodity.BASICMETALS, 1.);

		asc.writeToFile("ascs.json");

		ASCs ascs2 = ASCs.createFromFile("ascs.json", vehicles);

		System.out.println("... DONE");

	}

}
