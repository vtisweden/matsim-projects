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
package se.vti.samgods.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.utils.misc.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import se.vti.utils.misc.tabularfileparser.TabularFileParser;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = LogManager.getLogger(ChainChoiReader.class);

	private final static String Key = "Key";
	private final static String NRelations = "NRelations";
	private final static String AnnualVolumeTonnesPerRelation = "AnnualVolume_(Tonnes)";
	private final static String Prob = "Prob";
	private final static String ChainType = "ChainType";

	private final static String Orig = "Orig";
	private final static String Dest = "Dest";
	private final static String Orig2 = "Orig2";
	private final static String Orig3 = "Orig3";
	private final static String Orig4 = "Orig4";
	private final static String Orig5 = "Orig5";
	private final static List<List<String>> originsColumnsByChainLength = Arrays.asList(null, Arrays.asList(Orig),
			Arrays.asList(Orig, Orig2), Arrays.asList(Orig, Orig2, Orig3), Arrays.asList(Orig, Orig2, Orig3, Orig4),
			Arrays.asList(Orig, Orig2, Orig3, Orig4, Orig5));
	private final static List<List<String>> destinationColumnsByChainLength = Arrays.asList(null, Arrays.asList(Dest),
			Arrays.asList(Orig2, Dest), Arrays.asList(Orig2, Orig3, Dest), Arrays.asList(Orig2, Orig3, Orig4, Dest),
			Arrays.asList(Orig2, Orig3, Orig4, Orig5, Dest));

	private static final char SamgodsRoadFerryMode = 'P';
	private static final char SamgodsRailFerryMode = 'Q';
	private static final List<Character> SamgodsFerryModes = Collections
			.unmodifiableList(Arrays.asList(SamgodsRoadFerryMode, SamgodsRailFerryMode));

	private final static Map<Character, TransportMode> code2samgodsMode;
	static {
		code2samgodsMode = new LinkedHashMap<>();
		code2samgodsMode.put('A', SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put('X', SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put('D', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('d', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('E', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('F', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('f', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('J', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('K', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('L', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('V', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('B', SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put('C', SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put('S', SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put('c', SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put('G', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('H', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('h', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('I', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('T', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('U', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('i', SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('M', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('N', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('O', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put('W', SamgodsConstants.TransportMode.Sea);
		code2samgodsMode.put(SamgodsRoadFerryMode, SamgodsConstants.TransportMode.Road);
		code2samgodsMode.put(SamgodsRailFerryMode, SamgodsConstants.TransportMode.Rail);
		code2samgodsMode.put('R', SamgodsConstants.TransportMode.Air);
	}

	// -------------------- MEMBERS --------------------

	private final SamgodsConstants.Commodity commodity;

	private final TransportDemand transportDemand;

	private double samplingRate = 1.0;
	private Random rnd = null;

	// -------------------- CONSTRUCTION/CONFIGURATION --------------------

	public ChainChoiReader(final SamgodsConstants.Commodity commodity, final TransportDemand transportDemand) {
		this.commodity = commodity;
		this.transportDemand = transportDemand;
	}

	public ChainChoiReader setSamplingRate(double rate, Random rnd) {
		this.samplingRate = rate;
		this.rnd = rnd;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private Long key = null;
	private Double singleInstanceVolume_ton_yr = null;
	private Integer numberOfInstances = null;
	private Map<OD, Double> od2proba = new LinkedHashMap<>();

	private void addTmpDataToDemandAndReset() {
		assert ((this.samplingRate < 1.0)
				|| Math.abs(this.od2proba.values().stream().mapToDouble(p -> p).sum() - 1.0) <= 0.001);
		final Map.Entry<OD, Double> odEntry = this.od2proba.entrySet().stream()
				.max((e, f) -> e.getValue().compareTo(f.getValue())).get();
		this.transportDemand.add(this.commodity, odEntry.getKey(), this.singleInstanceVolume_ton_yr,
				this.numberOfInstances);

		this.key = null;
		this.singleInstanceVolume_ton_yr = null;
		this.numberOfInstances = null;
		this.od2proba = null;
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public void startCurrentDataRow() {

		if ((this.samplingRate < 1.0) && (this.rnd.nextDouble() >= this.samplingRate)) {
			// Only for testing.
			return;
		}

		// Load chain parameters.

		final long key = Long.parseLong(this.getStringValue(Key));
		final double proba = this.getDoubleValue(Prob);
		final double singleInstanceVolume_ton_yr = this.getDoubleValue(AnnualVolumeTonnesPerRelation);
		final int numberOfInstances = this.getIntValue(NRelations);
		final OD od = new OD(Id.createNodeId(Long.parseLong(this.getStringValue(Orig))),
				Id.createNodeId(Long.parseLong(this.getStringValue(Dest))));

		// Load OD demand.

		if ((this.key != null) && (this.key == key)) {
			assert (this.singleInstanceVolume_ton_yr == singleInstanceVolume_ton_yr);
			assert (this.numberOfInstances == numberOfInstances);
			this.od2proba.compute(od, (od2, p2) -> p2 == null ? proba : p2 + proba);
		} else {
			if (this.key != null) {
				this.addTmpDataToDemandAndReset();
			}
			this.key = key;
			this.singleInstanceVolume_ton_yr = singleInstanceVolume_ton_yr;
			this.numberOfInstances = numberOfInstances;
			this.od2proba = new LinkedHashMap<>();
			this.od2proba.put(od, proba);
		}

		// Load transport legs.

		String chainType = this.getStringValue(ChainType);
		{
			final String oldChainType = chainType;
			for (int i = 1; i < 10; i++) {
				chainType = chainType.replace("" + i, "");
			}
			if (oldChainType.length() > chainType.length()) {
				log.warn("Reduced chain type " + oldChainType + " to " + chainType + ".");
			}
		}

		final List<String> originColumns = originsColumnsByChainLength.get(chainType.length());
		final List<String> destinationColumns = destinationColumnsByChainLength.get(chainType.length());

		final List<OD> segmentODs = new ArrayList<>(chainType.length());
		final List<SamgodsConstants.TransportMode> modes = new ArrayList<>(chainType.length());
		for (int i = 0; i < chainType.length(); i++) {
			final long intermedOrigin = Long.parseLong(this.getStringValue(originColumns.get(i)));
			final long intermedDestination = Long.parseLong(this.getStringValue(destinationColumns.get(i)));
			segmentODs.add(new OD(Id.createNodeId(intermedOrigin), Id.createNodeId(intermedDestination)));
			modes.add(code2samgodsMode.get(chainType.charAt(i)));
		}

		// filter out ferry episodes

		assert (!SamgodsFerryModes.contains(chainType.charAt(0)));
		assert (!SamgodsFerryModes.contains(chainType.charAt(chainType.length() - 1)));

		int i = 1;
		while (i < segmentODs.size() - 1) {
			if (SamgodsFerryModes.contains(chainType.charAt(i))) {
				assert (!SamgodsFerryModes.contains(chainType.charAt(i - 1)));
				assert (!SamgodsFerryModes.contains(chainType.charAt(i + 1)));
				assert (modes.get(i - 1).equals(modes.get(i)));
				assert (modes.get(i).equals(modes.get(i + 1)));

				final OD condensedOD = new OD(segmentODs.get(i - 1).origin, segmentODs.get(i + 1).destination);
				segmentODs.remove(i + 1);
				segmentODs.remove(i);
				segmentODs.set(i - 1, condensedOD);

				modes.remove(i + 1);
				modes.remove(i);
			} else {
				i++;
			}
		}
		assert (segmentODs.size() == modes.size());

		/*
		 * Compose episodes from legs. The only case where an episode contains more than
		 * one leg are rail legs in sequence.
		 */

		if (segmentODs.size() > 0) {
			// TODO Could already here filter out impossible commodity/container
			// combinations.
			for (boolean isContainer : new boolean[] { true, false }) {
				final TransportChain transportChain = new TransportChain(this.commodity, isContainer);
				TransportEpisode currentEpisode = null;
				for (int segmentIndex = 0; segmentIndex < segmentODs.size(); segmentIndex++) {
					final OD segmentOD = segmentODs.get(segmentIndex);
					final SamgodsConstants.TransportMode segmentMode = modes.get(segmentIndex);
					if (currentEpisode == null || !SamgodsConstants.TransportMode.Rail.equals(currentEpisode.getMode())
							|| !SamgodsConstants.TransportMode.Rail.equals(segmentMode)) {
						// There are NOT two subsequent rail legs.
						currentEpisode = new TransportEpisode(segmentMode);
						transportChain.addEpisode(currentEpisode);
					}
					assert (currentEpisode.getMode().equals(segmentMode));
					currentEpisode.addSegmentOD(segmentOD);
				}
				this.transportDemand.add(transportChain);
			}
		}
	}

	@Override
	public void endDocument() {
		if (this.key != null) {
			this.addTmpDataToDemandAndReset();
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public ChainChoiReader parse(String fileName) {
		log.info(
				"Parsing file:" + fileName + (this.samplingRate < 1 ? " with sampling rate " + this.samplingRate : ""));
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
}
