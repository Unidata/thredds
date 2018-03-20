/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.io.Closeable;
import java.io.IOException;

/**
 * An iterator over StructureData.
  * Make sure that you call close().
  * Best to put in a try/finally block like:
<pre>
try {
  while (iter.hasNext())
   process(iter.next());
 } finally {
   iter.finish();
 }
</pre>
 * @author caron
 * @since Feb 23, 2008
 */
public interface StructureDataIterator extends Closeable {

  /**
   * See if theres more StructureData in the iteration.
   * You must always call this before calling next().
   *
   * @return true if more records are available
   * @throws java.io.IOException on read error
   */
  boolean hasNext() throws IOException;

  /**
   * Get the next StructureData in the iteration.
   *
   * @return next StructureData record.
   * @throws java.io.IOException on read error
   */
  StructureData next() throws IOException;

  /**
   * Hint to use this much memory in buffering the iteration.
   * No guarentee that it will be used by the implementation.
   *
   * @param bytes amount of memory in bytes
   */
  default void setBufferSize(int bytes)  {
      // doan do nuthin
    }

  /**
   * Start the iteration over again.
   *
   * @return a new or reset iterator.
   */
  StructureDataIterator reset();

  int getCurrentRecno();

  /**
   * Make sure that the iterator is complete, and recover resources.
   * Best to put in a try/finally block like:
   * <pre>
   try (StructureDataIterator iter = obj.getStructureDataIterator()) {
     while (iter.hasNext())
      process(iter.next());
    }
   </pre>
   */
  default void close() {
    // doan do nuthin
  }

  ////////////

  /**
   * @deprecated use close() or try-with-resource
   */
  default void finish() {
    close();
  }




}
