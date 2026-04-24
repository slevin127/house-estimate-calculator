package ru.kelohonka.estimate.model;

/**
 * Параметры печи, включаемой в смету.
 *
 * @param name название модели печи
 * @param stovePrice стоимость самой печи
 * @param installationPrice стоимость монтажа печи
 */
public record StoveItem(
        String name,
        double stovePrice,
        double installationPrice
) {
    /**
     * @return общая стоимость печи вместе с монтажом
     */
    public double totalCost() {
        return stovePrice + installationPrice;
    }
}
