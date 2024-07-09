package utils;


public class SimulationConfig {
    public static int MAX_PARKING_EPISODES;
    public static int TIME_BIN_COUNT;
    public static int ROUND_TRIP_COUNT;
    public static double LOCATION_PROPOSAL_WEIGHT;
    public static double DEPARTURE_PROPOSAL_WEIGHT;
    public static int WEIGHT;
    public static double MINIMAL_DURATION_AT_HOME;
    public static double MINIMAL_DURATION_AT_WORK;
    public static double AT_HOME_INTERVAL_LENGTH;
    public static double AT_WORK_INTERVAL_LENGTH;
    public static double MORNING_START;
    public static double EVENING_END;
    public static int LOGGER_INTERVAL;
    public static int MESSAGE_INTERVAL;

    public static String DISTRICTS_HOME_LOCATIONS;
    public static String DISTRICTS_WORK_LOCATIONS;
    public static String DISTRICTS_OD;
    public static String DISTRICTS_SKIM_H;
    public static String DISTRICTS_SKIM_KM;

    public SimulationConfig(
        int maxParkingEpisodes,
        int timeBinCount,
        int roundTripCount,
        double locationProposalWeight,
        double departureProposalWeight,
        int weight,
        double minimalDurationAtHome,
        double minimalDurationAtWork,
        double atHomeIntervalLength,
        double atWorkIntervalLength,
        double morningStart,
        double eveningEnd,
        int loggerInterval,
        int messageInterval,
        String districtsHomeLocations,
        String districtsWorkLocations,
        String districtsOD,
        String districtsSkimH,
        String districtsSkimKm
        ) {
            // Parameters
            MAX_PARKING_EPISODES = maxParkingEpisodes;
            TIME_BIN_COUNT = timeBinCount;
            ROUND_TRIP_COUNT = roundTripCount;
            LOCATION_PROPOSAL_WEIGHT = locationProposalWeight;
            DEPARTURE_PROPOSAL_WEIGHT = departureProposalWeight;
            WEIGHT = weight;
            MINIMAL_DURATION_AT_HOME = minimalDurationAtHome;
            MINIMAL_DURATION_AT_WORK = minimalDurationAtWork;
            AT_HOME_INTERVAL_LENGTH = atHomeIntervalLength;
            AT_WORK_INTERVAL_LENGTH = atWorkIntervalLength;
            MORNING_START = morningStart;
            EVENING_END = eveningEnd;
            LOGGER_INTERVAL = loggerInterval;
            MESSAGE_INTERVAL = messageInterval;

            // File paths
            DISTRICTS_HOME_LOCATIONS = districtsHomeLocations;
            DISTRICTS_WORK_LOCATIONS = districtsWorkLocations;
            DISTRICTS_OD = districtsOD;
            DISTRICTS_SKIM_H = districtsSkimH;
            DISTRICTS_SKIM_KM = districtsSkimKm;
        }

        // Getters
        public static int getMaxParkingEpisodes() {
            return MAX_PARKING_EPISODES;
        }

        public static int getTimeBinCount() {
            return TIME_BIN_COUNT;
        }

        public static int getRoundTripCount() {
            return ROUND_TRIP_COUNT;
        }

        public static double getLocationProposalWeight() {
            return LOCATION_PROPOSAL_WEIGHT;
        }

        public static double getDepartureProposalWeight() {
            return DEPARTURE_PROPOSAL_WEIGHT;
        }

        public static int getWeight() {
            return WEIGHT;
        }

        public static double getMinimalDurationAtHome() {
            return MINIMAL_DURATION_AT_HOME;
        }

        public static double getMinimalDurationAtWork() {
            return MINIMAL_DURATION_AT_WORK;
        }

        public static double getAtHomeIntervalLength() {
            return AT_HOME_INTERVAL_LENGTH;
        }

        public static double getAtWorkIntervalLength() {
            return AT_WORK_INTERVAL_LENGTH;
        }

        public static double getMorningStart() {
            return MORNING_START;
        }

        public static double getEveningEnd() {
            return EVENING_END;
        }

        public static int getLoggerInterval() {
            return LOGGER_INTERVAL;
        }

        public static int getMessageInterval() {
            return MESSAGE_INTERVAL;
        }

        public static String getDistrictsHomeLocations() {
            return DISTRICTS_HOME_LOCATIONS;
        }

        public static String getDistrictsWorkLocations() {
            return DISTRICTS_WORK_LOCATIONS;
        }

        public static String getDistrictsOD() {
            return DISTRICTS_OD;
        }

        public static String getDistrictsSkimH() {
            return DISTRICTS_SKIM_H;
        }

        public static String getDistrictsSkimKm() {
            return DISTRICTS_SKIM_KM;
        }

        @Override
        public String toString() {
            return "SimulationConfig{" +
                    "MAX_PARKING_EPISODES=" + MAX_PARKING_EPISODES +
                    ", \nTIME_BIN_COUNT=" + TIME_BIN_COUNT +
                    ", \nROUND_TRIP_COUNT=" + ROUND_TRIP_COUNT +
                    ", \nLOCATION_PROPOSAL_WEIGHT=" + LOCATION_PROPOSAL_WEIGHT +
                    ", \nDEPARTURE_PROPOSAL_WEIGHT=" + DEPARTURE_PROPOSAL_WEIGHT +
                    ", \nWEIGHT=" + WEIGHT +
                    ", \nMINIMAL_DURATION_AT_HOME=" + MINIMAL_DURATION_AT_HOME +
                    ", \nMINIMAL_DURATION_AT_WORK=" + MINIMAL_DURATION_AT_WORK +
                    ", \nAT_HOME_INTERVAL_LENGTH=" + AT_HOME_INTERVAL_LENGTH +
                    ", \nAT_WORK_INTERVAL_LENGTH=" + AT_WORK_INTERVAL_LENGTH +
                    ", \nMORNING_START=" + MORNING_START +
                    ", \nEVENING_END=" + EVENING_END +
                    ", \nLOGGER_INTERVAL=" + LOGGER_INTERVAL +
                    ", \nMESSAGE_INTERVAL=" + MESSAGE_INTERVAL +
                    ", \nDISTRICTS_HOME_LOCATIONS='" + DISTRICTS_HOME_LOCATIONS + '\'' +
                    ", \nDISTRICTS_WORK_LOCATIONS='" + DISTRICTS_WORK_LOCATIONS + '\'' +
                    ", \nDISTRICTS_OD='" + DISTRICTS_OD + '\'' +
                    ", \nDISTRICTS_SKIM_H='" + DISTRICTS_SKIM_H + '\'' +
                    ", \nDISTRICTS_SKIM_KM='" + DISTRICTS_SKIM_KM + '\'' +
                    '}';
        }
}
