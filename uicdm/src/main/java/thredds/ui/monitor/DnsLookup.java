/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.ui.monitor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * DnsLookup cache
 *
 * @author caron
 * @since 11/20/2015.
 */
public class DnsLookup {
  // just local for the duration of the program - could make persistent
  private Map<String, String> map = new HashMap<>();

  String reverseDNS(String ip) throws UnknownHostException {
    String cacheElem = map.get(ip);
    if (cacheElem == null) {
      InetAddress addr = InetAddress.getByName(ip);
      cacheElem = addr.getHostName();
      map.put(ip, cacheElem);
    }
    return cacheElem;
  }


}
