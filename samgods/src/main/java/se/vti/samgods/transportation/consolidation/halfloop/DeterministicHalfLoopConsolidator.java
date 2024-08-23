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
package se.vti.samgods.transportation.consolidation.halfloop;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSize;
import se.vti.samgods.transportation.DetailedTransportCost;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class DeterministicHalfLoopConsolidator {

	// -------------------- CONSTANTS --------------------

	private final VehicleFleet fleet;

	private final ConsolidationCostModel consolidationCostModel;

	private final Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days;
	private final Map<SamgodsConstants.Commodity, Double> commodity2numberOfServiceIntervals;

	private final boolean flexiblePeriod;

	private final boolean skipUnusedIntervals;

	// -------------------- CONSTRUCTION --------------------

	public DeterministicHalfLoopConsolidator(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days, boolean flexiblePeriod,
			boolean skipUnusedIntervals) {
		this.fleet = fleet;
		this.consolidationCostModel = consolidationCostModel;
		this.commodity2serviceInterval_days = commodity2serviceInterval_days;
		this.commodity2numberOfServiceIntervals = commodity2serviceInterval_days.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> 365.0 / e.getValue()));
		this.flexiblePeriod = flexiblePeriod;
		this.skipUnusedIntervals = skipUnusedIntervals;
	}

	// -------------------- IMPLEMENTATION --------------------

	private double computeSingleServiceIntervalUsageProba(double numberOfServiceIntervals,
			List<ChainAndShipmentSize> choices) {
		if (!this.skipUnusedIntervals) {
			return 1.0;
		}
		double probaSingleServiceIntervalNotUsed = 1.0;
		for (ChainAndShipmentSize choice : choices) {
			final double serviceRequestsPerYearForInstance = Math.max(1,
					Math.min(365, choice.annualShipment.getSingleInstanceAnnualAmount_ton()
							/ choice.sizeClass.getRepresentativeValue_ton()));
			final double probaServiceIntervalUsedForInstance = Math.min(1.0,
					serviceRequestsPerYearForInstance / numberOfServiceIntervals);
			probaSingleServiceIntervalNotUsed *= Math.pow(1.0 - probaServiceIntervalUsedForInstance,
					choice.annualShipment.getNumberOfInstances());
		}
		return Math.max(1.0 / numberOfServiceIntervals, 1.0 - probaSingleServiceIntervalNotUsed);
	}

	public static class FleetAssignment {

		public final VehicleType vehicleType;
		public final double halfLoopDuration_h;
		private final double halfLoopMovementDuration_h;
		public final int minVehicleCnt;
		public final int vehicleCnt;
		public final double payload_ton;
		public final double unitCost_1_ton;
		public final double effectiveNumberOfServiceIntervals;
		private final double serviceProba;

		public FleetAssignment(VehicleType vehicleType, DetailedTransportCost halfLoopCost, Integer vehicleCnt,
				double totalDemand_ton, boolean flexiblePeriod, int serviceInterval_days, double serviceProba) {

			final double vehCap_ton = FreightVehicleAttributes.getCapacity_ton(vehicleType);
			this.vehicleType = vehicleType;
			this.serviceProba = serviceProba;
			this.halfLoopMovementDuration_h = halfLoopCost.duration_h;

			final double serviceInterval_h = 24.0 * serviceInterval_days;
			double numberOfServiceIntervals = 365.0 / serviceInterval_days;
			this.effectiveNumberOfServiceIntervals = serviceProba * numberOfServiceIntervals;

			final double effectiveDemandRate_ton_h = totalDemand_ton / (365.0 * 24.0) / serviceProba;

			/*-
			 * Per realized loop:
			 *              supply >= minDemand
			 *  => vehCap * vehCnt >= minLoopDur * effectiveDemandRate
			 * <=>          vehCnt >= minLoopDur * effectiveDemandRate / vehCap
			 */
			this.minVehicleCnt = (int) Math
					.ceil(2.0 * halfLoopCost.duration_h * effectiveDemandRate_ton_h / vehCap_ton);
			if (vehicleCnt == null) {
				vehicleCnt = this.minVehicleCnt;
			} else if (vehicleCnt < this.minVehicleCnt) {
				throw new RuntimeException("vehicleCnt " + vehicleCnt + " < minVehicleCnt " + this.minVehicleCnt);
			}
			this.vehicleCnt = vehicleCnt;

			if (flexiblePeriod && (halfLoopCost.duration_h <= 0.5 * serviceInterval_h)) {
				/*
				 * TODO possible refinement: require that all loops are *completed* within the
				 * service duration.
				 */
				double unboundedHalfLoopDuration_h = this.vehicleCnt * vehCap_ton / 2.0 / effectiveDemandRate_ton_h;
				this.halfLoopDuration_h = Math.max(halfLoopCost.duration_h,
						Math.min(unboundedHalfLoopDuration_h, 0.5 * serviceInterval_h));
			} else {
				this.halfLoopDuration_h = halfLoopCost.duration_h;
			}

			this.payload_ton = 2.0 * this.halfLoopDuration_h * effectiveDemandRate_ton_h / this.vehicleCnt;
			this.unitCost_1_ton = halfLoopCost.monetaryCost / this.payload_ton;
		}

		public double expectedNumberOfSimultaneouslyMovingVehicles() {
			return this.serviceProba * this.vehicleCnt * (this.halfLoopMovementDuration_h / this.halfLoopDuration_h);
		}

		public double transportEfficiency() {
			return this.payload_ton / FreightVehicleAttributes.getCapacity_ton(this.vehicleType);
		}

		@Override
		public String toString() {
			return vehicleCnt + " vehicles of type " + this.vehicleType.getId() + " with capacity "
					+ FreightVehicleAttributes.getCapacity_ton(this.vehicleType) + " operating in "
					+ this.effectiveNumberOfServiceIntervals + " service intervals";
		}
	}

	private FleetAssignment dimensionFleetAssignment(VehicleType vehicleType, Integer vehicleCnt,
			Signature.ConsolidationEpisode signature, double totalDemand_ton, double serviceProba)
			throws InsufficientDataException {
		final FreightVehicleAttributes vehicleAttrs = FreightVehicleAttributes.getFreightAttributes(vehicleType);
		FleetAssignment result = new FleetAssignment(vehicleType,
				this.consolidationCostModel.computeSignatureCost(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton,
						signature),
				vehicleCnt, totalDemand_ton, this.flexiblePeriod,
				this.commodity2serviceInterval_days.get(signature.commodity), serviceProba);
		boolean done = false;
		while (!done) {
			final FleetAssignment newResult = new FleetAssignment(vehicleType,
					this.consolidationCostModel.computeSignatureCost(vehicleAttrs, result.payload_ton, signature),
					result.vehicleCnt, totalDemand_ton, this.flexiblePeriod,
					this.commodity2serviceInterval_days.get(signature.commodity), serviceProba);
			final double dev = Math.abs(newResult.unitCost_1_ton - result.unitCost_1_ton) / result.unitCost_1_ton;
			done = (dev < 1e-8);
			result = newResult;

			if (newResult.vehicleCnt > 100 * 1000) {
				throw new RuntimeException("More than 100'000 vehicles in dimensionFleetAssignment(..), total demand = "
						+ totalDemand_ton + " ton, signature: " + signature);
			}

		}
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(Signature.ConsolidationEpisode signature,
			List<ChainAndShipmentSize> choices) throws InsufficientDataException {

		final double serviceProba = this.computeSingleServiceIntervalUsageProba(
				this.commodity2numberOfServiceIntervals.get(signature.commodity), choices);
		final double totalDemand_ton = choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();

		final List<VehicleType> compatibleVehicleTypes = this.fleet.getCompatibleVehicleTypes(signature.commodity,
				signature.mode, signature.isContainer, signature.containsFerry);
		if ((compatibleVehicleTypes == null) || (compatibleVehicleTypes.size() == 0)) {
			throw new InsufficientDataException(this.getClass(), "No compatible vehicle type found.",
					signature.commodity, null, signature.mode, signature.isContainer, signature.containsFerry);
		}

		FleetAssignment overallBestAssignment = null;
		for (VehicleType vehicleType : compatibleVehicleTypes) {
			FleetAssignment bestAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType, null, signature,
					totalDemand_ton, serviceProba);
			boolean done = false;
			while (!done) {
				final FleetAssignment candAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType,
						bestAssignmentForVehicleType.vehicleCnt + 1, signature, totalDemand_ton, serviceProba);
				if (candAssignmentForVehicleType.unitCost_1_ton < bestAssignmentForVehicleType.unitCost_1_ton) {
					bestAssignmentForVehicleType = candAssignmentForVehicleType;
				} else {
					done = true;
				}

				if (candAssignmentForVehicleType.vehicleCnt > 100 * 1000) {
					throw new RuntimeException(
							"More than 100'000 vehicles in computeOptimalFleetAssignment(..), total demand = "
									+ totalDemand_ton + " ton, signature: " + signature);
				}

			}
			if ((overallBestAssignment == null)
					|| (bestAssignmentForVehicleType.unitCost_1_ton < overallBestAssignment.unitCost_1_ton)) {
				overallBestAssignment = bestAssignmentForVehicleType;
			}
		}
		return overallBestAssignment;
	}
}
