/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.dap4;

import dap4.core.data.*;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4shared.*;
import dap4.cdmshared.*;
import dap4.servlet.CDMDSP;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import thredds.core.TdsRequestedDataset;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.*;

/**
 * CDM->DAP DSP
 */

public class ThreddsDSP extends CDMDSP
{


    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Constructor(s)

    public ThreddsDSP()
    {
    }

    public ThreddsDSP(String path, DapContext cxt)
            throws DapException
    {
        super(path,cxt);
//        init(createNetcdfFile(path, null));
    }

    public ThreddsDSP(NetcdfFile ncd, DapContext cxt)
            throws DapException
    {
        super(ncd,cxt);
//        init(ncd);
    }


    //////////////////////////////////////////////////

    @Override
    protected NetcdfFile
    createNetcdfFile(String location, CancelTask canceltask)
            throws DapException
    {
        try {
            path = DapUtil.canonicalpath(location);
            NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(this.request, this.response, null);
//            NetcdfFile ncfile = DatasetHandler.getNetcdfFile(this.request, this.response,location);
            return ncfile;
        } catch (Exception e) {
            return null;
        }
    }

}
