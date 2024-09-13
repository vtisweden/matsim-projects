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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(ChainChoiReader.class);

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

	private final static Map<Character, TransportMode> code2mode;
	static {
		code2mode = new LinkedHashMap<>();
		code2mode.put('A', SamgodsConstants.TransportMode.Road);
		code2mode.put('X', SamgodsConstants.TransportMode.Road);
		code2mode.put('D', SamgodsConstants.TransportMode.Rail);
		code2mode.put('d', SamgodsConstants.TransportMode.Rail);
		code2mode.put('E', SamgodsConstants.TransportMode.Rail);
		code2mode.put('F', SamgodsConstants.TransportMode.Rail);
		code2mode.put('f', SamgodsConstants.TransportMode.Rail);
		code2mode.put('J', SamgodsConstants.TransportMode.Sea);
		code2mode.put('K', SamgodsConstants.TransportMode.Sea);
		code2mode.put('L', SamgodsConstants.TransportMode.Sea);
		code2mode.put('V', SamgodsConstants.TransportMode.Sea);
		code2mode.put('B', SamgodsConstants.TransportMode.Road);
		code2mode.put('C', SamgodsConstants.TransportMode.Road);
		code2mode.put('S', SamgodsConstants.TransportMode.Road);
		code2mode.put('c', SamgodsConstants.TransportMode.Road);
		code2mode.put('G', SamgodsConstants.TransportMode.Rail);
		code2mode.put('H', SamgodsConstants.TransportMode.Rail);
		code2mode.put('h', SamgodsConstants.TransportMode.Rail);
		code2mode.put('I', SamgodsConstants.TransportMode.Rail);
		code2mode.put('T', SamgodsConstants.TransportMode.Rail);
		code2mode.put('U', SamgodsConstants.TransportMode.Rail);
		code2mode.put('i', SamgodsConstants.TransportMode.Rail);
		code2mode.put('M', SamgodsConstants.TransportMode.Sea);
		code2mode.put('N', SamgodsConstants.TransportMode.Sea);
		code2mode.put('O', SamgodsConstants.TransportMode.Sea);
		code2mode.put('W', SamgodsConstants.TransportMode.Sea);
		code2mode.put(SamgodsRoadFerryMode, SamgodsConstants.TransportMode.Road);
		code2mode.put(SamgodsRailFerryMode, SamgodsConstants.TransportMode.Rail);
		code2mode.put('R', SamgodsConstants.TransportMode.Air);
	}

	// -------------------- MEMBERS --------------------

	private final SamgodsConstants.Commodity commodity;

	private final TransportDemand transportDemand;

	private double samplingRate = 1.0;
	private Random rnd = null;

	private Network network;

	private long usedOdCnt = 0;
	private long ignoredOdCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	public ChainChoiReader(final SamgodsConstants.Commodity commodity, final TransportDemand transportDemand) {
		this.commodity = commodity;
		this.transportDemand = transportDemand;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public ChainChoiReader setSamplingRate(double rate, Random rnd) {
		this.samplingRate = rate;
		this.rnd = rnd;
		return this;
	}

	public ChainChoiReader checkAgainstNetwork(Network network) {
		this.network = network;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public ChainChoiReader parse(String fileName) {
		this.usedOdCnt = 0;
		this.ignoredOdCnt = 0;
		Logger.getLogger(this.getClass()).info(
				"Parsing file:" + fileName + (this.samplingRate < 1 ? " with sampling rate " + this.samplingRate : ""));
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (this.ignoredOdCnt > 0) {
			log.warn("Ignored " + this.ignoredOdCnt + " OD pairs, kept " + this.usedOdCnt + " OD pairs.");
		}
		return this;
	}

	private Long key = null;
	private Double singleInstanceVolume_ton_yr = null;
	private Integer numberOfInstances = null;
	private Map<OD, Double> od2proba = new LinkedHashMap<>();

	private void addLastEntryToDemand() {

		if (this.samplingRate == 1.0) {
			assert (Math.abs(this.od2proba.values().stream().mapToDouble(p -> p).sum() - 1.0) <= 0.001);
		}
		final Map.Entry<OD, Double> odEntry = this.od2proba.entrySet().stream()
				.max((e, f) -> e.getValue().compareTo(f.getValue())).get();
		this.usedOdCnt++;
		this.ignoredOdCnt += (this.od2proba.size() - 1);

		this.transportDemand.add(this.commodity, odEntry.getKey(), this.singleInstanceVolume_ton_yr,
				this.numberOfInstances);
		this.key = null;
		this.singleInstanceVolume_ton_yr = null;
		this.numberOfInstances = null;
		this.od2proba = null;
	}

	@Override
	public void startCurrentDataRow() {

		if ((this.samplingRate < 1.0) && (this.rnd.nextDouble() >= this.samplingRate)) {
			return; // Only for testing.
		}

		// Load OD demand.

		final long key = Long.parseLong(this.getStringValue(Key));

		final double proba = this.getDoubleValue(Prob);
		final double singleInstanceVolume_ton_yr = this.getDoubleValue(AnnualVolumeTonnesPerRelation);
		final int numberOfInstances = this.getIntValue(NRelations);
		final OD od = new OD(Id.createNodeId(Long.parseLong(this.getStringValue(Orig))),
				Id.createNodeId(Long.parseLong(this.getStringValue(Dest))));

		if ((this.key != null) && (this.key == key)) {
			assert (this.singleInstanceVolume_ton_yr == singleInstanceVolume_ton_yr);
			assert (this.numberOfInstances == numberOfInstances);
			this.od2proba.compute(od, (od2, p2) -> p2 == null ? proba : p2 + proba);
		} else {
			if (this.key != null) {
				this.addLastEntryToDemand();
			}
			this.key = key;
			this.singleInstanceVolume_ton_yr = singleInstanceVolume_ton_yr;
			this.numberOfInstances = numberOfInstances;
			this.od2proba = new LinkedHashMap<>();
			this.od2proba.put(od, proba);
		}

		// Load chain legs.

		String chainType = this.getStringValue(ChainType);
		{
			final String oldChainType = chainType;
			for (int i = 1; i < 10; i++) {
				// TODO Replace leg types with number codes. Revisit.
				chainType = chainType.replace("" + i, "");
			}
			if (oldChainType.length() > chainType.length()) {
				Log.warn("Reduced chain type " + oldChainType + " to " + chainType + ".");
			}
		}

		final List<String> originColumns = originsColumnsByChainLength.get(chainType.length());
		final List<String> destinationColumns = destinationColumnsByChainLength.get(chainType.length());

		final List<TransportLeg> legs = new ArrayList<>(chainType.length());
		final List<SamgodsConstants.TransportMode> modes = new ArrayList<>(chainType.length());
		for (int i = 0; i < chainType.length(); i++) {
			final long intermedOrigin = Long.parseLong(this.getStringValue(originColumns.get(i)));
			final long intermedDestination = Long.parseLong(this.getStringValue(destinationColumns.get(i)));
			legs.add(new TransportLeg(Id.createNodeId(intermedOrigin), Id.createNodeId(intermedDestination)));
			modes.add(code2mode.get(chainType.charAt(i)));
		}

		// filter out ferry episodes

		assert (!SamgodsFerryModes.contains(chainType.charAt(0)));
		assert (!SamgodsFerryModes.contains(chainType.charAt(chainType.length() - 1)));

		int i = 1;
		while (i < legs.size() - 1) {
			if (SamgodsFerryModes.contains(chainType.charAt(i))) {
				assert (!SamgodsFerryModes.contains(chainType.charAt(i - 1)));
				assert (!SamgodsFerryModes.contains(chainType.charAt(i + 1)));
				assert (modes.get(i - 1).equals(modes.get(i)));
				assert (modes.get(i).equals(modes.get(i + 1)));

				final TransportLeg condensedLeg = new TransportLeg(legs.get(i - 1).getOrigin(),
						legs.get(i + 1).getDestination());
				legs.remove(i + 1);
				legs.remove(i);
				legs.set(i - 1, condensedLeg);

				final SamgodsConstants.TransportMode condensedMode = modes.get(i - 1);
				modes.remove(i + 1);
				modes.remove(i);
				modes.set(i - 1, condensedMode);
			} else {
				i++;
			}
		}
		assert (legs.size() == modes.size());

		// Compose episodes from legs.

		if (legs.size() > 0) {
			// TODO Could already here filter out impossible commodity/container
			// combinations.
			for (boolean isContainer : new boolean[] { true, false }) {

				final TransportChain transportChain = new TransportChain(this.commodity, isContainer);

				TransportEpisode currentEpisode = null;
				for (int legIndex = 0; legIndex < legs.size(); legIndex++) {
					final TransportLeg leg = legs.get(legIndex).deepCopy(); // individual legs for multiple chains
					final SamgodsConstants.TransportMode legMode = modes.get(legIndex);
					if (currentEpisode == null || !SamgodsConstants.TransportMode.Rail.equals(currentEpisode.getMode())
							|| !SamgodsConstants.TransportMode.Rail.equals(legMode)) {
						// There are NOT two subsequent rail legs.
						currentEpisode = new TransportEpisode(legMode);
						transportChain.addEpisode(currentEpisode);
					}
					assert (currentEpisode.getMode().equals(legMode));
					currentEpisode.addLeg(leg);
				}

				if ((this.network != null) && !this.network.getNodes().keySet()
						.containsAll(this.getLoadingTransferUnloadingNodesSet(transportChain))) {
					final Set<Id<Node>> nodeIdsNotInNetwork = new LinkedHashSet<>(
							this.getLoadingTransferUnloadingNodesSet(transportChain));
					nodeIdsNotInNetwork.removeAll(this.network.getNodes().keySet());
					new InsufficientDataException(this.getClass(),
							"Transport chain contained nodes " + nodeIdsNotInNetwork + " that are not in the network.",
							transportChain).log();
				} else {
					this.transportDemand.add(transportChain);
//					this.transportDemand.add(transportChain, singleInstanceVolume_ton_yr, numberOfInstances);
				}
			}
		}
	}

	@Override
	public void endDocument() {
		if (this.key != null) {
			this.addLastEntryToDemand();
		}
	}

	private Set<Id<Node>> getLoadingTransferUnloadingNodesSet(TransportChain chain) {
		final Set<Id<Node>> result = new LinkedHashSet<>();
		chain.getEpisodes().stream().flatMap(e -> e.getLegs().stream()).forEach(l -> {
			result.add(l.getOrigin());
			result.add(l.getDestination());
		});
		return result;
	}
}
