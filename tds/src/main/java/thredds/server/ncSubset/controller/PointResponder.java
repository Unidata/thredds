package thredds.server.ncSubset.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.NcssParamsBean;
import thredds.server.ncSubset.view.dsg.PointWriter;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ft.*;
import ucar.nc2.util.DiskCache2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 10/3/13
 */
public class PointResponder implements NcssResponder {
	static private final Logger log = LoggerFactory.getLogger(NcssResponder.class);

  public static PointResponder factory(FeatureDataset fd, NcssParamsBean queryParams, DiskCache2 diskCache, SupportedFormat format, OutputStream out) throws IOException, ParseException, NcssException{
 		FeatureDatasetPoint fdp = (FeatureDatasetPoint) fd;
 		List<FeatureCollection> coll = fdp.getPointFeatureCollectionList();
    PointFeatureCollection sfc = (PointFeatureCollection) coll.get(0);
    PointWriter writer = PointWriter.factory((FeatureDatasetPoint) fd, sfc, queryParams, diskCache, out, format);

 		return new PointResponder(diskCache, format, out, writer);
 	}

	//private DiskCache2 diskCache = null;
	//private SupportedFormat format;
	//private OutputStream out;

	private PointWriter writer;

	private PointResponder(DiskCache2 diskCache, SupportedFormat format, OutputStream out, PointWriter writer) {
		//this.diskCache = diskCache;
		//this.format = format;
		//this.out = out;
		this.writer = writer;
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

    writer.write();
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
		return writer.getHttpHeaders(fd, format, datasetPath);
	}

}
