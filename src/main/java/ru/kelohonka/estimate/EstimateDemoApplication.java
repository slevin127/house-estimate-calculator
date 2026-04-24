package ru.kelohonka.estimate;

import ru.kelohonka.estimate.model.BoardInput;
import ru.kelohonka.estimate.model.CrossCut;
import ru.kelohonka.estimate.model.EstimateResult;
import ru.kelohonka.estimate.model.GableType;
import ru.kelohonka.estimate.model.LogHouseInput;
import ru.kelohonka.estimate.model.NotchType;
import ru.kelohonka.estimate.model.OpeningItem;
import ru.kelohonka.estimate.model.OpeningType;
import ru.kelohonka.estimate.model.RoofInput;
import ru.kelohonka.estimate.model.StoveItem;
import ru.kelohonka.estimate.model.TerraceInput;
import ru.kelohonka.estimate.model.WorkingHeightMode;
import ru.kelohonka.estimate.service.EstimateCalculatorService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_BOARD_PRICE_PER_CUBIC_METER;
import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_REFERENCE_ROOF_PRICE;
import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_STOVE_INSTALLATION_PRICE;
import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_STOVE_PRICE;
import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_TERRACE_PRICE_PER_SQUARE_METER;
import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_TIMBER_PRICE_PER_CUBIC_METER;
import static ru.kelohonka.estimate.model.OpeningType.DOOR;
import static ru.kelohonka.estimate.model.OpeningType.WINDOW;

/**
 * Демонстрационная точка входа для ручной проверки расчета.
 * Создает набор тестовых проектов, запускает расчет и печатает результат в консоль.
 */
public class EstimateDemoApplication {
    /**
     * Точка входа demo-приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        EstimateCalculatorService calculator = new EstimateCalculatorService();

        List<LogHouseInput> demoHouses = List.of(
                house()
        );

        List<EstimateWordExporter.EstimateBundle> bundles = demoHouses.stream()
                .map(input -> new EstimateWordExporter.EstimateBundle(input, calculator.calculate(input)))
                .toList();

        for (EstimateWordExporter.EstimateBundle bundle : bundles) {
            printEstimate(bundle.input(), bundle.result());
            System.out.println();
        }

        try {
            Path outputPath = Path.of("demo-house-estimates.docx");
            new EstimateWordExporter().export(outputPath, bundles);
            System.out.println("Word-файл сформирован: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сформировать Word-файл", e);
        }
    }

    /**
     * Печатает результат расчета в консоль.
     *
     * @param input входные данные проекта
     * @param result результат расчета
     */
    private static void printEstimate(LogHouseInput input, EstimateResult result) {
        List<OpeningItem> windows = input.openings().stream()
                .filter(opening -> opening.type() == OpeningType.WINDOW)
                .toList();
        List<OpeningItem> doors = input.openings().stream()
                .filter(opening -> opening.type() == OpeningType.DOOR)
                .toList();
        int crossCutCount = input.crossCuts().stream()
                .mapToInt(crossCut -> crossCut.quantity())
                .sum();

        System.out.println("==== Предварительный расчет ====");
        System.out.println("Проект: " + input.projectName());
        System.out.printf("Диаметр бревна: %d мм%n", input.logDiameterMm());
        System.out.printf("Чистовая высота: %.2f м%n", input.requiredClearHeightMeters());
        System.out.printf("Рекомендуемое количество венцов: %.0f%n", result.suggestedCrownCount());
        System.out.printf("Высота сруба: %.2f м%n", result.logHeightMeters());
        System.out.printf("Количество перерубов: %d%n", crossCutCount);
        System.out.println("Окна: " + windows.size());
        if (!windows.isEmpty()) {
            System.out.println("  Размеры окон: " + windows.stream()
                    .map(window -> String.format("%.2f x %.2f м", window.widthMeters(), window.heightMeters()))
                    .collect(Collectors.joining(", ")));
        }
        System.out.println("Двери: " + doors.size());
        if (!doors.isEmpty()) {
            System.out.println("  Размеры дверей: " + doors.stream()
                    .map(door -> String.format("%.2f x %.2f м", door.widthMeters(), door.heightMeters()))
                    .collect(Collectors.joining(", ")));
        }
        System.out.printf("Площадь стен до вычета: %.3f м2%n", result.grossWallAreaSquareMeters());
        System.out.printf("Площадь проемов: %.3f м2%n", result.openingsAreaSquareMeters());
        System.out.printf("Чистая площадь стен: %.3f м2%n", result.netWallAreaSquareMeters());
        System.out.printf("Кубатура стен: %.3f м3%n", result.wallVolumeCubicMeters());
        System.out.printf("Кубатура перерубов: %.3f м3%n", result.crossCutVolumeCubicMeters());
        System.out.printf("Общая кубатура бревна: %.3f м3%n", result.totalLogVolumeCubicMeters());
        System.out.printf("Стоимость леса: %.2f руб%n", result.timberCost());
        System.out.printf("Доска потолка: %.3f м3, стоимость %.2f руб%n",
                result.ceilingBoardVolumeCubicMeters(), result.ceilingBoardCost());
        System.out.printf("Доска пола: %.3f м3, стоимость %.2f руб%n",
                result.floorBoardVolumeCubicMeters(), result.floorBoardCost());
        System.out.printf("Стоимость кровли: %.2f руб%n", result.roofCost());
        System.out.printf("Стоимость печи: %.2f руб%n", result.stoveCost());
        System.out.printf("Стоимость террасы: %.2f руб%n", result.terraceCost());
        System.out.printf("ИТОГО: %.2f руб%n", result.grandTotal());
    }

