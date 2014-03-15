@XmlSchema(
        namespace = "http://www.opengis.net/waterml-dr/2.0",
        elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "wml2dr", namespaceURI = "http://www.opengis.net/waterml-dr/2.0"),
                @XmlNs(prefix = "xlink",  namespaceURI = "http://www.w3.org/1999/xlink"),
                @XmlNs(prefix = "xsi",    namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
        },
        location = "http://www.opengis.net/waterml-dr/2.0 " +
                "http://schemas.opengis.net/waterml/2.0/domain-range-informative/timeseries-domain-range.xsd"
)
package net.opengis.waterml_dr.v_2_0_1;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;
