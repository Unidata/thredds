// $Id: Indexer.java,v 1.2 2004/09/24 03:26:30 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog.search;

import thredds.catalog.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.document.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Creates a lucene index for a list of datasets .
 *
 * @author John Caron
 * @version $Id: Indexer.java,v 1.2 2004/09/24 03:26:30 caron Exp $
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

    DataType dt = ds.getDataType();
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