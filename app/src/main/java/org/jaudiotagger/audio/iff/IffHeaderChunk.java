package org.jaudiotagger.audio.iff;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/*
 * Common to all IFF formats such as Wav and Aiff
 */
@SuppressWarnings("WeakerAccess")
public class IffHeaderChunk {
    public static final Logger logger = Logger.getLogger("org.jaudiotagger.audio.iff");

    public static final int SIGNATURE_LENGTH = 4;
    public static final int SIZE_LENGTH = 4;
    public static final int TYPE_LENGTH = 4;
    public static final int HEADER_LENGTH = SIGNATURE_LENGTH + SIZE_LENGTH + TYPE_LENGTH;

    /*
     * If Size is not even then we skip a byte, because chunks have to be aligned
     *
     * @param raf
     * @param chunkHeader
     * @throws java.io.IOException
     */
    public static void ensureOnEqualBoundary(final RandomAccessFile raf, ChunkHeader chunkHeader) throws IOException {
        if ((chunkHeader.getSize() & 1) != 0) {
            // Must come out to an even byte boundary unless at end of file
            if (raf.getFilePointer() < raf.length()) {
                logger.config("Skipping Byte because on odd boundary");
                raf.skipBytes(1);
            }
        }
    }
}
