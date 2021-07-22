import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.util.Iterator;


public class Png {
    public static void main(String[] args) throws IOException {
        var a = readImage("g.png");
        var aa = setDpi(png8(toBinaryImage(a)), 600);
        writeFile(aa, "g2.png");
    }

    private static void savePngFile(BufferedImage a, String name) throws IOException {
        ImageIO.write(a, "png", new File(name));
    }

    public static BufferedImage png8(BufferedImage src) throws IOException {
        // here goes custom palette
        IndexColorModel cm = new IndexColorModel(
                3, // 3 bits can store up to 8 colors
                6, // here I use only 6
                //          RED  GREEN1 GREEN2  BLUE  WHITE BLACK
                new byte[]{-100, 0, 0, 0, -1, 0},
                new byte[]{0, -100, 60, 0, -1, 0},
                new byte[]{0, 0, 0, -100, -1, 0});

        // draw source image on new one, with custom palette
        BufferedImage img = new BufferedImage(
                src.getWidth(), src.getHeight(), // match source
                BufferedImage.TYPE_BYTE_INDEXED, // required to work
                cm); // custom color model (i.e. palette)
        Graphics2D g2 = img.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();

        return img;   // 2,5 kb
    }

    private static void writeFile(byte[] aa, String filename) throws IOException {
        try (var os = new FileOutputStream(filename)) {
            os.write(aa);
        }
    }

    private static BufferedImage createImageFromBytes(byte[] imageData) throws IOException {
        var bais = new ByteArrayInputStream(imageData);
        return ImageIO.read(bais);
    }

    public static BufferedImage readImage(String imagePath) throws IOException {
        return ImageIO.read(new File(imagePath));
    }

    /**
     * 1英寸是2.54像素
     */
    private static final double INCH_2_CM = 2.54d;


    /**
     * 设置图片的DPI值
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

    // https://groups.google.com/g/comp.lang.java.programmer/c/1XFhLEgILJc
    static BufferedImage writePngFile(BufferedImage image, int dotsPerInch) throws IOException {
        var dotsPerMeter = String.valueOf((int) (dotsPerInch / 0.0254));

        // retrieve list of ImageWriters for png images (most likely only*/one but who knows)
        var imageWriters = ImageIO.getImageWritersByFormatName("png");

        // loop through available ImageWriters until one succeeds
        while (imageWriters.hasNext()) {
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
            if (childNodes.getLength() == 0) {
                physNode = new IIOMetadataNode("pHYs");
                pngNode.appendChild(physNode);
            } else if (childNodes.getLength() == 1) {
                physNode = (IIOMetadataNode) childNodes.item(0);
            } else {
                throw new IllegalStateException("Don't know what to do with multiple pHYs nodes");
            }

            physNode.setAttribute("pixelsPerUnitXAxis", dotsPerMeter);
            physNode.setAttribute("pixelsPerUnitYAxis", dotsPerMeter);
            physNode.setAttribute("unitSpecifier", "meter");

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try {
                metadata.setFromTree(pngFormatName, pngNode);
                IIOImage iioImage = new IIOImage(image, null, metadata);
                var ios = ImageIO.createImageOutputStream(output);
                iw.setOutput(ios);
                iw.write(iioImage);
                ios.flush();
                ios.close();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            return ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
        }


        return null;
    }

    // https://stackoverflow.com/questions/9759651/convert-an-image-to-2-colour-in-java/9759712
    public static BufferedImage toBinaryImage(final BufferedImage image) {
        final BufferedImage blackAndWhiteImage = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_BYTE_BINARY);
        final Graphics2D g = (Graphics2D) blackAndWhiteImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return blackAndWhiteImage;
    }


    /**
     * 处理图片，设置图片DPI值
     * https://convert-dpi.com/
     */
    public static byte[] setDpi(BufferedImage image, int dpi) throws IOException {
        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext(); ) {
            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
                continue;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageOutputStream stream = null;
            try {
                setDPI(metadata, dpi);
                stream = ImageIO.createImageOutputStream(output);
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            return output.toByteArray();
        }

        return null;
    }
}

// 代码来源：https://www.cnblogs.com/fanblogs/p/11268660.html
// https://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image
// png是一种图片格式，是Portable Networks Graphics的缩写。
//
// https://blog.csdn.net/Smy_yu/article/details/37058625
// https://stackoverflow.com/questions/22707130/what-is-difference-between-png8-and-png24
// PNG8 是指8位索引色位图，PNG24 是24位索引色位图；
// 每一张“png8”图像，都最多只能展示256种颜色，所以“png8”格式更适合那些颜色比较单一的图像，例如纯色、logo、图标等；因为颜色数量少，所以图片的体积也会更小；
// 每一张“png24”图像，可展示的颜色就远远多于“png8”了，最多可展示的颜色数量多大1600万；所以“png24”所展示的图片颜色会更丰富，图片的清晰度也会更好，
// 图片质量更高，当然图片的大小也会相应增加，所以“png24”的图片比较适合像摄影作品之类颜色比较丰富的图片；

// https://stackoverflow.com/questions/18824870/handling-weird-png-8-with-imageio

// 其它
// Go语言学习笔记11：将png-24 转成png-8 （8-bit indexed png） https://blog.csdn.net/u012560213/article/details/95638263
// How do I find out if a PNG is PNG-8 or PNG-24? https://askubuntu.com/questions/943625/how-do-i-find-out-if-a-png-is-png-8-or-png-24
// 3 Ways to Change the DPI of an Image https://www.makeuseof.com/tag/change-image-dpi-designers-need-know/
