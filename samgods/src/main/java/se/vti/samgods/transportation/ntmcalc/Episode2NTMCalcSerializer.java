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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;

/**
 * 
 * @author GunnarF
 *
 */
public class Episode2NTMCalcSerializer extends JsonSerializer<TransportEpisode> {

	// TODO Continue here. Add freight annotation.
	
	@Override
	public void serialize(TransportEpisode episode, JsonGenerator gen, SerializerProvider serializers) throws IOException {

		gen.writeStartObject(); 

		gen.writeNullField("episodeId");
		gen.writeStringField("mode", episode.getMode().toString());

		gen.writeFieldName("legs");
		gen.writeStartArray();
		for (TransportLeg leg : episode.getLegs()) {
			gen.writeObject(leg);
		}
		gen.writeEndArray();

		gen.writeEndObject(); 


	}

}
