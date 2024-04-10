package od2roundtrips_rup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ValuesFromCsvToList {

    public List<List<String>> readCSV(String csvFile, int limit) throws IOException {

        List<List<String>> skimKM = new ArrayList<>();

        String line;

        String cvsSplitBy = ",";

        int lineCount = 0;

        BufferedReader br = new BufferedReader(new FileReader(csvFile));

        while ((line = br.readLine()) != null && lineCount < limit) {  

        	String[] values = line.split(cvsSplitBy);

            List<String> rowValues = new ArrayList<>();

            for (String value : values) {

                rowValues.add(value);

            }

            if (lineCount > 0) { // Exclude header

            	skimKM.add(rowValues);

            }

            lineCount++;

        }

        br.close();

        return skimKM;

    }
	
	
}
