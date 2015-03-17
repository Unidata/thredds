/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.cdmremote.service;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import thredds.catalog.InvDatasetFeatureCollection;
import thredds.servlet.DatasetHandler;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;

@Service
public class FeatureDatasetPointServiceImpl implements 	FeatureDatasetPointService {

	private static final Logger log = LoggerFactory.getLogger(FeatureDatasetPointServiceImpl.class);
	
	/* (non-Javadoc)
	 * @see thredds.server.cdmremote.service.FeatureDatasetPointService#findFeatureDatasetPointByPath(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public FeatureDatasetPoint findFeatureDatasetPointByPath(
			HttpServletRequest req, HttpServletResponse res, String path) throws IOException {

		FeatureDatasetPoint fdp = null;
		// this looks for a featureCollection
		InvDatasetFeatureCollection fc = DatasetHandler.getFeatureCollection(req, res, path);

		if (fc != null) {
			fdp = (FeatureDatasetPoint) fc.getFeatureDataset();

		} else {
			// tom kunicki 12/18/10
			// allows a single NetcdfFile to be turned into a FeatureDataset
			NetcdfFile ncfile = DatasetHandler.getNetcdfFile(req, res, path);
      if (ncfile == null) return null;  // restricted access

			FeatureDataset fd = FeatureDatasetFactoryManager.wrap(
						FeatureType.ANY,                  // will check FeatureType below if needed...
						NetcdfDataset.wrap(ncfile, null),
						null,
						new Formatter(System.err));       // better way to do this?
      if (fd instanceof FeatureDatasetPoint)
        fdp = (FeatureDatasetPoint) fd;
  }

		//---//
		if (fdp == null) {
			res.sendError(HttpServletResponse.SC_NOT_FOUND, "not a point or station dataset");
			return null;
		}

		List<FeatureCollection> list = fdp.getPointFeatureCollectionList();
		if (list.size() == 0) {
			log.error(fdp.getLocation()+" does not have any PointFeatureCollections");
			res.sendError(HttpServletResponse.SC_NOT_FOUND, fdp.getLocation()+" does not have any PointFeatureCollections");
			return null;
		}

		// check on feature type, using suffix convention LOOK
		FeatureType ft = null;
		if (path.endsWith("/station")) {
			ft = FeatureType.STATION;
			path = path.substring(0, path.lastIndexOf('/'));
		} else if (path.endsWith("/point")) {
			ft = FeatureType.POINT;
			path = path.substring(0, path.lastIndexOf('/'));
		}

		if (ft != null && ft != fdp.getFeatureType()) {
			res.sendError(HttpServletResponse.SC_NOT_FOUND, "feature type mismatch:  expetected " + ft + " found" + fdp.getFeatureType());
		}		
		
		return fdp;
		
	}

}
