package ru.kelohonka.estimate.service;

import ru.kelohonka.estimate.model.BoardInput;
import ru.kelohonka.estimate.model.CrossCut;
import ru.kelohonka.estimate.model.EstimateResult;
import ru.kelohonka.estimate.model.GableInput;
import ru.kelohonka.estimate.model.GableType;
import ru.kelohonka.estimate.model.LogHouseInput;
import ru.kelohonka.estimate.model.OpeningItem;
import ru.kelohonka.estimate.model.RoofInput;
import ru.kelohonka.estimate.model.TerraceInput;
import ru.kelohonka.estimate.model.WorkingHeightMode;
import ru.kelohonka.estimate.util.MoneyUtils;

import java.util.List;

/**
 * Основной сервис предварительного расчета проекта.
 * Выполняет расчет сруба, фронтонов, перерубов, проемов, доски, кровли, печи, террасы и общей суммы.
 */
public class EstimateCalculatorService {

    /**
     * Выполняет полный расчет сметы по входным параметрам проекта.
     *
     * @param input входные данные проекта
     * @return агрегированный результат расчета объемов и стоимостей
     */
    public EstimateResult calculate(LogHouseInput input) {
        validate(input);

        double outerWallLength = input.outerWallLengthMeters();
        double innerWallLength = input.innerWallLengthMeters();
        double totalWallLength = input.totalWallLengthForCalculationMeters();
        double workingHeight = resolveWorkingHeight(input);
        int crownCount = (int) Math.ceil(input.wallHeightMeters() / workingHeight);
        double logHeightMeters = crownCount * workingHeight;
        double totalLogLengthMeters = totalWallLength * crownCount;

        double logDiameterMeters = input.logDiameterMeters();
        double logSectionArea = Math.PI * logDiameterMeters * logDiameterMeters / 4.0;
        double wallVolume = totalLogLengthMeters * logSectionArea;

        double grossWallArea = totalWallLength * logHeightMeters;
        double openingsArea = safeOpenings(input).stream()
                .mapToDouble(OpeningItem::areaSquareMeters)
                .sum();
        double openingsCost = safeOpenings(input).stream()
                .mapToDouble(OpeningItem::unitPrice)
                .sum();
        double netWallArea = Math.max(0.0, grossWallArea - openingsArea);

        double gableArea = calculateGableArea(input);
        double gableVolume = calculateGableVolume(input, gableArea);
        double openingsVolume = input.deductOpenings() ? openingsArea * logDiameterMeters : 0.0;
        double netVolume = Math.max(0.0, wallVolume + gableVolume - openingsVolume);

        double crossCutVolume = safeCrossCuts(input).stream()
                .mapToDouble(crossCut -> calculateCrossCutVolume(crossCut, input, logHeightMeters))
                .sum();
        double totalLogVolume = netVolume + crossCutVolume;
        double totalLogVolumeWithWaste = totalLogVolume * (1.0 + input.wastePercent() / 100.0);
        double timberCost = MoneyUtils.roundUpToStep(totalLogVolumeWithWaste * input.timberPricePerCubicMeter(), 50);

        double ceilingBoardVolume = calculateBoardVolume(input.ceilingBoard());
        double ceilingBoardCost = calculateBoardCost(input.ceilingBoard());

        double floorBoardVolume = calculateBoardVolume(input.floorBoard());
        double floorBoardCost = calculateBoardCost(input.floorBoard());

        double roofCost = calculateRoofCost(input.roof());
        double stoveCost = MoneyUtils.roundUpToStep(
                input.stove() == null ? 0.0 : input.stove().totalCost(), 50);
        double terraceCost = MoneyUtils.roundUpToStep(calculateTerraceCost(input.terrace()), 50);
        double roundedOpeningsCost = MoneyUtils.roundUpToStep(openingsCost, 50);

        double grandTotal = MoneyUtils.roundUpToStep(
                timberCost
                        + roundedOpeningsCost
                        + ceilingBoardCost
                        + floorBoardCost
                        + roofCost
                        + stoveCost
                        + terraceCost,
                50);

        return new EstimateResult(
                crownCount,
                workingHeight,
                logHeightMeters,
                outerWallLength,
                innerWallLength,
                totalWallLength,
                totalLogLengthMeters,
                grossWallArea,
                openingsArea,
                netWallArea,
                logSectionArea,
                wallVolume,
                gableArea,
                gableVolume,
                openingsVolume,
                netVolume,
                crossCutVolume,
                totalLogVolume,
                totalLogVolumeWithWaste,
                timberCost,
                roundedOpeningsCost,
                ceilingBoardVolume,
                ceilingBoardCost,
                floorBoardVolume,
                floorBoardCost,
                roofCost,
                stoveCost,
                terraceCost,
                grandTotal
        );
    }

