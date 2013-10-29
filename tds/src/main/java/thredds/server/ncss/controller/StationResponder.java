/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.view.dsg.StationWriter;
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
public class StationResponder implements NcssResponder {
	static private final Logger log = LoggerFactory.getLogger(StationResponder.class);

  public static StationResponder factory(FeatureDataset fd, NcssParamsBean queryParams, DiskCache2 diskCache, SupportedFormat format, OutputStream out) throws IOException, ParseException, NcssException{
 		FeatureDatasetPoint fdp = (FeatureDatasetPoint) fd;
 		List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
 		StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) coll.get(0);
 		StationWriter stationWriter = StationWriter.stationWriterFactory((FeatureDatasetPoint) fd, sfc, queryParams, diskCache, out, format);

 		return new StationResponder(diskCache, format, out, stationWriter);
 	}
	
	//private DiskCache2 diskCache = null;
	//private SupportedFormat format;
	//private OutputStream out;
	
	private StationWriter stationWriter;

	private StationResponder(DiskCache2 diskCache, SupportedFormat format, OutputStream out, StationWriter stationWriter) {
		//this.diskCache = diskCache;
		//this.format = format;
		//this.out = out;
		this.stationWriter = stationWriter;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * thredds.server.ncSubset.NCSSPointDataStream#pointDataStream(javax.servlet
	 * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * ucar.nc2.constants.FeatureType, java.lang.String,
	 * thredds.server.ncSubset.params.ParamsBean)
	 */
	@Override
	public void respond(HttpServletResponse res, FeatureDataset fd, String requestPathInfo, NcssParamsBean queryParams, SupportedFormat format)
			throws IOException, ParseException, InvalidRangeException, NcssException {
		
		stationWriter.write();
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * thredds.server.ncSubset.NCSSPointDataStream#getResponseHeaders(ucar.nc2
	 * .ft.FeatureDataset, thredds.server.ncSubset.format.SupportedFormat,
	 * java.lang.String)
	 */
	@Override
	public HttpHeaders getResponseHeaders(FeatureDataset fd, SupportedFormat format, String datasetPath) {
		return stationWriter.getHttpHeaders(fd, format, datasetPath);
	}

}
