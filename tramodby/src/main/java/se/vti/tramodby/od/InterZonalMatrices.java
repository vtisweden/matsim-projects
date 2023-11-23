/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.tramodby.od;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import floetteroed.utilities.TimeDiscretization;
import se.vti.tramodby.module.TramodByConfigGroup;
import se.vti.tramodby.od.ZonalSystem.Zone;

/**
 * This class acts as a container for Origin-Destination (OD) matrices and are sliced into time bins. 
 *
 */
public class InterZonalMatrices {

	/**
	 * This class is the internal structure of the OD-matrix.
	 *
	 */
	public class Matrix {

		private Map<Id<Zone>, Map<Id<Zone>, Double>> orig2dest2val;

		/**
		 * Main constructor.
		 */
		Matrix() {
			this.orig2dest2val = new LinkedHashMap<>();
		}

		/**
		 * This method gets the map of destinations for a specific origin zone.
		 * @param orig - the origin zone id
		 * @return map with the demand for each associated destination
		 */
		private Map<Id<Zone>, Double> getDest2Val(Id<Zone> orig) {
			return this.orig2dest2val.computeIfAbsent(orig, orig2 -> new LinkedHashMap<Id<Zone>, Double>());
		}

		/**
		 * This method sets the demand between a origin and a destination zone.
		 *  
		 * @param orig - the origin zone id
		 * @param dest - the destination zone id
		 * @param val - the demand
		 */
		synchronized void put(Id<Zone> orig, Id<Zone> dest, Double val) {
			// Removes the destination if the demand to it is zero.
			if (val == 0.0) {
				this.getDest2Val(orig).remove(dest);
			}
			// Sets the demand to the destination.
			else {
				this.getDest2Val(orig).put(dest, val);
			}
		}

		/**
		 * This method gets the demand between a origin and a destination zone.
		 * 
		 * @param orig - the origin zone id
		 * @param dest - the destination zone id
		 * @return the demand
		 */
		public synchronized double get(Id<Zone> orig, Id<Zone> dest) {
			return this.getDest2Val(orig).getOrDefault(dest, 0.0);
		}

		/**
		 * This method adds demand between between a origin and a destination zone.
		 * 
		 * @param orig - the origin zone id
		 * @param dest - the destination zone id
		 * @param val - the demand to add
		 */
		synchronized void add(Id<Zone> orig, Id<Zone> dest, double val) {
			this.put(orig, dest, val + this.get(orig, dest));
		}
		
		synchronized void clear() {
			this.orig2dest2val.clear();
		}

	}

	private final TimeDiscretization timeDiscr;

	private final List<Matrix> matrixList;

	/**
	 * Main constructor
	 * 
	 * @param timeDiscr - the time discretization data 
	 */
	public InterZonalMatrices(TimeDiscretization timeDiscr) {
		this.timeDiscr = timeDiscr;
		// Creates a OD matrix for each time bin.
		this.matrixList = new ArrayList<>(timeDiscr.getBinCnt());
		for (int bin = 0; bin < timeDiscr.getBinCnt(); bin++) {
			this.matrixList.add(new Matrix());
		}
	}

	/**
	 * Constructor with initialization from config group.
	 * 
	 * @param tramodByConfig - the TramodBy config group data
	 */
	public InterZonalMatrices(TramodByConfigGroup tramodByConfig) {
		this(new TimeDiscretization(tramodByConfig.getStartTime(), tramodByConfig.getBinSize(),
				tramodByConfig.getBinCount()));
	}

	/**
	 * This method gets the time discretization data.
	 * 
	 * @return the time discretization data 
	 */
	TimeDiscretization getTimeDiscretization() {
		return this.timeDiscr;
	}

	/**
	 * This method gets OD-matrix for each time bin as a list.
	 * 
	 * @return list of OD-matrices
	 */
	public List<Matrix> getMatrixListView() {
		return Collections.unmodifiableList(this.matrixList);
	}

	/**
	 * This method adds demand between a origin and destination zone for a specific time bin.
	 * 
	 * @param from - origin zone id
	 * @param to - destination zone id
	 * @param bin - time bin
	 * @param val - demand to add
	 */
	synchronized void addSynchronized(final Id<Zone> from, final Id<Zone> to, final int bin, final double val) {
		this.matrixList.get(bin).add(from, to, val);
	}

}
