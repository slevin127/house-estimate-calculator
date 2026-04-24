package ru.kelohonka.estimate.service;

import ru.kelohonka.estimate.model.BoardInput;
import ru.kelohonka.estimate.model.CrossCut;
import ru.kelohonka.estimate.model.EstimateResult;
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
            if (input.gableHeightMeters() == null || input.gableHeightMeters() <= 0) {
                throw new IllegalArgumentException("Gable height must be greater than 0 for log gables");
            }
            if (input.gableCount() == null || input.gableCount() <= 0) {
                throw new IllegalArgumentException("Gable count must be greater than 0 for log gables");
            }
            if (input.houseWidthMeters() == null || input.houseWidthMeters() <= 0) {
                throw new IllegalArgumentException("House width must be provided for log gable calculation");
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

    private List<OpeningItem> safeOpenings(LogHouseInput input) {
        return input.openings() == null ? List.of() : input.openings();
    }

    private List<CrossCut> safeCrossCuts(LogHouseInput input) {
        return input.crossCuts() == null ? List.of() : input.crossCuts();
    }

    private double resolveWorkingHeight(LogHouseInput input) {
        if (input.workingHeightMode() == WorkingHeightMode.CUSTOM) {
            return input.customWorkingHeightMeters();
        }
        return interpolate(input.logDiameterMm(),
                new int[]{200, 220, 240, 260, 280, 300},
                new double[]{0.18, 0.20, 0.22, 0.24, 0.26, 0.27});
    }

    private double calculateGableArea(LogHouseInput input) {
        if (input.gableType() != GableType.LOG) {
            return 0.0;
        }
        double singleGableArea = input.houseWidthMeters() * input.gableHeightMeters() / 2.0;
        return singleGableArea * input.gableCount();
    }

    private double calculateGableVolume(LogHouseInput input, double gableArea) {
        if (input.gableType() != GableType.LOG || gableArea <= 0) {
            return 0.0;
        }
        double gableCoefficient = interpolate(input.logDiameterMm(),
                new int[]{200, 220, 240, 260, 280, 300},
                new double[]{0.205, 0.225, 0.245, 0.265, 0.285, 0.305});
        return gableArea * gableCoefficient;
    }

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

    private double calculateBoardVolume(BoardInput boardInput) {
        if (boardInput == null) {
            return 0.0;
        }
        return boardInput.areaSquareMeters() * boardInput.thicknessMeters();
    }

    private double calculateBoardCost(BoardInput boardInput) {
        if (boardInput == null) {
            return 0.0;
        }
        return MoneyUtils.roundUpToStep(calculateBoardVolume(boardInput) * boardInput.pricePerCubicMeter(), 50);
    }

    private double calculateCrossCutVolume(CrossCut crossCut, LogHouseInput input, double logHeightMeters) {
        double lengthMeters = crossCut.customLengthMeters() != null
                ? crossCut.customLengthMeters()
                : resolveCrossCutLength(input.logDiameterMm());
        return input.logDiameterMeters() * lengthMeters * logHeightMeters * crossCut.quantity();
    }

    private double resolveCrossCutLength(int logDiameterMm) {
        if (logDiameterMm <= 300) {
            return 1.0;
        }
        if (logDiameterMm >= 400) {
            return 1.5;
        }
        return 1.2;
    }

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

    private double calculateTerraceCost(TerraceInput terraceInput) {
        if (terraceInput == null) {
            return 0.0;
        }
        return terraceInput.areaSquareMeters() * terraceInput.pricePerSquareMeter();
    }
}
