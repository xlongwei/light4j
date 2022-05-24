package com.xlongwei.light4j.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see https://blog.csdn.net/Z984830966/article/details/122944038?spm=1001.2014.3001.5502
 */
public class DocxUtil {
    private static Logger logger = LoggerFactory.getLogger(DocxUtil.class);

    /**
     * DOCX文档字符串替换
     *
     * @param document
     * @param map
     */
    public static void replaceMap(XWPFDocument document, Map<String, String> map) {
        try {
            // 对段落中的标记进行替换
            List<XWPFParagraph> parasList = document.getParagraphs();
            replaceInAllParagraphs(parasList, map);

            // 对表格中的标记进行替换
            List<XWPFTable> tables = document.getTables();
            replaceInTables(tables, map);
        } catch (Exception e) {
            logger.error("操作WORD文件失败！", e);
            // throw new BusinessException("操作WORD文件失败！");
        }
    }

    /**
     * DOCX文档字符串查找
     *
     * @param document 文档
     * @param str      查找字符串
     */
    public static boolean findText(XWPFDocument document, String str) {
        boolean flag;
        try {
            // 段落中的指定文字
            List<XWPFParagraph> XWPFParagraphList = document.getParagraphs();
            flag = findXWPFParagraphText(XWPFParagraphList, str);
            if (flag) {
                return true;
            }
            // 表格中的指定文字
            List<XWPFTable> XWPFTableList = document.getTables();
            for (XWPFTable xwpfTable : XWPFTableList) {
                List<XWPFTableRow> rows = xwpfTable.getRows();
                for (XWPFTableRow row : rows) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        List<XWPFParagraph> cellParagraphs = cell.getParagraphs();
                        flag = findXWPFParagraphText(cellParagraphs, str);
                        if (flag) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("操作WORD文件失败！", e);
            // throw new BusinessException("操作WORD文件失败！");
        }
        return false;
    }

    /**
     * 查询word段落文字查找
     *
     * @param XWPFParagraphList
     * @param str
     * @return
     */
    public static boolean findXWPFParagraphText(List<XWPFParagraph> XWPFParagraphList, String str) {
        for (XWPFParagraph paragraph : XWPFParagraphList) {
            if (StringUtils.isBlank(paragraph.getText()))
                continue;
            if (paragraph.getText().contains(str)) {
                List<Map<String, Integer>> list = findAllSubRunPosInParagraph(paragraph, str);
                if (CollectionUtils.isNotEmpty(list)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * DOCX文档字符串查找
     *
     * @param document 文档
     */
    public static String getText(Object document) {
        StringBuilder result = new StringBuilder();
        if (null == document) {
            return result.toString();
        }
        if (HWPFDocument.class.getName().equals(document.getClass().getName())) {
            HWPFDocument hwpfDocument = (HWPFDocument) document;
            Range range = hwpfDocument.getRange();
            return range.text();
        }
        if (XWPFDocument.class.getName().equals(document.getClass().getName())) {
            XWPFDocument xwpfDocument = (XWPFDocument) document;
            List<XWPFParagraph> XWPFParagraphList = xwpfDocument.getParagraphs();
            if (CollectionUtils.isNotEmpty(XWPFParagraphList)) {
                for (XWPFParagraph paragraph : XWPFParagraphList) {
                    if (StringUtils.isBlank(paragraph.getText()))
                        continue;
                    result.append(paragraph.getText());
                }
            }
            return result.toString();
        }
        return result.toString();
    }

    /**
     * 替换所有段落中的标记
     *
     * @param XWPFParagraphList
     * @param oldString
     * @param newString
     */
    public static void replaceInAllParagraphs(List<XWPFParagraph> XWPFParagraphList, String oldString,
            String newString) {
        for (XWPFParagraph paragraph : XWPFParagraphList) {
            if (StringUtils.isBlank(paragraph.getText()))
                continue;
            if (paragraph.getText().contains(oldString)) {
                replaceInParagraph(paragraph, oldString, newString);
            }
        }
    }

    /**
     * 替换所有段落中的标记
     *
     * @param XWPFParagraphList
     * @param params
     */
    public static void replaceInAllParagraphs(List<XWPFParagraph> XWPFParagraphList, Map<String, String> params) {
        for (XWPFParagraph paragraph : XWPFParagraphList) {
            if (StringUtils.isBlank(paragraph.getText()))
                continue;
            for (String key : params.keySet()) {
                if (paragraph.getText().contains(key)) {
                    replaceInParagraph(paragraph, key, params.get(key));
                }
            }
        }
    }

    /**
     * 替换段落中的字符串
     *
     * @param xwpfParagraph
     * @param oldString
     * @param newString
     */
    public static void replaceInParagraph(XWPFParagraph xwpfParagraph, String oldString, String newString) {
        List<Map<String, Integer>> list = findAllSubRunPosInParagraph(xwpfParagraph, oldString);
        if (CollectionUtils.isEmpty(list))
            return;
        for (Map<String, Integer> pos_map : list) {
            logger.info("替换字符串：{}，开始下标：{}，结束下标：{}", oldString, pos_map.get("start_pos"), pos_map.get("end_pos"));
            List<XWPFRun> runs = xwpfParagraph.getRuns();
            // 保留开始区域的样式，替换字体，将其余区域移除
            XWPFRun modelRun = runs.get(pos_map.get("start_pos"));
            modelRun.setText(newString, 0);
            for (int i = pos_map.get("end_pos"); i > pos_map.get("start_pos"); i--) {
                logger.info("替换字符串：{}，移除下标：{}", oldString, i);
                xwpfParagraph.removeRun(i);
            }
        }
    }

    /**
     * 找到段落中所有子串的起始XWPFRun下标和终止XWPFRun的下标
     *
     * @param xwpfParagraph
     * @param substring
     * @return
     */
    public static List<Map<String, Integer>> findAllSubRunPosInParagraph(XWPFParagraph xwpfParagraph,
            String substring) {
        List<Map<String, Integer>> posList = new ArrayList<>();
        Map<String, Integer> map;
        int start_pos;
        int end_pos;
        StringBuilder subTemp;
        List<XWPFRun> runs = xwpfParagraph.getRuns();
        for (int i = 0; i < runs.size(); i++) {
            subTemp = new StringBuilder();
            start_pos = i;
            for (int j = i; j < runs.size(); j++) {
                if (StringUtils.isBlank(runs.get(j).getText(runs.get(j).getTextPosition()))) {
                    break;
                }
                subTemp.append(runs.get(j).getText(runs.get(j).getTextPosition()));
                if (subTemp.toString().trim().equals(substring)) {
                    end_pos = j;
                    map = new HashMap<>();
                    map.put("start_pos", start_pos);
                    map.put("end_pos", end_pos);
                    posList.add(map);
                    break;
                }
            }
        }
        return posList;
    }

    /**
     * 替换所有的表格
     *
     * @param XWPFTableList
     * @param params
     */
    public static void replaceInTables(List<XWPFTable> XWPFTableList, Map<String, String> params) {
        for (XWPFTable table : XWPFTableList) {
            replaceInTable(table, params);
        }
    }

    /**
     * 替换一个表格中的所有行
     *
     * @param xwpfTable
     * @param params
     */
    public static void replaceInTable(XWPFTable xwpfTable, Map<String, String> params) {
        List<XWPFTableRow> rows = xwpfTable.getRows();
        replaceInRows(rows, params);
    }

    /**
     * 替换表格中的一行
     *
     * @param rows
     * @param params
     */
    public static void replaceInRows(List<XWPFTableRow> rows, Map<String, String> params) {
        for (XWPFTableRow row : rows) {
            replaceInCells(row.getTableCells(), params);
        }
    }

    /**
     * 替换一行中所有的单元格
     *
     * @param XWPFTableCellList
     * @param params
     */
    public static void replaceInCells(List<XWPFTableCell> XWPFTableCellList, Map<String, String> params) {
        for (XWPFTableCell cell : XWPFTableCellList) {
            replaceInCell(cell, params);
        }
    }

    /**
     * 替换表格中每一行中的每一个单元格中的所有段落
     *
     * @param cell
     * @param params
     */
    public static void replaceInCell(XWPFTableCell cell, Map<String, String> params) {
        List<XWPFParagraph> cellParagraphs = cell.getParagraphs();
        replaceInAllParagraphs(cellParagraphs, params);
    }

}
