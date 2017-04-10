/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;


import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static thredds.server.reify.LoadCommon.FileFormat;

/**
 * Process an HttpRequest to extract common download parameters
 */
class DownloadParameters extends Parameters
{
    //////////////////////////////////////////////////

    static String DEFAULTFILEFORMAT = "nc3";

    //////////////////////////////////////////////////
    // Known download parameters (allow direct access)

    public FileFormat format = null;
    public String url = null;
    public String target = null;
    public boolean overwrite = false;
    public boolean fromform = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DownloadParameters(HttpServletRequest req)
            throws IOException
    {
        super(req);

        // File Format: check for from form
        String sfmt;
        if(getparam("format") != null)
            sfmt = getparam("format");
        else
            sfmt = DEFAULTFILEFORMAT;
        this.format = FileFormat.getformat(sfmt);

        // url
        this.url = getparam("url");

        // target
        this.target = getparam("target");

        // inquiry key
        this.overwrite = (getparam("overwrite") != null);

        // Did this info come from a form?
        this.fromform = (getparam("fromform") != null);
    }

}
