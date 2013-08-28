/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import thredds.server.ncSubset.NCSSPointDataStream;
import thredds.server.ncSubset.exception.DateUnitException;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.OutOfBoundariesException;
import thredds.server.ncSubset.exception.TimeOutOfWindowException;
import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.PointDataRequestParamsBean;
import thredds.server.ncSubset.view.StationWriter;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.util.DiskCache2;

/**
 * @author mhermida
 *
 */
public class StationPointDataStream implements NCSSPointDataStream {

	
	private DiskCache2 diskCache = null; 
	private SupportedFormat format;
	
	public StationPointDataStream(DiskCache2 diskCache, SupportedFormat format){
		this.diskCache = diskCache;
		this.format = format;
	}
	
	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.NCSSPointDataStream#pointDataStream(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, ucar.nc2.constants.FeatureType, java.lang.String, thredds.server.ncSubset.params.ParamsBean)
	 */
	@Override
	public void pointDataStream(HttpServletRequest req,
			HttpServletResponse res, FeatureDataset fd, String requestPathInfo,
			PointDataRequestParamsBean queryParams, SupportedFormat format) throws IOException, ParseException, InvalidRangeException, NcssException {
		
		FeatureDatasetPoint fdp = (FeatureDatasetPoint) fd;
		
	    List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
	    StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);

	    StationWriter stationWriter = new StationWriter(fdp, sfc, queryParams, diskCache);
	    
	    	    
//	    res.setContentType(qb.getResponseType().toString());
//	    
//	    // special handling for netcdf files
//	    CdmrfQueryBean.ResponseType resType = qb.getResponseType();
//	    if (resType == CdmrfQueryBean.ResponseType.netcdf) {
//	      if (path.startsWith("/")) path = path.substring(1);
//	      path = StringUtil2.replace(path, "/", "-");
//	      res.setHeader("Content-Disposition", "attachment; filename=" + path + ".nc");
//
//	      File file = stationWriter.writeNetcdf();
//	      //ServletUtil.returnFile(req, res, file, getContentType(qb));
//	      ServletUtil.returnFile(req, res, file, "application/x-netcdf");
//	      if (!file.delete()) {
//	        log.warn("file delete failed =" + file.getPath());
//	      }
//	    }
//
//
	    StationWriter.Writer w = stationWriter.write(res, format);
		

	}
	
 

}
