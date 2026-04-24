package ru.kelohonka.estimate.model;

import java.util.List;

/**
 * Основные входные данные для предварительного расчета проекта сруба.
 * Содержит геометрию сруба, стоимость леса, проемы, перерубы и дополнительные позиции сметы.
 */
public record LogHouseInput(
        String projectName,
        Double totalWallLengthMeters,
        Double houseLengthMeters,
        Double houseWidthMeters,
        double wallHeightMeters,
        int logDiameterMm,
        double requiredClearHeightMeters,
        double timberPricePerCubicMeter,
        List<Double> innerWallLengthsMeters,
        GableType gableType,
        Double gableHeightMeters,
        Integer gableCount,
        WorkingHeightMode workingHeightMode,
        Double customWorkingHeightMeters,
        boolean deductOpenings,
        NotchType notchType,
        List<CrossCut> crossCuts,
        List<OpeningItem> openings,
        BoardInput ceilingBoard,
        BoardInput floorBoard,
        RoofInput roof,
        StoveItem stove,
        TerraceInput terrace,
        double wastePercent
) {
    public double logDiameterMeters() {
        return logDiameterMm / 1000.0;
    }

    public double outerWallLengthMeters() {
        if (houseLengthMeters != null && houseWidthMeters != null) {
            return 2 * (houseLengthMeters + houseWidthMeters);
        }
        return totalWallLengthMeters == null ? 0.0 : totalWallLengthMeters;
    }

    public double innerWallLengthMeters() {
        return innerWallLengthsMeters == null
                ? 0.0
                : innerWallLengthsMeters.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double totalWallLengthForCalculationMeters() {
        double explicitTotal = totalWallLengthMeters == null ? 0.0 : totalWallLengthMeters;
        double geometricTotal = outerWallLengthMeters() + innerWallLengthMeters();
        return geometricTotal > 0 ? geometricTotal : explicitTotal;
    }
}
