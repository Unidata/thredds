/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.cdm.dsp.CDMDSP;
import dap4.core.ce.CEConstraint;
import dap4.core.data.DSP;
import dap4.core.util.DapContext;
import dap4.core.util.DapException;
import dap4.dap4lib.AbstractDSP;
import dap4.dap4lib.DapCodes;
import dap4.dap4lib.FileDSP;
import dap4.dap4lib.HttpDSP;
import dap4.dap4lib.netcdf.Nc4DSP;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


/**
The old DAP4 code assumed that all source files
on the server could be specified using a path string.
It turns out that for virtual files like .ncml files,
this is false and instead, a NetcdfFile instance must be used.

Fixing this necessitated some significant changes to the DAP4 code.
The basic fix was to make the primary DAP4 code operate with respect
to a DSP object. Then the problem was to change the code that invoked DAP4
so that it figured out what kind of DSP object to use.

The primary change on the server side is to provide a function that,
given an object, can figure out what kind of DSP to use to convert that
kind of object to a DAP4 representation. This is defined in the new class
called DapDSP. DapDSP has two primary externally visible API functions:
1. open a NetcdfFile - returns a CDMDSP wrapping the NetcdfFile object.
2. open a String - uses the rules below to figure out what kind of DSP
   to return.

If the string parses as a URI, then look at the protocol.
If the protocol is file://, then treat as a file path and
use the rules below.

Otherwise, look at the protocol and the mode arg in the fragment
part and use the following mapping.
   * dap4://... use HttpDSP with URI converted to http://...
   * #mode=dap4 - use HttpDSP
   * #dap4 - use HttpDSP
In practice, HttpDSP is only used on the client side to read a
DAP4 encoded stream.

If the String is a file path (or file:// URL) then 
determine the DSP using the file path extension as follows:
   * .dmr|.syn| - use SynDSP
   * .dap|.raw - use FileDSP
   * .nc|.hdf5 - use Nc4DSP

As a rule, clients will end up using HttpDSP to read the
incoming DAP4 encode Http stream. Again as a rule, Servers will
assume an incoming Http request is for a NetcdfFile and will use
CDMDSP to convert that to a DAP4 Http stream. In the event that
Server detects that the file to be read is .nc, then it may
choose to use Nc4DSP to read the file, although it is not
required to and can instead open the file as a NetcdfFile object
and use CDMDSP.  All of the other cases are, again as a rule,
used for testing and can be identified by the special path/URI extension
(e.g. .syn or .dap or .raw).

The other major change, and the one that actually prompted this
whole change, is in the DapController and its
subclasses. Specifically, the function getNetcdfFile() has been
added to attempt to convert a String (a location) to a
NetcdfFile object (ultimately via DapDSP).  It is only if this
fails that an attempt is made to look for the other cases via
the location extension.
 */

abstract public class DapDSP
{

    //////////////////////////////////////////////////
    // Constants

    static final String driveletters = "abcdefghijklmnopqrstuvwxyz" +"abcdefghijklmnopqrstuvwxyz".toUpperCase();

    //////////////////////////////////////////////////
    // Types

    static protected class ExtMatch // match by extension
    {
        String ext;
        Class dsp;

        public ExtMatch(String ext, Class dsp) {
            this.ext = ext;
            this.dsp = dsp;
        }
    }

    static protected class FragMatch // match by URL fragment keys
    {
        String key;
        String value;
        Class dsp;

        public FragMatch(String key, String value, Class dsp)
        {
            this.key = key;
            this.value = value;
            this.dsp = dsp;
        }
    }

    static protected class ProtocolMatch // match by URL protocol
    {
        String proto;
        String replace;
        Class dsp;

        public ProtocolMatch(String proto, String replace, Class dsp)
        {
            this.proto = proto;
            this.replace = replace;
            this.dsp = dsp;
        }
    }

    //////////////////////////h////////////////////////
    // Static variables

    static ExtMatch[] match = new ExtMatch[]{
            new ExtMatch(".syn", SynDSP.class),
            new ExtMatch(".nc", Nc4DSP.class),
            new ExtMatch(".hdf5", Nc4DSP.class),
    };

    static FragMatch[] frags = new FragMatch[]{
            new FragMatch("mode", "dap4", HttpDSP.class),
            new FragMatch("proto", "dap4", HttpDSP.class),
            new FragMatch("protocol", "dap4", HttpDSP.class),
            new FragMatch("dap4", null, HttpDSP.class),
    };

    static ProtocolMatch[] protos = new ProtocolMatch[]{
            new ProtocolMatch("dap4", "https", HttpDSP.class),
    };

    /**************************************************/
    // Provide versions of open corresponding to the source type
    static protected DSP open(NetcdfFile ncfile, DapContext cxt)
            throws IOException
    {
        // Convert to CDMDSP
        CDMDSP dsp = new CDMDSP();
        if(dsp != null) dsp.setContext(cxt);
        dsp.open(ncfile);
        return dsp;
    }

