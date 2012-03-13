package thredds.server.ncSubset.dataservice;

import ucar.nc2.dt.GridDataset;

public interface NcssShowDatasetInfo {
	
	public String showForm( GridDataset gds, String datsetUrlPath, boolean wantXml, boolean isPoint );

}
