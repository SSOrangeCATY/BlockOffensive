package com.xuebi1145.xuplus_client;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GIF帧解码器：从GIF字节数据中提取所有帧和延迟时间
 */
public class GifFrameExtractor {

    public static class GifData {
        public final List<BufferedImage> frames;
        public final List<Integer> delaysMs;

        public GifData(List<BufferedImage> frames, List<Integer> delaysMs) {
            this.frames = frames;
            this.delaysMs = delaysMs;
        }
    }

    /**
     * 从GIF字节数据中提取所有帧
     */
    public static GifData extractFrames(byte[] gifData) throws IOException {
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();

        ByteArrayInputStream bais = new ByteArrayInputStream(gifData);
        ImageInputStream input = ImageIO.createImageInputStream(bais);

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("No GIF reader available");
        }

        ImageReader reader = readers.next();
        reader.setInput(input);

        int numFrames = reader.getNumImages(true);
        if (numFrames <= 0) {
            reader.dispose();
            input.close();
            throw new IOException("GIF has no frames");
        }

        BufferedImage firstImage = reader.read(0);
        int width = firstImage.getWidth();
        int height = firstImage.getHeight();

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvasGraphics = canvas.createGraphics();
        canvasGraphics.setBackground(new Color(0, 0, 0, 0));

        BufferedImage previousCanvas = null;

        for (int i = 0; i < numFrames; i++) {
            BufferedImage frame = reader.read(i);

            IIOMetadata metadata = reader.getImageMetadata(i);
            int delayMs = getDelayMs(metadata);
            int disposalMethod = getDisposalMethod(metadata);
            int frameX = getFrameLeftPosition(metadata);
            int frameY = getFrameTopPosition(metadata);

            // 保存当前画布状态（在绘制前）
            if (disposalMethod == 3) { // restoreToPrevious
                previousCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = previousCanvas.createGraphics();
                g.drawImage(canvas, 0, 0, null);
                g.dispose();
            }

            // disposalMethod 2: restoreToBackground - 清除画布
            if (disposalMethod == 2 && i > 0) {
                Graphics2D g = canvas.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, width, height);
                g.setComposite(AlphaComposite.SrcOver);
                g.dispose();
            }

            // 将当前帧绘制到画布上
            Graphics2D g = canvas.createGraphics();
            g.drawImage(frame, frameX, frameY, null);
            g.dispose();

            // 保存合成后的帧
            BufferedImage composited = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = composited.createGraphics();
            cg.drawImage(canvas, 0, 0, null);
            cg.dispose();
            frames.add(composited);
            delays.add(delayMs > 0 ? delayMs : 100);

            // disposalMethod 3: 恢复到前一帧
            if (disposalMethod == 3 && previousCanvas != null) {
                Graphics2D rg = canvas.createGraphics();
                rg.drawImage(previousCanvas, 0, 0, null);
                rg.dispose();
            }
        }

        canvasGraphics.dispose();
        reader.dispose();
        input.close();

        return new GifData(frames, delays);
    }

    /**
     * 检测字节数据是否为GIF格式
     */
    public static boolean isGif(byte[] data) {
        return data != null && data.length >= 3
                && data[0] == 'G' && data[1] == 'I' && data[2] == 'F';
    }

    private static int getDelayMs(IIOMetadata metadata) {
        for (String name : metadata.getMetadataFormatNames()) {
            Node root = metadata.getAsTree(name);
            Node delayNode = findNode(root, "delayTime");
            if (delayNode != null) {
                String value = delayNode.getNodeValue();
                if (value != null) {
                    try {
                        return Integer.parseInt(value) * 10; // GIF delay in hundredths of second
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 100;
    }

    // 0=none, 1=doNotDispose, 2=restoreToBackground, 3=restoreToPrevious
    private static int getDisposalMethod(IIOMetadata metadata) {
        for (String name : metadata.getMetadataFormatNames()) {
            Node root = metadata.getAsTree(name);
            Node node = findNode(root, "disposalMethod");
            if (node != null) {
                String value = node.getNodeValue();
                if (value != null) {
                    switch (value) {
                        case "restoreToBackground": return 2;
                        case "restoreToPrevious": return 3;
                        case "none":
                        case "doNotDispose":
                        default: return 1;
                    }
                }
            }
        }
        return 0;
    }

    private static int getFrameLeftPosition(IIOMetadata metadata) {
        for (String name : metadata.getMetadataFormatNames()) {
            Node root = metadata.getAsTree(name);
            Node node = findNode(root, "imageLeftPosition");
            if (node != null) {
                try { return Integer.parseInt(node.getNodeValue()); } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static int getFrameTopPosition(IIOMetadata metadata) {
        for (String name : metadata.getMetadataFormatNames()) {
            Node root = metadata.getAsTree(name);
            Node node = findNode(root, "imageTopPosition");
            if (node != null) {
                try { return Integer.parseInt(node.getNodeValue()); } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static Node findNode(Node parent, String nodeName) {
        if (parent == null) return null;
        if (parent.hasAttributes()) {
            Node attr = parent.getAttributes().getNamedItem(nodeName);
            if (attr != null) return attr;
        }
        if (parent.hasChildNodes()) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node found = findNode(children.item(i), nodeName);
                if (found != null) return found;
            }
        }
        return null;
    }
}
