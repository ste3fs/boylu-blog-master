package com.boylu.utils;

import com.boylu.common.RedisConstants;
import com.boylu.dto.Captcha;
import com.boylu.exception.ServiceException;
import org.apache.commons.lang3.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Slider captcha utility.
 *
 * @author: boylu
 * @date: 2025/3/18
 */
public class CaptchaUtil {

    private static final String IMG_URL = "https://v2.api-m.com/api/wallpaper?return=302";

    private static final String IMG_PATH = "E:/Temp/wallpaper/%s.jpg";

    private static final Integer ALLOW_DEVIATION = 3;

    public static void checkCaptcha(Captcha captcha) {
        if (captcha.getCanvasWidth() == null) {
            captcha.setCanvasWidth(320);
        }
        if (captcha.getCanvasHeight() == null) {
            captcha.setCanvasHeight(155);
        }
        if (captcha.getBlockWidth() == null) {
            captcha.setBlockWidth(65);
        }
        if (captcha.getBlockHeight() == null) {
            captcha.setBlockHeight(55);
        }
        if (captcha.getBlockRadius() == null) {
            captcha.setBlockRadius(9);
        }
        if (captcha.getPlace() == null) {
            captcha.setPlace(0);
        }
    }

    public static int getNonceByRange(int start, int end) {
        Random random = new Random();
        return random.nextInt(end - start + 1) + start;
    }

    public static BufferedImage getBufferedImage(Integer place) {
        int nonce = getNonceByRange(0, 1000);
        BufferedImage image = place != null && place == 0 ? loadRemoteImage() : loadLocalImage(nonce);
        return image != null ? image : createFallbackImage(320, 155);
    }

    private static BufferedImage loadRemoteImage() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(IMG_URL).openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (InputStream inputStream = connection.getInputStream()) {
                return ImageIO.read(inputStream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static BufferedImage loadLocalImage(int nonce) {
        try {
            File file = new File(String.format(IMG_PATH, nonce));
            return file.exists() ? ImageIO.read(file) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage createFallbackImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color start = new Color(getNonceByRange(80, 160), getNonceByRange(140, 220), getNonceByRange(180, 255));
            Color end = new Color(getNonceByRange(20, 120), getNonceByRange(80, 180), getNonceByRange(140, 240));
            g2d.setPaint(new GradientPaint(0, 0, start, width, height, end));
            g2d.fillRect(0, 0, width, height);

            for (int i = 0; i < 8; i++) {
                int alpha = getNonceByRange(40, 90);
                g2d.setColor(new Color(255, 255, 255, alpha));
                int circleWidth = getNonceByRange(30, 110);
                int circleHeight = getNonceByRange(30, 110);
                int x = getNonceByRange(0, Math.max(0, width - circleWidth));
                int y = getNonceByRange(0, Math.max(0, height - circleHeight));
                g2d.fillOval(x, y, circleWidth, circleHeight);
            }

            g2d.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 6; i++) {
                g2d.setColor(new Color(255, 255, 255, getNonceByRange(45, 110)));
                int x1 = getNonceByRange(0, width);
                int y1 = getNonceByRange(0, height);
                int x2 = getNonceByRange(0, width);
                int y2 = getNonceByRange(0, height);
                g2d.draw(new Line2D.Float(x1, y1, x2, y2));
            }
        } finally {
            g2d.dispose();
        }
        return image;
    }

    public static BufferedImage imageResize(BufferedImage bufferedImage, int width, int height) {
        BufferedImage source = bufferedImage != null ? bufferedImage : createFallbackImage(width, height);
        Image image = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resultImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.dispose();
        return resultImage;
    }

    public static void cutByTemplate(BufferedImage canvasImage, BufferedImage blockImage, int blockWidth, int blockHeight, int blockRadius, int blockX, int blockY) {
        BufferedImage waterImage = new BufferedImage(blockWidth, blockHeight, BufferedImage.TYPE_4BYTE_ABGR);
        int[][] blockData = getBlockData(blockWidth, blockHeight, blockRadius);
        for (int i = 0; i < blockWidth; i++) {
            for (int j = 0; j < blockHeight; j++) {
                try {
                    if (blockData[i][j] == 1) {
                        waterImage.setRGB(i, j, Color.BLACK.getRGB());
                        blockImage.setRGB(i, j, canvasImage.getRGB(blockX + i, blockY + j));
                        if (blockData[i + 1][j] == 0 || blockData[i][j + 1] == 0 || blockData[i - 1][j] == 0 || blockData[i][j - 1] == 0) {
                            blockImage.setRGB(i, j, Color.WHITE.getRGB());
                            waterImage.setRGB(i, j, Color.WHITE.getRGB());
                        }
                    } else {
                        blockImage.setRGB(i, j, Color.TRANSLUCENT);
                        waterImage.setRGB(i, j, Color.TRANSLUCENT);
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    // Ignore shape border overflows.
                }
            }
        }
        addBlockWatermark(canvasImage, waterImage, blockX, blockY);
    }

    private static int[][] getBlockData(int blockWidth, int blockHeight, int blockRadius) {
        int[][] data = new int[blockWidth][blockHeight];
        double po = Math.pow(blockRadius, 2);
        int face1 = RandomUtils.nextInt(0, 4);
        int face2;
        do {
            face2 = RandomUtils.nextInt(0, 4);
        } while (face1 == face2);

        int[] circle1 = getCircleCoords(face1, blockWidth, blockHeight, blockRadius);
        int[] circle2 = getCircleCoords(face2, blockWidth, blockHeight, blockRadius);
        int shape = getNonceByRange(0, 1);

        for (int i = 0; i < blockWidth; i++) {
            for (int j = 0; j < blockHeight; j++) {
                data[i][j] = 0;
                if (i >= blockRadius && i <= blockWidth - blockRadius && j >= blockRadius && j <= blockHeight - blockRadius) {
                    data[i][j] = 1;
                }
                double d1 = Math.pow(i - Objects.requireNonNull(circle1)[0], 2) + Math.pow(j - circle1[1], 2);
                double d2 = Math.pow(i - Objects.requireNonNull(circle2)[0], 2) + Math.pow(j - circle2[1], 2);
                if (d1 <= po || d2 <= po) {
                    data[i][j] = shape;
                }
            }
        }
        return data;
    }

    private static int[] getCircleCoords(int face, int blockWidth, int blockHeight, int blockRadius) {
        if (face == 0) {
            return new int[]{blockWidth / 2 - 1, blockRadius};
        } else if (face == 1) {
            return new int[]{blockRadius, blockHeight / 2 - 1};
        } else if (face == 2) {
            return new int[]{blockWidth / 2 - 1, blockHeight - blockRadius - 1};
        } else if (face == 3) {
            return new int[]{blockWidth - blockRadius - 1, blockHeight / 2 - 1};
        }
        return null;
    }

    private static void addBlockWatermark(BufferedImage canvasImage, BufferedImage blockImage, int x, int y) {
        Graphics2D graphics2D = canvasImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.8f));
        graphics2D.drawImage(blockImage, x, y, null);
        graphics2D.dispose();
    }

