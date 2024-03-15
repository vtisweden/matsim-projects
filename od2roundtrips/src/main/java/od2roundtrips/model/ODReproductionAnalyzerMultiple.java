/**
 * od2roundtrips.model
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
package od2roundtrips.model;

import java.util.Map;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class ODReproductionAnalyzerMultiple implements MHStateProcessor<MultiRoundTrip<TAZ, RoundTrip<TAZ>>> {

	private ODReproductionAnalyzerSingle analyzer;

	public ODReproductionAnalyzerMultiple(long burnInIterations, long samplingInterval,
			Map<Tuple<TAZ, TAZ>, Double> targetOdMatrix) {
		this.analyzer = new ODReproductionAnalyzerSingle(burnInIterations, samplingInterval, targetOdMatrix);
	}

	@Override
	public void start() {
		this.analyzer.start();
	}

	@Override
	public void processState(MultiRoundTrip<TAZ, RoundTrip<TAZ>> state) {
		for (int i = 0; i < state.size(); i++) {
			this.analyzer.processState(state.getRoundTrip(i));
		}
	}

	@Override
	public void end() {
		this.analyzer.end();
	}

	@Override
	public String toString() {
		return this.analyzer.toString();
	}
}
