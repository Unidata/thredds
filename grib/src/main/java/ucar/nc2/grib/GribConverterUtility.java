/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib;

/**
 * Created with IntelliJ IDEA.
 * User: madry
 * Date: 3/21/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */

import ucar.nc2.dt.GridDataset;
import ucar.nc2.grib.GribVariableRenamer;
import java.io.IOException;
import java.util.List;

public class GribConverterUtility {
    public static void main(final String[] args) {
        String usage = "usage: ucar.nc2.dataset.GribConverterUtility -grib1|-grib2 <fileIn> <oldGribName> [-matchNCEP]";
        String grbFile = null;      // declare a grbFile filename
        String grbType = null;      // declare a grbFile type - should be grib1 or grib2
        String oldGribName = null;  // declare the old Grib Variable Name
        Boolean matchNCEP = false;  // don't default to matching NCEP

        if (args.length == 3 || args.length == 4) {
            String s = args[0];
            if (s.equalsIgnoreCase("-grib1")) {
                grbType = "grib1";
            } else if (s.equalsIgnoreCase("-grib2")) {
                       grbType = "grib2";
                   } else {
                       System.out.println(usage);
                       System.exit(0);
                     }
            grbFile = args[1];
            oldGribName = args[2];
            if (args.length == 4) {
                matchNCEP = true;  // could be any fourth command line token, but assume this is what the user means
            }
        }  else {
               System.out.println(usage);
               System.exit(0);

        }

        try {
            GribVariableRenamer r = null;
            GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(grbFile);
            r = new GribVariableRenamer();
            List result = null;
            if (grbType.equalsIgnoreCase("grib1")) {
                if (matchNCEP) {
                    result = r.matchNcepNames(gds, oldGribName);
                } else {
                    result = r.getMappedNamesGrib1(oldGribName);
                }
            } else {
                if (matchNCEP) {
                    result = r.matchNcepNames(gds, oldGribName);
                } else {
                    result = r.getMappedNamesGrib2(oldGribName);
                }
            }
            if (null == result) {
                System.out.println("Could not find \"" + oldGribName + "\" in " + grbFile);
                System.out.println("-Note that variable names are case sensitive");
            } else {
                System.out.println(result);
            }
        } catch (IOException e) {
            System.out.println("oops...");
//            System.out.println(e);
            System.out.println("Check filename " + grbFile);
        } catch (Exception e) {
            System.out.println("oops...");
            System.out.println(e);
        }
    }
}