    /**
     * Создает demo-проект дома.
     *
     * @param name название проекта
     * @param length длина дома в метрах
     * @param width ширина дома в метрах
     * @param logDiameterMm диаметр бревна в миллиметрах
     * @param clearHeight требуемая чистовая высота
     * @param timberPrice цена леса за 1 м3
     * @param crossCutCount количество перерубов
     * @param openings список окон и дверей
     * @param ceilingThickness толщина доски потолка в миллиметрах
     * @param floorThickness толщина доски пола в миллиметрах
     * @param withStove нужна ли печь
     * @param terraceLength длина террасы
     * @param terraceWidth ширина террасы
     * @return входные данные проекта
     */
    private static LogHouseInput house() {
        double length = 8.0;
        double width = 10.0;
        return new LogHouseInput(
                "Демо-дом со всеми полями",
                null,
                length,
                width,
                3.0,
                240,
                3.0,
                DEFAULT_TIMBER_PRICE_PER_CUBIC_METER,
                List.of(8.0, 4.5),
                GableType.LOG,
                2.5,
                2,
                WorkingHeightMode.AUTO,
                null,
                true,
                NotchType.IN_BOWL,
                List.of(
                        new CrossCut("Переруб 1", 1, null),
                        new CrossCut("Переруб 2", 2, 1.2)
                ),
                openings(
                        window("Окно 1", 1.6, 1.4),
                        window("Окно 2", 1.2, 1.2),
                        door("Дверь входная", 1.0, 2.1),
                        door("Дверь внутренняя", 0.8, 2.0)
                ),
                new BoardInput(length, width, 32, DEFAULT_BOARD_PRICE_PER_CUBIC_METER),
                new BoardInput(length, width, 40, DEFAULT_BOARD_PRICE_PER_CUBIC_METER),
                new RoofInput(length, width, 6.0, 6.0, DEFAULT_REFERENCE_ROOF_PRICE, true),
                new StoveItem("Печь", DEFAULT_STOVE_PRICE, DEFAULT_STOVE_INSTALLATION_PRICE),
                new TerraceInput(3.0, 6.0, DEFAULT_TERRACE_PRICE_PER_SQUARE_METER),
                10.0
        );
    }

    /**
     * Собирает список проемов.
     *
     * @param openings массив проемов
     * @return список проемов
     */
    private static List<OpeningItem> openings(OpeningItem... openings) {
        return List.of(openings);
    }

    /**
     * Создает оконный проем.
     *
     * @param name название окна
     * @param width ширина окна
     * @param height высота окна
     * @return оконный проем
     */
    private static OpeningItem window(String name, double width, double height) {
        return new OpeningItem(WINDOW, name, width, height, 0);
    }

    /**
     * Создает дверной проем.
     *
     * @param name название двери
     * @param width ширина двери
     * @param height высота двери
     * @return дверной проем
     */
    private static OpeningItem door(String name, double width, double height) {
        return new OpeningItem(DOOR, name, width, height, 0);
    }
}
