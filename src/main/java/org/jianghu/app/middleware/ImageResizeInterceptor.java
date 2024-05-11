package org.jianghu.app.middleware;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

@Component
@Slf4j
public class ImageResizeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String size = request.getParameter("size");
        if (size != null) {
            try {
                String[] dimensions = size.split("x");
                int width = Integer.parseInt(dimensions[0]);
                int height = Integer.parseInt(dimensions[1]);

                // 加载原始图像
                String imagePath = request.getRequestURI();
                String dir = System.getProperty("user.dir");
                imagePath = imagePath.split("/upload/")[1];
                String originalImagePath = dir + "/upload/" + imagePath;

                // 获取原始文件的扩展名
                int dotIndex = originalImagePath.lastIndexOf(".");
                String extension = originalImagePath.substring(dotIndex);
                String extensionNotDot = extension.substring(1);

                // 获取不包含扩展名的文件名
                String baseName = originalImagePath.substring(0, dotIndex);

                // 创建新的文件名，包含了尺寸信息
                String resizedImagePath = baseName + "_" + size + extension;

                // 检查缩放后的图像是否已经存在
                File resizedImageFile = new File(resizedImagePath);
                if (resizedImageFile.exists() && resizedImageFile.isFile()) {
                    // 如果缩放后的图像已经存在，将其写入响应
                    BufferedImage resizedImage = ImageIO.read(resizedImageFile);
                    response.setContentType("image/" + extensionNotDot);
                    try (OutputStream out = response.getOutputStream()) {
                        ImageIO.write(resizedImage, extensionNotDot, out);
                    }
                    return false;
                }

                // 如果缩放后的图像不存在，创建它
                File originalImageFile = new File(originalImagePath);
                if (!originalImageFile.exists() || !originalImageFile.isFile()) {
                    throw new FileNotFoundException("Image file not found: " + originalImagePath);
                }
                BufferedImage originalImage = ImageIO.read(originalImageFile);

                // 获取原始图像的宽度和高度
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                // 计算宽度和高度的比例
                double ratio = (double) originalWidth / originalHeight;
                int newWidth;
                int newHeight;
                // 根据比例确定新的宽度和高度
                if (originalWidth > originalHeight) {
                    // 如果原始图像的宽度大于高度，那么新的宽度就设为给定的最大宽度，高度按比例缩放
                    newWidth = width;
                    newHeight = (int) (width / ratio);
                } else {
                    // 如果原始图像的高度大于或等于宽度，那么新的高度就设为给定的最大高度，宽度按比例缩放
                    newHeight = height;
                    newWidth = (int) (height * ratio);
                }

                // 缩放图像
                Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, extensionNotDot.equals("jpg") || extensionNotDot.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.drawImage(tmp, 0, 0, null);
                g2d.dispose();

                System.out.println(resizedImage);
                System.out.println(resizedImageFile);

                // 将缩放后的图像写入新文件
                try {
                    ImageIO.write(resizedImage, extensionNotDot, resizedImageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 将缩放后的图像写入响应
                response.setContentType("image/" + extensionNotDot);
                try (OutputStream out = response.getOutputStream()) {
                    ImageIO.write(resizedImage, extensionNotDot, out);
                }

                // 由于我们已经自己处理了响应，所以返回 false 以阻止进一步处理
                return false;
            } catch (IOException e) {
                // 记录异常并让请求通过以进行进一步处理
                e.printStackTrace();
            }
        }
        // 如果 size 参数不存在或者在处理过程中发生任何异常，我们让请求通过
        return true;
    }
}
