package ru.kelohonka.estimate.model;

/**
 * Итог расчета проекта с разбивкой по основным показателям и статьям стоимости.
 */
public record EstimateResult(
        double suggestedCrownCount,
        double workingHeightMeters,
        double logHeightMeters,
        double outerWallLengthMeters,
        double innerWallLengthMeters,
        double totalWallLengthMeters,
        double totalLogLengthMeters,
        double grossWallAreaSquareMeters,
        double openingsAreaSquareMeters,
        double netWallAreaSquareMeters,
        double logSectionAreaSquareMeters,
        double wallVolumeCubicMeters,
        double gableAreaSquareMeters,
        double gableVolumeCubicMeters,
        double openingsVolumeCubicMeters,
        double netVolumeCubicMeters,
        double crossCutVolumeCubicMeters,
        double totalLogVolumeCubicMeters,
        double totalLogVolumeWithWasteCubicMeters,
        double timberCost,
        double openingsCost,
        double ceilingBoardVolumeCubicMeters,
        double ceilingBoardCost,
        double floorBoardVolumeCubicMeters,
        double floorBoardCost,
        double roofCost,
        double stoveCost,
        double terraceCost,
        double grandTotal
) {
}
