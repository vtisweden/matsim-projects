/**
 * se.vti.od2roundtrips
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
package se.vti.od2roundtrips.model;

/**
 * 
 * @author GunnarF
 *
 */
public class OD2RoundtripsScenario extends se.vti.roundtrips.model.Scenario<TAZ> {

	public OD2RoundtripsScenario() {
		super(name -> new TAZ(name));
	}
	
	public static void main(String[] args) {
		
		// Example for building an example
		
		OD2RoundtripsScenario scenario = new OD2RoundtripsScenario();
		
		TAZ floridsdorf = scenario.createAndAddLocation("Floridsdorf");
		TAZ dobling = scenario.createAndAddLocation("Döbling");		
		// ...
		
		TAZ recoveredFloridsdorf = scenario.getLocation("Floridsdorf");
		
		scenario.setSymmetricDistance_km(floridsdorf, dobling, 5.0);
		scenario.setSymmetricTime_h(floridsdorf, dobling, 0.1);
		// ...
	}

}
