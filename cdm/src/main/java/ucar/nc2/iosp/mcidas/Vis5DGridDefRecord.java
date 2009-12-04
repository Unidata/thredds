/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */



package ucar.nc2.iosp.mcidas;


import ucar.grid.GridDefRecord;

import visad.data.vis5d.Vis5DCoordinateSystem;


/**
 * Class to hold the Vis5D grid navigation information.
 *
 * @author Unidata Development Team
 */
public class Vis5DGridDefRecord extends GridDefRecord {


    /** _more_ */
    private static final int PROJ_GENERIC = 0;

    /** _more_ */
    private static final int PROJ_LINEAR = 1;

    /** _more_ */
    private static final int PROJ_CYLINDRICAL = 20;

    /** _more_ */
    private static final int PROJ_SPHERICAL = 21;

    /** _more_ */
    private static final int PROJ_LAMBERT = 2;

    /** _more_ */
    private static final int PROJ_STEREO = 3;

    /** _more_ */
    private static final int PROJ_ROTATED = 4;

    /** _more_          */
    private int projection;

    //private Vis5DCoordinateSystem coord_sys;

    /**
     * Create a new grid nav block with the values
     *
     * @param words   analysis block values
     *
     * @param Projection _more_
     * @param projargs _more_
     * @param nr _more_
     * @param nc _more_
     */
    public Vis5DGridDefRecord(int Projection, double[] projargs, int nr,
                              int nc) {
        projection = Projection;
        //coord_sys = new Vis5DCoordinateSystem(Projection, projargs, nr, nc);
        setParams(Projection, projargs);
        addParam(GDS_KEY, this.toString());
    }

    /**
     * Get a short name for this GDSKey for the netCDF group.
     * Subclasses should implement as a short description
     * @return short name
     */
    public String getGroupName() {
        StringBuffer buf = new StringBuffer();
        buf.append(getParam(PROJ));
        buf.append("_");
        buf.append(getParam(NX));
        buf.append("x");
        buf.append(getParam(NY));
        return buf.toString();
    }

    /**
     * Print out a string representation of this
     *
     * @return  a String representation of this.
     */
    public String toString() {
        // TODO: make this more unique
        StringBuffer buf = new StringBuffer(getParam(PROJ));
        buf.append(" X:");
        buf.append(getParam(NX));
        buf.append(" ");
        buf.append("Y:");
        buf.append(getParam(NY));
        return buf.toString();
    }

    /**
     * Check for equality
     *
     * @param o  object to compare
     *
     * @return true if equal
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ( !(o instanceof McGridDefRecord)) {
            return false;
        }
        return this.toString().equals(o.toString());
    }

    /**
     * Get the hashcode
     *
     * @return the hashcode
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * _more_
     *
     * @param Projection _more_
     * @param projargs _more_
     */
    private void setParams(int Projection, double[] projargs) {
        double NorthBound;
        double SouthBound;
        double WestBound;
        double EastBound;
        double RowInc;
        double ColInc;
        double Lat1;
        double Lat2;
        double PoleRow;
        double PoleCol;
        double CentralLat;
        double CentralLon;
        double CentralRow;
        double CentralCol;
        double Rotation;  /* radians */
        double Cone;
        double Hemisphere;
        double ConeFactor;
        double CosCentralLat;
        double SinCentralLat;
        double StereoScale;
        double InvScale;
        double CylinderScale;
        switch (Projection) {

          case PROJ_GENERIC :
              addParam(PROJ, "GENERIC");
          case PROJ_LINEAR :
              addParam(PROJ, "LINEAR");
          case PROJ_CYLINDRICAL :
              addParam(PROJ, "CYLINDRICAL");
          case PROJ_SPHERICAL :
              addParam(PROJ, "SPHERICAL");
              NorthBound = projargs[0];
              WestBound  = -projargs[1];
              RowInc     = projargs[2];
              ColInc     = projargs[3];
              addParam(LA1, String.valueOf(NorthBound));
              addParam(LO1, String.valueOf(WestBound));
              addParam(DX, String.valueOf(ColInc));
              addParam(DY, String.valueOf(RowInc));
              break;

          case PROJ_ROTATED :
              addParam(PROJ, "ROTATED");
              NorthBound = projargs[0];
              WestBound  = projargs[1];
              RowInc     = projargs[2];
              ColInc     = projargs[3];
              CentralLat = projargs[4];
              CentralLon = projargs[5];
              Rotation   = projargs[6];
              break;

          case PROJ_LAMBERT :
              addParam(PROJ, "LAMBERT");
              Lat1       = projargs[0];
              Lat2       = projargs[1];
              PoleRow    = projargs[2];
              PoleCol    = projargs[3];
              CentralLon = projargs[4];
              ColInc     = projargs[5];
              break;

          case PROJ_STEREO :
              addParam(PROJ, "STEREO");
              CentralLat = projargs[0];
              CentralLon = projargs[1];
              CentralRow = projargs[2];
              CentralCol = projargs[3];
              ColInc     = projargs[4];
              break;

        }
    }

