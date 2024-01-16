/**
 * se.vti.skellefeaV2X
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
package se.vti.skellefteaV2X.model;

/**
 * 
 * @author GunnarF
 *
 */
public class Location {

	private final String name;
	
	private boolean allowsCharging;
	
	public Location(String name, boolean allowsCharging) {
		this.name = name;
		this.allowsCharging = allowsCharging;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setAllowsCharging(boolean allowsCharging) {
		this.allowsCharging = allowsCharging;
	}
	
	public boolean getAllowsCharging() {
		return this.allowsCharging;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
}
