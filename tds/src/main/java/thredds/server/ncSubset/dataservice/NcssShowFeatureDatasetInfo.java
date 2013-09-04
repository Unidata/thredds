package thredds.server.ncSubset.dataservice;

import ucar.nc2.ft.FeatureDataset;

public interface NcssShowFeatureDatasetInfo {
	
	public String showForm( FeatureDataset gds, String datsetUrlPath, boolean wantXml, boolean isPoint );

}
