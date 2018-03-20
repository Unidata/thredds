/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmrfeature;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Describe
 *
 * @author caron
 * @since 5/5/2015
 */
public class CdmrFeatureQueryBeanValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return CdmrFeatureQueryBean.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    ValidationUtils.rejectIfEmpty(errors, "req", "req.empty", "must have a req parameter");
    ValidationUtils.rejectIfEmpty(errors, "var", "var.empty", "data request must have a var paramater");

    CdmrFeatureQueryBean bean = (CdmrFeatureQueryBean) target;
    if (bean.getReq() == null) bean.addError("must have a req parameter");
    if (bean.getVar() == null) bean.addError("data request must have a var parameter");

    CdmrFeatureQueryBean.RequestType reqType;
    if (bean.getReq().equalsIgnoreCase("data")) reqType = CdmrFeatureQueryBean.RequestType.data;
    else if (bean.getReq().equalsIgnoreCase("header")) reqType = CdmrFeatureQueryBean.RequestType.header;
    else reqType = CdmrFeatureQueryBean.RequestType.data; // default

    bean.setReqType(reqType);
  }

}