    /**
     * Проверяет входные данные на корректность перед расчетом.
     *
     * @param input входные данные проекта
     */
    private void validate(LogHouseInput input) {
        if (input.houseLengthMeters() == null && (input.totalWallLengthMeters() == null || input.totalWallLengthMeters() <= 0)) {
            throw new IllegalArgumentException("House dimensions or total wall length must be provided");
        }
        if (input.houseLengthMeters() != null && input.houseLengthMeters() <= 0) {
            throw new IllegalArgumentException("House length must be greater than 0");
        }
        if (input.houseWidthMeters() != null && input.houseWidthMeters() <= 0) {
            throw new IllegalArgumentException("House width must be greater than 0");
        }
        if (input.wallHeightMeters() <= 0) {
            throw new IllegalArgumentException("Wall height must be greater than 0");
        }
        if (input.logDiameterMm() <= 0) {
            throw new IllegalArgumentException("Log diameter must be greater than 0");
        }
        if (input.timberPricePerCubicMeter() < 0) {
            throw new IllegalArgumentException("Timber price must be non-negative");
        }
        if (input.wastePercent() < 0) {
            throw new IllegalArgumentException("Waste percent must be non-negative");
        }
        if (input.workingHeightMode() == WorkingHeightMode.CUSTOM
                && (input.customWorkingHeightMeters() == null || input.customWorkingHeightMeters() <= 0)) {
            throw new IllegalArgumentException("Custom working height must be greater than 0");
        }
        if (input.gableType() == GableType.LOG) {
            if (!safeGables(input).isEmpty()) {
                if (safeGables(input).stream().anyMatch(g -> g.lengthMeters() <= 0 || g.heightMeters() <= 0)) {
                    throw new IllegalArgumentException("Each gable length and height must be greater than 0 for log gables");
                }
            } else {
                if (input.gableLengthMeters() == null || input.gableLengthMeters() <= 0) {
                    throw new IllegalArgumentException("Gable length must be greater than 0 for log gables");
                }
                if (input.gableHeightMeters() == null || input.gableHeightMeters() <= 0) {
                    throw new IllegalArgumentException("Gable height must be greater than 0 for log gables");
                }
                if (input.gableCount() == null || input.gableCount() <= 0) {
                    throw new IllegalArgumentException("Gable count must be greater than 0 for log gables");
                }
            }
        }
        if (input.innerWallLengthsMeters() != null && input.innerWallLengthsMeters().stream().anyMatch(length -> length < 0)) {
            throw new IllegalArgumentException("Inner wall lengths cannot be negative");
        }
        if (safeOpenings(input).stream().anyMatch(opening -> opening.widthMeters() < 0 || opening.heightMeters() < 0)) {
            throw new IllegalArgumentException("Openings dimensions cannot be negative");
        }
        validateBoard(input.ceilingBoard(), "Ceiling");
        validateBoard(input.floorBoard(), "Floor");
    }

    /**
     * Возвращает безопасный список проемов (пустой, если проемы не заданы).
     *
     * @param input входные данные проекта
     * @return список проемов без null
     */
    private List<OpeningItem> safeOpenings(LogHouseInput input) {
        return input.openings() == null ? List.of() : input.openings();
    }

    /**
     * Возвращает безопасный список перерубов (пустой, если перерубы не заданы).
     *
     * @param input входные данные проекта
     * @return список перерубов без null
     */
    private List<CrossCut> safeCrossCuts(LogHouseInput input) {
        return input.crossCuts() == null ? List.of() : input.crossCuts();
    }

    private List<GableInput> safeGables(LogHouseInput input) {
        return input.gables() == null ? List.of() : input.gables();
    }

    /**
     * Определяет рабочую высоту венца в зависимости от выбранного режима.
     *
     * @param input входные данные проекта
     * @return рабочая высота венца в метрах
     */
    private double resolveWorkingHeight(LogHouseInput input) {
        if (input.workingHeightMode() == WorkingHeightMode.CUSTOM) {
            return input.customWorkingHeightMeters();
        }
        return interpolate(input.logDiameterMm(),
                new int[]{200, 220, 240, 260, 280, 300},
                new double[]{0.18, 0.20, 0.22, 0.24, 0.26, 0.27});
    }

    /**
     * Рассчитывает суммарную площадь рубленых фронтонов.
     *
     * @param input входные данные проекта
     * @return площадь фронтонов в квадратных метрах
     */
    private double calculateGableArea(LogHouseInput input) {
        if (input.gableType() != GableType.LOG) {
            return 0.0;
        }
        if (!safeGables(input).isEmpty()) {
            return safeGables(input).stream()
                    .mapToDouble(GableInput::areaSquareMeters)
                    .sum();
        }
        double singleGableArea = input.gableLengthMeters() * input.gableHeightMeters() / 2.0;
        return singleGableArea * input.gableCount();
    }

