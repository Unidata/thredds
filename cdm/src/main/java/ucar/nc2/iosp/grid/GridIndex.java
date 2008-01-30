/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.nc2.iosp.grid;


import java.util.*;


/**
 * An "in memory" index for 2D grid files.
 *
 */
public final class GridIndex {

    /**
     * Contains GridRecords
     */
    private final ArrayList index = new ArrayList();

    /**
     * Contains list of grid definitions (mostly projection info)
     */
    private final ArrayList gcs = new ArrayList();

    /**
     * contains global attributes of the Index.
     */
    private final HashMap atts = new HashMap();

    /**
     * Constructor for creating an Index from the Grid file.
     * Use the addXXX() methods.
     */
    public GridIndex() {}

    /**
     * GlobalAttributes of index.
     * @return HashMap of type GlobalAttributes.
     */
    public final HashMap getGlobalAttributes() {
        return atts;
    }

    /**
     * Grib records of index, one for each parameter.
     * @return list of type GridRecord.
     */
    public final List<GridRecord> getGridRecords() {
        return index;
    }

    /**
     * GridDefs of the index.
     * @return list of type GridDef.
     */
    public final List getHorizCoordSys() {
        return gcs;
    }

    /**
     * adds a GridRecord to the index.
     * @param gr GridRecord
     */
    public final void addGridRecord(GridRecord gr) {
        index.add(gr);
    }

    /**
     * adds a GridDefRecord to the index.
     * @param gds GdsRecord
     */
    public final void addHorizCoordSys(GridDefRecord gds) {
        gcs.add(gds);
    }

    /**
     * adds a GlobalAttribute to the index.
     * @param name GlobalAttribute
     * @param value String
     */
    public final void addGlobalAttribute(String name, String value) {
        atts.put(name, value);
    }

    /**
     * Get the grid count
     * @return the count
     */
    public int getGridCount() {
        return index.size();
    }

}

