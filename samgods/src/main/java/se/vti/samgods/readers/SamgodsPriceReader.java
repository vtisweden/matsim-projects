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
package se.vti.samgods.readers;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.pricing.ProportionalLinkPrices;
import se.vti.samgods.transportation.pricing.ProportionalNodePrices;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsPriceReader {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(SamgodsPriceReader.class);

	// -------------------- MEMBERS --------------------

	private final Map<Commodity, Map<TransportMode, ProportionalLinkPrices>> commodity2mode2shipmentPrices = new LinkedHashMap<>(
			Commodity.values().length);

	private final Map<Commodity, ProportionalNodePrices> commodity2transshipmentPrices = new LinkedHashMap<>(
			Commodity.values().length);

	// -------------------- CONSTRUCTION --------------------

	public SamgodsPriceReader(String linkPriceFile, String nodePriceFile, String nodeDurationFile) {

		{
			log.info("Loading samgods shipment (link) prices: " + linkPriceFile);
			final TabularFileHandler handler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final TransportMode mode = TransportMode.valueOf(this.getStringValue("mainMode"));
					final double price_1_tonKm = this.getDoubleValue("prisPerKmOchTon");
					final Commodity commodity = Commodity.values()[this.getIntValue("varugrupp") - 1];
					getOrShipmentCreatePrices(commodity, mode).setMovePrice_1_kmH(price_1_tonKm);
				}
			};
			final TabularFileParser parser = new TabularFileParser();
			parser.setDelimiterTags(new String[] { "," });
			parser.setOmitEmptyColumns(false);
			try {
				parser.parse(linkPriceFile, handler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		{
			log.info("Loading samgods transshipment (node) prices: " + nodePriceFile);
			final TabularFileHandler handler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final String from = this.getStringValue("fromMode");
					final String to = this.getStringValue("fromMode");
					final double price_ton = this.getDoubleValue("prisPerTon");
					final Commodity commodity = Commodity.values()[this.getIntValue("varugrupp") - 1];

					if ("Zone".equals(from)) {
						getOrShipmentCreatePrices(commodity, TransportMode.valueOf(to))
								.setLoadingPrice_1_ton(price_ton);
					} else if ("Zone".equals(to)) {
						getOrShipmentCreatePrices(commodity, TransportMode.valueOf(from))
								.setUnloadingPrice_1_ton(price_ton);
					} else {
						getOrCreateTransshipmentPrices(commodity).setPrice_1_ton(TransportMode.valueOf(from),
								TransportMode.valueOf(to), price_ton);
					}
				}
			};
			final TabularFileParser parser = new TabularFileParser();
			parser.setDelimiterTags(new String[] { "," });
			parser.setOmitEmptyColumns(false);
			try {
				parser.parse(nodePriceFile, handler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		{
			log.info("Loading samgods transshipment (node) durations: " + nodeDurationFile);
			final TabularFileHandler handler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final String from = this.getStringValue("fromMode");
					final String to = this.getStringValue("fromMode");
					final double duration_min = this.getDoubleValue("tid");
					final Commodity commodity = Commodity.values()[this.getIntValue("varugrupp") - 1];

					if ("Zone".equals(from)) {
						getOrShipmentCreatePrices(commodity, TransportMode.valueOf(to))
								.setLoadingDuration_min(duration_min);
					} else if ("Zone".equals(to)) {
						getOrShipmentCreatePrices(commodity, TransportMode.valueOf(from))
								.setUnloadingDuration_min(duration_min);
					} else {
						getOrCreateTransshipmentPrices(commodity).setDuration_min(TransportMode.valueOf(from),
								TransportMode.valueOf(to), duration_min);
					}
				}
			};
			final TabularFileParser parser = new TabularFileParser();
			parser.setDelimiterTags(new String[] { "," });
			parser.setOmitEmptyColumns(false);
			try {
				parser.parse(nodeDurationFile, handler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	// -------------------- INTERNALS --------------------

	ProportionalLinkPrices getOrShipmentCreatePrices(Commodity commodity, TransportMode mode) {
		return this.commodity2mode2shipmentPrices.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new ProportionalLinkPrices(commodity, mode));
	}

	ProportionalNodePrices getOrCreateTransshipmentPrices(Commodity commodity) {
		return this.commodity2transshipmentPrices.computeIfAbsent(commodity,
				c -> new ProportionalNodePrices(commodity));
	}

	// -------------------- IMPLEMENTATION --------------------

}
