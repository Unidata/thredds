/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

/**
 * Define an interface for DODSnetcdffile nodes to
 * store the original DODS name from the DDS so we can get groups right.
 *
 * @author Heimbigner
 */

public interface DODSNode
{
    String getDODSName();
    void setDODSName(String name);
}
