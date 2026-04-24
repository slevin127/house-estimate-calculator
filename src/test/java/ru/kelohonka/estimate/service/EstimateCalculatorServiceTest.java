package ru.kelohonka.estimate.service;

import org.junit.jupiter.api.Test;
import ru.kelohonka.estimate.model.BoardInput;
import ru.kelohonka.estimate.model.CrossCut;
import ru.kelohonka.estimate.model.EstimateResult;
import ru.kelohonka.estimate.model.GableType;
import ru.kelohonka.estimate.model.LogHouseInput;
import ru.kelohonka.estimate.model.NotchType;
import ru.kelohonka.estimate.model.OpeningItem;
import ru.kelohonka.estimate.model.OpeningType;
import ru.kelohonka.estimate.model.RoofInput;
import ru.kelohonka.estimate.model.WorkingHeightMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstimateCalculatorServiceTest {

    private final EstimateCalculatorService service = new EstimateCalculatorService();

    @Test
    void shouldCalculateExampleFromUpdatedRequirements() {
        LogHouseInput input = new LogHouseInput(
                "Тестовый проект",
                null,
                8.0,
                10.0,
                3.0,
                240,
                3.0,
                33_000,
                List.of(8.0),
                GableType.LOG,
                2.5,
                2,
                WorkingHeightMode.AUTO,
                null,
                true,
                NotchType.IN_BOWL,
                List.of(),
                List.of(
                        new OpeningItem(OpeningType.WINDOW, "Проемы", 3.0, 4.0, 0)
                ),
                new BoardInput(8.0, 10.0, 32, 25_000),
                new BoardInput(8.0, 10.0, 40, 25_000),
                new RoofInput(8.0, 10.0, 6.0, 6.0, 700_000, true),
                null,
                null,
                10.0
        );

        EstimateResult result = service.calculate(input);

        assertEquals(36.0, result.outerWallLengthMeters(), 0.0001);
        assertEquals(8.0, result.innerWallLengthMeters(), 0.0001);
        assertEquals(44.0, result.totalWallLengthMeters(), 0.0001);
        assertEquals(0.22, result.workingHeightMeters(), 0.0001);
        assertEquals(14.0, result.suggestedCrownCount(), 0.0001);
        assertEquals(616.0, result.totalLogLengthMeters(), 0.0001);
        assertEquals(0.0452, result.logSectionAreaSquareMeters(), 0.0002);
        assertEquals(27.867, result.wallVolumeCubicMeters(), 0.02);
        assertEquals(25.0, result.gableAreaSquareMeters(), 0.0001);
        assertEquals(6.125, result.gableVolumeCubicMeters(), 0.001);
        assertEquals(2.88, result.openingsVolumeCubicMeters(), 0.0001);
        assertEquals(31.112, result.netVolumeCubicMeters(), 0.02);
        assertEquals(34.223, result.totalLogVolumeWithWasteCubicMeters(), 0.02);
    }

    @Test
    void shouldUseCustomWorkingHeight() {
        LogHouseInput input = new LogHouseInput(
                "Кастомная высота венца",
                null,
                6.0,
                8.0,
                3.0,
                260,
                3.0,
                33_000,
                List.of(),
                GableType.NONE,
                null,
                null,
                WorkingHeightMode.CUSTOM,
                0.25,
                false,
                NotchType.OTHER,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                0.0
        );

        EstimateResult result = service.calculate(input);

        assertEquals(0.25, result.workingHeightMeters(), 0.0001);
        assertEquals(12.0, result.suggestedCrownCount(), 0.0001);
    }

    @Test
    void shouldRejectNegativeInnerWallLength() {
        LogHouseInput input = new LogHouseInput(
                "Невалидные внутренние стены",
                null,
                8.0,
                10.0,
                3.0,
                240,
                3.0,
                33_000,
                List.of(8.0, -1.0),
                GableType.NONE,
                null,
                null,
                WorkingHeightMode.AUTO,
                null,
                false,
                NotchType.IN_BOWL,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                0.0
        );

        assertThrows(IllegalArgumentException.class, () -> service.calculate(input));
    }

    @Test
    void shouldRequireGableHeightForLogGable() {
        LogHouseInput input = new LogHouseInput(
                "Фронтон без высоты",
                null,
                8.0,
                10.0,
                3.0,
                240,
                3.0,
                33_000,
                List.of(),
                GableType.LOG,
                null,
                2,
                WorkingHeightMode.AUTO,
                null,
                false,
                NotchType.IN_BOWL,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                0.0
        );

        assertThrows(IllegalArgumentException.class, () -> service.calculate(input));
    }
}
