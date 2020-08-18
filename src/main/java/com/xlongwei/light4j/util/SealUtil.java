package com.xlongwei.light4j.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;

/** 
 * 图片印章工具 
 * @author xlongwei
 * */
public class SealUtil {
	
	/** 个人印章 */
	public static byte[] seal(String person) throws IOException {
		Person seal = new Person();
		seal.personName = person;
		return seal.export();
	}
	
	/** 单位印章 */
	public static byte[] seal(String name, String company, String license) throws IOException {
		Company seal = new Company();
		seal.name = name;
		seal.firm = company;
		seal.license = license;
		return seal.export();
	}
	
	/** 图片印章 */
	public static Image image(byte[] seal, float x, float y, float height) throws BadElementException, MalformedURLException, IOException {
		Image image = Image.getInstance(seal);
		image.scalePercent((height*100)/image.getHeight());
		image.setAbsolutePosition(x, y);
		return image;
	}
	
	/** 单位印章 */
	public static class Company {
		/** 印章大小和边框 */
		public int width = 230;
		public int height = 230;
		public int borderFix = 5;
		public float borderWidth = 6F;
		public Color borderColor = Color.RED;
		/** 五角星大小、边框、颜色 */
		public float starRadius = 40;
		public float starBorderWidth = 3F;
		public Color starBorderColor = Color.RED;
		public Color starColor = Color.RED;
		/** 印章名称、位置、颜色、字体 */
		public int nameOffset = 50;
		public String name = null;
		public Color nameColor = Color.RED;
		public Font nameFont = new Font("宋体", Font.PLAIN, 18);
		/** 印章单位名称、角度范围、颜色、字体、拉伸比例 */
		public float firmAngle = 120;
		public float firmScale = 1.0F;
		public String firm = null;
		public Color firmColor = Color.RED;
		public Font firmFont = new Font("宋体", Font.PLAIN, 28);
		/** 许可证号、角度范围、颜色、字体 */
		public float licenseAngle = 90;
		public float licenseScale = 1.0F;
		public String license = null;
		public Color licenseColor = Color.RED;
		public Font licenseFont = new Font("宋体", Font.PLAIN, 15);
		/** 导出印章 */
		public byte[] export() throws IOException {
			BufferedImage bi = new BufferedImage(width + borderFix * 2, height + borderFix * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.translate(borderFix, borderFix);
			drawCompany(g2d);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);
			return baos.toByteArray();
		}
		private void drawCompany(Graphics2D g2d) {
			// 把绘制起点挪到圆中心点
			g2d.translate(width / 2, height / 2);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Stroke stroke = g2d.getStroke();
			Polygon polygon = getPentaclePoints(starRadius);
			if (starColor != null) {
				// 填充五角星
				g2d.setColor(starColor);
				g2d.fill(polygon);
			}

			// 绘制五角星边框
			g2d.setStroke(new BasicStroke(starBorderWidth));
			g2d.setColor(starBorderColor);
			g2d.draw(polygon);

			// 绘制印章边框
			g2d.setColor(borderColor);
			g2d.setStroke(new BasicStroke(borderWidth));
			g2d.drawOval(-width / 2, -height / 2, width, height);
			g2d.setStroke(stroke);

			// 绘制印章名称
			drawName(g2d);

			// 绘制印章单位
			drawFirm(g2d);
			
			//绘制许可证号
			drawLicense(g2d);
		}
		private void drawName(Graphics2D g2d) {
			if(name!=null && name.length()>0) {
				g2d.setFont(nameFont);
				g2d.setColor(nameColor);
				FontMetrics fm = g2d.getFontMetrics();
				int w = fm.stringWidth(name);
				int h = fm.getHeight();
				int y = fm.getAscent() - h / 2;
				g2d.drawString(name, -w / 2, y + nameOffset);
			}
		}
		private void drawFirm(Graphics2D g2d) {
			if(firm!=null && firm.length()>0) {
				g2d.setFont(firmFont);
				g2d.setColor(firmColor);
				FontMetrics fm = g2d.getFontMetrics();
				int h = fm.getHeight();
	
				int count = firm.length();
				int r = width / 2;
	
				float angle = (360 - firmAngle) / (count - 1);
				float start = 90 + firmAngle / 2;
				double vr = Math.toRadians(90);
				char[] chars = firm.toCharArray();
				for (int i = 0; i < count; i++) {
					char c = chars[i];
					int cw = fm.charWidth(c);
					float a = start + angle * i;
	
					double radians = Math.toRadians(a);
					g2d.rotate(radians);
					double x = (double)r - h;
					g2d.translate(x, 0.0);
					g2d.rotate(vr);
					g2d.scale(firmScale, 1.0);
					g2d.drawString(String.valueOf(c), -cw / 2, 0);
					// 将所有设置还原,等待绘制下一个
					g2d.scale(1 / firmScale, 1.0);
					g2d.rotate(-vr);
					g2d.translate(-x, 0.0);
					g2d.rotate(-radians);
				}
			}
		}
		private void drawLicense(Graphics2D g2d) {
			if(license!=null && license.length()>0) {
				g2d.setFont(licenseFont);
				g2d.setColor(licenseColor);
				FontMetrics fm = g2d.getFontMetrics();
				int h = fm.getHeight();
				int count = license.length();
				int r = width / 2;
				float angle = licenseAngle / (count - 1);
				float start = 90 + licenseAngle / 2;
				double vr = Math.toRadians(270);
				
				char[] chars = license.toCharArray();
				for (int i = 0; i < count; i++) {
					char c = chars[i];
					int cw = fm.charWidth(c);
					double a = start - angle * i;
					
					double radians = Math.toRadians(a);
					g2d.rotate(radians);
					double x = (double)r - h;
					g2d.translate(x, 0.0);
					g2d.rotate(vr);
					g2d.scale(licenseScale, 1);
					g2d.drawString(String.valueOf(c), -cw / 2, 0);
					// 将所有设置还原,等待绘制下一个
					g2d.scale(1 / licenseScale, 1.0);
					g2d.rotate(-vr);
					g2d.translate(-x, 0.0);
					g2d.rotate(-radians);
				}
			}
		}
		/** 获取具有指定半径外接圆的五角星顶点 */
		private Polygon getPentaclePoints(float radius) {
			float lradius = radius * 0.381966f;
			/**  根据radius求内圆半径 */
			double halfpi = Math.PI / 180f;
			Point[] points = new Point[10];
			for (int i = 0; i < points.length; i++) {
				if (i % 2 == 1) {
					points[i] = new Point(
							(int) (Math.sin(halfpi * 36 * i) * radius),
							(int) (Math.cos(halfpi * 36 * i) * radius));
				} else {
					points[i] = new Point(
							(int) (Math.sin(halfpi * 36 * i) * lradius),
							(int) (Math.cos(halfpi * 36 * i) * lradius));
				}
			}
			Polygon polygon = new Polygon();
			for (Point p : points) {
				polygon.addPoint(p.x, p.y);
			}
			return polygon;
		}
	}
	