    /**
     * Рассчитывает объем рубленых фронтонов по площади и коэффициенту, зависящему от диаметра бревна.
     *
     * @param input входные данные проекта
     * @param gableArea площадь фронтонов
     * @return объем фронтонов в кубических метрах
     */
    private double calculateGableVolume(LogHouseInput input, double gableArea) {
        if (input.gableType() != GableType.LOG || gableArea <= 0) {
            return 0.0;
        }
        double gableCoefficient = interpolate(input.logDiameterMm(),
                new int[]{200, 220, 240, 260, 280, 300},
                new double[]{0.205, 0.225, 0.245, 0.265, 0.285, 0.305});
        return gableArea * gableCoefficient;
    }

    /**
     * Линейно интерполирует значение по табличным точкам.
     *
     * @param value входное значение оси X
     * @param x массив X-координат табличных точек
     * @param y массив Y-координат табличных точек
     * @return интерполированное значение Y
     */
    private double interpolate(int value, int[] x, double[] y) {
        if (value <= x[0]) {
            return y[0];
        }
        if (value >= x[x.length - 1]) {
            return y[y.length - 1];
        }
        for (int i = 0; i < x.length - 1; i++) {
            if (value >= x[i] && value <= x[i + 1]) {
                double ratio = (value - x[i]) / (double) (x[i + 1] - x[i]);
                return y[i] + ratio * (y[i + 1] - y[i]);
            }
        }
        return y[y.length - 1];
    }

    /**
     * Проверяет корректность параметров доски.
     *
     * @param board параметры доски
     * @param label имя секции для текста ошибки
     */
    private void validateBoard(BoardInput board, String label) {
        if (board == null) {
            return;
        }
        if (board.lengthMeters() <= 0 || board.widthMeters() <= 0) {
            throw new IllegalArgumentException(label + " board area dimensions must be greater than 0");
        }
        if (board.thicknessMm() < 20 || board.thicknessMm() > 50) {
            throw new IllegalArgumentException(label + " board thickness must be in range 20-50 mm");
        }
        if (board.pricePerCubicMeter() < 0) {
            throw new IllegalArgumentException(label + " board price must be non-negative");
        }
    }

    /**
     * Рассчитывает объем доски по площади и толщине.
     *
     * @param boardInput параметры доски
     * @return объем в кубических метрах
     */
    private double calculateBoardVolume(BoardInput boardInput) {
        if (boardInput == null) {
            return 0.0;
        }
        return boardInput.areaSquareMeters() * boardInput.thicknessMeters();
    }

    /**
     * Рассчитывает стоимость доски с округлением вверх до шага 50.
     *
     * @param boardInput параметры доски
     * @return стоимость доски
     */
    private double calculateBoardCost(BoardInput boardInput) {
        if (boardInput == null) {
            return 0.0;
        }
        return MoneyUtils.roundUpToStep(calculateBoardVolume(boardInput) * boardInput.pricePerCubicMeter(), 50);
    }

    /**
     * Рассчитывает объем переруба.
     *
     * @param crossCut параметры переруба
     * @param input входные данные проекта
     * @param logHeightMeters расчетная высота сруба
     * @return объем переруба в кубических метрах
     */
    private double calculateCrossCutVolume(CrossCut crossCut, LogHouseInput input, double logHeightMeters) {
        double lengthMeters = crossCut.customLengthMeters() != null
                ? crossCut.customLengthMeters()
                : resolveCrossCutLength(input.logDiameterMm());
        return input.logDiameterMeters() * lengthMeters * logHeightMeters * crossCut.quantity();
    }

    /**
     * Возвращает длину переруба по диаметру бревна при отсутствии пользовательского значения.
     *
     * @param logDiameterMm диаметр бревна в миллиметрах
     * @return длина переруба в метрах
     */
    private double resolveCrossCutLength(int logDiameterMm) {
        if (logDiameterMm <= 300) {
            return 1.0;
        }
        if (logDiameterMm >= 400) {
            return 1.5;
        }
        return 1.2;
    }

    /**
     * Рассчитывает стоимость кровли пропорционально эталонной площади и цене.
     *
     * @param roofInput параметры расчета кровли
     * @return стоимость кровли с целевым округлением
     */
    private double calculateRoofCost(RoofInput roofInput) {
        if (roofInput == null) {
            return 0.0;
        }
        double rawCost = (roofInput.baseAreaSquareMeters() / roofInput.referenceAreaSquareMeters())
                * roofInput.referencePrice();
        if (roofInput.roundUpToFiftyThousand()) {
            return MoneyUtils.roundUpToStep(rawCost, 50_000);
        }
        return MoneyUtils.roundUpToStep(rawCost, 50);
    }

    /**
     * Рассчитывает стоимость террасы по площади.
     *
     * @param terraceInput параметры террасы
     * @return стоимость террасы
     */
    private double calculateTerraceCost(TerraceInput terraceInput) {
        if (terraceInput == null) {
            return 0.0;
        }
        return terraceInput.areaSquareMeters() * terraceInput.pricePerSquareMeter();
    }
}
