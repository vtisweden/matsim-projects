/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.math.MathHelpers;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.external.rail.AnnualShipmentJsonSerializer;
import se.vti.samgods.utils.MiscUtils;

/**
 * This represents the annual shipments of one or more independent shippers.
 * Meaning that the singleInstanceAmount_ton is not the shipment size but the a
 * 
 * @author GunnarF
 *
 */
public class TransportDemand {

	// -------------------- INNER CLASS --------------------

	@JsonSerialize(using = AnnualShipmentJsonSerializer.class)
	public class AnnualShipment {

		private final SamgodsConstants.Commodity commodity;

		private final OD od;

		private final double singleInstanceAnnualAmount_ton;

		private final int numberOfInstances;

		private AnnualShipment(SamgodsConstants.Commodity commodity, OD od, double singleInstanceAnnualAmount_ton,
				int numberOfInstances) {
			this.commodity = commodity;
			this.od = od;
			this.singleInstanceAnnualAmount_ton = singleInstanceAnnualAmount_ton;
			this.numberOfInstances = numberOfInstances;
		}

		public SamgodsConstants.Commodity getCommodity() {
			return this.commodity;
		}

		public OD getOD() {
			return this.od;
		}

		public double getSingleInstanceAnnualAmount_ton() {
			return this.singleInstanceAnnualAmount_ton;
		}

		public int getNumberOfInstances() {
			return this.numberOfInstances;
		}

		public double getTotalAmount_ton() {
			return this.singleInstanceAnnualAmount_ton * this.numberOfInstances;
		}
	}

	// -------------------- MEMBERS --------------------

	public final Map<Commodity, Map<OD, Set<TransportChain>>> commodity2od2transportChains = new LinkedHashMap<>();

	public final Map<Commodity, Map<OD, List<AnnualShipment>>> commodity2od2annualShipments = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TransportDemand() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(TransportChain transportChain, double singleInstanceAmount_ton, int numberOfInstances) {
		final Commodity commodity = transportChain.getCommodity();
		final OD od = transportChain.getOD();

		final Set<TransportChain> existingChains = this.commodity2od2transportChains
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(od, od2 -> new LinkedHashSet<>(1));
		existingChains.add(transportChain);

		this.commodity2od2annualShipments.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(od, od2 -> new LinkedList<>())
				.add(new AnnualShipment(commodity, od, singleInstanceAmount_ton, numberOfInstances));
	}

	public Set<OD> collectAllODsWithChains() {
		return this.commodity2od2transportChains.values().stream().flatMap(od2ch -> od2ch.keySet().stream())
				.collect(Collectors.toSet());
	}

	public String createChainStatsTable(int maxRowCnt, Commodity commodity) {
		// TODO OD flow -> chain assignment may not yet be available, hence just
		// counting chains.

		final Map<List<List<SamgodsConstants.TransportMode>>, Integer> modeSeq2cnt = new LinkedHashMap<>();
		for (Set<TransportChain> chains : this.commodity2od2transportChains.get(commodity).values()) {
			for (TransportChain chain : chains) {
				final List<List<SamgodsConstants.TransportMode>> modes = chain.getTransportModeSequence();
				modeSeq2cnt.compute(modes, (m, c) -> c == null ? 1 : c + 1);
			}
		}
		final List<Map.Entry<List<List<SamgodsConstants.TransportMode>>, Integer>> sortedEntries = MiscUtils
				.getSortedEntryListLargestFirst(modeSeq2cnt);

		final long total = modeSeq2cnt.values().stream().mapToLong(c -> c).sum();
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Rank", "episodes", "Count", "Share [%]");
		table.addRule();
		table.addRow("", "Total", total, 100);
		table.addRule();
		for (int i = 0; i < Math.min(maxRowCnt, sortedEntries.size()); i++) {
			Map.Entry<List<List<SamgodsConstants.TransportMode>>, Integer> entry = sortedEntries.get(i);
			table.addRow(i + 1, entry.getKey(),
//					entry.getKey().stream()
//							.map(e -> "[" + e.stream().map(m -> m.toString()).collect(Collectors.joining(",")) + "]")
//							.collect(Collectors.joining(",")),
					entry.getValue(), MathHelpers.round(100.0 * entry.getValue().doubleValue() / total, 2));
			table.addRule();
		}

		final StringBuffer result = new StringBuffer();
		result.append(
				"\nCOMMODITY: " + commodity + ", counting occurrences of logistic chains (NOT freight volumes)\n");
		result.append(table.render());
		return result.toString();
	}
}
