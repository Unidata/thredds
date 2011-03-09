/*
 * Copyright 1998-2011 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/*  Copyright (C) 1988-2010 by Brian Doty and the
    Institute of Global Environment and Society (IGES).
    See file COPYRIGHT for more information.   */

package ucar.nc2.iosp.grads;


/**
 * Utility class for GrADS stuff
 *
 * @author Don Murray CU/CIRES
 */
public class GradsUtil {

    /** GAUSR15 identifier */
    public static final String GAUSR15 = "GAUSR15";

    /** GAUSR20 identifier */
    public static final String GAUSR20 = "GAUSR20";

    /** GAUSR30 identifier */
    public static final String GAUSR30 = "GAUSR30";

    /** GAUSR40 identifier */
    public static final String GAUSR40 = "GAUSR40";

    /** GAUST62 identifier */
    public static final String GAUST62 = "GAUST62";

    //J-

    /** GAUSR15 latitudes */
    private static double glts15[] = {
           -86.60,-82.19,-77.76,-73.32,-68.88,-64.43,-59.99,
           -55.55,-51.11,-46.66,-42.22,-37.77,-33.33,-28.89,
           -24.44,-20.00,-15.55,-11.11, -6.67, -2.22,  2.22,
             6.67, 11.11, 15.55, 20.00, 24.44, 28.89, 33.33,
            37.77, 42.22, 46.66, 51.11, 55.55, 59.99, 64.43,
            68.88, 73.32, 77.76, 82.19, 86.60};
    
    /** GAUSR20 latitudes */
    private static double glts20[] = {
           -87.38,-83.98,-80.56,-77.13,-73.71,-70.28,-66.85,
           -63.42,-59.99,-56.57,-53.14,-49.71,-46.28,-42.85,
           -39.43,-36.00,-32.57,-29.14,-25.71,-22.28,-18.86,
           -15.43,-12.00, -8.57, -5.14, -1.71,  1.71,  5.14,
             8.57, 12.00, 15.43, 18.86, 22.28, 25.71, 29.14,
            32.57, 36.00, 39.43, 42.85, 46.28, 49.71, 53.14,
            56.57, 59.99, 63.42, 66.85, 70.28, 73.71, 77.13,
            80.56, 83.98, 87.38};

    /** GAUSR30 latitudes */
    private static double glts30[] = {
           -88.29, -86.07, -83.84, -81.61, -79.37, -77.14, -74.90,
           -72.67, -70.43, -68.20, -65.96, -63.72, -61.49, -59.25,
           -57.02, -54.78, -52.55, -50.31, -48.07, -45.84, -43.60,
           -41.37, -39.13, -36.89, -34.66, -32.42, -30.19, -27.95,
           -25.71, -23.48, -21.24, -19.01, -16.77, -14.53, -12.30,
           -10.06,  -7.83,  -5.59,  -3.35,  -1.12,   1.12,   3.35,
             5.59,   7.83,  10.06,  12.30,  14.53,  16.77,  19.01,
            21.24,  23.48,  25.71,  27.95,  30.19,  32.42,  34.66,
            36.89,  39.13,  41.37,  43.60,  45.84,  48.07,  50.31,
            52.55,  54.78,  57.02,  59.25,  61.49,  63.72,  65.96,
            68.20,  70.43,  72.67,  74.90,  77.14,  79.37,  81.61,
            83.84,  86.07,  88.29};

    /** GAUSR40 latitudes */
    private static double glats[] = {
         -88.66,-86.91,-85.16,-83.41,-81.65,-79.90,-78.14,-76.39,-74.63,
         -72.88,-71.12,-69.36,-67.61,-65.85,-64.10,-62.34,-60.58,-58.83,
         -57.07,-55.32,-53.56,-51.80,-50.05,-48.29,-46.54,-44.78,-43.02,
         -41.27,-39.51,-37.76,-36.00,-34.24,-32.49,-30.73,-28.98,-27.22,
         -25.46,-23.71,-21.95,-20.19,-18.44,-16.68,-14.93,-13.17,-11.41,
          -9.66, -7.90, -6.15, -4.39, -2.63, -0.88,  0.88,  2.63,  4.39,
           6.15,  7.90,  9.66, 11.41, 13.17, 14.93, 16.68, 18.44, 20.19,
          21.95, 23.71, 25.46, 27.22, 28.98, 30.73, 32.49, 34.24, 36.00,
          37.76, 39.51, 41.27, 43.02, 44.78, 46.54, 48.29, 50.05, 51.80,
          53.56, 55.32, 57.07, 58.83, 60.58, 62.34, 64.10, 65.85, 67.61,
          69.36, 71.12, 72.88, 74.63, 76.39, 78.14, 79.90, 81.65, 83.41,
          85.16, 86.91, 88.66 };
    
    /** GAUSt62 latitudes */
    private static double gltst62[] = {
            -88.542, -86.6531, -84.7532, -82.8508, -80.9473, -79.0435,
            -77.1394, -75.2351, -73.3307, -71.4262, -69.5217, -67.6171,
            -65.7125, -63.8079, -61.9033, -59.9986, -58.0939, -56.1893,
            -54.2846, -52.3799, -50.4752, -48.5705, -46.6658, -44.7611,
            -42.8564, -40.9517, -39.047, -37.1422, -35.2375, -33.3328,
            -31.4281, -29.5234, -27.6186, -25.7139, -23.8092, -21.9044,
            -19.9997, -18.095, -16.1902, -14.2855, -12.3808, -10.47604,
            -8.57131, -6.66657, -4.76184, -2.8571, -0.952368, 0.952368,
            2.8571, 4.76184, 6.66657, 8.57131, 10.47604, 12.3808, 14.2855,
            16.1902, 18.095, 19.9997, 21.9044, 23.8092, 25.7139, 27.6186,
            29.5234, 31.4281, 33.3328, 35.2375, 37.1422, 39.047, 40.9517,
            42.8564, 44.7611, 46.6658, 48.5705, 50.4752, 52.3799, 54.2846,
            56.1893, 58.0939, 59.9986, 61.9033, 63.8079, 65.7125, 67.6171,
            69.5217, 71.4262, 73.3307, 75.2351, 77.1394, 79.0435, 80.9473,
            82.8508, 84.7532, 86.6531, 88.542 };
//J+

    /**
     * Get the latitude values for the given type.
     *
     * @param type gaussian type
     * @param start  starting index (1 based)
     * @param num  number of values
     *
     * @return the values
     *
     * @throws IllegalArgumentException  invalid or unsupported type
     */
    public static double[] getGaussianLatitudes(String type, int start,
            int num)
            throws IllegalArgumentException {
        double[] baseArray = null;
        start--;  // it's one based
        if (type.equalsIgnoreCase(GAUST62)) {
            baseArray = gltst62;
        } else if (type.equalsIgnoreCase(GAUSR15)) {
            baseArray = glts15;
        } else if (type.equalsIgnoreCase(GAUSR20)) {
            baseArray = glts20;
        } else if (type.equalsIgnoreCase(GAUSR30)) {
            baseArray = glts30;
        } else if (type.equalsIgnoreCase(GAUSR40)) {
            baseArray = glats;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        if (start + num > baseArray.length) {
            throw new IllegalArgumentException("Maximum " + baseArray.length
                    + " latitudes exceeded");
        }
        double[] retVals = new double[num];
        for (int i = 0; i < num; i++) {
            retVals[i] = baseArray[start + i];
        }
        return retVals;
    }

}

