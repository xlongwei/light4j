package com.xlongwei.light4j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.DocumentFamily;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import com.artofsolving.jodconverter.openoffice.converter.StreamOpenOfficeDocumentConverter;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfSmartCopy;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.SimpleBookmark;
import com.networknt.utility.Tuple;
import com.xlongwei.light4j.util.AdtUtil.Get.Round;

import lombok.extern.slf4j.Slf4j;

/**
 * pdf util
 * @author xlongwei
 *
 */
@Slf4j
public class PdfUtil {
	
	/** 合并pdf文件 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean merge(File target, boolean smart, File ... sources) {
		if(target==null || sources==null || sources.length==0) {
			return false;
		}
		try {
			List arraylist = new ArrayList<>();
			int i=0, j=0;
			Document document = null;
			PdfCopy pdfcopy = null;
			for (; j < sources.length; j++) {
				PdfReader pdfreader = new PdfReader(sources[j].getAbsolutePath());
				pdfreader.consolidateNamedDestinations();
				int k = pdfreader.getNumberOfPages();
				List list = SimpleBookmark.getBookmark(pdfreader);
				if (list != null) {
					if (i != 0) {
						SimpleBookmark.shiftPageNumbers(list, i, null);
					}
					arraylist.addAll(list);
				}
				i += k;
				if (j == 0) {
					document = new Document(pdfreader.getPageSizeWithRotation(1));
					pdfcopy = smart ? new PdfSmartCopy(document, new FileOutputStream(target)) : new PdfCopy(document, new FileOutputStream(target));
					document.open();
				}
				for (int l = 0; l < k; l++) {
					pdfcopy.addPage(pdfcopy.getImportedPage(pdfreader, l+1));
				}
	
				pdfcopy.freeReader(pdfreader);
			}
			if (!arraylist.isEmpty()) {
				pdfcopy.setOutlines(arraylist);
			}
			document.close();
		}catch (Exception e) {
			log.info("fail to concat pdf: "+e.getMessage());
		}
		return target.exists();
	}
	
	private static Round<String> roundGet = null;
	private static Semaphore roundLimit = null;
	private static Map<String, Tuple<OpenOfficeConnection, DocumentConverter>> soffices = new ConcurrentHashMap<>();
	static {
		String[] hostAndPorts = StringUtil.firstNotBlank(ConfigUtil.config("soffice").get("hosts"), "127.0.0.1:8100:false").split("[,;]");
		List<String> soffices = new ArrayList<>();
		for(String hostAndPort : hostAndPorts) {
			String[] split = hostAndPort.split("[:]");
			if(split.length != 3) {
				continue;
			}
			String host = split[0], port = split[1], stream = split[2];
			split = port.split("[-]");
			if(split.length==1) {
				soffices.add(hostAndPort);
			} else if(split.length==2){
				int start = Integer.parseInt(split[0]), end = Integer.parseInt(split[1]);
				if(start <= end) {
					for(int p=start; p<=end; p++) {
						soffices.add(host+":"+p+":"+stream);
					}
				}
			}
		}
		roundGet = new Round<>(soffices);
		roundLimit = new Semaphore(soffices.size());
		TaskUtil.addShutdownHook((Runnable)() -> {
				log.info("soffices shutdown");
				PdfUtil.soffices.values().parallelStream().map(tuple -> tuple.first).forEach(OpenOfficeConnection::disconnect);
		});
	}
	/** 转换文件格式，支持OpenOffice和LibreOffice代理转换
	 * @param source 支持doc(x) xls(x) ppt(x) txt(rtf) (x)html
	 * @param target 支持pdf html
	 */
	public static boolean doc2pdf(File source, File target) {
		try {
			roundLimit.acquire();
			String hostAndPort = roundGet.get();
			log.info("convert file source: {}, target: {}, hostAndPort: {}", source.getAbsolutePath(), target.getAbsolutePath(), hostAndPort);
			String[] split = hostAndPort.split("[:]");
			String host = split[0]; int port = NumberUtil.parseInt(split.length>1?split[1]:null, 8100); boolean stream = NumberUtil.parseBoolean(split.length>2?split[2]:null, false);
			Tuple<OpenOfficeConnection, DocumentConverter> tuple = soffices.get(hostAndPort);
			OpenOfficeConnection connection = null;
			DocumentConverter converter = null;
			if(tuple!=null) {
				if(tuple.first.isConnected()) {
					connection = tuple.first;
					converter = tuple.second;
				}else {
					tuple.first.disconnect();
				}
			}
			if(connection==null) {
				connection = new SocketOpenOfficeConnection(host, port);
				connection.connect();
				converter = stream ? new MyStreamOpenOfficeDocumentConverter(connection) : new MyOpenOfficeDocumentConverter(connection);
				tuple = new Tuple<>(connection, converter);
				soffices.put(hostAndPort, tuple);
			}
			long s = System.currentTimeMillis();
			converter.convert(source, target);
			//connection.disconnect();
			log.info("convert file success in ms: {}, hosts: {}, target: {}", (System.currentTimeMillis()-s), hostAndPort, target);
			return true;
		} catch (Exception e) {
			log.info("fail to convert file source: {}, ex: ", source, e.getMessage());
		} finally {
			roundLimit.release();
		}
		return false;
	}
	
