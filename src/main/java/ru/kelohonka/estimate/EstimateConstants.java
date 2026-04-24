package ru.kelohonka.estimate;

/**
 * Константы по умолчанию для demo-расчетов.
 */
public final class EstimateConstants {
    /** Цена леса за 1 кубический метр. */
    public static final double DEFAULT_TIMBER_PRICE_PER_CUBIC_METER = 55_000;

    /** Цена доски за 1 кубический метр. */
    public static final double DEFAULT_BOARD_PRICE_PER_CUBIC_METER = 55_000;

    /** Цена печи по умолчанию. */
    public static final double DEFAULT_STOVE_PRICE = 120_000;

    /** Стоимость монтажа печи по умолчанию. */
    public static final double DEFAULT_STOVE_INSTALLATION_PRICE = 50_000;

    /** Цена террасы за 1 квадратный метр. */
    public static final double DEFAULT_TERRACE_PRICE_PER_SQUARE_METER = 35_000;

    /** Эталонная цена кровли для дома 6x6. */
    public static final double DEFAULT_REFERENCE_ROOF_PRICE = 700_000;

    private EstimateConstants() {
    }
}
