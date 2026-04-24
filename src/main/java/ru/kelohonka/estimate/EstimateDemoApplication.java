package ru.kelohonka.estimate;

import ru.kelohonka.estimate.model.BoardInput;
import ru.kelohonka.estimate.model.CrossCut;
import ru.kelohonka.estimate.model.EstimateResult;
import ru.kelohonka.estimate.model.GableInput;
import ru.kelohonka.estimate.model.GableType;
import ru.kelohonka.estimate.model.LogHouseInput;
import ru.kelohonka.estimate.model.NotchType;
import ru.kelohonka.estimate.model.OpeningItem;
import ru.kelohonka.estimate.model.OpeningType;
import ru.kelohonka.estimate.model.RoofInput;
import ru.kelohonka.estimate.model.StoveItem;
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
import static ru.kelohonka.estimate.EstimateConstants.DEFAULT_TIMBER_PRICE_PER_CUBIC_METER;
import static ru.kelohonka.estimate.model.OpeningType.DOOR;
import static ru.kelohonka.estimate.model.OpeningType.WINDOW;

public class EstimateDemoApplication {

    public static void main(String[] args) {
        EstimateCalculatorService calculator = new EstimateCalculatorService();

        List<LogHouseInput> demoHouses = List.of(house());
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

    private static void printEstimate(LogHouseInput input, EstimateResult result) {
        List<OpeningItem> openings = input.openings() == null ? List.of() : input.openings();
        List<CrossCut> crossCuts = input.crossCuts() == null ? List.of() : input.crossCuts();
        List<GableInput> gables = input.gables() == null ? List.of() : input.gables();

        List<OpeningItem> windows = openings.stream()
                .filter(opening -> opening.type() == OpeningType.WINDOW)
                .toList();
        List<OpeningItem> doors = openings.stream()
                .filter(opening -> opening.type() == OpeningType.DOOR)
                .toList();
        int crossCutCount = crossCuts.stream()
                .mapToInt(CrossCut::quantity)
                .sum();

        System.out.println("==== Предварительный расчет ====");
        System.out.println("Проект: " + input.projectName());
        System.out.printf("Диаметр бревна: %d мм%n", input.logDiameterMm());
        System.out.printf("Чистовая высота: %.2f м%n", input.requiredClearHeightMeters());
        System.out.printf("Рекомендуемое количество венцов: %.0f%n", result.suggestedCrownCount());
        System.out.printf("Высота сруба: %.2f м%n", result.logHeightMeters());
        System.out.printf("Тип фронтонов: %s%n", input.gableType());
        if (input.gableType() == GableType.LOG) {
            if (!gables.isEmpty()) {
                System.out.println("Фронтоны:");
                for (GableInput gable : gables) {
                    System.out.printf("  %s: %.2f x %.2f м%n", gable.name(), gable.lengthMeters(), gable.heightMeters());
                }
            } else {
                System.out.printf("Фронтоны (длина x высота): %.2f x %.2f м, шт: %d%n",
                        input.gableLengthMeters(), input.gableHeightMeters(), input.gableCount());
            }
        }
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
        System.out.printf("Площадь фронтонов: %.3f м2%n", result.gableAreaSquareMeters());
        System.out.printf("Кубатура фронтонов: %.3f м3%n", result.gableVolumeCubicMeters());
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

    private static LogHouseInput house() {
        double length = 6.2;
        double width = 4.2;
        List<GableInput> gables = gables(
                gable("Фронтон 1", 4.2, 2.55),
                gable("Фронтон 2", 4.8, 2.20),
                gable("Фронтон 3", 3.6, 1.95)
        );

        return new LogHouseInput(
                "Баня 6*4",
                14.6,
                length,
                width,
                3.0,
                260,
                3.0,
                DEFAULT_TIMBER_PRICE_PER_CUBIC_METER,
                List.of(6.2, 4.2),
                GableType.LOG,
                gables,
                null,
                null,
                null,
                WorkingHeightMode.AUTO,
                null,
                true,
                NotchType.IN_BOWL,
                List.of(
                        new CrossCut("Переруб 1", 1, null)
                ),
                openings(
                        window("Окно 1", 1.6, 1.4),
                        window("Окно 2", 1.2, 1.2),
                        window("Окно 3", 0.5, 0.5),
                        window("Окно 4", 0.5, 0.5),
                        door("Дверь входная", 1.0, 2.05),
                        door("Дверь внутренняя", 0.9, 2.05),
                        door("Дверь парная", 0.7, 2.05)
                ),
                new BoardInput(length, width, 25, DEFAULT_BOARD_PRICE_PER_CUBIC_METER),
                new BoardInput(length, width, 32, DEFAULT_BOARD_PRICE_PER_CUBIC_METER),
                new RoofInput(length, width, 6.0, 6.0, DEFAULT_REFERENCE_ROOF_PRICE, true),
                new StoveItem("Печь", DEFAULT_STOVE_PRICE, DEFAULT_STOVE_INSTALLATION_PRICE),
                null,
                10.0
        );
    }

    private static List<GableInput> gables(GableInput... gables) {
        return List.of(gables);
    }

    private static GableInput gable(String name, double lengthMeters, double heightMeters) {
        return new GableInput(name, lengthMeters, heightMeters);
    }

    private static List<OpeningItem> openings(OpeningItem... openings) {
        return List.of(openings);
    }

    private static OpeningItem window(String name, double width, double height) {
        return new OpeningItem(WINDOW, name, width, height, 0);
    }

    private static OpeningItem door(String name, double width, double height) {
        return new OpeningItem(DOOR, name, width, height, 0);
    }
}
