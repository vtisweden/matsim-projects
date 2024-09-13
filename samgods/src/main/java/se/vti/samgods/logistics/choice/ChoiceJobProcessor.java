/**
 * se.vti.samgods.logistics.choicemodel
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
package se.vti.samgods.logistics.choice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.ShipmentSize;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.costs.NonTransportCost;
import se.vti.samgods.logistics.costs.NonTransportCostModel;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.utils.ChoiceModelUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class ChoiceJobProcessor implements Runnable {

	// -------------------- CONSTANTS --------------------

	private final ChoiceModelUtils choiceModel = new ChoiceModelUtils();

	private final double scale;

	private final LogisticChoiceData choiceData;

	private final NonTransportCostModel nonTransportCostModel;

	private final ChainAndShipmentSizeUtilityFunction utilityFunction;

	// -------------------- MEMBERS --------------------

	private final BlockingQueue<ChoiceJob> jobQueue;

	private final BlockingQueue<ChainAndShipmentSize> allChoices;

	// -------------------- CONSTRUCTION --------------------

	public ChoiceJobProcessor(double scale, LogisticChoiceData choiceData, NonTransportCostModel nonTransportCostModel,
			ChainAndShipmentSizeUtilityFunction utilityFunction, BlockingQueue<ChoiceJob> jobQueue,
			BlockingQueue<ChainAndShipmentSize> allChoices) {
		this.scale = scale;
		this.choiceData = choiceData;
		this.nonTransportCostModel = nonTransportCostModel;
		this.utilityFunction = utilityFunction;
		this.jobQueue = jobQueue;
		this.allChoices = allChoices;
	}

	// -------------------- IMPLEMENTATION OF Runnable --------------------

	@Override
	public void run() {
		try {
			while (true) {
				ChoiceJob job = this.jobQueue.take();
				if (job == ChoiceJob.TERMINATE) {
					break;
				}
				this.process(job);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- INTERNALS --------------------

	private void process(ChoiceJob job) throws InterruptedException {
		final Map<TransportChain, DetailedTransportCost> chain2transportUnitCost_1_ton = job.transportChains.stream()
				.collect(Collectors.toMap(c -> c, c -> this.choiceData.computeChain2transportUnitCost_1_ton(c)));
		if (chain2transportUnitCost_1_ton.size() > 0) {
			for (AnnualShipment annualShipment : job.annualShipments) {
				List<ChainAndShipmentSize> alternatives = new ArrayList<>();
				for (Map.Entry<TransportChain, DetailedTransportCost> e : chain2transportUnitCost_1_ton.entrySet()) {
					final TransportChain transportChain = e.getKey();
					final DetailedTransportCost transportUnitCost = e.getValue();
					for (ShipmentSize size : SamgodsConstants.ShipmentSize.values()) {
						if ((annualShipment.getSingleInstanceAnnualAmount_ton() >= size.getRepresentativeValue_ton())
								|| SamgodsConstants.ShipmentSize.getSmallestSize_ton().equals(size)) {
							final NonTransportCost totalNonTransportCost = this.nonTransportCostModel
									.computeNonTransportCost(job.commodity, size,
											annualShipment.getSingleInstanceAnnualAmount_ton(),
											transportUnitCost.duration_h);
							alternatives.add(new ChainAndShipmentSize(annualShipment, size, transportChain,
									this.utilityFunction.computeUtility(job.commodity,
											annualShipment.getSingleInstanceAnnualAmount_ton(), transportUnitCost,
											totalNonTransportCost)));
						}
					}
				}
				for (int instance = 0; instance < annualShipment.getNumberOfInstances(); instance++) {
					final ChainAndShipmentSize choice = this.choiceModel.choose(alternatives,
							a -> this.scale * a.utility);
					assert (choice != null);
					this.allChoices.put(choice);
				}
			}
		} else {
			new InsufficientDataException(this.getClass(), "No transport chains with transport cost available.",
					job.commodity, job.od, null, null, null);
		}
	}
}