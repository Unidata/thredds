/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.EscapeStrings;
import ucar.unidata.util.StringUtil2;
import ucar.unidata.util.Urlencoded;

import java.io.*;
import java.util.*;

/**
 * Detection of the protocol from a location string.
 * Split out from NetcdfDataset.
 * LOOK should be refactored
 *
 * @author caron
 * @since 10/20/2015.
 */
public class DatasetUrl {
  static final protected String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  static final protected String slashalpha = "\\/" + alpha;

  static final String[] FRAGPROTOCOLS =
          {"dap4", "dap2", "dods", "cdmremote", "thredds", "ncml"};
  static final ServiceType[] FRAGPROTOSVCTYPE =
          {ServiceType.DAP4, ServiceType.OPENDAP, ServiceType.OPENDAP, ServiceType.THREDDS, ServiceType.THREDDS, ServiceType.NCML};


  /**
   * Return the set of leading protocols for a url; may be more than one.
   * Watch out for Windows paths starting with a drive letter => protocol
   * names must all have a length > 1.
   * Watch out for '::'
   * Each captured protocol is saved without trailing ':'
   * Assume: the protocols MUST be terminated by the occurrence of '/'.
   *
   * @param url the url whose protocols to return
   * @return list of leading protocols without the trailing :
   */
  static public List<String> getProtocols(String url) {
    List<String> allprotocols = new ArrayList<>(); // all leading protocols upto path or host

    // Note, we cannot use split because of the context sensitivity
    // This code is quite ugly because of all the confounding cases
    // (e.g. windows path, embedded colons, etc.).
    // Specifically, the 'file:' protocol is a problem because
    // it has no many non-standard forms such as file:x/y file://x/y file:///x/y.
    StringBuilder buf = new StringBuilder(url);
    // If there are any leading protocols, then they must stop at the first '/'.
    int slashpos = buf.indexOf("/");
    // Check special case of file:<path> with no slashes after file:
    if (url.startsWith("file:") && "/\\".indexOf(url.charAt(5)) < 0) {
      allprotocols.add("file");
    } else if (slashpos >= 0) {
      // Remove everything after the first slash
      buf.delete(slashpos + 1, buf.length());
      for (; ; ) {
        int index = buf.indexOf(":");
        if (index < 0) break; // no more protocols
        // Validate protocol
        if (!validateprotocol(url, 0, index))
          break;
        String protocol = buf.substring(0, index);  // not including trailing ':'
        allprotocols.add(protocol);
        buf.delete(0, index + 1); // remove the leading protocol
      }
    }
    return allprotocols;
  }

  static private boolean validateprotocol(String url, int startpos, int endpos) {
    int len = endpos - startpos;
    if (len == 0) return false;
    char cs = url.charAt(startpos);
    char ce1 = url.charAt(endpos + 1);
    if (len == 1 //=>|protocol| == 1
            && alpha.indexOf(cs) >= 0 && "/\\".indexOf(ce1) >= 0)
      return false; // looks like windows drive letter
    // If trailing colon is not followed by alpha or /, then assume not url
    if (slashalpha.indexOf(ce1) < 0)
      return false;
    return true;
  }

  /////////////////////////////////////////////////////////////////////////////////////

