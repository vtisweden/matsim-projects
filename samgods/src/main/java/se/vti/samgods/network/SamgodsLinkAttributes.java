/**
 * se.vti.samgods.readers
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
package se.vti.samgods.network;

import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsLinkAttributes {

	// -------------------- CONSTANTS --------------------

	public static final String ATTRIBUTE_NAME = "freight";

	public SamgodsConstants.TransportMode samgodsMode;

	public final Double speed1_km_h;

	public final Double speed2_km_h;

	// -------------------- CONSTRUCTION --------------------

	public SamgodsLinkAttributes(SamgodsConstants.TransportMode samgodsMode, Double speed1_km_h, Double speed2_km_h) {
		this.samgodsMode = samgodsMode;
		this.speed1_km_h = speed1_km_h;
		this.speed2_km_h = speed2_km_h;
	}
}