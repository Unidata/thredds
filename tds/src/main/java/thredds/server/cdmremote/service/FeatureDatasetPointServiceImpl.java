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
			fdp = fc.getFeatureDatasetPoint();

		} else {
			// tom kunicki 12/18/10
			// allows a single NetcdfFile to be turned into a FeatureDataset
			NetcdfFile ncfile = DatasetHandler.getNetcdfFile(req, res, path);
			if (ncfile != null) {
				FeatureDataset fd = FeatureDatasetFactoryManager.wrap(
						FeatureType.ANY,                  // will check FeatureType below if needed...
						NetcdfDataset.wrap(ncfile, null),
						null,
						new Formatter(System.err));       // better way to do this?
				if (fd instanceof FeatureDatasetPoint) {
					fdp = (FeatureDatasetPoint) fd;
				}
			}
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
