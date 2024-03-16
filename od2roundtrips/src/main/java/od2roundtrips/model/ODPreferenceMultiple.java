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
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class ODPreferenceMultiple implements MHWeight<MultiRoundTrip<TAZ, RoundTrip<TAZ>>> {

	private final ODPreference odPreferenceSingle;

	public ODPreferenceMultiple(ODPreference odPreferenceSingle) {
		this.odPreferenceSingle = odPreferenceSingle;
	}

	@Override
	public double logWeight(MultiRoundTrip<TAZ, RoundTrip<TAZ>> multiRoundTrip) {
		this.odPreferenceSingle.assertNormalized();
		final Map<Tuple<TAZ, TAZ>, Integer> realizedOdMatrix = this.odPreferenceSingle
				.createRealizedMatrix(multiRoundTrip);
		return this.odPreferenceSingle.computePoissionLogWeight(realizedOdMatrix, multiRoundTrip.locationCnt());
	}

}
