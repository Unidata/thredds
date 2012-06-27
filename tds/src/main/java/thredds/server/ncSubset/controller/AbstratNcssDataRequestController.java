package thredds.server.ncSubset.controller;

import thredds.server.ncSubset.exception.UnsupportedOperationException;
import thredds.server.ncSubset.exception.VariableNotContainedInDatasetException;
import thredds.server.ncSubset.params.RequestParamsBean;
import ucar.nc2.dt.GridDataset;

public abstract class AbstratNcssDataRequestController extends AbstractNcssController {

	/**
	 * Checks all requested variables are in the dataset for a Ncss requests. 
	 * 
	 * @param gds
	 * @param params
	 * @throws VariableNotContainedInDatasetException
	 * @throws UnsupportedOperationException 
	 */
	protected abstract void checkRequestedVars(GridDataset gds, RequestParamsBean params) throws VariableNotContainedInDatasetException, UnsupportedOperationException;
}
