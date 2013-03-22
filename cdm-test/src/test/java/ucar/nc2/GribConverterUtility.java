package ucar.nc2;

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
        String usage = "usage: ucar.nc2.dataset.GribConverterUtility -grib1|-grib2 <fileIn> <oldGribName>";
        String grbFile = null;    // declare a grbFile filename
        String grbType = null;    // declare a grbFile type - should be grib1 or grib2
        String oldGribName = null;// declare the old Grib Variable Name

        if (args.length == 3) {
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
        }  else {
               System.out.println(usage);
        }

        try {
            GribVariableRenamer r = null;
            GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(grbFile);
            r = new GribVariableRenamer();
            List result = null;
            if (grbType.equalsIgnoreCase("grib1")) {
                result = r.getMappedNamesGrib1(oldGribName);
            } else {
                result = r.getMappedNamesGrib2(oldGribName);
            }
            if (null == result) {
                System.out.println("Could not find \"" + oldGribName + "\" in " + grbFile);
                System.out.println("-Note that variable names are case sensitive");
            } else {
                System.out.println(result);
            }
        } catch (IOException e) {
            System.out.println("oops...");
            System.out.println(e);
            System.out.println("Check filename " + grbFile);
        } catch (Exception e) {
            System.out.println("oops...");
            System.out.println(e);
        }
    }
}
