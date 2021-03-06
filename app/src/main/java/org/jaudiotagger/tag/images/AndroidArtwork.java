package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;

import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture;
import org.jaudiotagger.tag.id3.valuepair.ImageFormats;
import org.jaudiotagger.tag.reference.PictureTypes;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static android.graphics.BitmapFactory.decodeByteArray;

/*
 * Represents artwork in a format independent way
 */
public class AndroidArtwork implements Artwork {
    private byte[] binaryData;
    private String mimeType = "";
    private String description = "";
    private boolean isLinked = false;
    private String imageUrl = "";
    private int pictureType = -1;
    private int width;
    private int height;

    public AndroidArtwork() {
    }

    public byte[] getBinaryData() {
        return binaryData;
    }

    public void setBinaryData(byte[] binaryData) {
        this.binaryData = binaryData;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDescription() {
        return description;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * Should be called when you wish to prime the artwork for saving
     *
     * @return
     */
    @SuppressWarnings("JavaDoc")
    public boolean setImageFromData() {
        try {

            Bitmap bitmap = (Bitmap) getImage();

            setWidth(bitmap.getWidth());
            setHeight(bitmap.getHeight());

        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    public Object getImage() throws IOException {

        return getImage(getBinaryData());
    }

    private static Bitmap getImage(byte[] image) {

        try {
            return decodeByteArray(image, 0, image.length);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean isLinked() {
        return isLinked;
    }

    public void setLinked(boolean linked) {
        isLinked = linked;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getPictureType() {
        return pictureType;
    }

    public void setPictureType(int pictureType) {
        this.pictureType = pictureType;
    }

    /*
     * Create Artwork from File
     *
     * @param file
     * @throws java.io.IOException
     */
    @SuppressWarnings("JavaDoc")
    public void setFromFile(File file) throws IOException {
        RandomAccessFile imageFile = new RandomAccessFile(file, "r");
        byte[] imagedata = new byte[(int) imageFile.length()];
        imageFile.read(imagedata);
        imageFile.close();

        setBinaryData(imagedata);
        setMimeType(ImageFormats.getMimeTypeForBinarySignature(imagedata));
        setDescription("");
        setPictureType(PictureTypes.DEFAULT_ID);
    }

    /*
     * Create Artwork from File
     *
     * @param data
     * @throws java.io.IOException
     */
    @SuppressWarnings("JavaDoc")
    @Override
    public void setFromByte(byte[] data) throws IOException {
        setBinaryData(data);
        setMimeType(ImageFormats.getMimeTypeForBinarySignature(data));
        setDescription("");
        setPictureType(PictureTypes.DEFAULT_ID);
    }


    /*
     * Create Artwork from File
     *
     * @param file
     * @return
     * @throws java.io.IOException
     */
    @SuppressWarnings("JavaDoc")
    public static AndroidArtwork createArtworkFromFile(File file) throws IOException {
        AndroidArtwork artwork = new AndroidArtwork();
        artwork.setFromFile(file);
        return artwork;
    }

    public static AndroidArtwork createArtworkFromByte(byte[] value) throws IOException {
        AndroidArtwork artwork = new AndroidArtwork();
        artwork.setFromByte(value);
        return artwork;
    }

    /*
     *
     * @param url
     * @return
     * @throws IOException
     */
    @SuppressWarnings("JavaDoc")
    public static AndroidArtwork createLinkedArtworkFromURL(String url) throws IOException {
        AndroidArtwork artwork = new AndroidArtwork();
        artwork.setLinkedFromURL(url);
        return artwork;
    }

    /*
     * Create Linked Artwork from URL
     *
     * @param url
     * @throws java.io.IOException
     */
    @SuppressWarnings({"WeakerAccess", "RedundantThrows", "JavaDoc"})
    public void setLinkedFromURL(String url) throws IOException {
        setLinked(true);
        setImageUrl(url);
    }


    /*
     * Populate Artwork from MetadataBlockDataPicture as used by Flac and VorbisComment
     *
     * @param coverArt
     */
    @SuppressWarnings("JavaDoc")
    public void setFromMetadataBlockDataPicture(MetadataBlockDataPicture coverArt) {
        setMimeType(coverArt.getMimeType());
        setDescription(coverArt.getDescription());
        setPictureType(coverArt.getPictureType());
        if (coverArt.isImageUrl()) {
            setLinked(coverArt.isImageUrl());
            setImageUrl(coverArt.getImageUrl());
        } else {
            setBinaryData(coverArt.getImageData());
        }
        setWidth(coverArt.getWidth());
        setHeight(coverArt.getHeight());
    }

    /*
     * Create artwork from Flac block
     *
     * @param coverArt
     * @return
     */
    @SuppressWarnings("JavaDoc")
    public static AndroidArtwork createArtworkFromMetadataBlockDataPicture(MetadataBlockDataPicture coverArt) {
        AndroidArtwork artwork = new AndroidArtwork();
        artwork.setFromMetadataBlockDataPicture(coverArt);
        return artwork;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}