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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.RealizedInVehicleCost;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class HalfLoopConsolidationJobProcessor implements Runnable {

	// -------------------- CONSTANTS --------------------

	private final RealizedInVehicleCost realizedInVehicleCost = new RealizedInVehicleCost();

	private final NetworkData networkData;
	private final FleetData fleetData;

	private final BlockingQueue<ConsolidationJob> jobQueue;
	private final ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment;

	// -------------------- CONSTRUCTION --------------------

	public HalfLoopConsolidationJobProcessor(NetworkData networkData, FleetData fleetData,
			BlockingQueue<ConsolidationJob> jobQueue,
			ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment) {
		this.networkData = networkData;
		this.fleetData = fleetData;
		this.jobQueue = jobQueue;
		this.consolidationUnit2fleetAssignment = consolidationUnit2fleetAssignment;
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
				FleetAssignment fleetAssignment;
				try {
					fleetAssignment = this.computeOptimalFleetAssignment(job);
				} catch (InsufficientDataException e) {
					fleetAssignment = null;
					InsufficientDataException.log(e,
							new InsufficientDataException(this.getClass(), "could not consolidate"));
				}
				if (fleetAssignment != null) {
					this.consolidationUnit2fleetAssignment.put(job.consolidationUnit, fleetAssignment);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Handle thread interruption
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public static class FleetAssignment {

		public final VehicleType vehicleType;
		public final double loopLength_km;
		public final double minLoopDuration_h;

		public final double expectedSnapshotVehicleCnt;
		public final double payload_ton;
		public final double unitCost_1_tonKm;

		public FleetAssignment(VehicleType vehicleType, double vehicleCapacity_ton, DetailedTransportCost cost,
				double serviceDemandPerActiveServiceInterval_ton, double serviceInterval_h,
				double serviceIntervalActiveProba, double length_km) {

			this.vehicleType = vehicleType;
			this.loopLength_km = 2.0 * length_km;
			this.minLoopDuration_h = 2.0 * cost.duration_h;

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

	private FleetAssignment dimensionFleetAssignment(VehicleType vehicleType, ConsolidationJob job,
			double serviceDemand_ton, double serviceProba, double length_km) throws InsufficientDataException {
		final SamgodsVehicleAttributes vehicleAttrs = this.fleetData.getVehicleType2attributes().get(vehicleType);
		FleetAssignment result = new FleetAssignment(vehicleType, vehicleAttrs.capacity_ton,
				this.realizedInVehicleCost.compute(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton, job.consolidationUnit,
						this.networkData.getLinkId2unitCost(vehicleType), this.networkData.getFerryLinkIds()),
				serviceDemand_ton, Units.H_PER_D * job.serviceInterval_days, serviceProba, length_km);
		boolean done = false;
		while (!done) {
			FleetAssignment newResult = new FleetAssignment(vehicleType, vehicleAttrs.capacity_ton,
					this.realizedInVehicleCost.compute(vehicleAttrs, result.payload_ton, job.consolidationUnit,
							this.networkData.getLinkId2unitCost(vehicleType), this.networkData.getFerryLinkIds()),
					serviceDemand_ton, Units.H_PER_D * job.serviceInterval_days, serviceProba, length_km);
			final double dev = Math.abs(newResult.unitCost_1_tonKm - result.unitCost_1_tonKm) / result.unitCost_1_tonKm;
			done = (dev < 1e-8);
			result = newResult;
		}
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(ConsolidationJob job) throws InsufficientDataException {

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
		final double expectedDemandPerActiveServiceInterval_ton = (1.0 / serviceIntervalActiveProba)
				* (job.serviceInterval_days / 365.0)
				* job.choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();

		// Identify compatible vehicles. If none, give up.
		final List<VehicleType> compatibleVehicleTypes = this.fleetData
				.getCompatibleVehicleTypes(job.consolidationUnit);
		if ((compatibleVehicleTypes == null) || (compatibleVehicleTypes.size() == 0)) {
			throw new InsufficientDataException(this.getClass(), "No compatible vehicle type found.",
					job.consolidationUnit);
		}

		// Identify optimal fleet assigment.
		FleetAssignment overallBestAssignment = null;
		for (VehicleType vehicleType : compatibleVehicleTypes) {
			FleetAssignment bestAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType, job,
					expectedDemandPerActiveServiceInterval_ton, serviceIntervalActiveProba,
					job.consolidationUnit.length_km);
			if ((overallBestAssignment == null)
					|| (bestAssignmentForVehicleType.unitCost_1_tonKm < overallBestAssignment.unitCost_1_tonKm)) {
				overallBestAssignment = bestAssignmentForVehicleType;
			}
		}
		return overallBestAssignment;
	}
}