    public static String toBase64(BufferedImage bufferedImage, String type) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, type, byteArrayOutputStream);
            String base64 = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            return String.format("data:image/%s;base64,%s", type, base64);
        } catch (IOException e) {
            return null;
        }
    }

    public static void checkImageCode(String imageKey, String imageCode) {
        RedisUtil redisUtil = SpringUtil.getBean(RedisUtil.class);
        Object text = redisUtil.get(RedisConstants.SLIDER_CAPTCHA_CODE_KEY + imageKey);
        if (Objects.isNull(text)) {
            throw new ServiceException("Captcha expired");
        }
        if (Math.abs(Integer.parseInt(text.toString()) - Integer.parseInt(imageCode)) > ALLOW_DEVIATION) {
            throw new ServiceException("Captcha verification failed");
        }
    }

    public static void saveImageCode(String key, String code) {
        RedisUtil redisUtil = SpringUtil.getBean(RedisUtil.class);
        redisUtil.set(RedisConstants.SLIDER_CAPTCHA_CODE_KEY + key, code, RedisConstants.MINUTE_EXPIRE, TimeUnit.SECONDS);
    }

    public static void getCaptcha(Captcha captcha) {
        checkCaptcha(captcha);
        int canvasWidth = captcha.getCanvasWidth();
        int canvasHeight = captcha.getCanvasHeight();
        int blockWidth = captcha.getBlockWidth();
        int blockHeight = captcha.getBlockHeight();
        int blockRadius = captcha.getBlockRadius();
        BufferedImage canvasImage = imageResize(getBufferedImage(captcha.getPlace()), canvasWidth, canvasHeight);
        int blockX = getNonceByRange(blockWidth, canvasWidth - blockWidth - 10);
        int blockY = getNonceByRange(10, canvasHeight - blockHeight + 1);
        BufferedImage blockImage = new BufferedImage(blockWidth, blockHeight, BufferedImage.TYPE_4BYTE_ABGR);
        cutByTemplate(canvasImage, blockImage, blockWidth, blockHeight, blockRadius, blockX, blockY);

        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        saveImageCode(nonceStr, String.valueOf(blockX));

        captcha.setNonceStr(nonceStr);
        captcha.setBlockY(blockY);
        captcha.setBlockSrc(toBase64(blockImage, "png"));
        captcha.setCanvasSrc(toBase64(canvasImage, "png"));
    }
}
