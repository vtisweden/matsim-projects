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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.utils.MiscUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private boolean verbose = false;

//	private final static String Key = "Key";
	private final static String AnnualVolumeTonnes = "AnnualVolume_(Tonnes)";
	private final static String Prob = "Prob";
//	private final static String ShipmentFreqPerYear = "ShipmentFreq_(per_year)";
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

	private final static Map<Character, TransportMode> code2mode;
	static {
		// TODO Replace "(road) ferry" episodes by road episodes.
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
		code2mode.put('P', SamgodsConstants.TransportMode.Road); // road ferry
		code2mode.put('Q', SamgodsConstants.TransportMode.Rail); // rail ferry
		code2mode.put('R', SamgodsConstants.TransportMode.Air);
	}

	public static final char SAMGODS_ROAD_FERRY_MODE = 'P';
	public static final char SAMGODS_RAIL_FERRY_MODE = 'Q';
	public static final List<Character> SAMGODS_FERRY_MODES = Collections
			.unmodifiableList(Arrays.asList(SAMGODS_ROAD_FERRY_MODE, SAMGODS_RAIL_FERRY_MODE));

	// -------------------- MEMBERS --------------------

	private final PWCMatrix pwcMatrix;

	private final Map<OD, List<TransportChain>> od2chains;

	private final Set<String> chainTypes = new LinkedHashSet<>();

	private final SamgodsConstants.Commodity commodity;

	// -------------------- CONSTRUCTION --------------------

	public ChainChoiReader(final String fileName, final SamgodsConstants.Commodity commodity) {
		this.pwcMatrix = new PWCMatrix(commodity);
		this.od2chains = new LinkedHashMap<>();
		this.commodity = commodity;

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public PWCMatrix getPWCMatrix() {
		return this.pwcMatrix;
	}

	public Map<OD, List<TransportChain>> getOd2transportChains() {
		return this.od2chains;
	}

	// ----- IMPLEMENTATION OF AbstractTabularFileHandlerWithHeaderLine -----

	@Override
	public void startCurrentDataRow() {

		// Load OD demand.

		final double volume_ton_yr = this.getDoubleValue(Prob) * this.getDoubleValue(AnnualVolumeTonnes);
		final long origin = Long.parseLong(this.getStringValue(Orig));
		final long destination = Long.parseLong(this.getStringValue(Dest));
		final OD od = new OD(Id.createNodeId(origin), Id.createNodeId(destination));
		this.pwcMatrix.add(od, volume_ton_yr);

		// Load (relevant) chain legs.

		String chainType = this.getStringValue(ChainType);
		for (int i = 1; i < 10; i++) { // TODO Revisit.
			chainType = chainType.replace("" + i, "");
		}
		this.chainTypes.add(chainType);
		final List<String> originColumns = originsColumnsByChainLength.get(chainType.length());
		final List<String> destinationColumns = destinationColumnsByChainLength.get(chainType.length());

		final List<TransportLeg> legs = new ArrayList<>(chainType.length());
		for (int i = 0; i < chainType.length(); i++) {
			final char samgodsMode = chainType.charAt(i);
			if (!SAMGODS_FERRY_MODES.contains(samgodsMode)) { // Ferry is not a main mode.
				final long intermedOrigin = Long.parseLong(this.getStringValue(originColumns.get(i)));
				final long intermedDestination = Long.parseLong(this.getStringValue(destinationColumns.get(i)));
				legs.add(new TransportLeg(Id.createNodeId(intermedOrigin), Id.createNodeId(intermedDestination),
						code2mode.get(samgodsMode)));
			}
		}

		// Condense legs into episodes.

		if (legs.size() > 0) {
			final TransportChain transportChain = new TransportChain();
			TransportEpisode currentEpisode = null;
			for (TransportLeg leg : legs) {
				if (currentEpisode == null || !SamgodsConstants.TransportMode.Rail.equals(currentEpisode.getMode())
						|| !SamgodsConstants.TransportMode.Rail.equals(leg.getMode())) {
					currentEpisode = new TransportEpisode(leg.getMode());
					transportChain.addEpisode(currentEpisode, false);
				}
				currentEpisode.addLeg(leg);
			}
			this.od2chains.computeIfAbsent(od, od2 -> new LinkedList<>()).add(transportChain);
		}

		if (this.verbose) {
			System.out.println(this.pwcMatrix.getCommodity().twoDigitCode() + " " + this.pwcMatrix.getCommodity() + ": "
					+ this.pwcMatrix.computeTotal_ton_yr() + " tons, " + this.pwcMatrix.getLocationsView().size()
					+ " locations, " + this.chainTypes + " chain types");
		}
	}

	public String createChainStatsTable(int maxRowCnt) {

		final Map<List<SamgodsConstants.TransportMode>, Integer> modeSeq2cnt = new LinkedHashMap<>();
		for (List<TransportChain> chains : this.od2chains.values()) {
			for (TransportChain chain : chains) {
				List<SamgodsConstants.TransportMode> modes = chain.getEpisodeModeSequence();
				modeSeq2cnt.compute(modes, (m, c) -> c == null ? 1 : c + 1);
			}
		}

		List<Map.Entry<List<SamgodsConstants.TransportMode>, Integer>> sortedEntries = MiscUtils
				.getSortedInstance(modeSeq2cnt);

		final int total = modeSeq2cnt.values().stream().mapToInt(c -> c).sum();
		final StringBuffer result = new StringBuffer();
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Rank", "Mode sequence", "Count", "Share [%]");
		table.addRule();
		table.addRow("", "Total", total, 100);
		table.addRule();
		for (int i = 0; i < Math.min(maxRowCnt, sortedEntries.size()); i++) {
			Map.Entry<List<SamgodsConstants.TransportMode>, Integer> entry = sortedEntries.get(i);
			table.addRow(i + 1, entry.getKey().stream().map(e -> e.toString()).collect(Collectors.joining(",")),
					entry.getValue(), MathHelpers.round(100.0 * entry.getValue().doubleValue() / total, 2));
			table.addRule();
		}
		result.append("\nCOMMODITY: " + this.commodity + ", occurrence of logistic chains (NOT freight volumes)\n");
		result.append(table.render());
		return result.toString();
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
			final ChainChoiReader reader = new ChainChoiReader(
					"./input_2024/ChainChoi" + commodity.twoDigitCode() + "XTD.out", commodity);
			System.out.println(reader.createChainStatsTable(10));
			System.out.println(reader.getPWCMatrix().createProductionConsumptionStatsTable(10));
		}
	}

}