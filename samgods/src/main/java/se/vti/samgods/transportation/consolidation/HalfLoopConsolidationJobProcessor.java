/**
 * se.vti.samgods.transportation.consolidation.halfloop
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
import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSize;
import se.vti.samgods.network.NetworkData2;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class HalfLoopConsolidationJobProcessor implements Runnable {

	// -------------------- CONSTANTS --------------------

	private final ConsolidationCostModel consolidationCostModel;
	private final NetworkData2 networkData;

	private final BlockingQueue<ConsolidationJob> jobQueue;
	private final ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment;

	// -------------------- CONSTRUCTION --------------------

	public HalfLoopConsolidationJobProcessor(ConsolidationCostModel consolidationCostModel, NetworkData2 networkData,
			BlockingQueue<ConsolidationJob> jobQueue,
			ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment) {
		this.consolidationCostModel = consolidationCostModel;
		this.networkData = networkData;
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
				FleetAssignment fleetAssignment = null;
				try {
					fleetAssignment = this.computeOptimalFleetAssignment(job);
				} catch (InsufficientDataException e) {
					e.log(this.getClass(), "Consolidation data missing", job.consolidationUnit.commodity, null,
							job.consolidationUnit.mode, job.consolidationUnit.isContainer,
							job.consolidationUnit.containsFerry);
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
		public final int fleetSizeDuringActiveServiceInterval;
		public final double loopsPerActiveServiceInterval;
		public final double loopLength_km;
		public final double minLoopDuration_h;

		public final double serviceInterval_h;
		public final double serviceDemandPerActiveServiceInterval_ton;

		public final double payload_ton;
		public final double unitCost_1_tonKm;

		public final double serviceIntervalActiveProba;

		public FleetAssignment(VehicleType vehicleType, FreightVehicleAttributes vehicleAttrs,
				DetailedTransportCost cost, double serviceDemandPerActiveServiceInterval_ton, double serviceInterval_h,
				double serviceIntervalActiveProba, double length_km) {

			this.vehicleType = vehicleType;
			this.serviceInterval_h = serviceInterval_h;
			this.serviceDemandPerActiveServiceInterval_ton = serviceDemandPerActiveServiceInterval_ton;
			this.serviceIntervalActiveProba = serviceIntervalActiveProba;
			this.loopLength_km = 2.0 * length_km;
			this.minLoopDuration_h = 2.0 * cost.duration_h;

			// n is fleet size
			// f is circulation frequency, per service interval
			final double nf = Math.max(1.0, serviceDemandPerActiveServiceInterval_ton / vehicleAttrs.capacity_ton);

			final double fMin = 1.0; // desirable: complete at least one loop
			final double fMax = serviceInterval_h / this.minLoopDuration_h; // hard physical constraint

			final int n;
			final double f;
			if (fMin <= fMax) {
				/*
				 * Possible to complete at least fMin=1 loops and up to fMax loops.
				 */
				n = Math.max(1, (int) Math.ceil(nf / fMax)); // implies n >= nf / fMax <=> nf / n <= fMax
				f = Math.max(fMin, nf / n); // by the above, this yield fMin <= f <= fMax
			} else {
				/*
				 * Impossible to complete fMin=1 loops. Choose max. physically feasible f =
				 * fMax;
				 */
				n = Math.max(1, (int) Math.ceil(nf / fMax));
				f = fMax;
			}
			assert (f == Math.min(fMax, Math.max(fMin, nf / n)));

			this.fleetSizeDuringActiveServiceInterval = n;
			this.loopsPerActiveServiceInterval = f;

			this.payload_ton = serviceDemandPerActiveServiceInterval_ton / n / f;
			this.unitCost_1_tonKm = cost.monetaryCost / this.payload_ton / (0.5 * this.loopLength_km);

			final double supplyPerActiveServiceInterval_ton = this.fleetSizeDuringActiveServiceInterval
					* this.loopsPerActiveServiceInterval * vehicleAttrs.capacity_ton;
			assert (serviceDemandPerActiveServiceInterval_ton <= 1e-8 + supplyPerActiveServiceInterval_ton);
		}

		public double vehicleCapacity_ton() {
			return FreightVehicleAttributes.getFreightAttributesSynchronized(this.vehicleType).capacity_ton;
		}

		public double snapshotVehicleCnt() {
			return this.serviceIntervalActiveProba * this.fleetSizeDuringActiveServiceInterval;
		}

		public double annualNumberOfServices() {
			return this.serviceIntervalActiveProba * this.fleetSizeDuringActiveServiceInterval
					* this.loopsPerActiveServiceInterval;
		}

		public double annualKm() {
			return this.annualNumberOfServices() * this.loopLength_km;
		}

		public double transportEfficiency() {
			return this.payload_ton
					/ FreightVehicleAttributes.getFreightAttributesSynchronized(this.vehicleType).capacity_ton;
		}

