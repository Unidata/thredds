/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package ucar.nc2.iosp.gempak;


import ucar.grid.GridParameter;

import ucar.unidata.util.StringUtil;

import java.io.*;

// When ucar.unidata.util is in common, revert to using this
//import ucar.unidata.util.IOUtil;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class to hold a lookup of gempak parameters
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GempakParameterTable {

    /** table to hold the  values */
    private HashMap<String, GempakParameter> paramMap =
        new HashMap<String, GempakParameter>(256);

    /** table to hold the template values */
    private HashMap<String, GempakParameter> templateParamMap =
        new HashMap<String, GempakParameter>(20);

    /** indices of breakpoints in the table */
    private static int[] indices = {
        0, 4, 38, 59, 72, 79, 90, 97
    };

    /** lengths */
    private static int[] lengths = {
        4, 33, 21, 13, 7, 11, 6, 6
    };

    /**
     * Create a new table.
     */
    public GempakParameterTable() {}

    /*
ID# NAME                             UNITS                GNAM         SCALE   MISSING  HZREMAP DIRECTION
    */

    /**
     * Add parameters from the table
     *
     * @param tbl   table location
     *
     * @throws IOException   problem reading table.
     */
    public void addParameters(String tbl) throws IOException {
        //String content = IOUtil.readContents(tbl, GempakParameterTable.class);
        InputStream is = getInputStream(tbl);
        if (is == null) {
            throw new IOException("Unable to open " + tbl);
        }
        String         content = readContents(is);
        List           lines   = StringUtil.split(content, "\n", false);
        List<String[]> result  = new ArrayList<String[]>();
        for (int i = 0; i < lines.size(); i++) {
            String line  = (String) lines.get(i);
            String tline = line.trim();
            if (tline.length() == 0) {
                continue;
            }
            if (tline.startsWith("!")) {
                continue;
            }
            String[] words = new String[indices.length];
            for (int idx = 0; idx < indices.length; idx++) {
                if (indices[idx] >= tline.length()) {
                    continue;
                }
                if (indices[idx] + lengths[idx] > tline.length()) {
                    words[idx] = line.substring(indices[idx]);
                } else {
                    words[idx] = line.substring(indices[idx],
                            indices[idx] + lengths[idx]);
                }
                //if (trimWords) {
                words[idx] = words[idx].trim();
                //}
            }
            result.add(words);
        }
        for (int i = 0; i < result.size(); i++) {
            GempakParameter p = makeParameter((String[]) result.get(i));
            if (p != null) {
                if (p.getName().indexOf("(") >= 0) {
                    templateParamMap.put(p.getName(), p);
                } else {
                    paramMap.put(p.getName(), p);
                }
            }
        }

    }

    /**
     * Make a parameter from the tokens
     *
     * @param words   the tokens
     *
     * @return  a grid parameter (may be null)
     */
    private GempakParameter makeParameter(String[] words) {
        int    num = 0;
        String description;
        if (words[0] != null) {
            num = (int) Double.parseDouble(words[0]);
        }
        if ((words[3] == null) || words[3].equals("")) {  // no param name
            return null;
        }
        String name = words[3];
        if (name.indexOf("-") >= 0) {
            int          first = name.indexOf("-");
            int          last  = name.lastIndexOf("-");
            StringBuffer buf   = new StringBuffer(name.substring(0, first));
            buf.append("(");
            for (int i = first; i <= last; i++) {
                buf.append("\\d");
            }
            buf.append(")");
            buf.append(name.substring(last + 1));
            name = buf.toString();
        }

        if ((words[1] == null) || words[1].equals("")) {
            description = words[3];
        } else {
            description = words[1];
        }
        String unit = words[2];
        if (unit != null) {
            unit = unit.replaceAll("\\*\\*", "");
            if (unit.equals("-")) {
                unit = "";
            }
        }
        int decimalScale = 0;
        try {
            decimalScale = Integer.parseInt(words[4].trim());
        } catch (NumberFormatException ne) {
            decimalScale = 0;
        }

        return new GempakParameter(num, name, description, unit,
                                   decimalScale);
    }

    /**
     * Get the parameter for the given name
     *
     * @param name   name of the parameter (eg:, TMPK);
     *
     * @return  corresponding parameter or null if not found in table
     */
    public GempakParameter getParameter(String name) {
        GempakParameter param = (GempakParameter) paramMap.get(name);
        if (param == null) {  // try the regex list
            Set<String> keys = templateParamMap.keySet();
            if ( !keys.isEmpty()) {
                for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
                    String  key = (String) iter.next();
                    Pattern p   = Pattern.compile(key);
                    Matcher m   = p.matcher(name);
                    if (m.matches()) {
                        //System.out.println("found match " + key + " for " + name);
                        String value = m.group(1);
                        GempakParameter match =
                            (GempakParameter) templateParamMap.get(key);
                        param = new GempakParameter(match.getNumber(), name,
                                match.getDescription() + " (" + value
                                + " hour)", match.getUnit(),
                                            match.getDecimalScale());
                        paramMap.put(name, param);
                        break;
                    }
                }
            }
        }
        return param;
    }

    /**
     * Test
     *
     * @param args ignored
     *
     * @throws IOException  problem reading the table.
     */
    public static void main(String[] args) throws IOException {
        GempakParameterTable pt = new GempakParameterTable();
        //pt.addParameters("resources/nj22/tables/gempak/wmogrib3.tbl");
        pt.addParameters("resources/nj22/tables/gempak/params.tbl");
        if (args.length > 0) {
            String          param = args[0];
            GempakParameter parm  = pt.getParameter(param);
            if (parm != null) {
                System.out.println("Found " + param + ": " + parm);
            }
        }
    }



    /**
     * Read in the bytes from the given InputStream
     * and construct and return a String.
     * Closes the InputStream argument.
     *
     * @param is   InputStream to read from
     * @return  contents as a String
     *
     * @throws IOException  problem reading contents
     */
    private String readContents(InputStream is) throws IOException {
        return new String(readBytes(is));
    }

    /**
     * Read the bytes in the given input stream.
     *
     * @param is The input stream
     *
     * @return The bytes
     *
     * @throws IOException On badness
     */
    private byte[] readBytes(InputStream is) throws IOException {
        int    totalRead = 0;
        byte[] content   = new byte[1000000];
        while (true) {
            int howMany = is.read(content, totalRead,
                                  content.length - totalRead);
            if (howMany < 0) {
                break;
            }
            if (howMany == 0) {
                continue;
            }
            totalRead += howMany;
            if (totalRead >= content.length) {
                byte[] tmp       = content;
                int    newLength = ((content.length < 25000000)
                                    ? content.length * 2
                                    : content.length + 5000000);
                content = new byte[newLength];
                System.arraycopy(tmp, 0, content, 0, totalRead);
            }
        }
        is.close();
        byte[] results = new byte[totalRead];
        System.arraycopy(content, 0, results, 0, totalRead);
        return results;
    }

    /**
     * Get the input stream to the given resource
     *
     * @param resourceName The resource name. May be a file, url,
     *                     java resource, etc.
     *
     * @return The input stream to the resource
     */
    private InputStream getInputStream(String resourceName) {

        InputStream s = null;

        // Try class loader to get resource
        ClassLoader cl = GempakParameterTable.class.getClassLoader();
        s = cl.getResourceAsStream(resourceName);
        if (s != null) {
            return s;
        }

        //Try the file system
        File f = new File(resourceName);
        if (f.exists()) {
            try {
                s = new FileInputStream(f);
            } catch (Exception e) {}
        }
        if (s != null) {
            return s;
        }

        //Try it as a url
        try {
            Matcher       m = Pattern.compile(" ").matcher(resourceName);
            String        encodedUrl = m.replaceAll("%20");
            URL           dataUrl    = new URL(encodedUrl);
            URLConnection connection = dataUrl.openConnection();
            s = connection.getInputStream();
        } catch (Exception exc) {}

        return s;
    }


}

