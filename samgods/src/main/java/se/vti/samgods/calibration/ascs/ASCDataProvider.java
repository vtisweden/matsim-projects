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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

import se.vti.samgods.common.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class ASCDataProvider {

	// -------------------- MEMBERS --------------------

	private ConcurrentMap<VehicleType, Double> vehicleType2ASC = new ConcurrentHashMap<>();
//	private ConcurrentMap<TransportMode, Double> mode2ASC = new ConcurrentHashMap<>();
	private ConcurrentMap<Commodity, Double> railCommodity2ASC = new ConcurrentHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ASCDataProvider() {
		this(new LinkedHashMap<>(), new LinkedHashMap<>()); // , new LinkedHashMap<>());
	}

	public ASCDataProvider(Map<VehicleType, Double> vehicleType2ASC, 
//			Map<TransportMode, Double> mode2ASC,
			Map<Commodity, Double> railCommodity2ASC) {
		this.vehicleType2ASC = new ConcurrentHashMap<>(vehicleType2ASC);
//		this.mode2ASC = new ConcurrentHashMap<>( mode2ASC);
		this.railCommodity2ASC = new ConcurrentHashMap<>(railCommodity2ASC);
	}

	// -------------------- IMPLEMENTATION --------------------

	public ConcurrentMap<VehicleType, Double> getConcurrentVehicleType2ASC() {
		return this.vehicleType2ASC;
	}

//	public ConcurrentMap<TransportMode, Double> getConcurrentMode2ASC() {
//		return this.mode2ASC;
//	}

	public ConcurrentMap<Commodity, Double> getConcurrentRailCommodity2ASC() {
		return this.railCommodity2ASC;
	}

	// -------------------- JSON (DE)SERIALIZATION --------------------

	static class Serializer extends JsonSerializer<ASCDataProvider> {

		@Override
		public void serialize(ASCDataProvider ascs, JsonGenerator jsonGenerator, SerializerProvider serializers)
				throws IOException {
			jsonGenerator.writeStartObject();

			jsonGenerator.writeFieldName("vehicleType2ASC");
			jsonGenerator.writeStartObject();
			for (Map.Entry<VehicleType, Double> entry : ascs.vehicleType2ASC.entrySet()) {
				jsonGenerator.writeObjectField(entry.getKey().getId().toString(), entry.getValue());
			}
			jsonGenerator.writeEndObject();

//			jsonGenerator.writeFieldName("mode2ASC");
//			jsonGenerator.writeStartObject();
//			for (Map.Entry<TransportMode, Double> entry : ascs.mode2ASC.entrySet()) {
//				jsonGenerator.writeNumberField(entry.getKey().toString(), entry.getValue());
//			}
//			jsonGenerator.writeEndObject();

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
		module.addSerializer(ASCDataProvider.class, new Serializer());
		mapper.registerModule(module);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(new File(fileName), this);
	}

	static class Deserializer extends JsonDeserializer<ASCDataProvider> {

		private final Vehicles vehicles;

		Deserializer(Vehicles vehicles) {
			this.vehicles = vehicles;
		}

		@Override
		public ASCDataProvider deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
			JsonNode node = jsonParser.getCodec().readTree(jsonParser);

			ASCDataProvider result = new ASCDataProvider();

			JsonNode vehicleTypeNode = node.get("vehicleType2ASC");
			if (vehicleTypeNode != null) {
				vehicleTypeNode.fields().forEachRemaining(entry -> {
					Id<VehicleType> id = Id.create(entry.getKey(), VehicleType.class);
					result.vehicleType2ASC.put(this.vehicles.getVehicleTypes().get(id), entry.getValue().asDouble());
				});
			}

//			JsonNode modeNode = node.get("mode2ASC");
//			if (modeNode != null) {
//				modeNode.fields().forEachRemaining(entry -> {
//					TransportMode mode = TransportMode.valueOf(entry.getKey());
//					result.mode2ASC.put(mode, entry.getValue().asDouble());
//				});
//			}

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

	public static ASCDataProvider createFromFile(String fileName, Vehicles vehicles) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ASCDataProvider.class, new Deserializer(vehicles));
		mapper.registerModule(module);
		return mapper.readValue(new File(fileName), ASCDataProvider.class);

	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		ASCDataProvider asc = new ASCDataProvider();

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		Id<VehicleType> smallTruckId = Id.create("smalltruck", VehicleType.class);
		vehicles.addVehicleType(VehicleUtils.createVehicleType(smallTruckId));

		asc.vehicleType2ASC.put(vehicles.getVehicleTypes().get(smallTruckId), 12.34);
//		asc.mode2ASC.put(TransportMode.Rail, 6789.);
		asc.railCommodity2ASC.put(Commodity.BASICMETALS, 1.);

		asc.writeToFile("ascs.json");

		ASCDataProvider ascs2 = ASCDataProvider.createFromFile("ascs.json", vehicles);

		System.out.println("... DONE");

	}

}
