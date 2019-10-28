package com.xlongwei.light4j.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** 
 * 二维码工具类（zebra crossing）
 * @author xlongwei
 */
public class ZxingUtil {
	private static Logger logger = LoggerFactory.getLogger(ZxingUtil.class);
	private static MultiFormatWriter writer = new MultiFormatWriter();
	private static MultiFormatReader reader = new MultiFormatReader();
	private static ErrorCorrectionLevel[] levels = {ErrorCorrectionLevel.H, ErrorCorrectionLevel.Q, ErrorCorrectionLevel.M, ErrorCorrectionLevel.L};
	
	public static byte[] encode(String content) {
		return encode(content, 256, null);
	}

	/** 生成size大小的二维码，并嵌入logo图标 */
	public static byte[] encode(String content, int size, String logo) {
		return encode(content, size, logo, Color.BLACK, Color.WHITE);
	}
	
	public static byte[] encode(String content, int size, Color forground, Color background) {
		return encode(content, size, null, forground, background);
	}
	
	public static byte[] encode(String content, int size, String logo, Color forground, Color background) {
		byte[] ret = null;
		for(ErrorCorrectionLevel level : levels) {
			byte[] encode = encode(content, size, logo, forground, background, level);
			if(encode!=null && content.equals(decode(encode))) {
				return encode;
			}
			if(encode!=null && ret==null) {
				ret=encode;
			}
		}
		return ret;
	}
	
	public static byte[] encode(String content, int size, String logo, Color forground, Color background, ErrorCorrectionLevel level) {
		try {
			Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(4);  
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");  
			hints.put(EncodeHintType.MARGIN, 0);
			hints.put(EncodeHintType.ERROR_CORRECTION, level);
			BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
			MatrixToImageConfig config = new MatrixToImageConfig(forground!=null ? forground.getRGB() : MatrixToImageConfig.BLACK, background!=null ? background.getRGB() : MatrixToImageConfig.WHITE);
			byte[] bytes = null;
			if(StringUtil.isUrl(logo)) {
				bytes = FileUtil.bytes(logo);
			}else if(ImageUtil.isBase64(logo)) {
				bytes = ImageUtil.decode(logo);
			}
			if(bytes != null) {
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
				int x = (size-img.getWidth()) / 2, y = (size-img.getHeight()) / 2;
				BufferedImage qrcode = MatrixToImageWriter.toBufferedImage(bitMatrix, config);
				BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = (Graphics2D)image.getGraphics();
				g2.drawImage(qrcode, 0, 0, null);
				g2.setBackground(Color.WHITE);
				g2.clearRect(x, y, img.getWidth(), img.getHeight());
				g2.drawImage(img, x, y, null);
				return ImageUtil.bytes(image, "png");
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(bitMatrix, "png", baos, config);
			return baos.toByteArray();
		}catch(Exception e) {
			logger.info("fail to encode qrcode: "+e.getMessage()+", content: "+content);
			return null;
		}
	}
	
	public static String decode(byte[] encode) {
		try {
			BufferedImage image = ImageUtil.image(encode);
			if(image == null) {
				return null;
			}
			LuminanceSource source = new BufferedImageLuminanceSource(image);  
	        Binarizer binarizer = new HybridBinarizer(source);  
	        BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);  
	        Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>(1);  
	        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
	        Result decode = reader.decode(binaryBitmap, hints);
	        return decode.getText();
		}catch(Exception e) {
			logger.info("fail to decode qrcode: "+e.getMessage()+", byte length: "+encode.length);
			return null;
		}
	}
	
	/**
	 * barcode=国家+制造商+商品+校验码
	 * <br>国家：国际物品编码协会，00-09美加，45日本，69中国，471台湾，489香港
	 * <br>制造商：国家物品编码中心；商品：生产企业自定义；校验码，末位
	 * <br>校验规则：977167121601X
	 * <br>1，从右到左编号：1 2 3 ... 12
	 * <br>2，偶数位相加，乘以3：(1+6+2+7+1+7)*3=24*3=72
	 * <br>3，奇数位相加（不含末位校验位），求和：(0+1+1+6+7+9)+72=24+72=96
	 * <br>4，10减去个位数，X=10-6=4
	 * <br>码制区别：
	 * <br>1，UPC，只能表示数字，美加，A版本=1位UCC+5位厂家+5位产品+1位校验，E版本=7位数字
	 * <br>2，Code 3，字母+数字+-$%*，变长，用于工业、图书、票证等
	 * <br>3，Code 128，高密度数据，变长字符串，最多80个字符
	 * <br>4，Codebar，0-9$+-，abcd仅用作起始终止符，变长无校验
	 * @param barcode 字母数字
	 */
	public static byte[] barcode(String barcode) {
		return barcode(barcode, 5, 100);
	}
	
	/**
	 * @param barcode 条码内容
	 * @param margin 左右两边空白宽度
	 * @param height 高度（宽度自适应）
	 */
	public static byte[] barcode(String barcode, int margin, int height) {
		if(StringUtil.isBlank(barcode)) {
			return null;
		}
		try {
			Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(2);  
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");  
			hints.put(EncodeHintType.MARGIN, margin*2);
			BitMatrix bitMatrix = writer.encode(barcode, BarcodeFormat.CODE_128, 0, height, hints);
			MatrixToImageConfig config = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix, config);
			return ImageUtil.bytes(image);
		}catch(Exception e) {
			logger.info("fail to encode barcode: "+barcode+", ex: "+e.getMessage());
		}
		return null;
	}
}
