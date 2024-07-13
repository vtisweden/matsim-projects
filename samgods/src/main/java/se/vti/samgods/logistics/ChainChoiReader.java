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
		return this;
	}

	@Override
	public void startCurrentDataRow() {

		if ((this.samplingRate < 1.0) && (this.rnd.nextDouble() >= this.samplingRate)) {
			return;
		}

		// Load OD demand.

		final double singleInstanceVolume_ton_yr = this.getDoubleValue(Prob)
				* this.getDoubleValue(AnnualVolumeTonnesPerRelation);
		final int numberOfInstances = this.getIntValue(NRelations);

		final long origin = Long.parseLong(this.getStringValue(Orig));
		final long destination = Long.parseLong(this.getStringValue(Dest));
		final OD od = new OD(Id.createNodeId(origin), Id.createNodeId(destination));

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
					final TransportLeg leg = legs.get(legIndex);
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
						.containsAll(transportChain.getLoadingTransferUnloadingNodesSet())) {
					final Set<Id<Node>> nodeIdsNotInNetwork = new LinkedHashSet<>(
							transportChain.getLoadingTransferUnloadingNodesSet());
					nodeIdsNotInNetwork.removeAll(this.network.getNodes().keySet());
					new InsufficientDataException(this.getClass(),
							"Transport chain contained nodes " + nodeIdsNotInNetwork + " that are not in the network.",
							transportChain).log();
				} else {
					this.transportDemand.add(transportChain, singleInstanceVolume_ton_yr, numberOfInstances);
				}
			}
		}
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

//	public static void main(String[] args) {
//
//		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
//			final ChainChoiReader reader = new ChainChoiReader(commodity).setStoreSamgodsShipments(true)
//					.parse("./input_2024/ChainChoi" + commodity.twoDigitCode() + "XTD.out");
//			System.out.println(reader.createChainStatsTable(10));
//			System.out.println(reader.getPWCMatrix().createProductionConsumptionStatsTable(10));
//			AnnualShipmentJsonSerializer.writeToFile(reader.getSamgodsShipments(),
//					"./input_2024/" + commodity + "-chains.samgods-out.json");
//		}
//	}

}
