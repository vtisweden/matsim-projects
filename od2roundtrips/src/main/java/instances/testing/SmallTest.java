/**
 * instances.testing
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
package instances.testing;

import od2roundtrips.model.OD2RoundtripsScenario;
import od2roundtrips.model.TAZ;

/**
 * 
 * @author GunnarF
 *
 */
public class SmallTest {

	static OD2RoundtripsScenario createScenario() {
		OD2RoundtripsScenario scenario = new OD2RoundtripsScenario();

		TAZ a = scenario.createAndAddLocation("A");
		TAZ b = scenario.createAndAddLocation("B");
		TAZ c = scenario.createAndAddLocation("C");

		scenario.setSymmetricDistance_km(a, b, 10.0);
		scenario.setSymmetricDistance_km(a, c, 10.0);
		scenario.setSymmetricDistance_km(b, c, 10.0);

		scenario.setSymmetricTime_h(a, b, 0.1);
		scenario.setSymmetricTime_h(a, c, 0.1);
		scenario.setSymmetricTime_h(b, c, 0.1);

		scenario.setMaxParkingEpisodes(4);
		scenario.setTimeBinCnt(24);
		
		return scenario;
	}
	
}
