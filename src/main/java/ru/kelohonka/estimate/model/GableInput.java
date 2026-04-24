package ru.kelohonka.estimate.model;

/**
 * Параметры одного фронтона.
 *
 * @param name имя фронтона
 * @param lengthMeters длина основания фронтона в метрах
 * @param heightMeters высота фронтона в метрах
 */
public record GableInput(
        String name,
        double lengthMeters,
        double heightMeters
) {
    /**
     * @return площадь фронтона в квадратных метрах
     */
    public double areaSquareMeters() {
        return lengthMeters * heightMeters / 2.0;
    }
}
