/**
 * se.vti.samgods.utils
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
package se.vti.samgods.utils;

import java.util.Arrays;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class CommodityModeGrouping extends TupleGrouping<SamgodsConstants.Commodity, SamgodsConstants.TransportMode> {

	public CommodityModeGrouping(Iterable<Commodity> firstDomain, Iterable<TransportMode> secondDomain) {
		super(firstDomain, secondDomain);
	}

	public CommodityModeGrouping() {
		this(Arrays.asList(SamgodsConstants.Commodity.values()),
				Arrays.asList(SamgodsConstants.TransportMode.values()));
	}

}
