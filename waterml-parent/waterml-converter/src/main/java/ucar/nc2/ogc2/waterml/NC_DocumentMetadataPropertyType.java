package ucar.nc2.ogc2.waterml;

import net.opengis.waterml.x20.DocumentMetadataPropertyType;
import net.opengis.waterml.x20.DocumentMetadataType;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_DocumentMetadataPropertyType {
    // wml2:Collection/wml2:metadata
    public static DocumentMetadataPropertyType createMetadata() {
        DocumentMetadataPropertyType metadata = DocumentMetadataPropertyType.Factory.newInstance();

        // wml:DocumentMetdata
        DocumentMetadataType documentMetadata = NC_DocumentMetadataType.createDocumentMetadata();
        metadata.setDocumentMetadata(documentMetadata);

        return metadata;
    }

    private NC_DocumentMetadataPropertyType() { }
}
