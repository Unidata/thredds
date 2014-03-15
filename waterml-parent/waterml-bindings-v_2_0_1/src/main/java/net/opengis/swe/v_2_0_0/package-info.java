@XmlSchema(
        namespace = "http://www.opengis.net/swe/2.0",
        elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "swe",   namespaceURI = "http://www.opengis.net/swe/2.0"),
                @XmlNs(prefix = "xlink", namespaceURI = "http://www.w3.org/1999/xlink"),
                @XmlNs(prefix = "xsi",   namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
        },
        location = "http://www.opengis.net/swe/2.0 http://schemas.opengis.net/sweCommon/2.0/swe.xsd"
)
package net.opengis.swe.v_2_0_0;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;
