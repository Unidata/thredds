/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


import java.io.IOException;


/**
 * Wrapper around GempakParameterTable for use in a static context.
 */
public class GempakGridParameterTable {

    /** static table */
    private static GempakParameterTable paramTable =
        new GempakParameterTable();

    /**
     * Default ctor
     */
    public GempakGridParameterTable() {}

    /**
     * Add parameters from the table
     *
     * @param tbl   table location
     *
     * @throws IOException   problem reading table.
     */
    public static void addParameters(String tbl) throws IOException {
        paramTable.addParameters(tbl);
    }

    /**
     * Get the parameter for the given name
     *
     * @param name   name of the parameter (eg:, TMPK);
     *
     * @return  corresponding parameter or null if not found in table
     */
    public static GempakParameter getParameter(String name) {
        return paramTable.getParameter(name);
    }
}

