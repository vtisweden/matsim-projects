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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.PWCMatrix;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportLeg;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private boolean verbose = false;

	private final static String Key = "Key";
	private final static String AnnualVolumeTonnes = "AnnualVolume_(Tonnes)";
	private final static String Prob = "Prob";
	private final static String ShipmentFreqPerYear = "ShipmentFreq_(per_year)";
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
		code2mode.put('P', SamgodsConstants.TransportMode.Sea);
		code2mode.put('Q', SamgodsConstants.TransportMode.Sea);
		code2mode.put('R', SamgodsConstants.TransportMode.Air);
	}

	// -------------------- MEMBERS --------------------

	private final PWCMatrix pwcMatrix;

	private final Map<OD, List<TransportChain>> od2chains;

	private final Set<String> chainTypes = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public ChainChoiReader(final String fileName, final SamgodsConstants.Commodity commodity) {
		this.pwcMatrix = new PWCMatrix(commodity);
		this.od2chains = new LinkedHashMap<>();

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

		String chainType = this.getStringValue(ChainType);
		for (int i = 1; i < 10; i++) {
			chainType = chainType.replace("" + i, "");
		}
		this.chainTypes.add(chainType);

		final double volume_ton_yr = this.getDoubleValue(Prob) * this.getDoubleValue(AnnualVolumeTonnes);
		final long origin = Long.parseLong(this.getStringValue(Orig));
		final long destination = Long.parseLong(this.getStringValue(Dest));
		final OD od = new OD(Id.createNodeId(origin), Id.createNodeId(destination));
		this.pwcMatrix.add(od, volume_ton_yr);

		final List<String> originColumns = originsColumnsByChainLength.get(chainType.length());
		final List<String> destinationColumns = destinationColumnsByChainLength.get(chainType.length());

		final TransportChain transportChain = new TransportChain();
		for (int i = 0; i < chainType.length(); i++) {
			final long intermedOrigin = Long.parseLong(this.getStringValue(originColumns.get(i)));
			final long intermedDestination = Long.parseLong(this.getStringValue(destinationColumns.get(i)));
			final char samgodsMode = chainType.charAt(i);
			
			throw new RuntimeException("COMPOSE TRANSPORT CHAIN OF EPISODES");
//			transportChain.addLeg(new TransportLeg(Id.createNodeId(intermedOrigin),
//					Id.createNodeId(intermedDestination), code2mode.get(samgodsMode), samgodsMode));

		}
		this.od2chains.computeIfAbsent(od, od2 -> new LinkedList<>()).add(transportChain);

		if (this.verbose) {
			System.out.println(this.pwcMatrix.getCommodity().twoDigitCode() + " " + this.pwcMatrix.getCommodity() + ": "
					+ this.pwcMatrix.computeTotal_ton_yr() + " tons, " + this.pwcMatrix.getLocationsView().size()
					+ " locations, " + this.chainTypes + " chain types");
		}
//		}
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
			final ChainChoiReader reader = new ChainChoiReader(
					"./2023-06-01_basecase/ChainChoi" + commodity.twoDigitCode() + "STD.out", commodity);
			System.out.println(reader.pwcMatrix.getCommodity().twoDigitCode() + " " + commodity + ": "
					+ Math.round(1e-6 * reader.getPWCMatrix().computeTotal_ton_yr()) + " mio.tons, between "
					+ reader.pwcMatrix.getLocationsView().size() + " locations, chain types: " + reader.chainTypes);
		}
	}

}
