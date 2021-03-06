import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.*;

import static java.awt.Color.*;
import static javax.imageio.ImageIO.createImageOutputStream;
import static javax.imageio.ImageIO.getImageWritersByFormatName;
import static javax.imageio.ImageTypeSpecifier.createFromBufferedImageType;


public class Png {
    public static void main(String[] args) throws IOException {
        save(setDpi(color2(of("pngs/g.png")), 600), "pngs/g2.png");
        save(setDpi(color2(of("pngs/g.png"), WHITE.getRGB(), RED.getRGB()), 600), "pngs/g3.png");
        save(setDpi(convert8(color2(of("pngs/g.png"), WHITE.getRGB(), RED.getRGB())), 600), "pngs/g4.png");
    }

    // https://stackoverflow.com/questions/9759651/convert-an-image-to-2-colour-in-java/9759712
    public static BufferedImage color2(final BufferedImage image) {
        return color2(image, WHITE.getRGB(), BLACK.getRGB());
    }

    public static BufferedImage color2(final BufferedImage image, int bg, int fg) {
        int w = image.getWidth();
        int h = image.getHeight();

        // https://github.com/haraldk/TwelveMonkeys/blob/master/imageio/imageio-pict/src/main/java/com/twelvemonkeys/imageio/plugins/pict/BitMapPattern.java#L104
        var cm = new IndexColorModel(1, 2, new int[]{bg, fg}, 0, false, -1, DataBuffer.TYPE_BYTE);

        //  var blackAndWhiteImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        var redWhiteImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY, cm);
        var g = (Graphics2D) redWhiteImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return redWhiteImage;
    }

    /**
     * Converts the source image to 8-bit colour
     * using the default 256-colour palette. No transparency.
     * https://github.com/imcdonagh/image4j/blob/master/src/net/sf/image4j/util/ConvertUtil.java
     *
     * @param src the source image to convert
     * @return a copy of the source image with an 8-bit colour depth
     */
    public static BufferedImage convert8(BufferedImage src) {
        var dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
        var cco = new ColorConvertOp(src.getColorModel().getColorSpace(), dest.getColorModel().getColorSpace(), null);
        cco.filter(src, dest);
        return dest;
    }

    // https://stackoverflow.com/questions/6832434/png-is-it-possible-to-reduce-the-palette-using-java-2d
    public static BufferedImage color8(BufferedImage src) {
        // here goes custom palette
        var cm = new IndexColorModel(
                3, // 3 bits can store up to 8 colors
                6, // here I use only 6
                // RED  GREEN1 GREEN2  BLUE  WHITE BLACK
                new byte[]{-100, 0, 0, 0, -1, 0}, new byte[]{0, -100, 60, 0, -1, 0}, new byte[]{0, 0, 0, -100, -1, 0});

        // draw source image on new one, with custom palette
        var img = new BufferedImage(src.getWidth(), src.getHeight(), // match source
                BufferedImage.TYPE_BYTE_INDEXED, // required to work
                cm); // custom color model (i.e. palette)
        var g2 = img.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        return img;   // 2,5 kb
    }


    private static void savePng(BufferedImage a, String name) throws IOException {
        ImageIO.write(a, "png", new File(name));
    }

    private static void save(byte[] aa, String filename) throws IOException {
        try (var os = new FileOutputStream(filename)) {
            os.write(aa);
        }
    }

