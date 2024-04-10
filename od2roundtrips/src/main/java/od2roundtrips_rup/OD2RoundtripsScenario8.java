package od2roundtrips_rup;

import java.io.IOException;
import java.util.List;
import od2roundtrips.model.TAZ;



public class OD2RoundtripsScenario8 extends se.vti.roundtrips.model.Scenario<TAZ> {
 
	public OD2RoundtripsScenario8() {

		super(name -> new TAZ(name));

	}

	
 
	public static void main(String[] args) throws IOException {

		OD2RoundtripsScenario8 scenario = new OD2RoundtripsScenario8();

		
		
		// Reads in district names from .csv and safes to namesOfDistrics

		DistrictNamesFromCsvToList districtNamesFromCsvToList = new DistrictNamesFromCsvToList();

        List<String> namesOfDistricts = districtNamesFromCsvToList.readCSV("openstreetmap_km_sym.csv", 24);

        System.out.println("List namesOfDistrics: " + namesOfDistricts);


 
        // Create each district once; the scenario will memorize it by name.
        
        for (String name : namesOfDistricts) {        	
        	
        	scenario.createAndAddLocation(name);
        
        }
        
        

		// Reads in distance between districts [km] from .csv and safes to distanceBetweenDistrictsKm

    	ValuesFromCsvToList distanceFromCsvToList = new ValuesFromCsvToList();

        List<List<String>> distanceBetweenDistrictsKm = distanceFromCsvToList.readCSV("openstreetmap_km_sym.csv", 3);

        //System.out.println(distanceBetweenDistrictsKm);
        
        

        // Double Loop
        
        // First Loop (Origin): Iterates through all rows of the list 'distanceBetweenDistrictsKm' and writes it to 'row'
        
        // Second Loop (Destination): Iterates through all values inside the current 'row' and writes it to 'value'
        
        int originCounter = 0;

        int destinationCounter = 0;

        for (List<String> row : distanceBetweenDistrictsKm) {

        	String origin = namesOfDistricts.get(originCounter);
	
        	// TAZ originTAZ = scenario.createAndAddLocation(origin); // No more needed. The scenario class now understands string labels.

        	System.out.println("Current row in distanceBetweenDistrictsKm: " + row);

        	System.out.println();	

        	originCounter += 1;

        	destinationCounter = 0;       	

            for (int i = 1; i < row.size(); i++) {           	

            	String destination = namesOfDistricts.get(destinationCounter);

            	// TAZ destinationTAZ = scenario.createAndAddLocation(destination); // No more needed. The scenario class now understands string labels.

            	// String distance = row.get(i);

            	double dist_km = Double.parseDouble(row.get(i));
            	
            	scenario.setSymmetricDistance_km(origin, destination, dist_km);
            	
            	// scenario.setSymmetricDistance_km(originTAZ, destinationTAZ, dist_km); // Now using String labels instead of TAZ objects. (But TAZ objects still work.)

            	System.out.println("Origin: " + origin);

            	System.out.println("Destination: " + destination);

            	System.out.println("Distance: " + dist_km);

                System.out.println();

                destinationCounter += 1;

            }

        }
        
        

        
		// Reads in travel time between districts [min] from .csv and safes to distanceBetweenDistrictsKm

    	ValuesFromCsvToList minFromCsvToList = new ValuesFromCsvToList();

        List<List<String>> distanceBetweenDistrictsMin = minFromCsvToList.readCSV("openstreetmap_min_sym.csv", 3);

        // System.out.println(distanceBetweenDistrictsMin);

        

        // Double Loop for Travel Time
        
        originCounter = 0;

        destinationCounter = 0;
                     
        for (List<String> row : distanceBetweenDistrictsMin) {
        	
        	String origin = namesOfDistricts.get(originCounter);
	
        	System.out.println("Current row in distanceBetweenDistrictsMin: " + row);

        	System.out.println();	

        	originCounter += 1;

        	destinationCounter = 0;       	

            for (int i = 1; i < row.size(); i++) {           	

            	String destination = namesOfDistricts.get(destinationCounter);

             	double dist_min = Double.parseDouble(row.get(i));
             	
             	double dist_h = dist_min/60;
            	
            	scenario.setSymmetricTime_h(origin, destination, dist_h);
            	
            	System.out.println("Origin: " + origin);

            	System.out.println("Destination: " + destination);

            	System.out.println("Travel Time: " + dist_h);

                System.out.println();

                destinationCounter += 1;

            }

        } 
              
	}

}
