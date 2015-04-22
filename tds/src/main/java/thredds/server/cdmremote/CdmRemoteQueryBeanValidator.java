/* Copyright */
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
