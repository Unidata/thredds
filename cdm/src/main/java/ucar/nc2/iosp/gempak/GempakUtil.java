/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.nc2.iosp.gempak;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.unidata.util.StringUtil;


/**
 * Class for static GEMPAK utility methods
 */
public final class GempakUtil {

    /** day of month string */
    private static int[] month = new int[] {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    /** day of month string */
    public static String[] vertCoords = new String[] {
        "NONE", "PRES", "THTA", "HGHT", "SGMA", "DPTH"
    };

    /**
     * This subroutine converts the two integers stored in a grid file
     * into three integers containing the date, time and forecast time.
     * @param  iftime  input time array
     * @param  start offset into the array
     * @return  int[3] with date time and forecast time
     */
    public static int[] TG_FTOI(int[] iftime, int start) {

        int[] intdtf = new int[3];
        // If there is no forecast information, the string is stored as
        // date and time.

        if (iftime[start] < 100000000) {
            intdtf[0] = iftime[start];
            intdtf[1] = iftime[start + 1];
            intdtf[2] = 0;

            //  Otherwise, decode date/time and forecast info from the
            //  two integers. 

        } else {

            //  The first word contains MMDDYYHHMM.  This must be turned
            //  into YYMMDD and HHMM.

            intdtf[0] = iftime[start] / 10000;
            intdtf[1] = iftime[start] - intdtf[0] * 10000;
            int mmdd = intdtf[0] / 100;
            int iyyy = intdtf[0] - mmdd * 100;
            intdtf[0] = iyyy * 10000 + mmdd;

            //  The forecast time remains the same.

            intdtf[2] = iftime[start + 1];
        }
        return intdtf;
    }

    /**
     * This subroutine converts an integer time array containing the date,
     * time and forecast time into a GEMPAK grid time.
     *
     * @param intdtf  integer array of date, time and forecast time
     *
     * @return  formatted string
     */
    public static String TG_ITOC(int[] intdtf) {
        String gdattim = "";

        //Check for the blank time which may be found.

        if ((intdtf[0] == 0) && (intdtf[2] == 0) && (intdtf[2] == 0)) {
            return gdattim;
        }

        //  Put the date and time into the character time.

        gdattim = TI_CDTM(intdtf[0], intdtf[1]);

        //  Decode the forecast information if there is any.

        if (intdtf[2] != 0) {
            String[] timeType = TG_CFTM(intdtf[2]);
            String   ftype    = timeType[0];
            String   ftime    = timeType[1];

            //      Combine two parts into string.

            gdattim = gdattim.substring(0, 11) + ftype + ftime;
        }

        return gdattim;
    }

    /**
     * This subroutine converts an integer grid forecast time into
     * the character forecast type and time.  The forecast type
     * is  A (analysis), F (forecast), G (guess) or I (initialize).
     * If the forecast time is less than 100 and the minutes are 00,
     * only hh is returned.
     * @param ifcast   integer forecast time
     * @return type and time.
     */
    public static String[] TG_CFTM(int ifcast) {
        String ftype = "";
        String ftime = "";

        //Check for negative times.
        if (ifcast < 0) {
            return new String[] { ftype, ftime };
        }


        // Get the number representing forecast type and convert to a
        // character.

        int iftype = ifcast / 100000;
        if (iftype == 0) {
            ftype = "A";
        } else if (iftype == 1) {
            ftype = "F";
        } else if (iftype == 2) {
            ftype = "G";
        } else if (iftype == 3) {
            ftype = "I";
        }

        // Convert the time to a character.  Add 100000 so that leading
        // zeros will be encoded.

        int    iftime = ifcast - iftype * 100000;
        int    ietime = iftime + 100000;
        String fff    = ST_INCH(ietime);

        // If the forecast time has minutes, set the character output
        // to all five digits. Otherwise, use only the first three digits,
        // which represent hours.

        if (ietime % 100 == 0) {
            ftime = fff.substring(1, 4);
        } else {
            ftime = fff.substring(1);
        }

        return new String[] { ftype, ftime };
    }

    /**
     * This subroutine converts an integer date (YYMMDD) and time (HHMM)
     *
     * @param idate  integer date
     * @param itime  integer time
     *
     * @return  string date/time
     */
    public static String TI_CDTM(int idate, int itime) {
        String dattim;
        int[]  idtarr = new int[5];

        idtarr[0] = idate / 10000;
        idtarr[1] = (idate - idtarr[0] * 10000) / 100;
        idtarr[2] = idate % 100;
        idtarr[3] = itime / 100;
        idtarr[4] = itime % 100;
        dattim    = TI_ITOC(idtarr);

        return dattim;
    }

    /**
     * This subroutine converts an integer time array into a standard
     * GEMPAK time.  The integers are checked for validity.
     * @param idtarr   Time array (YYYY,MM,DD,HH,MM)
     * @return time as a string
     */
    public static String TI_ITOC(int[] idtarr) {
        String dattim;
        String date, time;

        dattim = "";

        //   Put array values into variables.                                

        int iyear  = idtarr[0];
        int imonth = idtarr[1];
        int iday   = idtarr[2];
        int ihour  = idtarr[3];
        int iminut = idtarr[4];

        //  Check for leap year.

        int ndays = TI_DAYM(iyear, imonth);
        iyear = iyear % 100;

        //  Check that each of these values is valid.

        /*  TODO: Check these
            IF  ( iyear .lt. 0 )  iret = -7
            IF  ( ( imonth .lt. 1 ) .or. ( imonth .gt. 12 ) ) iret = -8
            IF  ( ( iday   .lt. 1 ) .or. ( iday   .gt. ndays ) )
         +                                                    iret = -9
            IF  ( ( ihour  .lt. 0 ) .or. ( ihour  .gt. 24 ) ) iret = -10
            IF  ( ( iminut .lt. 0 ) .or. ( iminut .gt. 60 ) ) iret = -11
            IF  ( iret .ne. 0 )  RETURN
        */

        //  Get the date and time.

        int idate = iyear * 10000 + imonth * 100 + iday;
        int itime = ihour * 100 + iminut;

        //  Convert date and time to character strings.
        //  Fill in blanks with zeroes.
        date   = StringUtil.padZero(idate, 6);
        time   = StringUtil.padZero(itime, 4);

        dattim = date + "/" + time;

        return dattim;
    }



    /**
     * This subroutine returns the number of days in the given month.
     * The year must be a full four-digit year.
     *
     * @param iyear  integer year
     * @param imon  integer month
     *
     * @return  number of days
     */
    public static int TI_DAYM(int iyear, int imon) {

        int iday = 0;

        if ((imon > 0) && (imon < 13)) {

            //  Pick the number of days for the given month.

            iday = month[imon - 1];
            if ((imon == 2) && LEAP(iyear)) {
                iday = iday + 1;
            }
        }
        return iday;
    }

    /**
     * Check for leap year
     *
     * @param iyr  year to check
     *
     * @return  true if leap year
     */
    public static boolean LEAP(int iyr) {
        return (iyr % 4 == 0) && ((iyr % 100 != 0) || (iyr % 400 == 0));
    }

    /**
     * Convert a value to a string
     *
     * @param value  value to convert
     *
     * @return  string representation
     */
    public static String ST_INCH(int value) {
        return String.valueOf(value);
    }

    /**
     * Convert the int bits to a string
     *
     * @param value  value to convert
     *
     * @return  string representation
     */
    public static String ST_ITOC(int value) {
        return McIDASUtil.intBitsToString(value);
    }

    /**
     * Convert the int bits to a string
     *
     * @param values  array of values to convert
     *
     * @return  string representation
     */
    public static String ST_ITOC(int[] values) {
        return McIDASUtil.intBitsToString(values);
    }

    /**
     * This subroutine translates a numeric value for IVCORD into its
     * character value in VCOORD.
     * @param ivcord  integer coordinate value
     * @return  string coordinate name
     */
    public static String LV_CCRD(int ivcord) {
        //Translate known vertical coordinates or look for parameter name.
        String vcoord = "";


        //Check for numeric vertical coordinates.

        if ((ivcord >= 0) && (ivcord < vertCoords.length)) {
            vcoord = vertCoords[ivcord];

        } else if (ivcord > 100) {
            //     Check for character name as vertical coordinate.  Check that
            //     each character is an alphanumeric character.

            vcoord = ST_ITOC(ivcord);
            /*
              Check for bad values

                DO  i = 1, 4
                    v = vcoord (i:i)
                    IF  ( ( ( v .lt. 'A' ) .or. ( v .gt. 'Z' ) ) .and.
     +                    ( ( v .lt. '0' ) .or. ( v .gt. '9' ) ) )  THEN
                        ier = -1
                    END IF
                END DO
            END IF
            */
        }
        return vcoord;
    }

    /**
     * Swap the order of the integer.
     * @param value  array of int values
     * @return swapped value
     */
    public static int swp4(int value) {
        int[] vals = new int[] { value };
        swp4(vals, 0, 1);
        return vals[0];
    }

    /**
     * Swap the order of the integers in place.
     * @param values  array of int values
     * @param startIndex  starting index of the array
     * @param number of bytes
     * @return input array with values swapped
     */
    public static int[] swp4(int[] values, int startIndex, int number) {
        McIDASUtil.swbyt4(values, startIndex, number);
        return values;
    }

    /**
     * Get a name for the grid packing type
     *
     * @param pktyp   packing type
     *
     * @return  String version of packing type
     */
    public static String getGridPackingName(int pktyp) {
        String packingType = "UNKNOWN";
        switch (pktyp) {

          case GempakConstants.MDGNON :
              packingType = "MDGNON";
              break;

          case GempakConstants.MDGGRB :
              packingType = "MDGGRB";
              break;

          case GempakConstants.MDGNMC :
              packingType = "MDGNMC";
              break;

          case GempakConstants.MDGDIF :
              packingType = "MDGDIF";
              break;

          case GempakConstants.MDGDEC :
              packingType = "MDGDEC";
              break;

          case GempakConstants.MDGRB2 :
              packingType = "MDGRB2";
              break;

          default :
              break;
        }
        return packingType;
    }

}

