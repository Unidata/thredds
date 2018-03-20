/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmrfeature;

import com.google.common.base.MoreObjects;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 4/4/2015
 */
public class CdmrFeatureQueryBean {
  public enum RequestType {
    data, header
  }

  // raw query parameters
  private String req;
  private String var;

  // data subset
  private String X;
  private String Y;
  private String Z;

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

  public String getX() {
    return X;
  }

  public void setX(String x) {
    X = x;
  }

  public String getY() {
    return Y;
  }

  public void setY(String y) {
    Y = y;
  }

  public String getZ() {
    return Z;
  }

  public void setZ(String z) {
    Z = z;
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
