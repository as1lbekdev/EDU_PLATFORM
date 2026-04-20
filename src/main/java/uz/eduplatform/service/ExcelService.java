package uz.eduplatform.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.entity.Question;
import uz.eduplatform.repository.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final ChapterRepository chapterRepository;

    public int importTopicQuestions(MultipartFile file, Long topicId) throws Exception {
        List<Question> list = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Question q = build(row, i);
                if (q == null) continue;
                q.setTopic(topicRepository.getReferenceById(topicId));
                q.setType(Question.QuestionType.TOPIC);
                list.add(q);
            }
        }
        questionRepository.saveAll(list);
        return list.size();
    }

    public int importChapterQuestions(MultipartFile file, Long chapterId) throws Exception {
        List<Question> list = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Question q = build(row, i);
                if (q == null) continue;
                q.setChapter(chapterRepository.getReferenceById(chapterId));
                q.setType(Question.QuestionType.CHAPTER);
                list.add(q);
            }
        }
        questionRepository.saveAll(list);
        return list.size();
    }

    private Question build(Row row, int rowNum) {
        try {
            String questionText = str(row.getCell(0));
            if (questionText == null || questionText.isBlank()) return null;

            String opt1 = str(row.getCell(1));
            String opt2 = str(row.getCell(2));
            String opt3 = str(row.getCell(3));
            String opt4 = str(row.getCell(4));

            // F ustuni: 1, 2, 3, yoki 4 (1-asosida)
            int correctRaw = (int) Math.round(numVal(row.getCell(5)));

            // 1-4 oraliqda bo'lishi kerak
            if (correctRaw < 1 || correctRaw > 4) {
                System.err.println("Row " + rowNum + ": to'g'ri javob " + correctRaw + " - 1-4 oraliqda bo'lishi kerak!");
                return null;
            }

            int correctIndex = correctRaw - 1; // 0-asosiga o'tkazish

            Question q = new Question();
            q.setQuestionText(questionText);
            q.setOptions(List.of(opt1, opt2, opt3, opt4));
            q.setCorrectOptionIndex(correctIndex);
            return q;

        } catch (Exception e) {
            System.err.println("Row " + rowNum + " o'qishda xatolik: " + e.getMessage());
            return null;
        }
    }

    private String str(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private double numVal(Cell cell) {
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (Exception e) { yield 0; }
            }
            default -> 0;
        };
    }
}