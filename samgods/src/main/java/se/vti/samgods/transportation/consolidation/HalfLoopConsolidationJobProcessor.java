/**
 * se.vti.samgods.transportation.consolidation
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
package se.vti.samgods.transportation.consolidation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.RealizedInVehicleCost;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;
import se.vti.samgods.utils.ChoiceModelUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class HalfLoopConsolidationJobProcessor implements Runnable {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(HalfLoopConsolidationJobProcessor.class);

	private final RealizedInVehicleCost realizedInVehicleCost = new RealizedInVehicleCost();

	private final NetworkData networkData;
	private final FleetData fleetData;

	private final BlockingQueue<ConsolidationJob> jobQueue;
	private final ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment;

	private final Map<Commodity, Double> commodity2scale;

	private int noCompatibleVehicleTypeWarnings = 0;
	private int couldNotComputeFleetAssignmentWarnings = 0;

	// -------------------- CONSTRUCTION --------------------

	public HalfLoopConsolidationJobProcessor(NetworkData networkData, FleetData fleetData,
			BlockingQueue<ConsolidationJob> jobQueue,
			ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			final Map<Commodity, Double> commodity2scale) {
		this.networkData = networkData;
		this.fleetData = fleetData;
		this.jobQueue = jobQueue;
		this.consolidationUnit2fleetAssignment = consolidationUnit2fleetAssignment;
		this.commodity2scale = commodity2scale;
	}

	// -------------------- IMPLEMENTATION OF Runnable --------------------

	@Override
	public void run() {
		try {
			while (true) {
				ConsolidationJob job = this.jobQueue.take();
				if (job == ConsolidationJob.TERMINATE) {
					break;
				}
				final FleetAssignment fleetAssignment = this.computeOptimalFleetAssignment(job);
				if (fleetAssignment != null) {
					this.consolidationUnit2fleetAssignment.put(job.consolidationUnit, fleetAssignment);
				} else {
					if (this.couldNotComputeFleetAssignmentWarnings < 10) {
						log.warn("Could not compute fleet assignment: " + job);
					}
					if (++this.couldNotComputeFleetAssignmentWarnings == 10) {
						log.warn("Suppressing further warnings of this type.");
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Handle thread interruption
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public static class FleetAssignment {

		public final double realDemand_ton;

		public final VehicleType vehicleType;
		public final double loopLength_km;
		public final double minLoopDuration_h;

		public final double expectedSnapshotVehicleCnt;
		public final double payload_ton;
		public final double unitCost_1_tonKm;

		public final double domesticLoopLength_km;

		public FleetAssignment(double realDemand_ton, VehicleType vehicleType, double vehicleCapacity_ton,
				DetailedTransportCost cost, double serviceIntervalActiveProba, ConsolidationJob job) {

			this.realDemand_ton = realDemand_ton;

			this.vehicleType = vehicleType;
			this.loopLength_km = 2.0 * job.consolidationUnit.length_km;
			this.minLoopDuration_h = 2.0 * cost.duration_h;

			this.domesticLoopLength_km = 2.0 * job.consolidationUnit.domesticLength_km;

			final double serviceInterval_h = Units.H_PER_D * job.serviceInterval_days;
			final double serviceDemandPerActiveServiceInterval_ton = (1.0 / serviceIntervalActiveProba)
					* (job.serviceInterval_days / 365.0) * (realDemand_ton);

			// n is fleet size. f is circulation frequency, per service interval
			final double nf = Math.max(1.0, serviceDemandPerActiveServiceInterval_ton / vehicleCapacity_ton);

			final double fMin = 1.0; // desirable: complete at least one loop
			final double fMax = serviceInterval_h / this.minLoopDuration_h; // hard physical constraint

			final int n;
			final double f;
			if (fMin <= fMax) {
				// Possible to complete at least fMin=1 loops and up to fMax loops.
				n = Math.max(1, (int) Math.ceil(nf / fMax)); // implies n >= nf / fMax <=> nf / n <= fMax
				f = Math.max(fMin, nf / n); // by the above, this yield fMin <= f <= fMax
			} else {
				// Impossible to complete fMin=1 loops. Choose max. feasible f = fMax;
				n = Math.max(1, (int) Math.ceil(nf / fMax));
				f = fMax;
			}
			assert (f == Math.min(fMax, Math.max(fMin, nf / n)));

			this.payload_ton = serviceDemandPerActiveServiceInterval_ton / n / f;
			this.unitCost_1_tonKm = cost.monetaryCost / this.payload_ton / (0.5 * this.loopLength_km);

			final double supplyPerActiveServiceInterval_ton = n * f * vehicleCapacity_ton;
			assert (serviceDemandPerActiveServiceInterval_ton <= 1e-8 + supplyPerActiveServiceInterval_ton);

			this.expectedSnapshotVehicleCnt = serviceIntervalActiveProba * n;

		}

		@Override
		public String toString() {
			return "Loop of dimensions " + loopLength_km + "km, " + minLoopDuration_h + "h uses vehicle type "
					+ this.vehicleType.getId() + ". Random snapshot sees " + this.expectedSnapshotVehicleCnt
					+ " vehicles carrying an average payload of " + this.payload_ton + "ton at a unit cost of "
					+ this.unitCost_1_tonKm + " 1/ton";
		}
	}

	private FleetAssignment dimensionFleetAssignment(double realDemand_ton, VehicleType vehicleType,
			ConsolidationJob job, double serviceIntervalActiveProba) {
		final SamgodsVehicleAttributes vehicleAttrs = this.fleetData.getVehicleType2attributes().get(vehicleType);
		FleetAssignment result = new FleetAssignment(realDemand_ton, vehicleType, vehicleAttrs.capacity_ton,
				this.realizedInVehicleCost.compute(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton, job.consolidationUnit,
						this.networkData.getLinkId2unitCost(vehicleType), this.networkData.getFerryLinkIds()),
				serviceIntervalActiveProba, job);
		boolean done = false;
		final int maxIts = 100;
//		List<Double> devList = new ArrayList<>(maxIts);
		int its = 0;
		while (!done) {
			// TODO break when too many iterations
			FleetAssignment newResult = new FleetAssignment(realDemand_ton, vehicleType, vehicleAttrs.capacity_ton,
					this.realizedInVehicleCost.compute(vehicleAttrs, result.payload_ton, job.consolidationUnit,
							this.networkData.getLinkId2unitCost(vehicleType), this.networkData.getFerryLinkIds()),
					serviceIntervalActiveProba, job);
			final double dev = Math.abs(newResult.unitCost_1_tonKm - result.unitCost_1_tonKm) / result.unitCost_1_tonKm;
//			devList.add(dev);
			if (++its == maxIts) {
				this.log.warn("Too many iterations, terminating with relative unit cost deviation " + dev + ".");
				done = true;
			} else {
				done = (dev < 1e-8);
			}
			result = newResult;
		}
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(ConsolidationJob job) {

		// Compute real and background demand.
		final double realDemand_ton = job.choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton())
				.sum();
//		final double backgroundDemand_ton = 0;
//				this.logisticChoiceData.computeFreightOffset_ton(
//				job.consolidationUnit.commodity, job.consolidationUnit.samgodsMode, realDemand_ton);

		// Compute expected demand during an *active* service interval.
		final double serviceIntervalActiveProba;
		{
			double probaSingleServiceIntervalInactive = 1.0;
			for (ChainAndShipmentSize choice : job.choices) {
				final double meanShipmentsPerYear = Math.max(1.0,
						choice.annualShipment.getTotalAmount_ton() / choice.sizeClass.upperValue_ton);
				final double meanShipmentsPerServiceInterval = meanShipmentsPerYear * job.serviceInterval_days / 365.0;
				probaSingleServiceIntervalInactive *= Math.exp(-meanShipmentsPerServiceInterval);
			}
			serviceIntervalActiveProba = 1.0 - probaSingleServiceIntervalInactive;
		}

		// Identify compatible vehicles. If none, give up.
//		final List<VehicleType> compatibleVehicleTypes = this.fleetData
//				.getCompatibleVehicleTypes(job.consolidationUnit.commodity, job.consolidationUnit.samgodsMode,
//						job.consolidationUnit.isContainer, job.consolidationUnit.containsFerry);
		final List<VehicleType> compatibleVehicleTypes = new ArrayList<>(
				job.consolidationUnit.linkCompatibleVehicleTypes);
		assert (this.fleetData
				.getCompatibleVehicleTypes(job.consolidationUnit.commodity, job.consolidationUnit.samgodsMode,
						job.consolidationUnit.isContainer, job.consolidationUnit.containsFerry)
				.containsAll(compatibleVehicleTypes));
				
		if ((compatibleVehicleTypes == null) || (compatibleVehicleTypes.size() == 0)) {
			if (this.noCompatibleVehicleTypeWarnings < 10) {
				log.warn("No compatible vehicle types found: " + job);
				if (++this.noCompatibleVehicleTypeWarnings == 10) {
					log.warn("Suppressing further warnings of this type.");
				}
			}
			return null;
		}

		if (job.consolidationUnit.isContainer
				&& SamgodsConstants.TransportMode.Rail.equals(job.consolidationUnit.samgodsMode)) {

		}

		// Identify optimal fleet assigment.
		final double scale = this.commodity2scale.get(job.consolidationUnit.commodity);

		final List<FleetAssignment> assignments = new ArrayList<>(compatibleVehicleTypes.size());
		final Map<FleetAssignment, Double> assignment2utility = new LinkedHashMap<>(compatibleVehicleTypes.size());
		for (VehicleType vehicleType : compatibleVehicleTypes) {
			final FleetAssignment assignment = this.dimensionFleetAssignment(realDemand_ton, vehicleType, job,
					serviceIntervalActiveProba);
			final double utility = (-1.0) * scale * assignment.unitCost_1_tonKm * 0.5 * assignment.loopLength_km
					* realDemand_ton + this.fleetData.getVehicleType2asc().getOrDefault(vehicleType, 0.0);
			assignments.add(assignment);
			assignment2utility.put(assignment, utility);
		}

		return new ChoiceModelUtils().choose(assignments, a -> assignment2utility.get(a));
	}
}
