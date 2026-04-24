package ru.kelohonka.estimate;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import ru.kelohonka.estimate.model.EstimateResult;
import ru.kelohonka.estimate.model.LogHouseInput;
import ru.kelohonka.estimate.model.OpeningItem;
import ru.kelohonka.estimate.model.OpeningType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Экспортирует результаты расчета в Word-файл формата DOCX.
 */
public class EstimateWordExporter {

    /**
     * Сохраняет список расчетов в DOCX-файл.
     *
     * @param outputPath путь к итоговому файлу
     * @param estimates список расчетов
     * @throws IOException если файл не удалось записать
     */
    public void export(Path outputPath, List<EstimateBundle> estimates) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Демо-расчеты домов");

            for (int i = 0; i < estimates.size(); i++) {
                EstimateBundle bundle = estimates.get(i);
                addEstimate(document, bundle.input(), bundle.result());
                if (i < estimates.size() - 1) {
                    document.createParagraph();
                }
            }

            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                document.write(outputStream);
            }
        }
    }

    private void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(16);
        run.setText(text);
    }

    private void addEstimate(XWPFDocument document, LogHouseInput input, EstimateResult result) {
        List<OpeningItem> windows = input.openings().stream()
                .filter(opening -> opening.type() == OpeningType.WINDOW)
                .toList();
        List<OpeningItem> doors = input.openings().stream()
                .filter(opening -> opening.type() == OpeningType.DOOR)
                .toList();
        int crossCutCount = input.crossCuts().stream()
                .mapToInt(crossCut -> crossCut.quantity())
                .sum();

        addLine(document, input.projectName(), true);
        addLine(document, String.format("Диаметр бревна: %d мм", input.logDiameterMm()), false);
        addLine(document, String.format("Высота стен: %.2f м", input.wallHeightMeters()), false);
        addLine(document, String.format("Рабочая высота венца: %.3f м", result.workingHeightMeters()), false);
        addLine(document, String.format("Количество венцов: %.0f", result.suggestedCrownCount()), false);
        addLine(document, String.format("Расчетная высота сруба: %.2f м", result.logHeightMeters()), false);
        addLine(document, String.format("Длина наружных стен: %.2f м", result.outerWallLengthMeters()), false);
        addLine(document, String.format("Длина внутренних стен: %.2f м", result.innerWallLengthMeters()), false);
        addLine(document, String.format("Общая длина стен: %.2f м", result.totalWallLengthMeters()), false);
        addLine(document, String.format("Общий погонный метраж бревна: %.2f м.п.", result.totalLogLengthMeters()), false);
        addLine(document, String.format("Площадь сечения бревна: %.4f м2", result.logSectionAreaSquareMeters()), false);
        addLine(document, String.format("Количество перерубов: %d", crossCutCount), false);
        addLine(document, "Окна: " + windows.size() + formatOpenings(windows), false);
        addLine(document, "Двери: " + doors.size() + formatOpenings(doors), false);
        addLine(document, String.format("Площадь стен до вычета: %.3f м2", result.grossWallAreaSquareMeters()), false);
        addLine(document, String.format("Площадь проемов: %.3f м2", result.openingsAreaSquareMeters()), false);
        addLine(document, String.format("Чистая площадь стен: %.3f м2", result.netWallAreaSquareMeters()), false);
        addLine(document, String.format("Кубатура стен: %.3f м3", result.wallVolumeCubicMeters()), false);
        addLine(document, String.format("Площадь фронтонов: %.3f м2", result.gableAreaSquareMeters()), false);
        addLine(document, String.format("Кубатура фронтонов: %.3f м3", result.gableVolumeCubicMeters()), false);
        addLine(document, String.format("Объем проемов: %.3f м3", result.openingsVolumeCubicMeters()), false);
        addLine(document, String.format("Итого без запаса: %.3f м3", result.netVolumeCubicMeters()), false);
        addLine(document, String.format("Кубатура перерубов: %.3f м3", result.crossCutVolumeCubicMeters()), false);
        addLine(document, String.format("Общая кубатура бревна: %.3f м3", result.totalLogVolumeCubicMeters()), false);
        addLine(document, String.format("Общая кубатура бревна с запасом: %.3f м3", result.totalLogVolumeWithWasteCubicMeters()), false);
        addLine(document, String.format("Стоимость леса: %.2f руб", result.timberCost()), false);
        addLine(document, String.format("Доска потолка: %.3f м3, стоимость %.2f руб",
                result.ceilingBoardVolumeCubicMeters(), result.ceilingBoardCost()), false);
        addLine(document, String.format("Доска пола: %.3f м3, стоимость %.2f руб",
                result.floorBoardVolumeCubicMeters(), result.floorBoardCost()), false);
        addLine(document, String.format("Стоимость кровли: %.2f руб", result.roofCost()), false);
        addLine(document, String.format("Стоимость печи: %.2f руб", result.stoveCost()), false);
        addLine(document, String.format("Стоимость террасы: %.2f руб", result.terraceCost()), false);
        addLine(document, String.format("ИТОГО: %.2f руб", result.grandTotal()), true);
    }

    private String formatOpenings(List<OpeningItem> openings) {
        if (openings.isEmpty()) {
            return "";
        }
        return " (" + openings.stream()
                .map(opening -> String.format("%.2f x %.2f м", opening.widthMeters(), opening.heightMeters()))
                .collect(Collectors.joining(", ")) + ")";
    }

    private void addLine(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setText(text);
    }

    /**
     * Пара входных данных и результата расчета для экспорта.
     *
     * @param input входные данные проекта
     * @param result результат расчета
     */
    public record EstimateBundle(LogHouseInput input, EstimateResult result) {
    }
}
