/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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




package ucar.nc2.iosp.gempak;


/**
 * Class to hold the Grid Analysis Block
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class AnalysisBlock {

    /** raw values */
    float[] vals = null;

    /**
     * Create a new analysis block
     */
    public AnalysisBlock() {}

    /**
     * Create a new analysis block with the values
     *
     * @param words   analysis block values
     */
    public AnalysisBlock(float[] words) {
        setValues(words);
    }

    /**
     * Set the analysis block values
     *
     * @param values   the raw values
     */
    public void setValues(float[] values) {
        vals = values;
    }

    /**
     * Print out the analysis block
     *
     * @return  a String representation of this
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if ((vals != null) && false) {
            // TODO:  fill this in
        } else {
            buf.append("\n\tUNKNOWN ANALYSIS TYPE");
        }
        return buf.toString();
    }

}

