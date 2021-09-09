package com.xlongwei.light4j.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomUtils;

import com.networknt.utility.Tuple;

import lombok.extern.slf4j.Slf4j;

/**
 * 图片工具
 * @author xlongwei
 *
 */
@Slf4j
public class ImageUtil {
	public static final String attr = "imgCode";
	private static char[] normalChars = "ABCDEFGHJKLMNPRSTUWX3456789".toCharArray();
	private static char[] specialChars = "@#$%&3456789ABCDEFGHJKMNPQRSTUWX@#$%&3456789".toCharArray();
//	private static char[] chineseChars = DictUtil.frequent().toCharArray();
//	private static char[] simpleChars = DictUtil.simple().toCharArray();
	private static char[] operatorChars = "+-*".toCharArray();
	
	/** 生成字母数字随机串 */
	public static String random(int length, boolean specials) {
		StringBuilder random = new StringBuilder();
		int size = specials ? specialChars.length : normalChars.length;
		if(length < 1) {
			length = 4;
		}
		while(length-->0) {
			random.append(specials ? specialChars[RandomUtils.nextInt(0, size)] : normalChars[RandomUtils.nextInt(0, size)]);
		}
		return random.toString();
	}
	
	/** 生成特殊类型验证码
	 * @param type -1自动 0中文 1算术 2拆字
	 * @return arr[0]=提示 arr[1]=答案
	 */
	public static String[] special(int type) {
		if(type<0) {
			type = RandomUtils.nextInt(0, 3);
		}
		switch(type) {
//		case 0: 
//			StringBuilder random = new StringBuilder();
//			random.append(chineseChars[RandomUtils.nextInt(0, chineseChars.length)]);
//			random.append(chineseChars[RandomUtils.nextInt(0, chineseChars.length)]);
//			random.append(chineseChars[RandomUtils.nextInt(0, chineseChars.length)]);
//			return new String[]{random.toString()};
		case 1:
			int op1 = RandomUtils.nextInt(0, 10), op2 = RandomUtils.nextInt(0, 10);
			char op = operatorChars[RandomUtils.nextInt(0, operatorChars.length)];
			char minus = '-';
			while(minus==op && op1<op2) {
				op = operatorChars[RandomUtils.nextInt(0, operatorChars.length)];
			}
			int result = '+'==op ? op1+op2 : ('-'==op ? op1 - op2 : op1 * op2), ten = 10;
			if(result>ten && RandomUtils.nextBoolean()) {
				return new String[] {op1+String.valueOf(op)+op2+"="+(result/10)+"?", String.valueOf(result%10)};
			}
			if(RandomUtils.nextBoolean()) {
				return new String[] {"?"+String.valueOf(op)+op2+"="+result, String.valueOf(op1)};
			}
			if(RandomUtils.nextBoolean()) {
				return new String[] {op1+String.valueOf(op)+"?"+"="+result, String.valueOf(op2)};
			}
			return new String[] {op1+String.valueOf(op)+op2+"=?", String.valueOf(result)};
//		case 2:
//			String word = String.valueOf(simpleChars[RandomUtils.nextInt(0, simpleChars.length)]);
//			String[] parts = DictUtil.parts(word);
//			if(RandomUtils.nextBoolean()) {
//				return new String[] {"?+"+parts[1]+"="+word, parts[0]};
//			}
//			if(RandomUtils.nextBoolean()) {
//				return new String[] {parts[0]+"+?="+word, parts[1]};
//			}
//			return new String[] {parts[0]+"+"+parts[1]+"=?", word};
		default: return null;
		}
	}
	
	/** 判断是否base64字符串 */
	public static boolean isBase64(String base64) {
		return !StringUtil.isBlank(base64) && base64.indexOf("base64,") > -1;
	}

	/** 根据format添加base64前缀 */
	public static String prefix(String format) {
		return "data:" + ("WAV".equalsIgnoreCase(format) ? "audio" : "image") + "/"
				+ (StringUtil.isBlank(format) ? "PNG" : format) + ";base64,";
	}

