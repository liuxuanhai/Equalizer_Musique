package org.jaudiotagger.tag.images;

import android.graphics.Bitmap;

import java.io.IOException;

/*
 Image Handling to to use when running on Android

 TODO need to provide Android compatible implementations
 */
public class AndroidImageHandler implements ImageHandler {
    private static AndroidImageHandler instance;

    public static AndroidImageHandler getInstanceOf() {
        if (instance == null) {
            instance = new AndroidImageHandler();
        }
        return instance;
    }

    private AndroidImageHandler() {

    }

    /*
     * Resize the image until the total size require to store the image is less than maxsize
     * @param artwork
     * @param maxSize
     * @throws IOException
     */
    public void reduceQuality(Artwork artwork, int maxSize) throws IOException {
/*
        while(artwork.getBinaryData().length > maxSize)
        {
            Image srcImage = (Image)artwork.getImage();
            int w = srcImage.getWidth(null);
            int newSize = w /2;
            makeSmaller(artwork,newSize);
        }
        */

        throw new UnsupportedOperationException();
    }

    /*
    * Resize image using Java 2D
     * @param artwork
     * @param size
     * @throws java.io.IOException
     */
    public void makeSmaller(Artwork artwork, int size) throws IOException {

/*        Image srcImage = (Image)artwork.getImage();

        int w = srcImage.getWidth(null);
        int h = srcImage.getHeight(null);

        // Determine the scaling required to get desired result.
        float scaleW = (float) size / (float) w;
        float scaleH = (float) size / (float) h;

        //Create an image buffer in which to paint on, create as an opaque Rgb type image, it doesnt matter what type
        //the original image is we want to convert to the best type for displaying on screen regardless
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        // Set the scale.
        AffineTransform tx = new AffineTransform();
        tx.scale(scaleW, scaleH);

        // Paint image.
        Graphics2D g2d = bi.createGraphics();
        g2d.drawImage(srcImage, tx, null);
        g2d.dispose();


        if(artwork.getMimeType()!=null && isMimeTypeWritable(artwork.getMimeType()))
        {
            artwork.setBinaryData(writeImage(bi,artwork.getMimeType()));
        }
        else
        {
            artwork.setBinaryData(writeImageAsPng(bi));
        }*/

        throw new UnsupportedOperationException();
    }

    public boolean isMimeTypeWritable(String mimeType) {

/*        Iterator<ImageWriter> writers =  ImageIO.getImageWritersByMIMEType(mimeType);
        return writers.hasNext();*/


        throw new UnsupportedOperationException();
    }

    /*
     *  Write buffered image as required format
     *
     * @param bi
     * @param mimeType
     * @return
     * @throws IOException
     */
    public byte[] writeImage(Bitmap bi, String mimeType) throws IOException {

/*        Iterator<ImageWriter> writers =  ImageIO.getImageWritersByMIMEType(mimeType);
        if(writers.hasNext())
        {
            ImageWriter writer = writers.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer.setOutput(ImageIO.createImageOutputStream(baos));
            writer.write(bi);
            return baos.toByteArray();
        }
        throw new IOException("Cannot write to this mimetype");*/


        throw new UnsupportedOperationException();
    }

    /*
     *
     * @param bi
     * @return
     * @throws IOException
     */
    public byte[] writeImageAsPng(Bitmap bi) throws IOException {

/*        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, ImageFormats.MIME_TYPE_PNG,baos);
        return baos.toByteArray();*/


        throw new UnsupportedOperationException();
    }

    /*
     * Show read formats
     *
     * On Windows supports png/jpeg/bmp/gif
     */
    public void showReadFormats() {

/*        String[] formats = ImageIO.getReaderMIMETypes();
        for(String f:formats)
        {
            System.out.println("r"+f);
        }*/


        throw new UnsupportedOperationException();
    }

    /*
     * Show write formats
     *
     * On Windows supports png/jpeg/bmp
     */
    public void showWriteFormats() {

/*        String[] formats = ImageIO.getWriterMIMETypes();
        for(String f:formats)
        {
            System.out.println(f);
        }*/

        throw new UnsupportedOperationException();
    }
}
