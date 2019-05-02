/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.nc2.dt.GridDataset;
import java.io.IOException;
import java.util.List;

/** @deprecated */
public class GribConverterUtility {
    public static void main(final String[] args) {
        String usage = "usage: ucar.nc2.dataset.GribConverterUtility -grib1|-grib2 <fileIn> <oldGribName> [-matchNCEP]";
        String grbFile = null;      // declare a grbFile filename
        String grbType = null;      // declare a grbFile type - should be grib1 or grib2
        String oldGribName = null;  // declare the old Grib Variable Name
        boolean matchNCEP = false;  // don't default to matching NCEP

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
            GribVariableRenamer r;
            GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(grbFile);
            r = new GribVariableRenamer();
            List result;
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
            System.out.println("Check filename " + grbFile);
        } catch (Exception e) {
            System.out.println("oops...");
            e.printStackTrace();
        }
    }
}
