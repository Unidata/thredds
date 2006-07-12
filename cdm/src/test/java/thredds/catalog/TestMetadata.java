package thredds.catalog;

import junit.framework.*;
import java.util.*;

/** Test catalog read JUnit framework. */

public class TestMetadata extends TestCase {
  private static boolean showValidation = false;

  public TestMetadata( String name) {
    super(name);
  }

  String urlString = "catalogDev.xml";
  public void testXLink() {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    StringBuffer buff = new StringBuffer();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    System.out.println(" validation output=\n" + buff);

    String url = getMetadataURL(cat, "Zonal", MetadataType.THREDDS);
    assert url.equals("ZoneMetadata.xml") : url;

    getProject( cat, "Metars", "DIF", "test1");
    getProject( cat, "Radars", "DIF", "test2");
    getProject( cat, "Zonal", "DIF", "NASA Earth Science Project Office, Ames Research Center");

    getKeyword( cat, "Zonal", null, "Ocean Biomass");
    getKeyword( cat, "Zonal", "DIF-Discipline", "Earth Science");
 }

  private String getMetadataURL(InvCatalogImpl cat, String name, MetadataType mtype) {
    InvDataset ds = cat.findDatasetByID(name);
    assert ds != null;
    List list = ds.getMetadata(mtype);
    assert list != null;
    assert list.size() > 0;
    InvMetadata m = (InvMetadata) list.get(0);
    assert m != null;
    System.out.println(name+" = "+m.getXlinkHref());
    assert m.getXlinkHref() != null;
    return m.getXlinkHref().toString();
  }

  private void getProject(InvCatalogImpl cat, String datasetId, String vocab, String text) {
    InvDataset ds = cat.findDatasetByID(datasetId);
    assert ds != null;
    List projects = ds.getProjects();
    assert projects != null;
    assert projects.size() > 0;
    ThreddsMetadata.Vocab p = (ThreddsMetadata.Vocab) projects.get(0);
    assert p != null;
    assert p.getVocabulary().equals(vocab);
    assert p.getText().equals(text);
  }

  private void getKeyword(InvCatalogImpl cat, String datasetId, String vocab, String text) {
    InvDataset ds = cat.findDatasetByID(datasetId);
    assert ds != null;
    List list = ds.getKeywords();
    assert list != null;
    assert list.size() > 0;
    for (int i=0; i<list.size(); i++) {
      ThreddsMetadata.Vocab elem = (ThreddsMetadata.Vocab) list.get(i);
      if (vocab == null) {

       if ((elem.getVocabulary()==null) && elem.getText().equals(text))
        return;
      } else {

        if ((elem.getVocabulary() != null) && elem.getVocabulary().equals(vocab) && elem.getText().equals(text))
          return;
      }
    }
    assert false : "cant find keyword "+text+" vocab "+vocab;
  }

  public void testNamespaces() {
    InvCatalogImpl cat = TestCatalogAll.open("testMetadata.xml", true);

    StringBuffer buff = new StringBuffer();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    System.out.println(" validation output=\n" + buff);

    InvMetadata m;
    /* m = getMetadataByNamespace(cat, "solve", XMLEntityResolver.CATALOG_NAMESPACE_10);
    assert m.isInherited();
    assert !m.hasXlink();
    assert m.isThreddsMetadata();
    assert m.getThreddsMetadata() != null;
    assert m.getContentObject() != null; */

    m = getMetadataByNamespace(cat, "solve", "somethingdifferent");
    assert !m.isInherited();
    assert !m.isThreddsMetadata();
    assert !m.hasXlink();
    assert m.getContentObject() != null;

    m = getMetadataByType(cat, "solve", MetadataType.ADN);
    assert !m.isInherited();
    assert !m.isThreddsMetadata();
    assert m.hasXlink();

    m = getMetadataByType(cat, "solve", MetadataType.DIF);
    assert !m.isInherited();
    assert !m.isThreddsMetadata();
    assert m.hasXlink();
 }

  private InvMetadata getMetadataByType(InvCatalogImpl cat, String name, MetadataType mtype) {
    InvDataset ds = cat.findDatasetByID(name);
    assert ds != null;
    List list = ds.getMetadata(mtype);
    assert list != null;
    assert list.size() > 0;
    InvMetadata m = (InvMetadata) list.get(0);
    assert m != null;
    return m;
  }

  public InvMetadata getMetadataByNamespace( InvCatalogImpl cat, String name, String wantNs) {
    InvDataset ds = cat.findDatasetByID(name);
    assert ds != null;
    List mlist = ds.getMetadata();
    assert mlist != null;
    assert mlist.size() > 0;

    for (int i=0; i<mlist.size(); i++) {
      InvMetadata m = (InvMetadata) mlist.get(i);
      String ns = m.getNamespaceURI();
      // System.out.println(" ns = "+ns);
      if (ns.equals(wantNs))
        return m;
    }
    assert false;
    return null;
  }

}
