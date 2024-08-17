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

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.logistics.TransportEpisode;
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
public class HalfLoopConsolidator {

	// -------------------- CONSTANTS --------------------

	private final VehicleFleet fleet;

	private final ConsolidationCostModel consolidationCostModel;

	private final double serviceInterval_days;
	private final double numberOfServiceIntervals;

	private final boolean flexiblePeriod;

	// -------------------- CONSTRUCTION --------------------

	public HalfLoopConsolidator(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			int serviceInterval_days, boolean flexiblePeriod) {
		this.fleet = fleet;
		this.consolidationCostModel = consolidationCostModel;
		this.serviceInterval_days = serviceInterval_days;
		this.numberOfServiceIntervals = Math.max(1, Math.min(365, 365.0 / this.serviceInterval_days));
		this.flexiblePeriod = flexiblePeriod;
	}

	// -------------------- IMPLEMENTATION --------------------

	public static class FleetAssignment {

		public final VehicleType vehicleType;
		public final double halfLoopDuration_h;
		public final double demandRate_ton_h;
		public final int minVehicleCnt;
		public final int vehicleCnt;
		public final double payload_ton;
		public final double effectiveCost_1_ton;
		public final double effectiveAssignmentPeriod_days;

		public FleetAssignment(VehicleType vehicleType, DetailedTransportCost halfLoopCost, Integer vehicleCnt,
				double demandRate_ton_h, double effectiveAssignmentPeriod_h, double serviceWindow_days,
				boolean flexiblePeriod) {
			this.vehicleType = vehicleType;
			final double vehCap_ton = FreightVehicleAttributes.getCapacity_ton(vehicleType);
			/*-
			 *              supply >= minDemand
			 *  => vehCap * vehCnt >= minLoopDur * demandRate
			 * <=>          vehCnt >= minLoopDur * demandRate / vehCap
			 */
			this.minVehicleCnt = (int) Math.ceil(2.0 * halfLoopCost.duration_h * demandRate_ton_h / vehCap_ton);

			if (vehicleCnt == null) {
				vehicleCnt = this.minVehicleCnt;
			} else if (vehicleCnt < this.minVehicleCnt) {
				throw new RuntimeException("vehicleCnt " + vehicleCnt + " < minVehicleCnt " + this.minVehicleCnt);
			}
			this.vehicleCnt = vehicleCnt;

			if (flexiblePeriod && (halfLoopCost.duration_h <= 0.5 * 24.0 * serviceWindow_days)) {
				double unboundedHalfLoopDuration_h = this.vehicleCnt * vehCap_ton / 2.0 / demandRate_ton_h;
				this.halfLoopDuration_h = Math.max(halfLoopCost.duration_h,
						Math.min(unboundedHalfLoopDuration_h, 0.5 * 24.0 * serviceWindow_days));
			} else {
				this.halfLoopDuration_h = halfLoopCost.duration_h;
			}

			this.demandRate_ton_h = demandRate_ton_h;
			this.effectiveAssignmentPeriod_days = effectiveAssignmentPeriod_h / 24.0;

			this.payload_ton = 2.0 * this.halfLoopDuration_h * demandRate_ton_h / this.vehicleCnt;
			this.effectiveCost_1_ton = halfLoopCost.monetaryCost / this.payload_ton;
		}

		@Override
		public String toString() {
			return vehicleCnt + " vehicles of type " + this.vehicleType.getId() + " with capacity "
					+ FreightVehicleAttributes.getCapacity_ton(this.vehicleType) + " ton each for total demand "
					+ this.demandRate_ton_h * 24 * 30 * 12 + " ton during effectively "
					+ this.effectiveAssignmentPeriod_days + " days";
		}
	}

//	private final static int hoursPerYear = 24 * 30 * 12;

