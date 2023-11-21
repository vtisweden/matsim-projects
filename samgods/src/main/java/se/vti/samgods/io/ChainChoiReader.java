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

import static se.vti.samgods.io.ChainChoiReader.TransportMode.Rail;
import static se.vti.samgods.io.ChainChoiReader.TransportMode.Road;
import static se.vti.samgods.io.ChainChoiReader.TransportMode.Sea;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.logistics.Location;
import se.vti.samgods.logistics.PWCMatrix;
import se.vti.samgods.logistics.Samgods;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private boolean verbose = false;

	static String Key = "Key";
	static String AnnualVolumeTonnes = "AnnualVolume_(Tonnes)";
	static String Prob = "Prob";
	static String ShipmentFreqPerYear = "ShipmentFreq_(per_year)";
	static String ChainType = "ChainType";

	static String Orig = "Orig";
	static String Dest = "Dest";
	static String Orig2 = "Orig2";
	static String Orig3 = "Orig3";
	static String Orig4 = "Orig4";
	static String Orig5 = "Orig5";
	static List<List<String>> originsColumnsByChainLength = Arrays.asList(null, Arrays.asList(Orig),
			Arrays.asList(Orig, Orig2), Arrays.asList(Orig, Orig2, Orig3), Arrays.asList(Orig, Orig2, Orig3, Orig4),
			Arrays.asList(Orig, Orig2, Orig3, Orig4, Orig5));
	static List<List<String>> destinationColumnsByChainLength = Arrays.asList(null, Arrays.asList(Dest),
			Arrays.asList(Orig2, Dest), Arrays.asList(Orig2, Orig3, Dest), Arrays.asList(Orig2, Orig3, Orig4, Dest),
			Arrays.asList(Orig2, Orig3, Orig4, Orig5, Dest));

	public enum TransportMode {
		Road, Rail, Sea, Air
	};

	public enum TransportChainType {

		A(1, true, Road),
		ADA(2, true, Road, Rail, Road),
		AdA(3, true, Road, Rail, Road),
		ADJA(4, true, Road, Rail, Sea, Road),
		ADJDA(5, true, Road, Rail, Sea, Rail, Road),
		ADKL(6, true, Road, Rail, Sea, Sea),
		AJ(7, true, Road, Sea),
		AJA(8, true, Road, Sea, Road),
		AJDA(9, true, Road, Sea, Rail, Road),
		AJdX(10, true, Road, Sea, Rail, Road),
		AKL(11, true, Road, Sea, Sea);
		
		// TODO continue here. right reduction? -> see also comment in handler
		
		
		
		
		public final int number;
		public final boolean container;
		public final List<TransportMode> modesView;

		private TransportChainType(final int number, final boolean container, final TransportMode... transportModes) {
			this.number = number;
			this.container = container;
			this.modesView = Collections.unmodifiableList(Arrays.asList(transportModes));
		}

	};

	// -------------------- MEMBERS --------------------

	private PWCMatrixImpl pwcMatrix = null;

	private Map<Tuple<Location, Location>, Set<String>> od2chainTypes = null;

	// -------------------- CONSTRUCTION --------------------

	public ChainChoiReader() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void readChainChoiFile(final String fileName, final Samgods.Commodity commodity) {

		this.pwcMatrix = new PWCMatrixImpl(commodity);
		this.od2chainTypes = new LinkedHashMap<>();

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<String> computeAllChainTypes() {
		return this.od2chainTypes.values().stream().flatMap(s -> s.stream()).collect(Collectors.toSet());
	}

	public PWCMatrix getPWCMatrix() {
		return this.pwcMatrix;
	}

	// ----- IMPLEMENTATION OF AbstractTabularFileHandlerWithHeaderLine -----

	@Override
	public void startCurrentDataRow() {

		final String chainType = this.getStringValue(ChainType);
//		final String parseChainType = originalChainType.replace("1", "").replace("2", ""); // TODO Documented chain types?

//		List<String> originColumns = originsColumnsByChainLength.get(parseChainType.length());
//		List<String> destinationColumns = destinationColumnsByChainLength.get(parseChainType.length());
//		for (int i = 0; i < parseChainType.length(); i++) {
		final long origin = Long.parseLong(this.getStringValue(Orig));
		final long destination = Long.parseLong(this.getStringValue(Dest));
		final double volume_ton_yr = this.getDoubleValue(Prob) * this.getDoubleValue(AnnualVolumeTonnes);
		final Tuple<Location, Location> od = new Tuple<>(new Location(origin), new Location(destination));
		this.pwcMatrix.add(od, volume_ton_yr);
		this.od2chainTypes.computeIfAbsent(od, od2 -> new LinkedHashSet<>()).add(chainType);

		// TODO include transshipment points in chains

		if (this.verbose) {
			System.out.println(this.pwcMatrix.getCommodity().twoDigitCode() + " " + this.pwcMatrix.getCommodity() + ": "
					+ this.pwcMatrix.computeTotal_ton_yr() + " tons, " + this.pwcMatrix.getLocationsView().size()
					+ " locations, " + this.computeAllChainTypes() + " chain types");
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
					+ reader.computeAllChainTypes());
		}
	}

}