    // Open a File
    static protected DSP open(File file, DapContext cxt)
            throws IOException
    {
        // Choose the DSP based on the file extension;
        // Note that we need to ignore any trailing .dmr or .dap
        String path = file.getPath();
        Class cldsp = null;
        String core = filepathcore(path);
        if(core != null && core.length() > 0) {
            for(ExtMatch em : match) {
                if(core.endsWith(em.ext)) {
                    cldsp = em.dsp;
                    break;
                }
            }
        }
        if(cldsp == null)
            throw new DapException("Indeciperable file: " + file);
        AbstractDSP dsp = null;
        try {
            dsp = (AbstractDSP) cldsp.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IOException("Class instance creation failed", e);
        }
        if(dsp != null) dsp.setContext(cxt);
        dsp.open(file);
        return dsp;
    }

    // Open a URL
    static protected DSP open(URI uri, DapContext cxt)
            throws IOException
    {
        AbstractDSP dsp = null;
        // See if this is a DAP4 url
        Class cldsp = null;
        // Search the fragment list for markers
        String fragments = uri.getFragment();
        Map<String, String> fragmap = parsefragment(fragments);
        for(FragMatch fm : frags) {
            String values = fragmap.get(fm.key);
            if(values != null) {
                if(fm.value == null) {
                    cldsp = fm.dsp;
                    break;
                } // singleton case
                else {// search for match
                    if(values.indexOf(fm.key) >= 0) {
                        cldsp = fm.dsp;
                        break;
                    }
                }
            }
            if(cldsp == null)
                throw new DapException("Indeciperable URI: " + uri);
            try {
                dsp = (AbstractDSP) cldsp.newInstance();
                break;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IOException("Class instance creation failed", e);
            }
        }
        if(dsp != null) {
            dsp.setContext(cxt);
            dsp.open(uri);
        }
        return dsp;
    }

    /**************************************************/
    // Exported versions for NetcdfFile and String

    public static synchronized DSP open(DapRequest drq, NetcdfFile ncfile, DapContext cxt)
            throws IOException
    {
        assert cxt != null && ncfile != null;
        // Convert to CDMDSP
        DSP dsp = open(ncfile,cxt);
        return dsp;
    }

    public static synchronized DSP open(DapRequest drq, String target, DapContext cxt)
            throws IOException
    {
        assert cxt != null && target != null;
        String path = null;
        DSP dsp = null;

        // See if this parses as a URL
        try {
            URI uri = new URI(target);
            String scheme = uri.getScheme();
            if(scheme == null)
                System.err.println("XXXX: "+target);
            assert(scheme != null);
            // Windows drive letters cause URI to succeed, so special hack for that
            if(scheme.length() == 1 && driveletters.indexOf(scheme.charAt(0)) >= 0)
                throw new URISyntaxException("windows drive letter", target);
            // If uri protocol is file, then extract the path
            if(scheme.equals("file"))
                path = uri.getPath();
            else
                dsp = open(uri, cxt); // open as general URI
        } catch (URISyntaxException use) {
            // assume it is a simple file path
            path = target;
        }

        String core = filepathcore(path); // remove any trailing .dmr|.dap

        if(dsp == null) {
            // See if this can open as a NetcdfFile|NetcdfDataset
            NetcdfFile ncfile = null;
            try {
                ncfile = drq.getController().getNetcdfFile(drq,core);
            } catch (IOException ioe) {
                ncfile = null;
            }
            if(ncfile != null) {
                dsp = open(ncfile,cxt);
            }
        }

        if(dsp == null) {
            // Finally, try to open as a some kind of File object
            File file = new File(core);
            // Complain if it does not exist
            if(!file.exists())
                throw new DapException("Not found: " + target)
                        .setCode(DapCodes.SC_NOT_FOUND);
            else
                dsp = open(file,cxt);
            if(dsp != null) dsp.setContext(cxt);
            ((AbstractDSP)dsp).open(file);
        }
        return dsp;
    }

    static protected Map<String, String>
    parsefragment(String fragments)
    {
        String[] pieces = fragments.split("[&]");
        Map<String, String> map = new HashMap<>();
        for(String p : pieces) {
            String[] pair = p.split("[=]");
            if(pair.length == 1) {
                map.put(pair[0].trim(), "");
            } else {
                map.put(pair[0].trim(), pair[1].trim());
            }
        }

        return map;
    }

    // Given a path that might end in .dmr|.dap, strip it of that ending
    // to get what should be the essential file path
    static String filepathcore(String path)
    {
        if(path == null) return path;
        int cut = 0;
        if(path.endsWith(".dmr")) cut = ".dmr".length();
        else if(path.endsWith(".dap")) cut = ".dap".length();
        return path.substring(0,path.length() - cut);
    }

}
