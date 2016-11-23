/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */

package thredds.server.ncss.view.gridaspoint;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.vertical.VerticalTransform;

public class GeoCsvWriter extends CSVPointDataWriter {

    static private final String GEOCSV_CONTAINER_TYPE = "GeoCSV 2.0";
    static private final String DELIMITER = ",";
    static private final String DEFAULT_MISSING_VALUE = "";
    static private final String UNKNOWN_UNIT = "unknown";

    private List<String> fieldNames = new ArrayList<>();
    private List<String> fieldUnits = new ArrayList<>();
    private List<String> fieldTypes = new ArrayList<>();
    private List<String> fieldMissing = new ArrayList<>();

    protected GeoCsvWriter(OutputStream os) {
        super(os);
    }

    public static GeoCsvWriter factory(OutputStream os) {
        return new GeoCsvWriter(os);
    }

    /**
     * Helper to append new metadata to header. Use this when the variable
     * does not have a missing value defined.
     *
     * @param name name of the variable
     * @param unit unit of measurement
     * @param type data type of the variable
     */
    private void appendMetadata(String name, String unit, String type) {
        appendMetadata(name, unit, type, "");
    }

    /**
     * Helper to append new metadata to header
     *
     * @param name    name of the variable
     * @param unit    unit of measurement
     * @param type    data type of the variable
     * @param missing missing value
     */
    private void appendMetadata(String name, String unit, String type, String missing) {
        fieldNames.add(name);
        fieldUnits.add(unit);
        fieldTypes.add(type);
        fieldMissing.add(missing);
    }

    /**
     * Write out the header of the GeoCSV file
     *
     * @param varGroup    List of Variables
     * @param gridDataset GridDataset
     * @param hasEnsAxis  Has an ensemble axis?
     * @param hasTimeAxis Has a time axis?
     */
    @Override
    protected void writeGroupHeader(List<String> varGroup, GridDataset gridDataset, boolean hasEnsAxis, boolean hasTimeAxis) {

        if (hasTimeAxis) {
            appendMetadata("time", "ISO_8601", "datetime");
        }

        appendMetadata("latitude", "degrees_north", "double");

        appendMetadata("longitude", "degrees_east", "double");

        if (hasEnsAxis) {
            appendMetadata("ensMember", "unitless", "double");
        }

        GridCoordSystem coordSystem = gridDataset.findGridDatatype(varGroup.get(0)).getCoordinateSystem();
        CoordinateAxis1D zAxis = coordSystem.getVerticalAxis();

        if (zAxis != null) {
            appendMetadata("vertCoord,", zAxis.getUnitsString(), zAxis.getDataType().toString(), "");
        }

        VerticalTransform vt = coordSystem.getVerticalTransform();
        if (vt != null) {
            String ft = "unknown";
            try {
                ft = vt.getCoordinateArray(0).getDataType().toString();
            } catch (IOException e) {
                // should get caught in super
            } catch (InvalidRangeException e) {
                // should get caught in super
            }
            appendMetadata("vertCoord", vt.getUnitString(), ft, "");
        }

        Iterator<String> it = varGroup.iterator();
        while (it.hasNext()) {
            GridDatatype grid = gridDataset.findGridDatatype(it.next());
            String name = grid.getName();

            String unit = UNKNOWN_UNIT;
            if (grid.getUnitsString() != null) {
                unit = grid.getUnitsString();
            }

            String type = grid.getDataType().toString();

            String missing = DEFAULT_MISSING_VALUE;
            if (grid.hasMissingData()) {
                missing = grid.findAttributeIgnoreCase("missing_value").getStringValue();
            }
            appendMetadata(name, unit, type, missing);
        }

        printWriter.print("# dataset: ".concat(GEOCSV_CONTAINER_TYPE));
        printWriter.println();

        printWriter.print("# delimiter: ".concat(DELIMITER));
        printWriter.println();

        printWriter.print("# field_unit: ".concat(StringUtils.join(fieldUnits, DELIMITER)));
        printWriter.println();

        printWriter.print("# field_types: ".concat(StringUtils.join(fieldTypes, DELIMITER)));
        printWriter.println();

        printWriter.print("# field_missing: ".concat(StringUtils.join(fieldMissing, DELIMITER)));
        printWriter.println();

        printWriter.print(StringUtils.join(fieldNames, DELIMITER));
        printWriter.println();
    }
}