    private static BufferedImage of(byte[] imageData) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(imageData));
    }

    public static BufferedImage of(String imagePath) throws IOException {
        return ImageIO.read(new File(imagePath));
    }

    /**
     * 1?????????2.54??????
     */
    private static final double INCH_2_CM = 2.54d;


    /**
     * ???????????????DPI???
     */
    private static void setDPI(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {
        // for PMG, it's dots per millimeter
        double dotsPerMilli = 1.0 * dpi / 10 / INCH_2_CM;
        var horiz = new IIOMetadataNode("HorizontalPixelSize");
        horiz.setAttribute("value", Double.toString(dotsPerMilli));
        var vert = new IIOMetadataNode("VerticalPixelSize");
        vert.setAttribute("value", Double.toString(dotsPerMilli));

        var dim = new IIOMetadataNode("Dimension");
        dim.appendChild(horiz);
        dim.appendChild(vert);

        var root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dim);
        metadata.mergeTree("javax_imageio_1.0", root);
    }


    /**
     * ???????????????????????????DPI???
     * https://convert-dpi.com/
     */
    public static byte[] setDpi(BufferedImage image, int dpi) throws IOException {
        for (var iw = getImageWritersByFormatName("png"); iw.hasNext(); ) {
            var writer = iw.next();
            var param = writer.getDefaultWriteParam();
            var typeSpecifier = createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            var meta = writer.getDefaultImageMetadata(typeSpecifier, param);
            if (meta.isReadOnly() || !meta.isStandardMetadataFormatSupported()) {
                continue;
            }

            setDPI(meta, dpi);

            var output = new ByteArrayOutputStream();
            try (var stream = createImageOutputStream(output);) {
                writer.setOutput(stream);
                writer.write(meta, new IIOImage(image, null, meta), param);
            }
            return output.toByteArray();
        }

        return null;
    }


    // https://groups.google.com/g/comp.lang.java.programmer/c/1XFhLEgILJc
    static byte[] setDpi2(BufferedImage image, int dotsPerInch) throws IOException {
        var dotsPerMeter = String.valueOf((int) (dotsPerInch / 0.0254));

        // retrieve list of ImageWriters for png images (most likely only*/one but who knows)
        var imageWriters = getImageWritersByFormatName("png");

        // loop through available ImageWriters until one succeeds
        if (imageWriters.hasNext()) {
            var iw = imageWriters.next();

            // get default metadata for png files
            var iwp = iw.getDefaultWriteParam();
            var metadata = iw.getDefaultImageMetadata(new ImageTypeSpecifier(image), iwp);

            // get png specific metatdata tree
            String pngFormatName = metadata.getNativeMetadataFormatName();
            var pngNode = (IIOMetadataNode) metadata.getAsTree(pngFormatName);

            // find pHYs node, or create it if it doesn't exist
            IIOMetadataNode physNode;
            var childNodes = pngNode.getElementsByTagName("pHYs");
            int childNodesLength = childNodes.getLength();
            if (childNodesLength == 0) {
                physNode = new IIOMetadataNode("pHYs");
                pngNode.appendChild(physNode);
            } else if (childNodesLength == 1) {
                physNode = (IIOMetadataNode) childNodes.item(0);
            } else {
                throw new IllegalStateException("Don't know what to do with multiple pHYs nodes");
            }

            physNode.setAttribute("pixelsPerUnitXAxis", dotsPerMeter);
            physNode.setAttribute("pixelsPerUnitYAxis", dotsPerMeter);
            physNode.setAttribute("unitSpecifier", "meter");

            var output = new ByteArrayOutputStream();
            try (var ios = createImageOutputStream(output)) {
                metadata.setFromTree(pngFormatName, pngNode);
                iw.setOutput(ios);
                iw.write(new IIOImage(image, null, metadata));
            }
            return output.toByteArray();
        }

        return null;
    }

}

// ???????????????https://www.cnblogs.com/fanblogs/p/11268660.html
// https://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image
// png???????????????????????????Portable Networks Graphics????????????
//
// https://blog.csdn.net/Smy_yu/article/details/37058625
// https://stackoverflow.com/questions/22707130/what-is-difference-between-png8-and-png24
// PNG8 ??????8?????????????????????PNG24 ???24?????????????????????
// ????????????png8?????????????????????????????????256?????????????????????png8?????????????????????????????????????????????????????????????????????logo???????????????????????????????????????????????????????????????????????????
// ????????????png24????????????????????????????????????????????????png8?????????????????????????????????????????????1600???????????????png24???????????????????????????????????????????????????????????????????????????
// ????????????????????????????????????????????????????????????????????????png24???????????????????????????????????????????????????????????????????????????

// https://stackoverflow.com/questions/18824870/handling-weird-png-8-with-imageio

// ??????
// Go??????????????????11??????png-24 ??????png-8 ???8-bit indexed png??? https://blog.csdn.net/u012560213/article/details/95638263
// How do I find out if a PNG is PNG-8 or PNG-24? https://askubuntu.com/questions/943625/how-do-i-find-out-if-a-png-is-png-8-or-png-24
// 3 Ways to Change the DPI of an Image https://www.makeuseof.com/tag/change-image-dpi-designers-need-know/
