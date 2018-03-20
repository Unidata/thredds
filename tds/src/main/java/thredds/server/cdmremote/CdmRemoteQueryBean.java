/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmremote;

import com.google.common.base.MoreObjects;

import ucar.nc2.stream.NcStreamCompression;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the query parameters for cdmRemote datasets.
 * This is the Model in Spring MVC
 *
 * @author caron
 * @since May 11, 2009
 */
public class CdmRemoteQueryBean {

  public enum RequestType {
    capabilities, cdl, data, header, ncml
  }

  // raw query parameters
  private String req;
  private String var;
  private int deflate = -1;

  // type of compression
  private NcStreamCompression compressType = NcStreamCompression.none(); // default

  // parsed
  private RequestType reqType = null;

  public String getReq() {
    return req;
  }

  public void setReq(String req) {
    this.req = req;
  }

  public String getVar() {
    return var;
  }

  public void setVar(String var) {
    this.var = var;
  }

  public RequestType getReqType() {
    return reqType;
  }

  public void setReqType(RequestType reqType) {
    this.reqType = reqType;
  }

  NcStreamCompression getCompression() {
    return compressType;
  }

  public int getDeflate() {
    return deflate;
  }

  public void setDeflate(int level) {
    compressType = NcStreamCompression.deflate(level);
  }

  private List<String> errs;
  public void addError(String mess) {
    if (errs == null) errs = new ArrayList<>();
    errs.add(mess);
  }
  public boolean hasErrors() {
    return errs != null;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("errs", errs)
            .toString();
  }
}
