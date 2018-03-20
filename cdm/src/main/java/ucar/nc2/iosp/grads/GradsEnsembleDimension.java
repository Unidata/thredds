/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 *
 */
package ucar.nc2.iosp.grads;


import java.util.List;


/**
 * Extension of GradsDimension to handle the complexities of ensembles
 *
 * @author Don Murray, CU-CIRES
 */
public class GradsEnsembleDimension extends GradsDimension {

    /** ensemble names identifier */
    public static final String NAMES = "NAMES";

    /** ensemble filename template */
    public static final String ENS_TEMPLATE_ID = "%e";

    /**
     * Create a new ensemble dimension holder
     *
     * @param name  the dimension name
     * @param size  the dimension size
     * @param mapping  the dimension mapping type
     */
    public GradsEnsembleDimension(String name, int size, String mapping) {
        super(name, size, mapping);
    }

    /**
     * Get the ensemble member names
     *
     * @return the list of names
     */
    public List<String> getEnsembleNames() {
        return getLevels();
    }

    /**
     * Make the level values from the specifications
     *
     * @return the level values
     */
    protected double[] makeLevelValues() {
        double[] vals = new double[getSize()];
        for (int i = 0; i < getSize(); i++) {
            vals[i] = i;
        }
        return vals;
    }

    /**
     * Replace the ensemble template parameter in a filename
     *
     * @param filespec  the file template
     * @param ensIndex the ensemble index
     *
     * @return  the filled in template
     */
    public String replaceFileTemplate(String filespec, int ensIndex) {
        return filespec.replaceAll(ENS_TEMPLATE_ID,
                                   getEnsembleNames().get(ensIndex));
    }

}

