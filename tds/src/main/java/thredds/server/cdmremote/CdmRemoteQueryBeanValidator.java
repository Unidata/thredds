/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.cdmremote;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * CdmRemoteQueryBean Validator
 *
 * @author caron
 * @since 4/4/2015
 */
public class CdmRemoteQueryBeanValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return CdmRemoteQueryBean.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "req", "req.empty", "must have a req parameter");
    ValidationUtils.rejectIfEmpty(errors, "var", "var.empty", "data request must have a var paramater");

    CdmRemoteQueryBean bean = (CdmRemoteQueryBean) target;
    if (bean.getReq() == null) bean.addError("must have a req parameter");
    if (bean.getVar() == null) bean.addError("data request must have a var parameter");

    CdmRemoteQueryBean.RequestType reqType;
    if (bean.getReq().equalsIgnoreCase("capabilities")) reqType = CdmRemoteQueryBean.RequestType.capabilities;
    else if (bean.getReq().equalsIgnoreCase("cdl")) reqType = CdmRemoteQueryBean.RequestType.cdl;
    else if (bean.getReq().equalsIgnoreCase("form")) reqType = CdmRemoteQueryBean.RequestType.cdl;
    else if (bean.getReq().equalsIgnoreCase("data")) reqType = CdmRemoteQueryBean.RequestType.data;
    else if (bean.getReq().equalsIgnoreCase("header")) reqType = CdmRemoteQueryBean.RequestType.header;
    else if (bean.getReq().equalsIgnoreCase("ncml")) reqType = CdmRemoteQueryBean.RequestType.ncml;
    else reqType = CdmRemoteQueryBean.RequestType.data; // default

    bean.setReqType(reqType);
  }

}
