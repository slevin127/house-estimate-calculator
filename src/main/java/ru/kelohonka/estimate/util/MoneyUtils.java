package ru.kelohonka.estimate.util;

/**
 * Вспомогательные методы для округления денежных и расчетных значений.
 */
public final class MoneyUtils {
    private MoneyUtils() {
    }

    /**
     * Округляет значение вверх до ближайшего шага.
     * Например, при шаге 50 значение 12 341 станет 12 350,
     * а при шаге 50 000 значение 311 000 станет 350 000.
     *
     * @param value исходное значение
     * @param step шаг округления
     * @return округленное вверх значение
     */
    public static double roundUpToStep(double value, double step) {
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be positive");
        }
        return Math.ceil(value / step) * step;
    }

    /**
     * Округляет значение до двух знаков после запятой.
     *
     * @param value исходное значение
     * @return округленное значение
     */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