  static public DatasetUrl findDatasetUrl(String orgLocation) throws IOException {
    ServiceType svctype = null;

    // Canonicalize the location
    String location = StringUtil2.replace(orgLocation.trim(), '\\', "/");
    List<String> allprotocols = DatasetUrl.getProtocols(location);

    String trueurl = location;
    String leadprotocol;
    if (allprotocols.size() == 0) {
      leadprotocol = "file";  // The location has no leading protocols, assume file:
    } else {
      leadprotocol = allprotocols.get(0);
    }

    // Priority in deciding
    // the service type is as follows.
    // 1. "protocol" tag in fragment
    // 2. specific protocol in fragment
    // 3. leading protocol
    // 4. path extension
    // 5. contact the server (if defined)

    // temporarily remove any trailing query or fragment
    String fragment = null;
    int pos = trueurl.lastIndexOf('#');
    if (pos >= 0) {
      fragment = trueurl.substring(pos + 1, trueurl.length());
      trueurl = trueurl.substring(0, pos);
    }
    pos = location.lastIndexOf('?');
    String query = null;
    if (pos >= 0) {
      query = trueurl.substring(pos + 1, trueurl.length());
      trueurl = trueurl.substring(0, pos);
    }
    if (fragment != null)
      svctype = searchFragment(fragment);

    if (svctype == null) // See if leading protocol tells us how to interpret
      svctype = decodeLeadProtocol(leadprotocol);

    if (svctype == null) // See if path tells us how to interpret
      svctype = searchPath(trueurl);

    if (svctype == null) {
      //There are several possibilities at this point; all of which
      // require further info to disambiguate
      //  - we have file://<path> or file:<path>; we need to see if
      //    the extension can help, otherwise, start defaulting.
      //  - we have a simple url: e.g. http://... ; contact the server
      if (leadprotocol.equals("file")) {
        svctype = decodePathExtension(trueurl); // look at the path extension
        if(svctype == null && checkIfNcml(new File(location))) {
          svctype = ServiceType.NCML;
        }
      } else {
        svctype = disambiguateHttp(trueurl);
        // special cases
        if ((svctype == null || svctype == ServiceType.HTTPServer)) {
          // ncml file being served over http?
          if (checkIfRemoteNcml(trueurl)) {
            svctype = ServiceType.NCML;
          }
        }
      }
    }

    if (svctype == ServiceType.NCML) { // ??
      // If lead protocol was null and then pretend it was a file
      // Note that technically, this should be 'file://'
      trueurl = (allprotocols.size() == 0 ? "file:" + trueurl : location);
    }

    // Add back the query and fragment (if any)
    if (query != null || fragment != null) {
      StringBuilder buf = new StringBuilder(trueurl);
      if (query != null) {
        buf.append('?');
        buf.append(query);
      }
      if (fragment != null) {
        buf.append('#');
        buf.append(fragment);
      }
      trueurl = buf.toString();
    }
    return new DatasetUrl(svctype, trueurl);
  }

  /**
   * Given a location, find markers indicated which protocol to use
   * LOOK what use case is this handling ?
   *
   * @param fragment the fragment is to be examined
   * @return The discovered ServiceType, or null
   */
  static private ServiceType searchFragment(String fragment) {
    if (fragment.length() == 0)
      return null;
    Map<String, String> map = parseFragment(fragment);
    if (map == null) return null;
    String protocol = map.get("protocol");
    if(protocol == null) {
      for(String p: FRAGPROTOCOLS) {
        if(map.get(p) != null) {protocol = p; break;}
      }
    }
    if (protocol != null) {
      if (protocol.equalsIgnoreCase("dap") || protocol.equalsIgnoreCase("dods"))
        return ServiceType.OPENDAP;
      if (protocol.equalsIgnoreCase("dap4"))
        return ServiceType.DAP4;
      if (protocol.equalsIgnoreCase("cdmremote"))
        return ServiceType.CdmRemote;
      if (protocol.equalsIgnoreCase("thredds"))
        return ServiceType.THREDDS;
      if (protocol.equalsIgnoreCase("ncml"))
        return ServiceType.NCML;
    }
    return null;
  }

  /**
   * Given the fragment part of a url, see if it
   * parses as name=value pairs separated by '&'
   * (same as query part).
   *
   * @param fragment the fragment part of a url
   * @return a map of the name value pairs (possibly empty),
   * or null if the fragment does not parse.
   */
  static private Map<String, String> parseFragment(String fragment) {
    Map<String, String> map = new HashMap<>();
    if (fragment != null && fragment.length() >= 0) {
      if (fragment.charAt(0) == '#')
        fragment = fragment.substring(1);
      String[] pairs = fragment.split("[ \t]*[&][ \t]*");
      for (String pair : pairs) {
        String[] pieces = pair.split("[ \t]*[=][ \t]*");
        switch (pieces.length) {
          case 1:
            map.put(EscapeStrings.unescapeURL(pieces[0]).toLowerCase(), "true");
            break;
          case 2:
            map.put(EscapeStrings.unescapeURL(pieces[0]).toLowerCase(),
                    EscapeStrings.unescapeURL(pieces[1]).toLowerCase());
            break;
          default:
            return null; // does not parse
        }
      }
    }
    return map;
  }

  /**
   * Given a url, search the path to look for protocol indicators
   *
   * @param url the url is to be examined
   * @return The discovered ServiceType, or null
   */
  static private ServiceType searchPath(String url) {
      if(false) { // Disable for now
      if(url == null || url.length() == 0)
        return null;
      url = url.toLowerCase(); // for matching purposes
      for(int i=0; i<FRAGPROTOCOLS.length;i++) {
        String p = FRAGPROTOCOLS[i];
        if(url.indexOf("/thredds/"+p.toLowerCase()+"/")>= 0) {
          return FRAGPROTOSVCTYPE[i];
        }
      }
      }
      return null;
    }

