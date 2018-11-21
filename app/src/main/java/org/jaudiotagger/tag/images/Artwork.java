package org.jaudiotagger.tag.images;

import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture;

import java.io.File;
import java.io.IOException;

/*
 * Represents artwork in a format independent  way
 */
@SuppressWarnings("unused")
public interface Artwork {

    byte[] getBinaryData();

    void setBinaryData(byte[] binaryData);

    String getMimeType();

    void setMimeType(String mimeType);

    String getDescription();

    int getHeight();

    int getWidth();

    @SuppressWarnings("unused")
    void setDescription(String description);

    /*
     * Should be called when you wish to prime the artwork for saving
     *
     * @return
     */
    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "JavaDoc"})
    boolean setImageFromData();

    @SuppressWarnings({"RedundantThrows", "unused"})
    Object getImage() throws IOException;

    boolean isLinked();

    void setLinked(boolean linked);

    String getImageUrl();

    void setImageUrl(String imageUrl);

    int getPictureType();

    void setPictureType(int pictureType);

    /*
     * Create Artwork from File
     *
     * @param file
     * @throws IOException
     */
    @SuppressWarnings({"unused", "JavaDoc"})
    void setFromFile(File file) throws IOException;

    /*
     * Create Artwork from byte[]
     *
     * @param data
     * @throws IOException
     */
    @SuppressWarnings({"RedundantThrows", "unused", "JavaDoc"})
    void setFromByte(byte[] data) throws IOException;


    /*
     * Populate Artwork from MetadataBlockDataPicture as used by Flac and VorbisComment
     *
     * @param coverArt
     */
    @SuppressWarnings({"unused", "JavaDoc"})
    void setFromMetadataBlockDataPicture(MetadataBlockDataPicture coverArt);


    @SuppressWarnings("unused")
    void setWidth(int width);

    @SuppressWarnings("unused")
    void setHeight(int height);
}
