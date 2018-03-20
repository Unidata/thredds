/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.ft.PointFeature;

/**
 * A factory for making deep copies of PointFeature, so all data is self contained.
 * A factory will use the first PointFeature to get the StructureMembers object, and the DateUnits, and uses that for all copies.
 * So all PointFeatures must have the same StructureMembers and DateUnit.
 *
 * @author caron
 * @since 6/20/2014
 */
public class PointFeatureCopyFactory {
  static private final int POINTER_SIZE = 8; // assume 64 bit pointers could do better with -XX:+UseCompressedOops
  static private final int OBJECT_SIZE = 40; // overhead per object estimate
  static private final int ARRAY_SIZE = 8;   // assume 64 bit pointers

  private final StructureMembers sm;
  private final int sizeInBytes;

  public PointFeatureCopyFactory(PointFeature proto) throws IOException {
    StructureData sdata = proto.getFeatureData();
    sm = new StructureMembers(sdata.getStructureMembers());
    sizeInBytes =  OBJECT_SIZE + POINTER_SIZE +       // PointFeatureCopy - 1 pointer                                             48
            2 * 8 + 2 * POINTER_SIZE +                // PointFeatureImpl - 2 doubles and 2 pointers                              32
            OBJECT_SIZE + 3 * 8 +                     // Earth Location - 3 doubles                                               64
            OBJECT_SIZE +                             // StructureDataDeep
            4 + POINTER_SIZE +                        // StructureDataA  - 1 int and 1 pointer
            OBJECT_SIZE + 4 + 2 * POINTER_SIZE +      // ArrayStructureBB - 1 int and 2 pointers (heap is optional)
            2 * POINTER_SIZE + 4 +                    // ArrayStructure - 2 pointers and an int
            OBJECT_SIZE + 8 * 4 + 8 + POINTER_SIZE +  // ByteBuffer - 8 ints, 1 long, 1 pointer
            sm.getStructureSize();                    // LOOK vlens, Strings  (Heap Size)
  }

  /**
   * approx size of each copy
   * @return approx size of each copy
   */
  public int getSizeInBytes() {
    return sizeInBytes;
  }

  public PointFeature deepCopy(PointFeature from) throws IOException {
    PointFeatureCopy deep = new PointFeatureCopy(from);
    deep.data = StructureDataDeep.copy(from.getFeatureData(), sm);
    return deep;
  }

  private class PointFeatureCopy extends PointFeatureImpl {

    StructureData data;

    PointFeatureCopy(PointFeature pf) {
      super(pf.getFeatureCollection(), pf.getLocation(), pf.getObservationTime(), pf.getNominalTime(),
              pf.getFeatureCollection().getTimeUnit());
    }

    @Nonnull
    @Override
    public StructureData getDataAll() throws IOException {
      return data;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return data;
    }
  }
}

/*
// http://stackoverflow.com/questions/5839434/pointer-size-how-big-is-an-object-reference
A object or array reference occupies one 32 bit word (4 bytes) on a 32 bit JVM or Davlik VM. A null takes the same space as a reference.
(It has to, because a null has to fit in a reference-typed slot; i.e. instance field, local variable, etc.)

On the other hand, an object occupies a minimum of 2 32 bit words (8 bytes), and an array occupies a minimum of 3 32 bit words (12 bytes).
The actual size depends on the number and kinds of fields for an object, and on the number and kind of elements for an array.

For a 64 bit JVM, the size of a reference is 64 bits, unless you have configured the JVM to use compressed pointers:  -XX:+UseCompressedOops

// http://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object?rq=1
There is some fixed overhead per object. It's JVM-specific, but I usually estimate 40 bytes. Then you have to look at the members of the class.
Object references are 4 (8) bytes in a 32-bit (64-bit) JVM. Primitive types are:

boolean and byte: 1 byte
char and short: 2 bytes
int and float: 4 bytes
long and double: 8 bytes
Arrays follow the same rules; that is, it's an object reference so that takes 4 (or 8) bytes in your object, and then its length multiplied by the size of its element.
 */
