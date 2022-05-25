package com.xlongwei.light4j.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import lombok.extern.slf4j.Slf4j;

/**
 * WordUtil helps to fill doc+docx template files
 * <li>doc2fill(doc|docx, file, replaces, tables)
 * <li>read(doc), readx(docx)
 * <li>replace(doc, replaces), replacex(docx, replaces)
 * <li>tables(doc, tables), tablesx(docx, tables|replaces)
 * <li>write(doc, file), writex(docx, file)
 * @author xlongwei
 */
@Slf4j
@SuppressWarnings("unchecked")
public class WordUtil {
	
	private static final char DOT = '.';
	private static final String DOCX = "docx";
	
	public static boolean doc2fill(File template, File target, Map<String, String> replaces, List<List<Map<String, String>>> tables) {
		try {
			HWPFDocument doc = null; XWPFDocument docx = null;
			if(DOCX.equalsIgnoreCase(FileUtil.getFileExt(template.getName()))) {
				docx = readx(template);
				if(docx == null) {
					doc = read(template);
				}
			}else {
				doc = read(template);
				if(doc == null) {
					docx = readx(template);
				}
			}
			if(docx != null) {
				tablesx(docx, tables);
				replacex(docx, replaces);
				tablesx(docx, replaces);
				writex(docx, target);
			}else if (doc != null) {
				replace(doc, replaces);
				tables(doc, tables);
				write(doc, target);
			}
			return docx!=null || doc!=null;
		}catch(Exception e) {
			log.warn("fail to doc2fill doc file: " + template.getAbsolutePath(), e);
		}
		return false;
	}

	public static HWPFDocument read(File doc) {
		try(InputStream is = new FileInputStream(doc)) {
			HWPFDocument document = new HWPFDocument(is);
			log.info("read doc file: " + doc);
			return document;
		} catch (Exception e) {
			log.warn("fail to read doc file: " + doc.getAbsolutePath(), e);
			return null;
		}
	}
	public static XWPFDocument readx(File docx) {
		try(InputStream is = new FileInputStream(docx)) {
			XWPFDocument document = new XWPFDocument(is);
			log.info("read docx file: " + docx);
			return document;
		} catch (Exception e) {
			log.warn("fail to read docx file: " + docx.getAbsolutePath(), e);
			return null;
		}
	}

	public static boolean write(HWPFDocument doc, File docFile) {
		try(OutputStream os = new FileOutputStream(docFile)) {
			doc.write(os);
			log.info("write doc to file: " + docFile);
			return true;
		} catch (IOException e) {
			log.warn("fail to write doc to file: " + docFile, e);
			return false;
		}
	}
	public static boolean writex(XWPFDocument docx, File docxFile) {
		try(OutputStream os = new FileOutputStream(docxFile)) {
			docx.write(os);
			log.info("write docx to file: " + docxFile);
			return true;
		} catch (IOException e) {
			log.warn("fail to write docx to file: " + docxFile, e);
			return false;
		}
	}

	public static boolean replace(HWPFDocument doc, Map<String, String> replaces) {
		if (replaces != null && replaces.size() > 0) {
			Range range = doc.getRange();
			String text = range.text();
			boolean hasVariable = false;
			for (String key : replaces.keySet()) {
				if(text.contains("{"+key+"}")) { hasVariable = true; break; }
			}
			for (String key : replaces.keySet()) {
				String value = replaces.get(key);
				if(hasVariable) {
					key = "{"+key+"}";
				}
				range.replaceText(key, value);
			}
			log.info("replace doc file ok");
			return true;
		}else {
			log.info("nothing to replace cause of empty replaces");
			return false;
		}
	}
	public static boolean replacex(XWPFDocument docx, Map<String, String> replaces) {
		if (replaces != null && replaces.size() > 0) {
			DocxUtil.replaceMap(docx, replaces.entrySet().stream().filter(entry -> !ImageUtil.isBase64(entry.getValue()))
					.collect(Collectors.toMap(entry -> "{" + entry.getKey() + "}", entry -> entry.getValue())));
			List<XWPFParagraph> paragraphs = docx.getParagraphs();
			replace(paragraphs, replaces);
			log.info("replace docx file ok");
			return true;
		}else {
			log.info("nothing to replace cause of empty replaces");
			return false;
		}
	}