	/**
	 * 给pdf文件添加图片印章
	 * @param name 合同专用章
	 * @param company 重庆**有限公司
	 * @param page N 第N页，-N 倒数第N页，0 全部页
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param height 印章大小（默认120）
	 * @return true 成功，false 失败
	 */
	public static boolean seal(File fromPdf, File toPdf, String name, String company, String license, int page, float x, float y, float height) {
		try {
			byte[] data = SealUtil.seal(name, company, license);
			if(height<=0) {
				height = 120;
			}
			Image image = SealUtil.image(data, x, y, height);
			PdfReader reader = new PdfReader(fromPdf.getAbsolutePath());
			PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(toPdf.getAbsoluteFile()));
			int pages = reader.getNumberOfPages();
			if(page!=0)
			 {
				//0表示全部页
				page = calculatePage(pages, page);
			}
			PdfContentByte pdfPage;
			if(page == 0) {
				for(int i = 1; i <= pages; i++) {
					pdfPage = stamper.getUnderContent(i);
					pdfPage.addImage(image);
				}
			}else {
				pdfPage = stamper.getUnderContent(page);
				pdfPage.addImage(image);
			}
			stamper.close();
			log.info("success to seal pdf: "+fromPdf+" to: "+toPdf);			
			return true;
		}catch(Exception e) {
			log.warn("fail to seal pdf: "+fromPdf+" at page: "+page+", ex: "+e.getMessage());
		}
		return false;
	}
	
	/**
	 * 给pdf文件添加图片印章
	 * @param person 张三
	 * @param page N 第N页，-N 倒数第N页，0 全部页
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param height 印章大小（默认32）
	 * @return true 成功，false 失败
	 */
	public static boolean seal(File fromPdf, File toPdf, String person, int page, float x, float y, float height) {
		try {
			byte[] seal = SealUtil.seal(person);
			if(height<=0) {
				height = 32;
			}
			Image image = SealUtil.image(seal, x, y, height);
			PdfReader reader = new PdfReader(fromPdf.getAbsolutePath());
			PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(toPdf.getAbsoluteFile()));
			int pages = reader.getNumberOfPages();
			if(page!=0)
			 {
				//0表示全部页
				page = calculatePage(pages, page);
			}
			PdfContentByte pdfPage;
			if(page == 0) {
				for(int i = 1; i <= pages; i++) {
					pdfPage = stamper.getUnderContent(i);
					pdfPage.addImage(image);
				}
			}else {
				pdfPage = stamper.getUnderContent(page);
				pdfPage.addImage(image);
			}
			stamper.close();
			log.info("success to seal pdf: "+fromPdf+" to: "+toPdf);			
			return true;
		}catch(Exception e) {
			log.warn("fail to seal pdf: "+fromPdf+" at page: "+page+", ex: "+e.getMessage());
		}
		return false;
	}
	
	
	/**
	 * 给pdf添加多个印章
	 * @param fromPdf
	 * @param toPdf
	 * @param seals [{name,company,license,page,x,y,height},{person,page,x,y,height}]
	 * @return true 成功，false 失败
	 */
	public static boolean seal(File fromPdf, File toPdf, String seals) {
		JSONArray jsonArray = JsonUtil.parseArray(seals);
		if(jsonArray==null || jsonArray.size()==0) {
			return false;
		}
		try {
			PdfReader reader = new PdfReader(fromPdf.getAbsolutePath());
			PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(toPdf.getAbsoluteFile()));
			int pages = reader.getNumberOfPages();
			PdfContentByte pdfPage;
			for(int i=0; i<jsonArray.size(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String person = jsonObject.getString("person");
				String name = jsonObject.getString("name");
				String company = jsonObject.getString("company");
				String license = jsonObject.getString("license");
				int page = NumberUtil.parseInt(jsonObject.getString("page"), 1);
				int x = NumberUtil.parseInt(jsonObject.getString("x"), 360);
				int y = NumberUtil.parseInt(jsonObject.getString("y"), 40);
				int height = NumberUtil.parseInt(jsonObject.getString("height"), 0);
				Image image = null;
				if(!StringUtil.isBlank(person)) {
					byte[] seal = SealUtil.seal(person);
					if(height<=0) {
						height = 32;
					}
					image = SealUtil.image(seal, x, y, height);
				}else {
					if(StringUtil.isBlank(name) && StringUtil.isBlank(company) && StringUtil.isBlank(license)) {
						continue;
					}
					byte[] seal = SealUtil.seal(name, company, license);
					if(height<=0) {
						height = 120;
					}
					image = SealUtil.image(seal, x, y, height);
				}
				if(page!=0)
				 {
					//0表示全部页
					page = calculatePage(pages, page);
				}
				if(page == 0) {
					for(int p = 1; p <= pages; p++) {
						pdfPage = stamper.getUnderContent(p);
						pdfPage.addImage(image);
					}
				}else {
					pdfPage = stamper.getUnderContent(page);
					pdfPage.addImage(image);
				}
			}
			stamper.close();
			log.info("success to seal pdf: "+fromPdf+" to: "+toPdf);			
			return true;
		}catch(Exception e) {
			log.warn("fail to seal pdf: "+fromPdf+", ex: "+e.getMessage());
		}		
		return false;
	}
	
	/** 个人签名 */
	public static boolean sign(File fromPdf, File toPdf, PrivateKey key, Certificate[] chain, String person, int page, float x, float y, float height) {
		try {
			PdfReader reader = new PdfReader(fromPdf.getAbsolutePath());
			//sign不允许0
			page = calculatePage(reader.getNumberOfPages(), page);
			PdfStamper stamper = PdfStamper.createSignature(reader, new FileOutputStream(toPdf), '\0');
			PdfSignatureAppearance appearance= stamper.getSignatureAppearance();
			byte[] bs = SealUtil.seal(person);
			if(height<=0) {
				height = 32;
			}
			Image image = SealUtil.image(bs, x, y, height);
			appearance.setVisibleSignature(new Rectangle(x, y, x+image.getWidth(), y+image.getHeight()), page, null);
			appearance.setCrypto(key, chain, null, PdfSignatureAppearance.VERISIGN_SIGNED);
			appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
			appearance.setAcro6Layers(true);
			appearance.setLayer2Text("");
			appearance.setSignatureGraphic(image);
			appearance.setRender(PdfSignatureAppearance.SignatureRenderGraphicAndDescription);
			stamper.close();
			return true;
		}catch(Exception e) {
			log.warn("fail to sign pdf: "+fromPdf+", ex: "+e.getMessage());
		}
		return false;
	}
	
	/** 单位签名 */
	public static boolean sign(File fromPdf, File toPdf, PrivateKey key, Certificate[] chain, String name, String company, String license, int page, float x, float y, float height) {
		try {
			PdfReader reader = new PdfReader(fromPdf.getAbsolutePath());
			page = calculatePage(reader.getNumberOfPages(), page);
			PdfStamper stamper = PdfStamper.createSignature(reader, new FileOutputStream(toPdf), '\0');
			PdfSignatureAppearance appearance= stamper.getSignatureAppearance();
			byte[] seal = SealUtil.seal(name, company, license);
			if(height<=0) {
				height = 120;
			}
			Image image = SealUtil.image(seal, x, y, height);
			appearance.setVisibleSignature(new Rectangle(x, y, x+image.getWidth(), y+image.getHeight()), page, null);
			appearance.setCrypto(key, chain, null, PdfSignatureAppearance.VERISIGN_SIGNED);
			appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
			appearance.setAcro6Layers(true);
			appearance.setLayer2Text("");
			appearance.setSignatureGraphic(image);
			appearance.setRender(PdfSignatureAppearance.SignatureRenderGraphicAndDescription);
			stamper.close();
			return true;
		}catch(Exception e) {
			log.warn("fail to sign pdf: "+fromPdf+", ex: "+e.getMessage());
		}
		return false;
	}
	
	/**
	 * 给pdf添加多个签名
	 * @param fromPdf
	 * @param toPdf
	 * @param signs [{name,company,license,page,x,y,height},{person,page,x,y,height}]
	 * @return true 成功，false 失败
	 */
	public static boolean sign(File fromPdf, File toPdf, PrivateKey key, Certificate[] chain, String signs) {
		JSONArray jsonArray = JsonUtil.parseArray(signs);
		if(jsonArray==null || jsonArray.size()==0) {
			return false;
		}
		try {
			PdfReader reader = new PdfReader(fromPdf.getAbsolutePath());
			ByteArrayOutputStream baos = null;
			int pages = reader.getNumberOfPages();
			for(int i=0; i<jsonArray.size(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String person = jsonObject.getString("person");
				String name = jsonObject.getString("name");
				String company = jsonObject.getString("company");
				String license = jsonObject.getString("license");
				int page = NumberUtil.parseInt(jsonObject.getString("page"), 1);
				int x = NumberUtil.parseInt(jsonObject.getString("x"), 360);
				int y = NumberUtil.parseInt(jsonObject.getString("y"), 40);
				int height = NumberUtil.parseInt(jsonObject.getString("height"), 0);
				Image image = null;
				if(!StringUtil.isBlank(person)) {
					byte[] seal = SealUtil.seal(person);
					if(height<=0) {
						height = 32;
					}
					image = SealUtil.image(seal, x, y, height);
				}else {
					if(StringUtil.isBlank(name) && StringUtil.isBlank(company) && StringUtil.isBlank(license)) {
						continue;
					}
					byte[] seal = SealUtil.seal(name, company, license);
					if(height<=0) {
						height = 120;
					}
					image = SealUtil.image(seal, x, y, height);
				}
				page = calculatePage(pages, page);
				baos = new ByteArrayOutputStream();
				PdfStamper stamper = PdfStamper.createSignature(reader, baos, '\0');
				PdfSignatureAppearance appearance= stamper.getSignatureAppearance();
				appearance.setVisibleSignature(new Rectangle(x, y, x+image.getWidth(), y+image.getHeight()), page, null);
				appearance.setCrypto(key, chain, null, PdfSignatureAppearance.VERISIGN_SIGNED);
				appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
				appearance.setAcro6Layers(true);
				appearance.setLayer2Text("");
				appearance.setSignatureGraphic(image);
				appearance.setRender(PdfSignatureAppearance.SignatureRenderGraphicAndDescription);
				stamper.close();
				reader = new PdfReader(new ByteArrayInputStream(baos.toByteArray()));
			}
			if(baos==null) {
				return false;
			}
			FileUtil.copyStream(new ByteArrayInputStream(baos.toByteArray()), new FileOutputStream(toPdf));
			log.info("success to sign pdf: "+fromPdf+" to: "+toPdf);			
			return true;
		}catch(Exception e) {
			log.warn("fail to sign pdf: "+fromPdf+", ex: "+e.getMessage());
		}		
		return false;
	}
	
	private static class MyOpenOfficeDocumentConverter extends OpenOfficeDocumentConverter {
		public MyOpenOfficeDocumentConverter(OpenOfficeConnection connection) {
			super(connection);
			//参考新版本3.0-beta-4添加docx支持
			DefaultDocumentFormatRegistry formats = (DefaultDocumentFormatRegistry)getDocumentFormatRegistry();
			addFormats(formats);
		}
	}
	private static void addFormats(DefaultDocumentFormatRegistry formats) {
		DocumentFormat docx = new DocumentFormat("Microsoft Word 2007 XML", DocumentFamily.TEXT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
		docx.setExportFilter(DocumentFamily.TEXT, "MS Word 2007");
		formats.addDocumentFormat(docx);
		
		DocumentFormat xlsx = new DocumentFormat("Microsoft Excel 2007 XML", DocumentFamily.SPREADSHEET, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
		xlsx.setExportFilter(DocumentFamily.SPREADSHEET, "MS Excel 2007");
		formats.addDocumentFormat(xlsx);
		
		DocumentFormat pptx = new DocumentFormat("Microsoft PowerPoint 2007 XML", DocumentFamily.PRESENTATION, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
		pptx.setExportFilter(DocumentFamily.PRESENTATION, "MS PowerPoint 2007");
		formats.addDocumentFormat(pptx);
	}
	private static int calculatePage(int pages, int page) {
		//计算一个合法的页码（page=0时返回1，0不是合法页码）
		if(page==0 || page<-pages) {
			return 1;
		} else if(page<0) {
			return pages+page+1;
		} else if(page<=pages) {
			return page;
		} else {
			return pages;
		}
	}
	private static class MyStreamOpenOfficeDocumentConverter extends StreamOpenOfficeDocumentConverter {
		public MyStreamOpenOfficeDocumentConverter(OpenOfficeConnection connection) {
			super(connection);
			DefaultDocumentFormatRegistry formats = (DefaultDocumentFormatRegistry)getDocumentFormatRegistry();
			addFormats(formats);
		}
	}
}
