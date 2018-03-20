/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

/**
 * Enumeration of the kinds of nodes in a CDM model.
 * @link CDMNode.java
 *
 * @author Dennis Heimbigner
 */

public enum CDMSort {
    ATTRIBUTE,
    DIMENSION,
    ENUMERATION,
    VARIABLE, // Atomic
    SEQUENCE,
    STRUCTURE,
    GROUP
}
