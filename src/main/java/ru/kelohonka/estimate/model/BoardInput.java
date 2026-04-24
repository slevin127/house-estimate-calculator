package ru.kelohonka.estimate.model;

/**
 * Параметры доски для расчета объема и стоимости.
 * Используется как для потолка, так и для пола.
 *
 * @param lengthMeters длина участка в метрах
 * @param widthMeters ширина участка в метрах
 * @param thicknessMm толщина доски в миллиметрах
 * @param pricePerCubicMeter цена доски за один кубический метр
 */
public record BoardInput(
        double lengthMeters,
        double widthMeters,
        double thicknessMm,
        double pricePerCubicMeter
) {
    /**
     * @return площадь участка в квадратных метрах
     */
    public double areaSquareMeters() {
        return lengthMeters * widthMeters;
    }

    /**
     * @return толщина доски в метрах
     */
    public double thicknessMeters() {
        return thicknessMm / 1000.0;
    }
}
