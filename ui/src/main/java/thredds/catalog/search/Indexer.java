// $Id: Indexer.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.catalog.search;

import thredds.catalog.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.document.*;

import java.io.*;
import java.util.*;

import ucar.nc2.constants.FeatureType;

/**
 * Creates a lucene index for a list of datasets .
 *
 * @author John Caron
 * @version $Id: Indexer.java 50 2006-07-12 16:30:06Z caron $
 */

public class Indexer {

  private StringBuffer indexMessages;

  public void index( StringBuffer indexMessages, ArrayList datasets) {
    this.indexMessages = indexMessages;

    try {
      IndexWriter writer = new IndexWriter("index", new StandardAnalyzer(), true);

      Iterator iter = datasets.iterator();
      while (iter.hasNext()) {
        InvDataset ds = (InvDataset) iter.next();
        indexMessages.append("Indexing dataset " + ds.getName() + " " +ds.getID() + "\n");
        Document doc = makeDocument(ds);
        if (null != doc)
          writer.addDocument(doc);
      }

      // finish indexer
      writer.optimize();
      writer.close();
    } catch (IOException ioe) {
      indexMessages.append( "Lucene ERROR = "+ioe.getMessage()+"\n");
    }

  }

  private Document makeDocument ( InvDataset ds) {
    Document doc = new Document();
    ArrayList uq = new ArrayList();

    String subsetURL = ds.getSubsetUrl();
    if (subsetURL == null) {
      System.out.println("subsetURL null on "+ds);
      return null;
    }

    doc.add(Field.UnIndexed("subsetURL", ds.getSubsetUrl()));
    doc.add(Field.Text("name", ds.getName()));

    String summary = ds.getDocumentation("summary");
    if (null != summary) {
      doc.add(Field.Text("summary", summary));
      indexMessages.append("  indexed summary\n");
    }

    java.util.List keywords = ds.getKeywords();
    for (int i=0; i<keywords.size(); i++) {
      ThreddsMetadata.Vocab k = (ThreddsMetadata.Vocab) keywords.get(i);
      doc.add(Field.Text("keyword", k.getText()));
      indexMessages.append("  indexed keyword= "+k.getText()+"\n");
    }

    FeatureType dt = ds.getDataType();
    if ((null != dt) && !uq.contains(dt))
      uq.add( dt);

    // find serviceTypes, dataFormats recursively
    findUniques( doc, ds, uq);

    // add the uniques
    for (int i=0; i<uq.size(); i++) {
      Object elem = (Object) uq.get(i);
      String className = elem.getClass().getName();
      int pos = className.lastIndexOf("."); // just want the last part
      className = className.substring(pos+1);
      doc.add( Field.Text(className, elem.toString()));
      indexMessages.append("  indexed "+className+" == "+elem.toString()+"\n");
    }
    return doc;
  }

  private void findUniques( Document doc, InvDataset ds, ArrayList uq) {

    java.util.List access = ds.getAccess();
    for (int i=0; i<access.size(); i++) {
      InvAccess a = (InvAccess) access.get(i);
      DataFormatType dft = ds.getDataFormatType();
      if ( (null != dft) && !uq.contains(dft))
        uq.add(dft);

      ServiceType st = a.getService().getServiceType();
      if ( (null != st) && !uq.contains(st))
        uq.add(st);
    }

    // nested datasets
    java.util.List list = ds.getDatasets();
    for (int i=0; i<list.size(); i++) {
      InvDataset nested = (InvDataset) list.get(i);
      if (nested instanceof InvCatalogRef) // does proxy defeat this?
        continue;

      if (!nested.isHarvest())
        findUniques( doc, nested, uq);
    }

  }



}


/* Change History:
   $Log: Indexer.java,v $
   Revision 1.2  2004/09/24 03:26:30  caron
   merge nj22

   Revision 1.1  2004/06/12 02:01:10  caron
   dqc 0.3


 */