/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsConstants {

	public static enum TransportMode {
		Road, Rail, Sea, Ferry, Air
	};

	public static final Map<SamgodsConstants.TransportMode, String> samgodsMode2matsimMode;

//	public static final Map<SamgodsConstants.TransportMode, Double> samgodsMode2fallbackSpeed_km_h;

	static {
		samgodsMode2matsimMode = new ConcurrentHashMap<>(SamgodsConstants.TransportMode.values().length);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Road, org.matsim.api.core.v01.TransportMode.car);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Ferry,
				org.matsim.api.core.v01.TransportMode.car /* Assume only car ferries */);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Rail, org.matsim.api.core.v01.TransportMode.train);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Sea, org.matsim.api.core.v01.TransportMode.ship);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Air, org.matsim.api.core.v01.TransportMode.airplane);
		assert (samgodsMode2matsimMode.size() == SamgodsConstants.TransportMode.values().length);

//		samgodsMode2fallbackSpeed_km_h = new ConcurrentHashMap<>(SamgodsConstants.TransportMode.values().length);
//		samgodsMode2fallbackSpeed_km_h.put(SamgodsConstants.TransportMode.Road, 60.0);
//		samgodsMode2fallbackSpeed_km_h.put(SamgodsConstants.TransportMode.Ferry, 10.0);
//		samgodsMode2fallbackSpeed_km_h.put(SamgodsConstants.TransportMode.Rail, 80.0);
//		samgodsMode2fallbackSpeed_km_h.put(SamgodsConstants.TransportMode.Sea, 15.0);
//		samgodsMode2fallbackSpeed_km_h.put(SamgodsConstants.TransportMode.Air, 800.0);
//		assert (samgodsMode2fallbackSpeed_km_h.size() == SamgodsConstants.TransportMode.values().length);

	}

	public static enum Commodity {

		AGRICULTURE(1, "Products of agriculture, forestry and fishing"),
		COAL(2, "Coal and lignite; crude petroleum and natural gas"),
		METAL(3, "Metal ores and other mining and quarrying products"), FOOD(4, "Food products, beverages and tobacco"),
		TEXTILES(5, "Textiles and textile products; leather and leather products"),
		WOOD(6, "Wood and products of wood; pulp, paper and paper products; printed matter and recorded media"),
		COKE(7, "Coke and refined petroleum products"),
		CHEMICALS(8, "Chemicals, chemical products, and man-made fibers; rubber and plastic products; nuclear fuel"),
		OTHERMINERAL(9, "Other non-metallic mineral products"),
		BASICMETALS(10, "Basic metals; fabricated metal products, except machinery and equipment"),
		MACHINERY(11, "Machinery and equipment"), TRANSPORT(12, "Transport equipment"),
		FURNITURE(13, "Furniture; other manufactured goods n.e.c."),
		SECONDARYRAW(14, "Secondary raw materials; municipal wastes and other wastes"), TIMBER(15, "Timber"),
		AIR(16, "Air freight (fractions of other commodities)");

		public final int code;
		public final String description;

		public String twoDigitCode() {
			return (this.code < 10) ? ("0" + this.code) : ("" + this.code);
		}

		private Commodity(int code, String name) {
			this.code = code;
			this.description = name;
		}
	};

}
