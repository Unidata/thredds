package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.DocumentMetadataPropertyType;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_DocumentMetadataPropertyType {
    // wml2:Collection/wml2:metadata
    public static DocumentMetadataPropertyType initMetadata(DocumentMetadataPropertyType metadata) {
        // wml:DocumentMetdata
        NC_DocumentMetadataType.initDocumentMetadata(metadata.addNewDocumentMetadata());

        return metadata;
    }

    private NC_DocumentMetadataPropertyType() { }
}
