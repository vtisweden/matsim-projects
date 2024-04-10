package od2roundtrips_rup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DistrictNamesFromCsvToList {
    public List<String> readCSV(String csvFile, int limit) throws IOException {

        List<String> namesOfDistrics = new ArrayList<>();

        String line = "";

        String cvsSplitBy = ",";

        int lineCount = 0;

        BufferedReader br = new BufferedReader(new FileReader(csvFile));

        while ((line = br.readLine()) != null && lineCount < 24) {

        	String[] values = line.split(cvsSplitBy);

            if (lineCount > 0) {

            	//System.out.println("Erster Wert der Zeile " + (lineCount + 1) + ": " + values[0]);

            	namesOfDistrics.add(values[0]);                

            }

            lineCount++;

        }

        br.close();

        return namesOfDistrics;

    }
}
