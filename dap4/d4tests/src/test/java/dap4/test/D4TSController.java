/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.d4ts.D4TSServlet;
import dap4.dap4lib.DapCodes;
import dap4.servlet.DapRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;


/* This is an extension of D4TSServlet to allow it to be tested
as using Spring mocking
*/

@Controller
@RequestMapping("/d4ts")
public class D4TSController extends D4TSServlet
{

    @Autowired
    private ServletContext servletContext;

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4TSController()
    {
        super();
    }

    //////////////////////////////////////////////////

    @RequestMapping("**")
    public void handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws IOException
    {
        super.handleRequest(req, res);
    }

    @Override
    public String
    getResourcePath(DapRequest drq, String location)
            throws DapException
    {
        String prefix = drq.getResourceRoot();
        if(prefix == null)
            throw new DapException("Cannot find location resource: " + location)
                    .setCode(DapCodes.SC_NOT_FOUND);
        location = DapUtil.canonicalpath(location);
        String datasetfilepath = DapUtil.canonjoin(prefix, location);
        // See if it really exists and is readable and of proper type
        File dataset = new File(datasetfilepath);
        if(!dataset.exists()) {
            String msg = String.format("Requested file does not exist: prefix=%s location=%s datasetfilepath=%s",
                    prefix, location, datasetfilepath);
            throw new DapException(msg)
                    .setCode(HttpServletResponse.SC_NOT_FOUND);
        }
        if(!dataset.canRead())
            throw new DapException("Requested file not readable: " + datasetfilepath)
                    .setCode(HttpServletResponse.SC_FORBIDDEN);
        return datasetfilepath;
    }

}
