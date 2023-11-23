/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.models.saana;

import java.util.Arrays;
import java.util.List;

import se.vti.samgods.legacy.Samgods.Commodity;
import se.vti.samgods.logistics.AbstractTransportChainAndShipmentChoiceModel;
import se.vti.samgods.logistics.Shipment;
import se.vti.samgods.logistics.ShipmentCostCalculator;
import se.vti.samgods.logistics.ShipmentCostCalculator.ShipmentCost;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportLeg;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChainAndShipmentChoiceModelImpl extends AbstractTransportChainAndShipmentChoiceModel {

	private enum ShipmentSizeClass implements SizeClass {

		SIZE01(1e-3, 51), SIZE02(51, 201), SIZE03(201, 801), SIZE04(801, 3001), SIZE05(3001, 7501), SIZE06(7501, 12501),
		SIZE07(12501, 20001), SIZE08(20001, 30001), SIZE09(30001, 35001), SIZE10(35001, 40001), SIZE11(40001, 45001),
		SIZE12(45001, 100001), SIZE13(100001, 200001), SIZE14(200001, 400001), SIZE15(400001, 800001),
		SIZE16(800001, 2500000);

		public static final double MIN_SHIPMENT_SIZE = 1e-3;

		private final double lowerValue_ton;
		private final double upperValue_ton;

		private ShipmentSizeClass(double lowerValue_ton, double upperValue_ton) {
			if (lowerValue_ton < MIN_SHIPMENT_SIZE) {
				throw new IllegalArgumentException();
			}
			if (upperValue_ton < lowerValue_ton) {
				throw new IllegalArgumentException();
			}
			this.lowerValue_ton = lowerValue_ton;
			this.upperValue_ton = upperValue_ton;
		}

		@Override
		public String toString() {
			return "size[" + this.lowerValue_ton + "," + this.upperValue_ton + ")tons";
		}

		@Override
		public double getLowerValue_ton() {
			return this.lowerValue_ton;
		}

		@Override
		public double getUpperValue_ton() {
			return this.upperValue_ton;
		}
	};

	public TransportChainAndShipmentChoiceModelImpl(ShipmentCostCalculator costCalculator,
			UtilityFunction utilityFunction) {
		super(costCalculator, utilityFunction);
	}

	@Override
	protected SizeClass[] allSizeClasses() {
		return ShipmentSizeClass.values();
	}

	@Override
	public Shipment chooseShipment(Commodity commodity, double totalAmount_ton, List<TransportChain> transportChains) {
		return null;
	}

	public static void main(String[] args) {

		TransportChainAndShipmentChoiceModelImpl model = new TransportChainAndShipmentChoiceModelImpl(
				new ShipmentCostCalculator() {
					@Override
					public ShipmentCost computeCost(Shipment shipment) {
						return null;
					}
				}, new UtilityFunction() {

					@Override
					public double computeUtility(Shipment shipment, ShipmentCost shipmentCost) {
						return 0;
					}

				});

		TransportChain testChain = new TransportChain() {

			@Override
			public List<TransportLeg> getLegs() {
				return null;
			}
		};

		for (Alternative alt : model.createChoiceSet(Arrays.asList(testChain), 10000, null)) {
			System.out.println(alt);
		}
	}
}
