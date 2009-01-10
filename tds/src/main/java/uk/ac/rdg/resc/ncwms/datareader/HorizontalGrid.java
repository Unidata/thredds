/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;

/**
 * A Grid of points onto which data is to be projected.  This is the grid that
 * is defined by the request CRS, the width, height and bounding box.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class HorizontalGrid
{
    private static final Logger logger = Logger.getLogger(HorizontalGrid.class);
    public static final String PLATE_CARREE_CRS_CODE = "CRS:84";
    public static final CoordinateReferenceSystem PLATE_CARREE_CRS;
    public static final List<String> SUPPORTED_CRS_CODES = new ArrayList<String>();
    
    private int width;      // Width of the grid in pixels
    private int height;     // Height of the grid in pixels
    private double[] bbox;  // Array of four floats representing the bounding box
    private String crsCode; // String representing the CRS
    private CoordinateReferenceSystem crs;
    private MathTransform transformToLatLon;
    
    private double[] xAxisValues;
    private double[] yAxisValues;
    
    static
    {
        // Find the supported CRS codes
        // I think this is the appropriate method to get all the CRS codes
        // that we can support
        for (Object codeObj : CRS.getSupportedCodes("urn:ogc:def"))
        {
            SUPPORTED_CRS_CODES.add((String)codeObj);
        }
        try
        {
            PLATE_CARREE_CRS = CRS.decode(PLATE_CARREE_CRS_CODE, true); // force longitude-first
        }
        catch (Exception ex)
        {
            throw new ExceptionInInitializerError("Can't find CRS:84");
        }
    }
    
    /**
     * Creates a HorizontalGrid from the parameters in the given GetMapDataRequest
     * 
     * @throws InvalidCrsException if the given CRS code is not recognized
     * @throws Exception if there was an internal error.
     * @todo check validity of the bounding box?
     */
    public HorizontalGrid(GetMapDataRequest dr) throws InvalidCrsException, Exception
    {
        this(dr.getCrsCode(), dr.getWidth(), dr.getHeight(), dr.getBbox());
    }
    
    /**
     * Creates a HorizontalGrid.
     * 
     * @param crsCode Code for the CRS of the grid
     * @param width Width of the grid in pixels
     * @param height Height of the grid in pixels
     * @param bbox Bounding box of the grid in the units of the given CRS
     * @throws InvalidCrsException if the given CRS code is not recognized
     * @throws Exception if there was an internal error.
     * @todo check validity of the bounding box?
     */
    public HorizontalGrid(String crsCode, int width, int height, double[] bbox)
        throws InvalidCrsException, Exception
    {
        try
        {
            logger.debug("Getting CRS object for code {}", crsCode);
            // The "true" means "force longitude-first axis order"
            this.crs = CRS.decode(crsCode, true);
            logger.debug("Finding transform for CRS {} to Plate Carree", crsCode);
            // Get a converter to convert the grid's CRS to lat-lon
            // The "true" means "lenient", i.e. ignore datum shifts.  This 
            // is necessary to prevent "Bursa wolf parameters required"
            // errors (Some CRSs, including British National Grid, fail if
            // we are not "lenient".)
            this.transformToLatLon = CRS.findMathTransform(
                this.crs, PLATE_CARREE_CRS, true);
            logger.debug("Found transform to Plate Carree");
        }
        catch (NoSuchAuthorityCodeException ex)
        {
            logger.error("No CRS with code {} can be found", crsCode);
            throw new InvalidCrsException(crsCode);
        }
        catch (FactoryException ex)
        {
            logger.error("FactoryException when creating CRS {} or its " +
                "transform to Plate Carree", crsCode);
            throw ex; // This is an internal error 
        }
        this.crsCode = crsCode;
        this.width = width;
        this.height = height;
        this.bbox = bbox;
        
        // Now calculate the values along the x and y axes of this grid
        double dx = (this.bbox[2] - this.bbox[0]) / this.width;
        this.xAxisValues = new double[this.width];
        for (int i = 0; i < this.xAxisValues.length; i++)
        {
            this.xAxisValues[i] = this.bbox[0] + (i + 0.5) * dx;
        }
        
        double dy = (this.bbox[3] - this.bbox[1]) / this.height;
        this.yAxisValues = new double[this.height];
        for (int i = 0; i < this.yAxisValues.length; i++)
        {
            // The y axis is flipped
            this.yAxisValues[i] = this.bbox[1] + (this.height - i - 0.5) * dy;
        }
        logger.debug("Created HorizontalGrid object for CRS {}", crsCode);
    }

    public int getWidth()
    {
        return this.width;
    }

    public int getHeight()
    {
        return this.height;
    }

    public double[] getBbox()
    {
        return this.bbox;
    }

    /**
     * @return a new array of points along the x axis in this coordinate
     * reference system
     */
    public double[] getXAxisValues()
    {
        return this.xAxisValues;
    }

    /**
     * @return a new array of points along the latitude axis
     */
    public double[] getYAxisValues()
    {
        return this.yAxisValues;
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem()
    {
        return this.crs;
    }
    
    /**
     * @return true if the given coordinate pair is valid for this CRS
     */
    public boolean isPointValidForCrs(double x, double y)
    {
        CoordinateSystemAxis xAxis = this.crs.getCoordinateSystem().getAxis(0);
        CoordinateSystemAxis yAxis = this.crs.getCoordinateSystem().getAxis(1);
        if (x < xAxis.getMinimumValue() || x > xAxis.getMaximumValue() ||
            y < yAxis.getMinimumValue() || y > yAxis.getMaximumValue())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public int getSize()
    {
        return this.width * this.height;
    }
    
    /**
     * Transforms the given x-y point to a LatLonPoint.
     * @throws TransformException if the required transformation could not be performed
     */
    public LatLonPoint transformToLatLon(double x, double y) throws TransformException
    {
        // We know x must go first in this array because we selected
        // "force longitude-first" when creating the CRS for this grid
        double[] point = new double[]{x, y};
        // Transform to lat-lon in-place
        this.transformToLatLon.transform(point, 0, point, 0, 1);
        return new LatLonPointImpl(point[1], point[0]);
    }
    
    /**
     * @return true if this grid is a lat-lon grid
     */
    public boolean isLatLon()
    {
        return this.transformToLatLon.isIdentity();
    }

    public String getCrsCode()
    {
        return crsCode;
    }
}