    /**
     * Check path extension; assumes no query or fragment
     *
     * @param path the path to examine for extension
     * @return ServiceType inferred from the extension or null
     */
  static private ServiceType decodePathExtension(String path) {
    // Look at the path extensions
    if (path.endsWith(".dds") || path.endsWith(".das") || path.endsWith(".dods"))
      return ServiceType.OPENDAP;

    if (path.endsWith(".dmr") || path.endsWith(".dap") || path.endsWith(".dsr"))
      return ServiceType.DAP4;

    if (path.endsWith(".xml") || path.endsWith(".ncml"))
      return ServiceType.NCML;
    return null;
  }


  /*
 * Attempt to map a leading url protocol url to a service type (see thredds.catalog.ServiceType).
 * Possible service types should include at least the following.
 * <ol>
 * <li> OPENDAP (DAP2 protocol)
 * <li> DAP4 (DAP4 protocol)
 * <li> CdmRemote (remote ncstream)
 * </ol>
 *
 * @param protocol The leading protocol
 * @return ServiceType indicating how to handle the url, or null.
 */
  @Urlencoded
  static private ServiceType decodeLeadProtocol(String protocol) throws IOException {
    if (protocol.equals("dods"))
      return ServiceType.OPENDAP;

    else if (protocol.equals("dap4"))
      return ServiceType.DAP4;

    else if (protocol.equals("httpserver") || protocol.equals("nodods"))
      return ServiceType.HTTPServer;

    else if (protocol.equals(CdmRemote.PROTOCOL))
      return ServiceType.CdmRemote;

    else if (protocol.equals(DataFactory.PROTOCOL)) //thredds
      return ServiceType.THREDDS;

    return null;
  }

  //////////////////////////////////////////////////////////////////

  /**
   * If the URL alone is not sufficient to disambiguate the location,
   * then this method will attempt to do a specific kind of request on
   * the server, typically a HEAD call using the URL.
   * It finds the header "Content-Description"
   * and uses it value (e.g. "ncstream" or "dods", etc)
   * in order to disambiguate.
   *
   * @param location the url to disambiguate
   * @return ServiceType indicating how to handle the url
   */
  @Urlencoded
  static private ServiceType disambiguateHttp(String location) throws IOException {
    boolean checkDap2 = false, checkDap4 = false, checkCdmr = false;

    // some TDS specific tests
    if (location.contains("cdmremote")) {
      ServiceType result = checkIfCdmr(location);
      if (result != null) return result;
      checkCdmr = true;
    }
    if (location.contains("dodsC")) {
      ServiceType result = checkIfDods(location);
      if (result != null) return result;
      checkDap2 = true;
    }

    if (location.contains("dap4")) {
      ServiceType result = checkIfDap4(location);
      if (result != null) return result;
      checkDap4 = true;
    }

    if (!checkDap2) {
      ServiceType result = checkIfDods(location);
      if (result != null)
        return result;
    }

    if (!checkDap4) {
      ServiceType result = checkIfDap4(location);
      if (result != null)
        return result;
    }

    if (!checkCdmr) {
      ServiceType result = checkIfCdmr(location);
      if (result != null)
        return result;
    }
    return null;
  }