	public static boolean table(HWPFDocument doc, int index, List<Map<String, String>> data) {
		Range range = doc.getRange();
		TableIterator tableIterator = new TableIterator(range);
		int tableIndex = 0;
		Table table = null;
		while (tableIterator.hasNext()) {
			tableIndex++;
			table = tableIterator.next();
			if (tableIndex == index) {
				break;
			}
		}
		if (tableIndex==index && table!=null) {
			table(table, data);
			log.info("table doc file ok, index: " + index);
		} else {
			log.warn("fail to find table of index: " + index);
		}
		return true;
	}
	public static boolean tablex(XWPFDocument docx, int index, List<Map<String, String>> data) {
		Iterator<XWPFTable> tableIterator = docx.getTablesIterator();
		int tableIndex = 0;
		XWPFTable table = null;
		while (tableIterator.hasNext()) {
			tableIndex++;
			table = tableIterator.next();
			if (tableIndex == index) {
				break;
			}
		}
		if (tableIndex==index && table!=null) {
			tablex(table, data);
			log.info("table docx file ok, index: " + index);
		} else {
			log.warn("fail to find table of index: " + index);
		}
		return true;
	}

	public static boolean tables(HWPFDocument doc, List<List<Map<String, String>>> tables) {
		if (tables != null && tables.size() > 0) {
			Range range = doc.getRange();
			TableIterator tableIterator = new TableIterator(range);
			Iterator<List<Map<String, String>>> dataIterator = tables.iterator();
			while (tableIterator.hasNext() && dataIterator.hasNext()) {
				Table table = tableIterator.next();
				List<Map<String, String>> data = dataIterator.next();
				table(table, data);
				log.info("table doc file ok");
			}
			if (tableIterator.hasNext()) {
				log.warn("more table left with no data");
			} else if (dataIterator.hasNext()) {
				log.warn("less table than data blocks");
			}
			log.info("tables doc file ok");
			return true;
		} else {
			log.info("nothing to tables cause of empty tables");
			return false;
		}
	}
	public static boolean tablesx(XWPFDocument docx, List<List<Map<String, String>>> tables) {
		if (tables != null && tables.size() > 0) {
			Iterator<XWPFTable> tableIterator = docx.getTablesIterator();
			Iterator<List<Map<String, String>>> dataIterator = tables.iterator();
			while (tableIterator.hasNext() && dataIterator.hasNext()) {
				XWPFTable table = tableIterator.next();
				List<Map<String, String>> data = dataIterator.next();
				tablex(table, data);
				log.info("table docx file ok");
			}
			if (tableIterator.hasNext()) {
				log.warn("more table left with no data");
			} else if (dataIterator.hasNext()) {
				log.warn("less table than data blocks");
			}
			log.info("tables docx file ok");
			return true;
		} else {
			log.info("nothing to tables cause of empty tables");
			return false;
		}
	}
	public static boolean tablesx(XWPFDocument docx, Map<String, String> replaces) {
		Iterator<XWPFTable> tableIterator = docx.getTablesIterator();
		while (tableIterator.hasNext()) {
			XWPFTable table = tableIterator.next();
			tablex(table, replaces);
			log.info("table docx file ok");
		}
		log.info("tables docx file ok");
		return true;
	}