	private double estimateEffectiveAssignmentPeriod_h(List<ChainAndShipmentSize> choices) {

		double probaSingleServiceIntervalNotUsed = 1.0;
		for (ChainAndShipmentSize choice : choices) {
			double shipmentsPerYearForInstance = Math.max(1,
					Math.min(365, choice.annualShipment.getSingleInstanceAnnualAmount_ton()
							/ choice.sizeClass.getRepresentativeValue_ton()));
			double shipmentsPerServiceIntervalForInstance = shipmentsPerYearForInstance / this.numberOfServiceIntervals;
			double probaServiceIntervalUsedForInstance = Math.min(1.0, shipmentsPerServiceIntervalForInstance);
			probaSingleServiceIntervalNotUsed *= Math.pow(1.0 - probaServiceIntervalUsedForInstance,
					choice.annualShipment.getNumberOfInstances());
		}

		double probaServiceIntervalUsed = 1.0 - probaSingleServiceIntervalNotUsed;
		return 24.0 * Math.min(365.0, this.serviceInterval_days / probaServiceIntervalUsed);

//		double probaSingleDayNotUsed = 1.0;
//		for (ChainAndShipmentSize choice : choices) {
//			double shipmentsPerYear = Math.max(1,
//					Math.min(365, choice.annualShipment.getSingleInstanceAnnualAmount_ton()
//							/ choice.sizeClass.getRepresentativeValue_ton()));
//			double probaSingleInstanceCoversDay = shipmentsPerYear / 365.0;
//			probaSingleDayNotUsed *= Math.pow(1.0 - probaSingleInstanceCoversDay,
//					choice.annualShipment.getNumberOfInstances());
//		}
//
//		double probaServiceIntervalUsed = 1.0 - Math.pow(probaSingleDayNotUsed, this.serviceWindow_days);
//		return Math.min(24 * 30 * 12, (24.0 * this.serviceWindow_days) / probaServiceIntervalUsed);
	}

	private FleetAssignment dimensionFleetAssignment(VehicleType vehicleType, Integer vehicleCnt,
			TransportEpisode episode, double demandRate_ton_h, double effectiveAssignmentPeriod_h)
			throws InsufficientDataException {
		final FreightVehicleAttributes vehicleAttrs = FreightVehicleAttributes.getFreightAttributes(vehicleType);

		FleetAssignment result = new FleetAssignment(vehicleType,
				this.consolidationCostModel.computeEpisodeCost(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton, episode),
				vehicleCnt, demandRate_ton_h, effectiveAssignmentPeriod_h, this.serviceInterval_days,
				this.flexiblePeriod);

		boolean done = false;
		while (!done) {
			FleetAssignment newResult = new FleetAssignment(vehicleType,
					this.consolidationCostModel.computeEpisodeCost(vehicleAttrs, result.payload_ton, episode),
					result.vehicleCnt, demandRate_ton_h, effectiveAssignmentPeriod_h, this.serviceInterval_days,
					this.flexiblePeriod);
			final double dev = Math.abs(newResult.effectiveCost_1_ton - result.effectiveCost_1_ton);
//			System.out.println(dev); // for testing;
			done = (dev < 0.01 / 1.0); // TODO ett öre / ton ... make configurable
			result = newResult;
		}
//		System.out.println(); // for testing
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(TransportEpisode episode, List<ChainAndShipmentSize> choices)
			throws InsufficientDataException {

		final double effectiveAssignmentPeriod_h = this.estimateEffectiveAssignmentPeriod_h(choices);
		final double demandRate_ton_h = choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum()
				/ effectiveAssignmentPeriod_h;

		final List<VehicleType> compatibleVehicleTypes = this.fleet.getCompatibleVehicleTypes(episode.getCommodity(),
				episode.getMode(), episode.isContainer(), episode.containsFerry());
		if (compatibleVehicleTypes == null || compatibleVehicleTypes.size() == 0) {
			throw new InsufficientDataException(this.getClass(), "No compatible vehicle type found.", episode);
		}

		FleetAssignment overallBestAssignment = null;

		for (VehicleType vehicleType : compatibleVehicleTypes) {

			FleetAssignment bestAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType, null, episode,
					demandRate_ton_h, effectiveAssignmentPeriod_h);
			boolean done = false;
			while (!done) {
				FleetAssignment candAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType,
						bestAssignmentForVehicleType.vehicleCnt + 1, episode, demandRate_ton_h,
						effectiveAssignmentPeriod_h);
				if (candAssignmentForVehicleType.effectiveCost_1_ton < bestAssignmentForVehicleType.effectiveCost_1_ton) {
					bestAssignmentForVehicleType = candAssignmentForVehicleType;
				} else {
					done = true;
				}
			}

			if ((overallBestAssignment == null)
					|| (bestAssignmentForVehicleType.effectiveCost_1_ton < overallBestAssignment.effectiveCost_1_ton)) {
				overallBestAssignment = bestAssignmentForVehicleType;
			}
		}

		return overallBestAssignment;
	}
}
