/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grads;


import ucar.nc2.constants.CDM;

import java.util.ArrayList;
import java.util.List;


/**
 * Hold information about a GradDimension
 * @author         Don Murray - CU/CIRES
 */
public class GradsDimension {

    /** linear type mapping */
    public static final String LINEAR = "LINEAR";

    /** levels type mapping */
    public static final String LEVELS = "LEVELS";

    /** the name of the dimension */
    private String name;

    /** the size of the dimension */
    private int size;

    /** the type of mapping (LINEAR, LEVELS, etc) */
    private String mapping;

    /** list of levels or params for LINEAR */
    private List<String> levels;

    /** the actual levels */
    private double[] levelVals;

    /** the unit */
    private String unitName = "";

    /**
     * Make a new GradsDimension from the values
     *
     * @param name  the dimension name
     * @param size  the dimension size
     * @param mapping  the dimension mapping type
     */
    public GradsDimension(String name, int size, String mapping) {
        this.name    = name;
        this.size    = size;
        this.mapping = mapping;
        levels       = new ArrayList<String>();
        if (name.equalsIgnoreCase(GradsDataDescriptorFile.XDEF)) {
            unitName = CDM.LON_UNITS;
        } else if (name.equalsIgnoreCase(GradsDataDescriptorFile.YDEF)) {
            unitName = CDM.LAT_UNITS;
        } else if (name.equalsIgnoreCase(GradsDataDescriptorFile.ZDEF)) {
            unitName = "hPa";
        }
    }

    /**
     * Add a level to the list of levels
     *
     * @param level level to add
     */
    protected void addLevel(String level) {
        levels.add(level);
    }

    /**
     * Get the levels
     *
     * @return  the list of levels
     */
    protected List<String> getLevels() {
        return levels;
    }

    /**
     * Get the name of this dimension
     *
     * @return  the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the size of this dimension
     *
     * @return  the size
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the values
     *
     * @return  the values
     */
    public double[] getValues() {
        if (levelVals == null) {
            levelVals = makeLevelValues();
        }

        return levelVals;
    }

    /**
     * Get the units
     *
     * @return  the units
     */
    public String getUnit() {
        return unitName;
    }

    /**
     * Set the unit
     *
     * @param unit  the unit
     */
    protected void setUnit(String unit) {
        unitName = unit;
    }

    /**
     * Get the mapping type
     *
     * @return  the mapping type
     */
    public String getType() {
        return mapping;
    }

    /**
     * Make the level values from the specifications
     *
     * @return the level values
     */
    protected double[] makeLevelValues() {
        if (levels == null) {
            return null;
        }
        if (levels.size() != size) {
            // do someting
        }
        double[] vals = new double[size];
        if (mapping.equalsIgnoreCase(LEVELS)) {
            for (int i = 0; i < vals.length; i++) {
                vals[i] = Double.parseDouble(levels.get(i));
            }
        } else if (mapping.equalsIgnoreCase(LINEAR)) {
            double start = 0;
            double inc   = 0;
            start = Double.parseDouble(levels.get(0));
            inc   = Double.parseDouble(levels.get(1));
            for (int i = 0; i < size; i++) {
                vals[i] = start + i * inc;
            }
            // TODO: figure out a better way to do this in case they don't start with gaus (e.g. MOM32)
        } else if (mapping.toLowerCase().startsWith("gaus")) {
            vals = GradsUtil.getGaussianLatitudes(mapping,
                    (int) Double.parseDouble(levels.get(0)), size);
        }
        // sanity check on z units
        if (name.equals(GradsDataDescriptorFile.ZDEF)) {
            for (int i = 0; i < vals.length; i++) {
                double val = vals[i];
                if (val > 1050) {
                    unitName = "Pa";

                    break;
                } else if (val < 10) {
                    // sometimes it's just a level number
                    // probably should be something else, but dimensionless
                    unitName = "";

                    break;
                }
            }
        }
        return vals;
    }

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Dimension: ");
        buf.append(name.toUpperCase());
        buf.append("\n");
        buf.append("\tSize: ");
        buf.append(size);
        buf.append("\n");
        buf.append("\tLevels Size: ");
        buf.append(levels.size());
        buf.append("\n");
        buf.append("\tMappingType: ");
        buf.append(mapping.toUpperCase());
        buf.append("\n");
        buf.append("\tLevels: ");
        buf.append(levels.toString());
        buf.append("\n");
        buf.append("\tUnits: ");
        buf.append(unitName);
        buf.append("\n");

        return buf.toString();
    }
}

