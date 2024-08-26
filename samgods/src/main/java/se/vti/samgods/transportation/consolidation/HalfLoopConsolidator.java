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
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSize;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
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

	private final Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days;
	private final Map<SamgodsConstants.Commodity, Double> commodity2numberOfServiceIntervals;

	// -------------------- CONSTRUCTION --------------------

	public HalfLoopConsolidator(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days, boolean flexiblePeriod,
			boolean skipUnusedIntervals) {
		this.fleet = fleet;
		this.consolidationCostModel = consolidationCostModel;
		this.commodity2serviceInterval_days = commodity2serviceInterval_days;
		this.commodity2numberOfServiceIntervals = commodity2serviceInterval_days.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> 365.0 / e.getValue()));
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

		public FleetAssignment(VehicleType vehicleType, DetailedTransportCost cost,
				double serviceDemandPerActiveServiceInterval_ton, double serviceInterval_h,
				double serviceIntervalActiveProba, double length_km) {

			final double vehCap_ton = FreightVehicleAttributes.getCapacity_ton(vehicleType);

			this.vehicleType = vehicleType;
			this.serviceInterval_h = serviceInterval_h;
			this.serviceDemandPerActiveServiceInterval_ton = serviceDemandPerActiveServiceInterval_ton;
			this.serviceIntervalActiveProba = serviceIntervalActiveProba;
			this.loopLength_km = 2.0 * length_km;
			this.minLoopDuration_h = 2.0 * cost.duration_h;

			// n is fleet size
			// f is circulation frequency, per service interval
			final double nf = Math.max(1.0, serviceDemandPerActiveServiceInterval_ton / vehCap_ton);

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
					* this.loopsPerActiveServiceInterval * FreightVehicleAttributes.getCapacity_ton(vehicleType);
			assert (serviceDemandPerActiveServiceInterval_ton <= 1e-8 + supplyPerActiveServiceInterval_ton);
		}

		public double vehicleCapacity_ton() {
			return FreightVehicleAttributes.getCapacity_ton(this.vehicleType);
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
			return this.payload_ton / FreightVehicleAttributes.getCapacity_ton(this.vehicleType);
		}

//		@Override
//		public String toString() {
//			return vehicleCnt + " vehicles of type " + this.vehicleType.getId() + " with capacity "
//					+ FreightVehicleAttributes.getCapacity_ton(this.vehicleType) + " operating in "
//					+ this.effectiveNumberOfServiceIntervals + " service intervals";
//		}
	}

	private FleetAssignment dimensionFleetAssignment(VehicleType vehicleType, Signature.ConsolidationUnit signature,
			double serviceDemand_ton, double serviceProba, double length_km) throws InsufficientDataException {
		final FreightVehicleAttributes vehicleAttrs = FreightVehicleAttributes.getFreightAttributes(vehicleType);

		FleetAssignment result = new FleetAssignment(vehicleType,
				this.consolidationCostModel.computeSignatureCost(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton,
						signature, true, true),
				serviceDemand_ton, Units.H_PER_D * this.commodity2serviceInterval_days.get(signature.commodity),
				serviceProba, length_km);
		boolean done = false;
		while (!done) {
			FleetAssignment newResult = new FleetAssignment(vehicleType,
					this.consolidationCostModel.computeSignatureCost(vehicleAttrs, result.payload_ton, signature, true,
							true),
					serviceDemand_ton, Units.H_PER_D * this.commodity2serviceInterval_days.get(signature.commodity),
					serviceProba, length_km);
			final double dev = Math.abs(newResult.unitCost_1_tonKm - result.unitCost_1_tonKm) / result.unitCost_1_tonKm;
			done = (dev < 1e-8);
			result = newResult;
		}
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(Signature.ConsolidationUnit signature,
			List<ChainAndShipmentSize> choices) throws InsufficientDataException {

		final double totalDemand_ton = choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();
		final double length_km = Units.KM_PER_M
				* signature.links.stream().flatMap(l -> l.stream()).mapToDouble(l -> l.getLength()).sum();
		final double intervalLength_days = this.commodity2serviceInterval_days.get(signature.commodity);

		// >>>

		final double serviceIntervalActiveProba;
		{
			double probaSingleServiceIntervalInactive = 1.0;
			for (ChainAndShipmentSize choice : choices) {
				final double meanShipmentsPerYear = Math.max(1.0,
						choice.annualShipment.getTotalAmount_ton() / choice.sizeClass.upperValue_ton);
				final double meanShipmentsPerServiceInterval = meanShipmentsPerYear * intervalLength_days / 365.0;
				probaSingleServiceIntervalInactive *= Math.exp(-meanShipmentsPerServiceInterval);
			}
			serviceIntervalActiveProba = 1.0 - probaSingleServiceIntervalInactive;
		}

		final double demandExpectationPerActiveServiceInterval_ton = (1.0 / serviceIntervalActiveProba)
				* (intervalLength_days / 365.0) * totalDemand_ton;
//		for (ChainAndShipmentSize choice : choices) {
//			final double meanShipmentsPerYear = Math.max(1.0,
//					choice.annualShipment.getTotalAmount_ton() / choice.sizeClass.upperValue_ton);
//			final double meanShipmentSize_ton = choice.annualShipment.getTotalAmount_ton() / meanShipmentsPerYear;
//			final double meanShipmentsPerServiceInterval = meanShipmentsPerYear
//					* this.commodity2serviceInterval_days.get(signature.commodity) / 365.0;
//
//			final double expectedNumberOfShipmentsPerActiveServiceInterval = meanShipmentsPerServiceInterval
//					/ (1.0 - Math.exp(-meanShipmentsPerServiceInterval));
//			demandExpectationPerActiveServiceInterval_ton +=
//
//					meanShipmentSize_ton * expectedNumberOfShipmentsPerActiveServiceInterval;
//		}

		// <<<

		final List<VehicleType> compatibleVehicleTypes = this.fleet.getCompatibleVehicleTypes(signature.commodity,
				signature.mode, signature.isContainer, signature.containsFerry);
		if ((compatibleVehicleTypes == null) || (compatibleVehicleTypes.size() == 0)) {
			throw new InsufficientDataException(this.getClass(), "No compatible vehicle type found.",
					signature.commodity, null, signature.mode, signature.isContainer, signature.containsFerry);
		}

		FleetAssignment overallBestAssignment = null;
		for (VehicleType vehicleType : compatibleVehicleTypes) {
			FleetAssignment bestAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType, signature,
					demandExpectationPerActiveServiceInterval_ton, serviceIntervalActiveProba, length_km);
			if ((overallBestAssignment == null)
					|| (bestAssignmentForVehicleType.unitCost_1_tonKm < overallBestAssignment.unitCost_1_tonKm)) {
				overallBestAssignment = bestAssignmentForVehicleType;
			}
		}
		return overallBestAssignment;
	}
}