//		@Override
//		public String toString() {
//			return vehicleCnt + " vehicles of type " + this.vehicleType.getId() + " with capacity "
//					+ FreightVehicleAttributes.getCapacity_ton(this.vehicleType) + " operating in "
//					+ this.effectiveNumberOfServiceIntervals + " service intervals";
//		}
	}

	private FleetAssignment dimensionFleetAssignment(VehicleType vehicleType, FreightVehicleAttributes vehicleAttrs,
			ConsolidationJob job, double serviceDemand_ton, double serviceProba, double length_km)
			throws InsufficientDataException {
		FleetAssignment result = new FleetAssignment(vehicleType, vehicleAttrs,
				this.consolidationCostModel.computeSignatureCost(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton,
						job.consolidationUnit, true, true,
						this.networkData.getLinkId2unitCost(job.consolidationUnit.commodity, vehicleType),
						this.networkData.getFerryLinkIds()),
				serviceDemand_ton, Units.H_PER_D * job.serviceInterval_days, serviceProba, length_km);
		boolean done = false;
		while (!done) {
			FleetAssignment newResult = new FleetAssignment(vehicleType, vehicleAttrs,
					this.consolidationCostModel.computeSignatureCost(vehicleAttrs, result.payload_ton,
							job.consolidationUnit, true, true,
							this.networkData.getLinkId2unitCost(job.consolidationUnit.commodity, vehicleType),
							this.networkData.getFerryLinkIds()),
					serviceDemand_ton, Units.H_PER_D * job.serviceInterval_days, serviceProba, length_km);
			final double dev = Math.abs(newResult.unitCost_1_tonKm - result.unitCost_1_tonKm) / result.unitCost_1_tonKm;
			done = (dev < 1e-8);
			result = newResult;
		}
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(ConsolidationJob job) throws InsufficientDataException {

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
		final double demandExpectationPerActiveServiceInterval_ton = (1.0 / serviceIntervalActiveProba)
				* (job.serviceInterval_days / 365.0)
				* job.choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();

		final List<VehicleType> compatibleVehicleTypes = this.networkData.getCompatibleVehicleTypes(
				job.consolidationUnit.commodity, job.consolidationUnit.mode, job.consolidationUnit.isContainer,
				job.consolidationUnit.containsFerry);

		if ((compatibleVehicleTypes == null) || (compatibleVehicleTypes.size() == 0)) {
			throw new InsufficientDataException(this.getClass(), "No compatible vehicle type found.",
					job.consolidationUnit.commodity, null, job.consolidationUnit.mode,
					job.consolidationUnit.isContainer, job.consolidationUnit.containsFerry);
		}

		FleetAssignment overallBestAssignment = null;
		for (VehicleType vehicleType : compatibleVehicleTypes) {
			FleetAssignment bestAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType,
					FreightVehicleAttributes.getFreightAttributesSynchronized(vehicleType), job,
					demandExpectationPerActiveServiceInterval_ton, serviceIntervalActiveProba,
					Units.KM_PER_M * job.consolidationUnit.length_m);
			if ((overallBestAssignment == null)
					|| (bestAssignmentForVehicleType.unitCost_1_tonKm < overallBestAssignment.unitCost_1_tonKm)) {
				overallBestAssignment = bestAssignmentForVehicleType;
			}
		}
		return overallBestAssignment;
	}
}
