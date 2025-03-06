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
import java.util.List;
import java.util.Map;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.utils.MiscUtils;
import se.vti.utils.misc.math.MathHelpers;

/**
 * ONLY FOR TESTING.
 * 
 * @author GunnarF
 *
 */
public class TransportDemandStatsTable {

	
	// TODO
//	log.info("commodity\tinland[gTon]\ttotal[gTon]");
//	final NetworkData networkData = this.getOrCreateNetworkDataProvider().createNetworkData();
//	for (Map.Entry<Commodity, Map<OD, List<AnnualShipment>>> od2xEntry : this.transportDemand
//			.getCommodity2od2annualShipments().entrySet()) {
//		final Commodity commodity = od2xEntry.getKey();
//		final Map<OD, List<AnnualShipment>> od2shipments = od2xEntry.getValue();
//		final double inlandAmount_gTon = 1e-9 * od2shipments.entrySet().stream()
//				.filter(e -> networkData.getDomesticNodeIds().contains(e.getKey().origin)
//						&& networkData.getDomesticNodeIds().contains(e.getKey().destination))
//				.flatMap(e -> e.getValue().stream()).mapToDouble(as -> as.getTotalAmount_ton()).sum();
//		final double totalAmount_gTon = 1e-9 * od2shipments.entrySet().stream().flatMap(e -> e.getValue().stream())
//				.mapToDouble(as -> as.getTotalAmount_ton()).sum();
//		log.info(commodity + "\t" + inlandAmount_gTon + "\t" + totalAmount_gTon);
//	}

	private List<SamgodsConstants.TransportMode> getTransportModeSequence(TransportChain chain) {
		return chain.getEpisodes().stream().map(e -> e.getMode()).toList();
	}

	public String createChainStatsTable(int maxRowCnt, Commodity commodity, TransportDemand demand) {
		// OD flow -> chain assignment may not be available, hence just counting chains.
		final Map<List<SamgodsConstants.TransportMode>, Integer> modeSeq2cnt = new LinkedHashMap<>();
		for (List<TransportChain> chains : demand.getCommodity2od2transportChains().get(commodity).values()) {
			for (TransportChain chain : chains) {
				final List<SamgodsConstants.TransportMode> modes = this.getTransportModeSequence(chain);
				modeSeq2cnt.compute(modes, (m, c) -> c == null ? 1 : c + 1);
			}
		}
		final List<Map.Entry<List<SamgodsConstants.TransportMode>, Integer>> sortedEntries = MiscUtils
				.getSortedEntryListLargestFirst(modeSeq2cnt);

		final long total = modeSeq2cnt.values().stream().mapToLong(c -> c).sum();
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Rank", "episodes", "Count", "Share [%]");
		table.addRule();
		table.addRow("", "Total", total, 100);
		table.addRule();
		for (int i = 0; i < Math.min(maxRowCnt, sortedEntries.size()); i++) {
			Map.Entry<List<SamgodsConstants.TransportMode>, Integer> entry = sortedEntries.get(i);
			table.addRow(i + 1, entry.getKey(), entry.getValue(),
					MathHelpers.round(100.0 * entry.getValue().doubleValue() / total, 2));
			table.addRule();
		}

		final StringBuffer result = new StringBuffer();
		result.append(
				"\nCOMMODITY: " + commodity + ", counting occurrences of logistic chains (NOT freight volumes)\n");
		result.append(table.render());
		return result.toString();
	}

}
