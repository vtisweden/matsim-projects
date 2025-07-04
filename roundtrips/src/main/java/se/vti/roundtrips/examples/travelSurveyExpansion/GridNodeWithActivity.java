/**
 * se.vti.roundtrips.examples.truckServiceCoverage
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.examples.travelSurveyExpansion;

import se.vti.roundtrips.common.Node;

/**
 * 
 * @author GunnarF
 *
 */
class GridNodeWithActivity extends Node {

	final int row;

	final int column;

	GridNodeWithActivity(int row, int column, Activity activity) {
		super("(" + row + "," + column + ")", activity);
		this.row = row;
		this.column = column;
	}

	Activity getActivity() {
		return (Activity) this.getLabels().get(0);
	}

	int computeGridDistance(GridNodeWithActivity other) {
		return Math.abs(this.row - other.row) + Math.abs(this.column - other.column);
	}
}
