package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.DocumentMetadataPropertyType;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NcDocumentMetadataPropertyType {
    // wml2:Collection/wml2:metadata
    public static DocumentMetadataPropertyType initMetadata(DocumentMetadataPropertyType metadata) {
        // wml2:DocumentMetadata
        NcDocumentMetadataType.initDocumentMetadata(metadata.addNewDocumentMetadata());

        return metadata;
    }

    private NcDocumentMetadataPropertyType() { }
}
