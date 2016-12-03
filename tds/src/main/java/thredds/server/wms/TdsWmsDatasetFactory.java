package thredds.server.wms;

import uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory;

import java.io.IOException;

import ucar.nc2.dataset.NetcdfDataset;

/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */

/**
 *  Basically, this class exists to use a NetcdfDataset that has been opened by the
 *  TDS into ncWMS. We needed to do this to use NcML wrapped datasets being served by
 *  the TDS, but since we already get a NetcdfDataset from the TDS side, we might as well
 *  pass it along to ncWMS for non-NcML cases too.
 */
public class TdsWmsDatasetFactory extends CdmGridDatasetFactory {

    private NetcdfDataset netcdfDataset;

    public void setNetcdfDataset(NetcdfDataset ncd) {
        this.netcdfDataset = ncd;
    }

    /**
     *
     * None of this matters really. For NcML datasets, we cannot rely on a location, as the
     * ncml is modifying the dataset in memory. So, we are ignoring location and force reset
     * while accssing the NetcdfDataset directly (which is set in ThreddsWmsCatalogue)
     *
     * @param location - location of the netCDF File
     * @param forceRefresh - this is an ncWMS specific thing
     * @return
     * @throws IOException
     */
    @Override
    protected NetcdfDataset getNetcdfDatasetFromLocation(String location, boolean forceRefresh) {
        return this.netcdfDataset;
    }

}
