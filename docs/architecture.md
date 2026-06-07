# Architecture — pdf-extractor

## Стек технологий

| Компонент | Технология | Версия |
|-----------|-----------|--------|
| Язык | Java | 21 |
| Фреймворк | Spring Boot | 3.4.4 |
| PDF → изображения | Apache PDFBox | 3.0.4 |
| OCR | Tesseract (Tess4J) | 5.16.0 |
| DOCX-экспорт | Apache POI | 5.4.0 |
| Сборка | Maven | (wrapper) |

## Структура пакетов

```
org.example.pdfextractor
├── PdfExtractorApplication.java     # Main (Spring Boot)
├── controller/
│   └── ExtractionController.java    # REST API
├── service/
│   ├── PdfExtractionService.java    # Извлечение страниц из PDF (PDFBox)
│   ├── ImagePreprocessor.java       # Препроцессинг изображений для OCR
│   ├── OcrService.java             # OCR-распознавание (Tess4J)
│   ├── DocxExportService.java      # Экспорт в DOCX (Apache POI)
│   └── ProcessingPipelineService.java # Оркестратор пайплайна
├── model/
│   ├── ExtractedPage.java          # Страница (изображение + текст + метаданные)
│   └── ExtractionResult.java       # Результат обработки всего PDF
└── exception/
    └── ExtractionException.java    # Единый runtime-экception
```

## Пайплайн обработки

1. **PDF → Images** (`PdfExtractionService`)
   - Читает PDF через PDFBox
   - Рендерит каждую страницу в `BufferedImage` при 300 DPI
   - Возвращает `List<ExtractedPage>`

2. **Image Preprocessing** (`ImagePreprocessor`)
   - Конвертирует в градации серого
   - Бинаризация (Otsu thresholding)
   - Удаление шума (медианный фильтр)
   - Увеличение контраста

3. **OCR** (`OcrService`)
   - Использует Tess4J для вызова Tesseract
   - Язык: eng (настраивается через `tesseract.language`)
   - Page segmentation mode: 6 (предполагает uniform блок текста)
   - Возвращает распознанный текст + оценку confidence

4. **DOCX Export** (`DocxExportService`)
   - Создаёт XWPFDocument (Apache POI)
   - Каждая страница — отдельный параграф с номером страницы
   - Сохраняет в `.docx`

## REST API

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/extract` | Полный пайплайн: PDF → DOCX |

Параметры запроса (multipart/form-data):
- `file` — PDF-файл

Ответ: DOCX-файл (application/vnd.openxmlformats-officedocument.wordprocessingml.document)

## Конфигурация (application.properties)

```properties
tesseract.datapath=/usr/local/share/tessdata
tesseract.language=eng
tesseract.pagesegmode=6
```

## Известные проблемы

1. **Invalid resolution 1 dpi warning**: Tess4J предупреждает при обработке изображений, полученных из PDFBox. Использует 70 DPI как fallback. Не влияет на качество распознавания, но шумит в логах.
   - Возможное решение: передавать изображение с явно установленным DPI через `BufferedImage` с `Density` в metadata.

2. **JNA restricted methods warning**: JNA использует `System.load`, что deprecated в Java 26+. Пока только warning, но в будущем может блокироваться.
   - Флаг: `--enable-native-access=ALL-UNNAMED`
