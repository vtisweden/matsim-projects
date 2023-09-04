/**
 * se.vti.matsim.dynameq
 * 
 * Copyright (C) 2023 by Filip Kristofferson (VTI) and Gunnar Flötteröd (VTI, LiU).
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
package se.vti.matsim.dynameq.network;

import se.vti.matsim.dynameq.utils.Units;

/**
 * 
 * @author FilipK
 *
 */
public class Utils {

	public static double freespeedAndNumLanesToCapacity_VEH_H(double freespeed_M_S, int numLanes) {
		final double w = 15.0 * Units.M_S_PER_KM_H;
		final double rMax = 140.0 / 1000.0; // In veh per m
		return (freespeed_M_S * w / (freespeed_M_S + w) * rMax) * Units.VEH_H_PER_VEH_S * numLanes;
	}

	public static String toCentroidId(String id) {
		return "centroid_" + id;
	}

	public static String toVirtualLinkId(String id) {
		return "virtual_link_" + id;
	}

	public static String centroidToSuperCentroidId(String id) {
		return "super_" + id;
	}
	
	public static final String NODE_LINK_TYPE_ATTRIBUTE_KEY = "matsim_type";

	public class NodeTypeConstants {
		public static final String NODE = "node";
		public static final String CENTROID = "centroid";
		public static final String SUPER_CENTROID = "super_centroid";
	}

	public class LinkTypeConstants {
		public static final String LINK = "link";
		public static final String VIRTUAL_LINK = "virtual_link";
		public static final String SUPER_VIRTUAL_LINK = "super_virtual_link";
	}
}