	/** 个人印章 */
	public static class Person {
		/** 印章大小和边框 */
		public int personHeight = 54;
		public int borderFix = 5;
		public int personBorderH = 3;
		public int personBorderV = 1;
		public Color borderColor = Color.RED;
		/** 个人印章 */
		public String personName = null;
		public Color personColor = Color.RED;
		public Font personFont = new Font("宋体", Font.BOLD, 36);
		/** 导出印章 */
		public byte[] export() throws IOException {
			int personWidth = (personName!=null?Math.max(2, personName.length()):2) * 36 + 24;
			BufferedImage bi = new BufferedImage(personWidth + borderFix * 2, personHeight + borderFix * 2, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.translate(borderFix, borderFix);
			drawPerson(g2d, personWidth);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);
			return baos.toByteArray();
		}		
		private void drawPerson(Graphics2D g2d, int personWidth) {
			g2d.translate(personWidth / 2, personHeight / 2);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Stroke stroke = g2d.getStroke();
			g2d.setColor(borderColor);
			g2d.setStroke(new BasicStroke(personBorderH));
			int x = -personWidth / 2, y = -personHeight / 2;
			g2d.drawLine(x, y, -x, y);
			g2d.drawLine(x, -y, -x, -y);
			g2d.setStroke(new BasicStroke(personBorderV));
			x -= personBorderV;
			g2d.drawLine(x, y, x, -y);
			g2d.drawLine(-x, y, -x, -y);
			g2d.setStroke(stroke);
			if(personName!=null && personName.length()>0) {
				g2d.setFont(personFont);
				g2d.setColor(personColor);
				FontMetrics fm = g2d.getFontMetrics();
				int w = fm.stringWidth(personName);
				int h = fm.getHeight();
				g2d.drawString(personName, -w / 2, fm.getAscent() - h / 2);
			}
		}
	}
}