    /**
     * _more_
     *
     * @param Projection _more_
     * @param projargs _more_
     */
    public static void printProjArgs(int Projection, double[] projargs) {
        double NorthBound;
        double SouthBound;
        double WestBound;
        double EastBound;
        double RowInc;
        double ColInc;
        double Lat1;
        double Lat2;
        double PoleRow;
        double PoleCol;
        double CentralLat;
        double CentralLon;
        double CentralRow;
        double CentralCol;
        double Rotation;  /* radians */
        double Cone;
        double Hemisphere;
        double ConeFactor;
        double CosCentralLat;
        double SinCentralLat;
        double StereoScale;
        double InvScale;
        double CylinderScale;
        switch (Projection) {

          case PROJ_GENERIC :
          case PROJ_LINEAR :
          case PROJ_CYLINDRICAL :
          case PROJ_SPHERICAL :
              NorthBound = projargs[0];
              WestBound  = projargs[1];
              RowInc     = projargs[2];
              ColInc     = projargs[3];
              System.out.println("Generic, Linear, Cylindrical, Spherical:");
              System.out.println("NB: " + NorthBound + ", WB: " + WestBound
                                 + ", rowInc: " + RowInc + ", colInc: "
                                 + ColInc);
              break;

          case PROJ_ROTATED :
              NorthBound = projargs[0];
              WestBound  = projargs[1];
              RowInc     = projargs[2];
              ColInc     = projargs[3];
              CentralLat = projargs[4];
              CentralLon = projargs[5];
              Rotation   = projargs[6];
              System.out.println("Rotated:");
              System.out.println("NB: " + NorthBound + ", WB: " + WestBound
                                 + ", rowInc: " + RowInc + ", colInc: "
                                 + ColInc + ", clat: " + CentralLat
                                 + ", clon: " + CentralLon + ", rotation: "
                                 + Rotation);
              break;

          case PROJ_LAMBERT :
              Lat1       = projargs[0];
              Lat2       = projargs[1];
              PoleRow    = projargs[2];
              PoleCol    = projargs[3];
              CentralLon = projargs[4];
              ColInc     = projargs[5];
              System.out.println("Lambert: ");
              System.out.println("lat1: " + Lat1 + ", lat2: " + Lat2
                                 + ", poleRow: " + PoleRow + ", PoleCol: "
                                 + PoleCol + ", clon: " + CentralLon
                                 + ", colInc: " + ColInc);
              break;

          case PROJ_STEREO :
              CentralLat = projargs[0];
              CentralLon = projargs[1];
              CentralRow = projargs[2];
              CentralCol = projargs[3];
              ColInc     = projargs[4];
              System.out.println("Stereo: ");
              System.out.println("clat: " + CentralLat + ", clon: "
                                 + CentralLon + ", cRow: " + CentralRow
                                 + ", cCol: " + CentralCol + ", colInc: "
                                 + ColInc);
              break;

          default :
              System.out.println("Projection unknown");
        }
    }
}

