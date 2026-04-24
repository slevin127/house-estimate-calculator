package ru.kelohonka.estimate.model;

/**
 * Параметры расчета стоимости кровли.
 * В текущей версии стоимость считается пропорционально площади
 * относительно эталонного размера и эталонной цены.
 *
 * @param baseLengthMeters длина рассчитываемого объекта в метрах
 * @param baseWidthMeters ширина рассчитываемого объекта в метрах
 * @param referenceLengthMeters длина эталонного объекта в метрах
 * @param referenceWidthMeters ширина эталонного объекта в метрах
 * @param referencePrice стоимость кровли для эталонного объекта
 * @param roundUpToFiftyThousand если true, стоимость округляется вверх до 50 000
 */
public record RoofInput(
        double baseLengthMeters,
        double baseWidthMeters,
        double referenceLengthMeters,
        double referenceWidthMeters,
        double referencePrice,
        boolean roundUpToFiftyThousand
) {
    /**
     * @return площадь текущего объекта в квадратных метрах
     */
    public double baseAreaSquareMeters() {
        return baseLengthMeters * baseWidthMeters;
    }

    /**
     * @return площадь эталонного объекта в квадратных метрах
     */
    public double referenceAreaSquareMeters() {
        return referenceLengthMeters * referenceWidthMeters;
    }
}
