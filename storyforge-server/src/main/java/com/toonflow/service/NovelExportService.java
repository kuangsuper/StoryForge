package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.entity.Novel;
import com.toonflow.mapper.NovelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelExportService {

    private final NovelMapper novelMapper;

    /**
     * 导出为 TXT 格式
     */
    public String exportTxt(Long projectId) {
        List<Novel> novels = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .orderByAsc(Novel::getChapterIndex));

        StringBuilder sb = new StringBuilder();
        String currentReel = null;
        for (Novel novel : novels) {
            if (novel.getReel() != null && !novel.getReel().equals(currentReel)) {
                currentReel = novel.getReel();
                sb.append("\n\n========== ").append(currentReel).append(" ==========\n\n");
            }
            sb.append("第").append(novel.getChapterIndex()).append("章 ");
            if (novel.getChapter() != null) sb.append(novel.getChapter());
            sb.append("\n\n");
            if (novel.getChapterData() != null) sb.append(novel.getChapterData());
            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 导出为 DOCX 格式（Apache POI）
     */
    public byte[] exportDocx(Long projectId) {
        List<Novel> novels = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .orderByAsc(Novel::getChapterIndex));

        try (XWPFDocument doc = new XWPFDocument()) {
            // 先注册 Heading1 / Heading2 样式，避免空文档无内置样式
            ensureHeadingStyles(doc);

            String currentReel = null;

            for (Novel novel : novels) {
                // 卷名 → Heading 1
                if (novel.getReel() != null && !novel.getReel().equals(currentReel)) {
                    currentReel = novel.getReel();
                    XWPFParagraph heading1 = doc.createParagraph();
                    heading1.setStyle("Heading1");
                    XWPFRun run1 = heading1.createRun();
                    run1.setText(currentReel);
                    run1.setBold(true);
                    run1.setFontSize(18);
                }

                // 章节名 → Heading 2
                String chapterTitle = "第" + novel.getChapterIndex() + "章"
                        + (novel.getChapter() != null ? " " + novel.getChapter() : "");
                XWPFParagraph heading2 = doc.createParagraph();
                heading2.setStyle("Heading2");
                XWPFRun run2 = heading2.createRun();
                run2.setText(chapterTitle);
                run2.setBold(true);
                run2.setFontSize(14);

                // 正文段落
                if (novel.getChapterData() != null) {
                    String[] paragraphs = novel.getChapterData().split("\n\n");
                    for (String para : paragraphs) {
                        if (para.isBlank()) continue;
                        XWPFParagraph p = doc.createParagraph();
                        p.setIndentationFirstLine(720); // 首行缩进 2 字符
                        XWPFRun run = p.createRun();
                        run.setText(para.trim());
                        run.setFontSize(12);
                    }
                }

                // 章节间分页符
                XWPFParagraph pageBreak = doc.createParagraph();
                pageBreak.setPageBreak(true);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("DOCX export failed for projectId={}", projectId, e);
            // 降级为 TXT 字节数组
            return exportTxt(projectId).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * 在空 XWPFDocument 中注册 Heading1 和 Heading2 样式。
     * 空文档默认没有内置样式定义，直接 setStyle("Heading1") 会静默失败。
     */
    private void ensureHeadingStyles(XWPFDocument doc) {
        XWPFStyles styles = doc.createStyles();

        // Heading 1: 18pt, bold
        addHeadingStyle(styles, "Heading1", 1, 36, true);
        // Heading 2: 14pt, bold
        addHeadingStyle(styles, "Heading2", 2, 28, true);
    }

    private void addHeadingStyle(XWPFStyles styles, String styleId, int outlineLevel,
                                  int halfPointSize, boolean bold) {
        CTStyle ctStyle = CTStyle.Factory.newInstance();
        ctStyle.setStyleId(styleId);
        ctStyle.setType(STStyleType.PARAGRAPH);

        CTString name = CTString.Factory.newInstance();
        name.setVal(styleId);
        ctStyle.setName(name);

        // 大纲级别（用于生成目录）
        CTPPrGeneral pPr = ctStyle.addNewPPr();
        pPr.addNewOutlineLvl().setVal(BigInteger.valueOf(outlineLevel - 1));

        // 字体样式
        CTRPr rPr = ctStyle.addNewRPr();
        rPr.addNewSz().setVal(BigInteger.valueOf(halfPointSize));
        rPr.addNewSzCs().setVal(BigInteger.valueOf(halfPointSize));
        if (bold) {
            rPr.addNewB().setVal(STOnOff.TRUE);
            rPr.addNewBCs().setVal(STOnOff.TRUE);
        }

        XWPFStyle xwpfStyle = new XWPFStyle(ctStyle);
        styles.addStyle(xwpfStyle);
    }
}
