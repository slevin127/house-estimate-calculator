package ru.kelohonka.estimate.model;

/**
 * Описывает один проем в проекте, окно или дверь.
 * Площадь проема вычитается из общей площади стен, а цена может входить в смету.
 *
 * @param type тип проема, окно или дверь
 * @param name название или условное имя позиции
 * @param widthMeters ширина проема в метрах
 * @param heightMeters высота проема в метрах
 * @param unitPrice стоимость позиции
 */
public record OpeningItem(
        OpeningType type,
        String name,
        double widthMeters,
        double heightMeters,
        double unitPrice
) {
    /**
     * @return площадь проема в квадратных метрах
     */
    public double areaSquareMeters() {
        return widthMeters * heightMeters;
    }
}
