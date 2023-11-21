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
package se.vti.samgods.io;

import static se.vti.samgods.io.ChainChoiReader.TransportMode.Air;
import static se.vti.samgods.io.ChainChoiReader.TransportMode.Rail;
import static se.vti.samgods.io.ChainChoiReader.TransportMode.Road;
import static se.vti.samgods.io.ChainChoiReader.TransportMode.Sea;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.logistics.Location;
import se.vti.samgods.logistics.PWCMatrix;
import se.vti.samgods.logistics.Samgods;
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	public enum TransportMode {
		Road, Rail, Sea, Air
	};

	private boolean verbose = false;

	final static String Key = "Key";
	final static String AnnualVolumeTonnes = "AnnualVolume_(Tonnes)";
	final static String Prob = "Prob";
	final static String ShipmentFreqPerYear = "ShipmentFreq_(per_year)";
	final static String ChainType = "ChainType";

	final static String Orig = "Orig";
	final static String Dest = "Dest";
	final static String Orig2 = "Orig2";
	final static String Orig3 = "Orig3";
	final static String Orig4 = "Orig4";
	final static String Orig5 = "Orig5";
	final static List<List<String>> originsColumnsByChainLength = Arrays.asList(null, Arrays.asList(Orig),
			Arrays.asList(Orig, Orig2), Arrays.asList(Orig, Orig2, Orig3), Arrays.asList(Orig, Orig2, Orig3, Orig4),
			Arrays.asList(Orig, Orig2, Orig3, Orig4, Orig5));
	final static List<List<String>> destinationColumnsByChainLength = Arrays.asList(null, Arrays.asList(Dest),
			Arrays.asList(Orig2, Dest), Arrays.asList(Orig2, Orig3, Dest), Arrays.asList(Orig2, Orig3, Orig4, Dest),
			Arrays.asList(Orig2, Orig3, Orig4, Orig5, Dest));

	final static Map<Character, TransportMode> code2mode;

	static {
		code2mode = new LinkedHashMap<>();
		code2mode.put('A', Road);
		code2mode.put('X', Road);
		code2mode.put('D', Rail);
		code2mode.put('d', Rail);
		code2mode.put('E', Rail);
		code2mode.put('F', Rail);
		code2mode.put('f', Rail);
		code2mode.put('J', Sea);
		code2mode.put('K', Sea);
		code2mode.put('L', Sea);
		code2mode.put('V', Sea);
		code2mode.put('B', Road);
		code2mode.put('C', Road);
		code2mode.put('S', Road);
		code2mode.put('c', Road);
		code2mode.put('G', Rail);
		code2mode.put('H', Rail);
		code2mode.put('h', Rail);
		code2mode.put('I', Rail);
		code2mode.put('T', Rail);
		code2mode.put('U', Rail);
		code2mode.put('i', Rail);
		code2mode.put('M', Sea);
		code2mode.put('N', Sea);
		code2mode.put('O', Sea);
		code2mode.put('W', Sea);
		code2mode.put('P', Sea);
		code2mode.put('Q', Sea);
		code2mode.put('R', Air);
	}
//	public enum TransportChainType {
//
//		A(1, true, Road), ADA(2, true, Road, Rail, Road), AdA(3, true, Road, Rail, Road),
//		ADJA(4, true, Road, Rail, Sea, Road), ADJDA(5, true, Road, Rail, Sea, Rail, Road),
//		ADKL(6, true, Road, Rail, Sea, Sea), AJ(7, true, Road, Sea), AJA(8, true, Road, Sea, Road),
//		AJDA(9, true, Road, Sea, Rail, Road), AJdX(10, true, Road, Sea, Rail, Road), AKL(11, true, Road, Sea, Sea);
//
//		// TODO continue here. right reduction? -> see also comment in handler
//
//		public final int number;
//		public final boolean container;
//		public final List<TransportMode> modesView;
//
//		private TransportChainType(final int number, final boolean container, final TransportMode... transportModes) {
//			this.number = number;
//			this.container = container;
//			this.modesView = Collections.unmodifiableList(Arrays.asList(transportModes));
//		}
//
//	};

	// -------------------- MEMBERS --------------------

	private PWCMatrixImpl pwcMatrix = null;

	private Map<Tuple<Location, Location>, List<TransportChain>> od2chains = null;

	private Set<String> chainTypes = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public ChainChoiReader() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void readChainChoiFile(final String fileName, final Samgods.Commodity commodity) {

		this.pwcMatrix = new PWCMatrixImpl(commodity);
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

	public PWCMatrix getPWCMatrix() {
		return this.pwcMatrix;
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
		final Tuple<Location, Location> od = new Tuple<>(new Location(origin), new Location(destination));
		this.pwcMatrix.add(od, volume_ton_yr);

		final List<String> originColumns = originsColumnsByChainLength.get(chainType.length());
		final List<String> destinationColumns = destinationColumnsByChainLength.get(chainType.length());

		final TransportChainImpl transportChain = new TransportChainImpl();
		for (int i = 0; i < chainType.length(); i++) {
			final long intermedOrigin = Long.parseLong(this.getStringValue(originColumns.get(i)));
			final long intermedDestination = Long.parseLong(this.getStringValue(destinationColumns.get(i)));
			final TransportMode mode = code2mode.get(chainType.charAt(i));
			transportChain.addLeg(
					new TransportLegImpl(new Location(intermedOrigin), new Location(intermedDestination), mode));
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

		for (Samgods.Commodity commodity : Samgods.Commodity.values()) {
			final ChainChoiReader reader = new ChainChoiReader();
			reader.readChainChoiFile("./2023-06-01_basecase/ChainChoi" + commodity.twoDigitCode() + "STD.out",
					commodity);
			System.out.println(reader.pwcMatrix.getCommodity().twoDigitCode() + " " + commodity + ": "
					+ Math.round(1e-6 * reader.getPWCMatrix().computeTotal_ton_yr()) + " mio.tons, between "
					+ reader.pwcMatrix.getLocationsView().size() + " locations, chain types: "
					+ reader.chainTypes);
		}
	}

}
