/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package thredds.ldm;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 14, 2008
 */
public class LuceneIndexer {

  /**
   * Makes a document for a File.
   * <p/>
   * The document has three fields:
   * <ul>
   * <li><code>path</code>--containing the pathname of the file, as a stored,
   * untokenized field;
   * <li><code>modified</code>--containing the last modified date of the file as
   * a field as created by <a
   * href="lucene.document.DateTools.html">DateTools</a>; and
   * <li><code>contents</code>--containing the full contents of the file, as a
   * Reader field;
   */
  public Document makeDocument(File f)  throws java.io.FileNotFoundException {

    // make a new, empty document
    Document doc = new Document();

    // Add the path of the file as a field named "path".  Use a field that is
    // indexed (i.e. searchable), but don't tokenize the field into words.
    doc.add(new Field("path", f.getPath(), Field.Store.YES, Field.Index.UN_TOKENIZED));

    // Add the last modified date of the file a field named "modified".  Use
    // a field that is indexed (i.e. searchable), but don't tokenize the field
    // into words.
    doc.add(new Field("modified",
            DateTools.timeToString(f.lastModified(), DateTools.Resolution.MINUTE),
            Field.Store.YES, Field.Index.UN_TOKENIZED));

    // Add the contents of the file to a field named "contents".  Specify a Reader,
    // so that the text of the file is tokenized and indexed, but not stored.
    // Note that FileReader expects the file to be in the system's default encoding.
    // If that's not the case searching for special characters will fail.
    // doc.add(new Field("contents", new FileReader(f)));

    // return the document
    return doc;
  }


  void indexDocs(IndexWriter writer, File file) throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file);
        try {
          writer.addDocument(makeDocument(file));
        }
        // at least on windows, some temporary files raise this exception with an "access denied" message
        // checking if the file can be read doesn't help
        catch (FileNotFoundException fnfe) {
          ;
        }
      }
    }
  }


  static final File INDEX_DIR = new File("D:/bufr/index/");
  static final File DOC_DIR = new File("D:/bufr/");

  /**
   * Index all text files under a directory.
   */
  public static void main1(String[] args) {

    if (INDEX_DIR.exists()) {
      System.out.println("Cannot save index to '" + INDEX_DIR + "' directory, please delete it first");
      System.exit(1);
    }

    LuceneIndexer indexer = new LuceneIndexer();
    Date start = new Date();
    try {
      IndexWriter writer = new IndexWriter(INDEX_DIR, new StandardAnalyzer(), true);
      System.out.println("Indexing to directory '" + INDEX_DIR + "'...");
      indexer.indexDocs(writer, DOC_DIR);
      System.out.println("Optimizing...");
      writer.optimize();
      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
    }
  }

    public static void main(String[] args) throws IOException {
      IndexReader reader = IndexReader.open(INDEX_DIR);

      System.out.println("Terms= ");
      TermEnum te = reader.terms();
      while (te.next()) {
        Term t = te.term();
        System.out.println(" term= "+t);
      }

      System.out.println("Documents= ");
      for (int docno=0; docno<reader.maxDoc(); docno++) {
        Document d = reader.document(docno);
        System.out.println(" doc= "+d);
      }

      reader.close();      
    }

}