	/** 从base64字符串获取format */
	public static String prefixFormat(String base64) {
		return StringUtil.getPatternString(base64, "data:(?:image|audio)/(\\p{Alnum}+);base64,");
	}

	/** 删除base64前缀 */
	public static String prefixRemove(String base64) {
		int tag = base64.indexOf("base64,");
		return tag > 0 ? base64.substring(tag + 7) : base64;
	}

	/**
	 * 编码图片或音频为base64字符串
	 * @param format PNG JPEG WAV
	 */
	public static String encode(byte[] img, String format) {
		return prefix(format) + Base64.encodeBase64String(img);
	}
	
	
	/** 解码base64图片 */
	public static byte[] decode(String base64) {
		return Base64.decodeBase64(prefixRemove(base64));
	}
	
	/** 解码字节为图片 */
	public static BufferedImage image(byte[] bytes) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			BufferedImage img = ImageIO.read(bais);
			bais.close();
			return img;
		}catch(Exception e) {}
		return null;
	}
	
	/** 获取图片的字节码 */
	public static byte[] bytes(BufferedImage img) {
		return bytes(img, "PNG");
	}
	
	/** 获取图片的字节码 */
	public static byte[] bytes(BufferedImage img, String format) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, format, baos);
			baos.close();
			return baos.toByteArray();
		}catch (Exception e) {}
		return null;
	}
	
	/** 创建随机串的图片 */
	public static BufferedImage create(String imgCode) {
		int length = imgCode.length(), width = 0, height = 20;
		char[] cs = imgCode.toCharArray();
		for(char c:cs) {
			width += StringUtil.isChinese(c) ? 18 : 15;
		}
		boolean hasChinese = width>15*length, isMath = imgCode.indexOf('=')>0;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		for(int i = 0; i < width; i++) {
			g.setColor(color(200, 50));
			g.fillRect(i, 0, 1, height);
		}
		g.setFont(new Font(hasChinese ? "微软雅黑" : "Times New Roman", Font.PLAIN, hasChinese ? 16 : 18));
		double r = 0.27, l = 0; int a = 7, b = a/2;
		for (int i = 0; i < length; i++) {
			String rand = imgCode.substring(i, i+1);
			g.setColor(color(20, 110));
			if(g instanceof Graphics2D) {
				Graphics2D g2d = (Graphics2D)g;
				l = (r*(RandomUtils.nextInt(0, a)-b))/b;
				if(i>0) {
					g2d.translate(StringUtil.isChinese(rand) ? 16 : 13, 0);
				}
				if(!isMath) {
					g2d.rotate(l);
				}
				g2d.drawString(rand, 6, 16);
				if(!isMath) {
					g2d.rotate(-l);
				}
			}else {
				g.drawString(rand, 13 * i + 6, 16);
			}
		}
		if(g instanceof Graphics2D) {
			g.translate(13*(1-length), 0);
		}
		g.dispose();
		return image;
	}
	
	/**
	 * @param type -2 length+specials 0中文 1算术 2拆字 -1随机
	 */
	public static Tuple<String, String> create(int length, boolean specials, int type) {
		String[] special = type<-1 ? null : special(type);
		String code = special!=null&& special.length>0 ? special[0] : null;
		String check = null;
		if(code==null) {
			check = code = random(length, specials);
		}else {
			check = special.length>1 ? special[1] : code;
		}
		return new Tuple<>(code, check);
	}
	
	public static Tuple<String, BufferedImage> create(String code, String check) {
		BufferedImage image = create(code);
		String sid = String.valueOf(IdWorker.getId());
		RedisCache.set(ImageUtil.attr, sid, check);
		log.info("sid:{}, check:{}, code:{}", sid, check, code);
		return new Tuple<>(sid, image);
	}
	
	private static Color color(int f, int s) {
		return new Color(f+RandomUtils.nextInt(0, s), f+RandomUtils.nextInt(0, s), f+RandomUtils.nextInt(0, s));
	}
}
