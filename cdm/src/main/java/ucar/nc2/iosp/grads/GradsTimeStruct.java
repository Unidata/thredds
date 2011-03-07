package ucar.nc2.iosp.grads;


    /**
     * A class to hold a GrADS time structure.  The full time spec is:
     *
     *        HH:mm'Z'ddMMMyyyy (e.g. 12:04Z05Mar2011)
     * 
     * @author   Don Murray CU-CIRES
     */
    public class GradsTimeStruct {
    	
    	/** months */
    	public static final String[] months = 
    	{"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };

        /** year field */
        int year = 0;

        /** month field (1 based) */
        int month = 0;

        /** day field */
        int day = 0;

        /** hour field */
        int hour = 0;

        /** minute field */
        int minute = 0;

        /** julian day field */
        int jday = 0;

        /**
         * Create a new time structure
         */
        public GradsTimeStruct() {}
        
        /**
         * Get a String representation of this object
         */
        public String toString() {
        	return String.format("%02d:%02dZ%02d%s%d", hour, minute, day, months[month-1], year);
        }
    }