	public static void table(Table table, List<Map<String, String>> data) {
		int rows = table.numRows(); Boolean hasVariable = null;
		for (int r = 0; r < rows - 1; r++) {
			// first row is title
			TableRow tr = table.getRow(r + 1);
			Map<String, String> map = data.size() > r ? data.get(r) : null;
			if (map != null) {
				if(hasVariable == null) {
					String text = tr.text();
					for (String key : map.keySet()) {
						if(text.contains("{"+key+"}")) { hasVariable = true; break; }
					}
					if(hasVariable == null) {
						hasVariable = false;
					}
				}
				for (String key : map.keySet()) {
					String value = map.get(key);
					if(hasVariable) {
						key = "{"+key+"}";
					}
					tr.replaceText(key, value);
				}
			} else {
				tr.delete(); // remove empty rows
				log.debug("table row removed");
			}
		}
		if (data.size() > rows) {
			for (int r = rows; r < data.size(); r++) {
				// how to insert row ?
				log.info("how to insert row: " + r);
			}
		}
	}
	public static void tablex(XWPFTable table, List<Map<String, String>> data) {
		List<XWPFTableRow> rows = table.getRows();
		int tableRows = rows.size()-1, dataRows = data.size(), handleRows = tableRows>=dataRows?tableRows:dataRows;
		List<String> lastRow = new ArrayList<>();
		for(XWPFTableCell cell : rows.get(tableRows).getTableCells()) {
			lastRow.add(cell.getText());
		}
		for (int r = 0; r < handleRows; r++) {
			if(r>=dataRows) {
				table.removeRow(r);
			}else {
				XWPFTableRow row = r>=tableRows ? table.createRow() : rows.get(r+1);
				Map<String, String> map = data.get(r);
				
				List<XWPFTableCell> cells = row.getTableCells();
				for(int c=0,len=cells.size(); c < len; c++) {
					if(r>=tableRows) {
						String var = lastRow.get(c);
						String text = StringUtil.replace(var, map);
						cells.get(c).setText(text);
					}else {
						List<XWPFParagraph> paragraphs = cells.get(c).getParagraphs();
						replace(paragraphs, map);
					}
				}
			}
		}
	}
	public static void tablex(XWPFTable table, Map<String, String> replaces) {
		List<XWPFTableRow> rows = table.getRows();
		for (int r=0,rl=rows.size()-1; r < rl; r++) {
			XWPFTableRow row = rows.get(r+1);
			List<XWPFTableCell> cells = row.getTableCells();
			for(int c=0,cl=cells.size(); c < cl; c++) {
				List<XWPFParagraph> paragraphs = cells.get(c).getParagraphs();
				replace(paragraphs, replaces);
			}
		}
	}
	private static void replace(List<XWPFParagraph> paragraphs, Map<String, String> params) {
		if(paragraphs==null || paragraphs.size()==0) {
			return;
		}
		for(XWPFParagraph paragraph : paragraphs) {
			String text = paragraph.getText();
			if(StringUtil.isBlank(text) || text.indexOf('{')==-1) {
				continue;
			}
			List<XWPFRun> runs = paragraph.getRuns();
			if(runs==null || runs.size()==0) {
				continue;
			}
			String var = ""; boolean append = false; int size = runs.size(), appends = 0;
			for(int i=0; i<size; i++) {
				var = append ? var+runs.get(i).text() : runs.get(i).text();
				if(var.endsWith("{")) { append = true; appends++; continue; }
				text = StringUtil.replace(var, params);
				if(!var.equals(text)) {
					if(ImageUtil.isBase64(text)) {
						try {
							byte[] decode = ImageUtil.decode(text);
							BufferedImage image = ImageUtil.image(decode);
							int pictureType = pictureType(ImageUtil.prefixFormat(text));
							if(pictureType>0) {
								if(append) {
									size -= appends;
									i -= appends;
									while(appends-->=0) {
										paragraph.removeRun(i);
									}
								}else {
									paragraph.removeRun(i);
								}
								XWPFRun run = paragraph.insertNewRun(i);
								run.addPicture(new ByteArrayInputStream(decode), pictureType, var, Units.toEMU(image.getWidth()), Units.toEMU(image.getHeight()));
							}
						}catch(Exception e) {
							log.warn("fail to insert image: "+var+", ex: "+e.getMessage());
						}
					}else {
						String fontFamily = runs.get(i).getFontFamily();
						int fontSize = runs.get(i).getFontSize();
						UnderlinePatterns underline = runs.get(i).getUnderline();
						if(append) {
							size -= appends;
							i -= appends;
							while(appends-->=0) {
								paragraph.removeRun(i);
							}
						}else {
							paragraph.removeRun(i);
						}
						XWPFRun newRun = paragraph.insertNewRun(i);
						newRun.setText(text);
						newRun.setFontFamily(fontFamily);
						newRun.setUnderline(underline);
						if(fontSize>0) {
							newRun.setFontSize(fontSize);
						}
					}
					append = false;
					appends = 0;
				}else {
					appends++;
				}
			}
		}
	}
	public static int pictureType(String format) {
		if(StringUtil.isBlank(format)) {
			return -1;
		}
		format = format.toLowerCase();
		if(DOT == format.charAt(0)) {
			format = format.substring(1);
		}
		switch(format) {
		case "jpg":
		case "jpeg": return XWPFDocument.PICTURE_TYPE_JPEG;
		case "png": return XWPFDocument.PICTURE_TYPE_PNG;
		case "gif": return XWPFDocument.PICTURE_TYPE_GIF;
		default: return -1;
		}
	}

	public static Map<String, String> row(String... cols) {
		return StringUtil.params(cols);
	}
	public static List<Map<String, String>> table(Map<String, String>... rows) {
		List<Map<String, String>> table = new LinkedList<>();
		if(rows!=null && rows.length>0) {
			for (Map<String, String> row : rows) {
				table.add(row);
			}
		}
		return table;
	}
	public static List<List<Map<String, String>>> tables(
			List<Map<String, String>>... tables) {
		List<List<Map<String, String>>> content = new LinkedList<>();
		if(tables!=null && tables.length>0) {
			for (List<Map<String, String>> table : tables) {
				content.add(table);
			}
		}
		return content;
	}
}
