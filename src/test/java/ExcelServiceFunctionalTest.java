import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.entity.*;
import uz.eduplatform.repository.*;
import uz.eduplatform.service.*;

import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelService - Funksional testlar")
class ExcelServiceFunctionalTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private ChapterRepository chapterRepository;

    @InjectMocks private ExcelService excelService;

    private byte[] createExcelBytes(List<Object[]> rows) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Savol");
        header.createCell(1).setCellValue("A");
        header.createCell(2).setCellValue("B");
        header.createCell(3).setCellValue("C");
        header.createCell(4).setCellValue("D");
        header.createCell(5).setCellValue("To'g'ri");
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Object[] r = rows.get(i);
            for (int j = 0; j < r.length; j++) {
                if (r[j] instanceof Number)
                    row.createCell(j).setCellValue(((Number) r[j]).doubleValue());
                else
                    row.createCell(j).setCellValue(String.valueOf(r[j]));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    @Test
    @DisplayName("1.1 - To'g'ri Excel fayl yuklanganda savollar saqlanadi")
    void importTopicQuestions_validFile_savesCalled() throws Exception {
        List<Object[]> rows = List.of(
                new Object[]{"2+2=?", "3", "4", "5", "6", 2},
                new Object[]{"5*5=?", "20", "25", "30", "35", 2},
                new Object[]{"10/2=?", "4", "5", "6", "7", 2}
        );
        byte[] bytes = createExcelBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
        );

        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(questionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        int count = excelService.importTopicQuestions(file, 1L);

        assertEquals(3, count);
        verify(questionRepository).saveAll(argThat(list ->
                ((List<?>) list).size() == 3
        ));
    }

    @Test
    @DisplayName("1.2 - correctOptionIndex 1-4 dan tashqari bo'lsa savol o'tkazib yuboriladi")
    void importTopicQuestions_invalidCorrectIndex_skipsRow() throws Exception {
        List<Object[]> rows = List.of(
                new Object[]{"Savol 1", "A", "B", "C", "D", 2},
                new Object[]{"Savol 2", "A", "B", "C", "D", 0},
                new Object[]{"Savol 3", "A", "B", "C", "D", 5}
        );
        byte[] bytes = createExcelBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
        );

        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());
        when(questionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        int count = excelService.importTopicQuestions(file, 1L);

        assertEquals(1, count);
    }

    @Test
    @DisplayName("1.3 - correctOptionIndex 1-asosidan 0-asosiga o'tkaziladi")
    void importTopicQuestions_correctIndexConverted() throws Exception {
        List<Object[]> rows = List.of(
                new Object[][]{new Object[]{"Savol", "A", "B", "C", "D", 3}}
        );
        byte[] bytes = createExcelBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
        );

        when(topicRepository.getReferenceById(1L)).thenReturn(new Topic());

        List<Question> saved = new ArrayList<>();
        when(questionRepository.saveAll(anyList())).thenAnswer(i -> {
            saved.addAll((List<Question>) i.getArgument(0));
            return saved;
        });

        excelService.importTopicQuestions(file, 1L);

        assertFalse(saved.isEmpty());
        assertEquals(2, saved.get(0).getCorrectOptionIndex());
    }

    @Test
    @DisplayName("1.4 - Bob uchun Excel yuklash ishlaydi")
    void importChapterQuestions_success() throws Exception {
        List<Object[]> rows = List.of(
                new Object[]{"Savol 1", "A", "B", "C", "D", 1},
                new Object[]{"Savol 2", "A", "B", "C", "D", 4}
        );
        byte[] bytes = createExcelBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
        );

        when(chapterRepository.getReferenceById(2L)).thenReturn(new Chapter());
        when(questionRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        int count = excelService.importChapterQuestions(file, 2L);

        assertEquals(2, count);
    }
}