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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
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
public class PoissonHalfLoopConsolidator {

	// -------------------- CONSTANTS --------------------

	private final VehicleFleet fleet;

	private final ConsolidationCostModel consolidationCostModel;

	private final Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days;
	private final Map<SamgodsConstants.Commodity, Double> commodity2numberOfServiceIntervals;

	private final boolean flexiblePeriod;

	private final boolean skipUnusedIntervals;

	private boolean enforceAtLeastOneActiveServiceInterval = false;

	// -------------------- CONSTRUCTION --------------------

	public PoissonHalfLoopConsolidator(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
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

	public static class FleetAssignment {

		public final VehicleType vehicleType;
		public final double halfLoopDuration_h;
		private final double halfLoopMovementDuration_h;
		public final int minVehicleCnt;
		public final int vehicleCnt;
		public final double payload_ton;
		public final double unitCost_1_ton;

		@Deprecated
		/*
		 * This is only to compute the number of simultaneously moving vehicles.
		 */
		public final double serviceProba;

		public FleetAssignment(VehicleType vehicleType, DetailedTransportCost halfLoopCost, Integer vehicleCnt,
				double demandExpectationPerActiveServiceInterval_ton, boolean flexiblePeriod, int serviceInterval_days,
				double serviceProba) {
			this.serviceProba = serviceProba;

			final double vehCap_ton = FreightVehicleAttributes.getCapacity_ton(vehicleType);
			this.vehicleType = vehicleType;
			this.halfLoopMovementDuration_h = halfLoopCost.duration_h;

			final double serviceInterval_h = 24.0 * serviceInterval_days;
			double numberOfServiceIntervals = 365.0 / serviceInterval_days;

//			final double effectiveDemandRate_ton_h = totalDemand_ton / (365.0 * 24.0) / serviceProba;
			final double effectiveDemandRate_ton_h = demandExpectationPerActiveServiceInterval_ton / serviceInterval_h;

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
					+ FreightVehicleAttributes.getCapacity_ton(this.vehicleType);
		}
	}

	private FleetAssignment dimensionFleetAssignment(VehicleType vehicleType, Integer vehicleCnt,
			Signature.ConsolidationEpisode signature, double demandExpectationPerActiveServiceInterval,
			double probaServiceIntervalActive) throws InsufficientDataException {
		final FreightVehicleAttributes vehicleAttrs = FreightVehicleAttributes.getFreightAttributes(vehicleType);
		FleetAssignment result = new FleetAssignment(vehicleType,
				this.consolidationCostModel.computeSignatureCost(vehicleAttrs, 0.5 * vehicleAttrs.capacity_ton,
						signature),
				vehicleCnt, demandExpectationPerActiveServiceInterval, this.flexiblePeriod,
				this.commodity2serviceInterval_days.get(signature.commodity), probaServiceIntervalActive);
		boolean done = false;
		while (!done) {
			final FleetAssignment newResult = new FleetAssignment(vehicleType,
					this.consolidationCostModel.computeSignatureCost(vehicleAttrs, result.payload_ton, signature),
					result.vehicleCnt, demandExpectationPerActiveServiceInterval, this.flexiblePeriod,
					this.commodity2serviceInterval_days.get(signature.commodity), probaServiceIntervalActive);
			final double dev = Math.abs(newResult.unitCost_1_ton - result.unitCost_1_ton) / result.unitCost_1_ton;
			done = (dev < 1e-8);
			result = newResult;

			if (newResult.vehicleCnt > 100 * 1000) {
				throw new RuntimeException(
						"More than 100'000 vehicles in dimensionFleetAssignment(..), total demand per service interval = "
								+ demandExpectationPerActiveServiceInterval + " ton, signature: " + signature);
			}

		}
		return result;
	}

	public FleetAssignment computeOptimalFleetAssignment(Signature.ConsolidationEpisode signature,
			List<ChainAndShipmentSize> choices) throws InsufficientDataException {

		// >>>

		double demandExpectationPerActiveServiceInterval = 0.0;
		double demandVariancePerActiveServiceInterval = 0.0;

		final double probaServiceIntervalActive;
		if (!this.skipUnusedIntervals) {
			probaServiceIntervalActive = 1.0;
		} else {
			double probaSingleServiceIntervalInactive = 1.0;
			for (ChainAndShipmentSize choice : choices) {
				final double expectedNumberOfShipmentsPerYear = choice.annualShipment.getTotalAmount_ton()
						/ choice.sizeClass.getRepresentativeValue_ton();
				final double expectedNumberOfShipmentsPerServiceInterval = expectedNumberOfShipmentsPerYear
						* this.commodity2serviceInterval_days.get(signature.commodity) / 365.0;
				probaSingleServiceIntervalInactive *= Math.exp(-expectedNumberOfShipmentsPerServiceInterval);

				final double expectedNumberOfShipmentsPerActiveServiceInterval = expectedNumberOfShipmentsPerServiceInterval
						/ (1.0 - Math.exp(-expectedNumberOfShipmentsPerServiceInterval));
				demandExpectationPerActiveServiceInterval += choice.sizeClass.getRepresentativeValue_ton()
						* expectedNumberOfShipmentsPerActiveServiceInterval;
				demandVariancePerActiveServiceInterval += Math.pow(choice.sizeClass.getRepresentativeValue_ton(), 2.0)
						* expectedNumberOfShipmentsPerActiveServiceInterval
						* (1.0 + expectedNumberOfShipmentsPerServiceInterval
								- expectedNumberOfShipmentsPerActiveServiceInterval);
			}
			probaServiceIntervalActive = Math.max(1.0 - probaSingleServiceIntervalInactive,
					this.enforceAtLeastOneActiveServiceInterval
							? 1.0 / this.commodity2numberOfServiceIntervals.get(signature.commodity)
							: 0.0);
		}

		// <<<

		final double totalDemand_ton = choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();

		final List<VehicleType> compatibleVehicleTypes = this.fleet.getCompatibleVehicleTypes(signature.commodity,
				signature.mode, signature.isContainer, signature.containsFerry);
		if ((compatibleVehicleTypes == null) || (compatibleVehicleTypes.size() == 0)) {
			throw new InsufficientDataException(this.getClass(), "No compatible vehicle type found.",
					signature.commodity, null, signature.mode, signature.isContainer, signature.containsFerry);
		}

		List<FleetAssignment> possibleFleets = new ArrayList<>(compatibleVehicleTypes.size());
		List<Double> supplyVariances = new ArrayList<>(compatibleVehicleTypes.size());
		List<Double> expectedServicesInActiveIntervalList = new ArrayList<>(compatibleVehicleTypes.size());
		for (VehicleType vehicleType : compatibleVehicleTypes) {
			FleetAssignment bestAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType, null, signature,
					demandExpectationPerActiveServiceInterval, probaServiceIntervalActive);
			boolean done = false;
			while (!done) {
				final FleetAssignment candAssignmentForVehicleType = this.dimensionFleetAssignment(vehicleType,
						bestAssignmentForVehicleType.vehicleCnt + 1, signature,
						demandExpectationPerActiveServiceInterval, probaServiceIntervalActive);
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
			possibleFleets.add(bestAssignmentForVehicleType);

			final double vehicleCap_ton = FreightVehicleAttributes.getCapacity_ton(vehicleType);
			final double expectedServicesInActiveInterval = bestAssignmentForVehicleType.vehicleCnt
					* (Units.H_PER_D * this.commodity2serviceInterval_days.get(signature.commodity))
					/ (2.0 * bestAssignmentForVehicleType.halfLoopDuration_h);
			expectedServicesInActiveIntervalList.add(expectedServicesInActiveInterval);
			final double supplyExpectationInActiveServiceInterval = vehicleCap_ton * expectedServicesInActiveInterval;
			assert (demandExpectationPerActiveServiceInterval <= supplyExpectationInActiveServiceInterval + 1e-8);
			double supplyVarianceInActiveServiceInterval = Math.pow(vehicleCap_ton, 2.0)
					* expectedServicesInActiveInterval;
			supplyVariances.add(supplyVarianceInActiveServiceInterval);
		}

		// >>>>>>>>>>

		final double demandVariance = demandVariancePerActiveServiceInterval;
		UnivariateFunction demandStddevMinusSupplyStddev = new UnivariateFunction() {
			@Override
			public double value(double eta) {
				final double[] args = possibleFleets.stream().mapToDouble(a -> -eta * a.unitCost_1_ton).toArray();
				final double maxArg = Arrays.stream(args).max().getAsDouble();
				double num = 0;
				double den = 0;
				for (int i = 0; i < possibleFleets.size(); i++) {
					final double expVal = Math.exp(args[i] - maxArg);
					num += Math.pow(FreightVehicleAttributes.getCapacity_ton(possibleFleets.get(i).vehicleType), 2.0)
							* expectedServicesInActiveIntervalList.get(i) * expVal;
					den += expVal;
				}
				return Math.sqrt(demandVariance) - Math.sqrt(num / den);
			}
		};

		final double maxUnitCost = possibleFleets.stream().mapToDouble(a -> a.unitCost_1_ton).max().getAsDouble();

		final double minEta = 0.0;
		final double maxEta = 15.0 / maxUnitCost;

		final double demandStddevMinusSupplyStddevAtMinEta = demandStddevMinusSupplyStddev.value(minEta);
		final double demandStddevMinusSupplyStddevAtMaxEta = demandStddevMinusSupplyStddev.value(maxEta);

		final double eta;

		if (demandStddevMinusSupplyStddevAtMinEta * demandStddevMinusSupplyStddevAtMaxEta < 0) {
			eta = new BrentSolver(1e-8, 1e-8).solve(1000 * 1000, demandStddevMinusSupplyStddev, minEta, maxEta);
			System.out.println("Internal solution: improvement: " + Math.abs(demandStddevMinusSupplyStddev.value(eta))
					/ Math.min(Math.abs(demandStddevMinusSupplyStddevAtMinEta),
							Math.abs(demandStddevMinusSupplyStddevAtMaxEta)));
		} else if (Math.abs(demandStddevMinusSupplyStddevAtMinEta) < Math
				.abs(demandStddevMinusSupplyStddevAtMaxEta)) {
			eta = minEta;
			System.out.println("  Border solution: eta = " + eta);
		} else {
			eta = maxEta;
			System.out.println("  Border solution: eta = " + eta);
		}

		// <<<<<<<<<<

		FleetAssignment bestAssignment = possibleFleets.stream()
				.min((a, b) -> Double.compare(a.unitCost_1_ton, b.unitCost_1_ton)).get();
		return bestAssignment;
	}
}
