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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.MathHelpers;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.utils.MiscUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class PWCMatrix {

	// -------------------- MEMBERS --------------------

	private final Commodity commodity;

	private final Map<OD, Double> od2amount_ton_yr = new LinkedHashMap<>();

	private final Set<Id<Node>> locations = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public PWCMatrix(final Commodity commodity) {
		this.commodity = commodity;
	}

	// -------------------- INTERNALS --------------------

	// -------------------- IMPLEMENTATION --------------------

	public Commodity getCommodity() {
		return this.commodity;
	}

	public void add(final OD od, final double amount_ton) {
		this.locations.add(od.origin);
		this.locations.add(od.destination);
		this.od2amount_ton_yr.compute(od,
				(od2, prev_ton_yr) -> (prev_ton_yr == null) ? amount_ton : prev_ton_yr + amount_ton);
	}

	public double computeTotal_ton_yr() {
		return this.od2amount_ton_yr.values().stream().mapToDouble(v -> v).sum();
	}

	public Set<Id<Node>> getLocationsView() {
		return Collections.unmodifiableSet(this.locations);
	}

	public int getRelationsCnt() {
		return this.od2amount_ton_yr.size();
	}

	public double getTotalFreightDemand(final Tuple<Id<Node>, Id<Node>> od) {
		return this.od2amount_ton_yr.getOrDefault(od, 0.0);
	}

	public Map<OD, Double> getOd2Amount_ton_yr() {
		return this.od2amount_ton_yr;
	}

	public String createProductionConsumptionStatsTable(int maxRows) {

		final Map<Id<Node>, Double> loc2prod_ton_yr = new LinkedHashMap<>();
		final Map<Id<Node>, Double> loc2cons_ton_yr = new LinkedHashMap<>();
		for (Map.Entry<OD, Double> e : this.od2amount_ton_yr.entrySet()) {
			loc2prod_ton_yr.compute(e.getKey().origin, (l, p) -> p == null ? e.getValue() : p + e.getValue());
			loc2cons_ton_yr.compute(e.getKey().destination, (l, p) -> p == null ? e.getValue() : p + e.getValue());
		}
		final double totalProd_ton_yr = loc2prod_ton_yr.values().stream().mapToDouble(c -> c).sum();
		final double totalCons_ton_yr = loc2cons_ton_yr.values().stream().mapToDouble(c -> c).sum();

		final StringBuffer result = new StringBuffer();
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Rank", "Producer", "Production [1000 ton/yr]", "Production [%]", "Consumer",
				"Consumption [1000 ton/yr]", "Consumption [%]");
		table.addRule();
		table.addRow("", "Total", totalProd_ton_yr, "100", "Total", totalCons_ton_yr, "100");
		table.addRule();

		List<Map.Entry<Id<Node>, Double>> sortedProductions = MiscUtils.getSortedInstance(loc2prod_ton_yr);
		List<Map.Entry<Id<Node>, Double>> sortedConsumptions = MiscUtils.getSortedInstance(loc2cons_ton_yr);

		for (int i = 0; i < maxRows; i++) {
			final Map.Entry<Id<Node>, Double> prod = (sortedProductions.size() > i ? sortedProductions.get(i) : null);
			final Map.Entry<Id<Node>, Double> cons = (sortedConsumptions.size() > i ? sortedConsumptions.get(i) : null);
			table.addRow(i + 1, prod != null ? prod.getKey() : "",
					prod != null ? MathHelpers.round(0.001 * prod.getValue(), 2) : "",
					prod != null ? MathHelpers.round(100.0 * prod.getValue() / totalProd_ton_yr, 2) : "",
					cons != null ? cons.getKey() : "",
					cons != null ? MathHelpers.round(0.001 * cons.getValue(), 2) : "",
					cons != null ? MathHelpers.round(100.0 * cons.getValue() / totalCons_ton_yr, 2) : "");
			table.addRule();
		}

		result.append("\nCOMMODITY: " + this.commodity + ", largest producers and consumers\n");
		result.append(table.render());
		return result.toString();
	}

}
