@XmlSchema(
        namespace = "http://www.opengis.net/gml/3.2",
        elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "gml",   namespaceURI = "http://www.opengis.net/gml/3.2"),
                @XmlNs(prefix = "xlink", namespaceURI = "http://www.w3.org/1999/xlink"),
                @XmlNs(prefix = "xsi",   namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
        },
        location = "http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd"
)
package net.opengis.gml.v_3_2_1;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;