  // cdmremote
  static private ServiceType checkIfCdmr(String location) throws IOException {

    try (HTTPMethod method = HTTPFactory.Head(location + "?req=header")) {
      int statusCode = method.execute();
      if (statusCode >= 300) {
        if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN)
          throw new IOException("Unauthorized to open dataset " + location);
        else
          throw new IOException(location + " is not a valid URL, return status=" + statusCode);
      }

      Header h = method.getResponseHeader("Content-Description");
      if ((h != null) && (h.getValue() != null)) {
        String v = h.getValue();
        if (v.equalsIgnoreCase("ncstream"))
          return ServiceType.CdmRemote;
      }
    }
    return null;
  }

  // not sure what other opendap servers do, so fall back on check for dds
  static private ServiceType checkIfDods(String location) throws IOException {
    int len = location.length();
    // Strip off any trailing .dds, .das, or .dods
    if (location.endsWith(".dds"))
      location = location.substring(0, len - ".dds".length());
    if (location.endsWith(".das"))
      location = location.substring(0, len - ".das".length());
    if (location.endsWith(".dods"))
      location = location.substring(0, len - ".dods".length());

    // Opendap assumes that the caller has properly escaped the url
    try (
            // For some reason, the head method is not using credentials
            // method = session.newMethodHead(location + ".dds");
            HTTPMethod method = HTTPFactory.Get(location + ".dds")) {

      int status = method.execute();
      if (status == 200) {
        Header h = method.getResponseHeader("Content-Description");
        if ((h != null) && (h.getValue() != null)) {
          String v = h.getValue();
          if (v.equalsIgnoreCase("dods-dds") || v.equalsIgnoreCase("dods_dds"))
            return ServiceType.OPENDAP;
          else
            throw new IOException("OPeNDAP Server Error= " + method.getResponseAsString());
        }
      }
      if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN)
        throw new IOException("Unauthorized to open dataset " + location);

      // not dods
      return null;
    }
  }

  // check for dmr
  static private ServiceType checkIfDap4(String location) throws IOException {
    // Strip off any trailing DAP4 prefix
    if (location.endsWith(".dap"))
      location = location.substring(0, location.length() - ".dap".length());
    else if (location.endsWith(".dmr"))
      location = location.substring(0, location.length() - ".dmr".length());
    else if (location.endsWith(".dmr.xml"))
      location = location.substring(0, location.length() - ".dmr.xml".length());
    else if (location.endsWith(".dsr"))
      location = location.substring(0, location.length() - ".dsr".length());
    try (HTTPMethod method = HTTPFactory.Get(location + ".dmr.xml")) {
      int status = method.execute();
      if (status == 200) {
        Header h = method.getResponseHeader("Content-Type");
        if ((h != null) && (h.getValue() != null)) {
          String v = h.getValue();
          if (v.startsWith("application/vnd.opendap.org"))
            return ServiceType.DAP4;
        }
      }
      if (status == HttpStatus.SC_UNAUTHORIZED || status == HttpStatus.SC_FORBIDDEN)
        throw new IOException("Unauthorized to open dataset " + location);

      // not dods
      return null;
    }
  }

  // The first 128 bytes should contain enough info to tell if this looks like an actual ncml file or not.
  // For example, here is an example 128 byte response:
  // <?xml version="1.0" encoding="UTF-8"?>\n<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="dods://ma
  private static int NUM_BYTES_TO_DETERMINE_NCML = 128;

  static private boolean checkIfRemoteNcml(String location) throws IOException {
    if (decodePathExtension(location)==ServiceType.NCML) {
      // just because location ends with ncml does not mean it's ncml
      // if the ncml file is being served up via http by a remote server,
      // we should be able to read the first bit of it and see if it even
      // looks like an ncml file.
      try (HTTPMethod method = HTTPFactory.Get(location)) {
        method.setRange(0, NUM_BYTES_TO_DETERMINE_NCML);
        method.setRequestHeader("accept-encoding", "identity");
        int statusCode = method.execute();
        if (statusCode >= 300) {
          if (statusCode == 401) {
            throw new IOException("Unauthorized to open dataset " + location);
          } else if (statusCode == 406) {
            String msg = location + " - this server does not support returning content without any encoding.";
            msg = msg + " Please download the file locally. Return status=" + statusCode;
            throw new IOException(msg);
          } else {
            throw new IOException(location + " is not a valid URL, return status=" + statusCode);
          }
        }

        return checkIfNcml(method.getResponseAsString());
      }
    }

    return false;
  }

  static private boolean checkIfNcml(File file) throws IOException {
    if (!file.exists()) {
      return false;
    }

    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), NUM_BYTES_TO_DETERMINE_NCML)) {
      byte[] bytes = new byte[NUM_BYTES_TO_DETERMINE_NCML];
      int bytesRead = in.read(bytes);

      if (bytesRead <= 0) {
        return false;
      } else {
        return checkIfNcml(new String(bytes, 0, bytesRead));
      }
    }
  }

  static private boolean checkIfNcml(String string) {
    // Look for the ncml element as well as a reference to the ncml namespace URI.
    return string.contains("<netcdf ") && string.contains("unidata.ucar.edu/namespaces/netcdf/ncml");
  }

  /////////////////////////////////////////////////////////////////////
  public final ServiceType serviceType;
  public final String trueurl;

  public DatasetUrl(ServiceType serviceType, String trueurl) {
    this.serviceType = serviceType;
    this.trueurl = trueurl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatasetUrl that = (DatasetUrl) o;
    return serviceType == that.serviceType && Objects.equals(trueurl, that.trueurl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceType, trueurl);
  }
}
