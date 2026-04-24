package ru.kelohonka.estimate.model;

/**
 * Параметры террасы, включаемой в смету.
 *
 * @param lengthMeters длина террасы в метрах
 * @param widthMeters ширина террасы в метрах
 * @param pricePerSquareMeter стоимость одного квадратного метра террасы
 */
public record TerraceInput(
        double lengthMeters,
        double widthMeters,
        double pricePerSquareMeter
) {
    /**
     * @return площадь террасы в квадратных метрах
     */
    public double areaSquareMeters() {
        return lengthMeters * widthMeters;
    }
}
