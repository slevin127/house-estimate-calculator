package ru.kelohonka.estimate;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import ru.kelohonka.estimate.model.CrossCut;
import ru.kelohonka.estimate.model.EstimateResult;
import ru.kelohonka.estimate.model.GableInput;
import ru.kelohonka.estimate.model.GableType;
import ru.kelohonka.estimate.model.LogHouseInput;
import ru.kelohonka.estimate.model.OpeningItem;
import ru.kelohonka.estimate.model.OpeningType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports estimate results to DOCX.
 */
public class EstimateWordExporter {

    public void export(Path outputPath, List<EstimateBundle> estimates) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Demo house estimates");

            for (int i = 0; i < estimates.size(); i++) {
                EstimateBundle bundle = estimates.get(i);
                addEstimate(document, bundle.input(), bundle.result());
                if (i < estimates.size() - 1) {
                    document.createParagraph();
                }
            }

            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                document.write(outputStream);
            }
        }
    }

    private void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(16);
        run.setText(text);
    }

    private void addEstimate(XWPFDocument document, LogHouseInput input, EstimateResult result) {
        List<OpeningItem> openings = input.openings() == null ? List.of() : input.openings();
        List<CrossCut> crossCuts = input.crossCuts() == null ? List.of() : input.crossCuts();
        List<GableInput> gables = input.gables() == null ? List.of() : input.gables();

        List<OpeningItem> windows = openings.stream()
                .filter(opening -> opening.type() == OpeningType.WINDOW)
                .toList();
        List<OpeningItem> doors = openings.stream()
                .filter(opening -> opening.type() == OpeningType.DOOR)
                .toList();
        int crossCutCount = crossCuts.stream()
                .mapToInt(CrossCut::quantity)
                .sum();

        addLine(document, input.projectName(), true);
        addLine(document, String.format("Log diameter: %d mm", input.logDiameterMm()), false);
        addLine(document, String.format("Wall height: %.2f m", input.wallHeightMeters()), false);
        addLine(document, String.format("Working crown height: %.3f m", result.workingHeightMeters()), false);
        addLine(document, String.format("Crown count: %.0f", result.suggestedCrownCount()), false);
        addLine(document, String.format("Calculated wall height: %.2f m", result.logHeightMeters()), false);

        addLine(document, "Gable type: " + input.gableType(), false);
        if (input.gableType() == GableType.LOG) {
            if (!gables.isEmpty()) {
                addLine(document, "Gables:", false);
                for (GableInput gable : gables) {
                    addLine(document, String.format("  %s: %.2f x %.2f m",
                            gable.name(), gable.lengthMeters(), gable.heightMeters()), false);
                }
            } else {
                addLine(document, String.format("Gables (length x height): %.2f x %.2f m, count: %d",
                        input.gableLengthMeters(), input.gableHeightMeters(), input.gableCount()), false);
            }
        }

        addLine(document, String.format("Outer wall length: %.2f m", result.outerWallLengthMeters()), false);
        addLine(document, String.format("Inner wall length: %.2f m", result.innerWallLengthMeters()), false);
        addLine(document, String.format("Total wall length: %.2f m", result.totalWallLengthMeters()), false);
        addLine(document, String.format("Total log length: %.2f m", result.totalLogLengthMeters()), false);
        addLine(document, String.format("Log section area: %.4f m2", result.logSectionAreaSquareMeters()), false);
        addLine(document, String.format("Cross cuts: %d", crossCutCount), false);
        addLine(document, "Windows: " + windows.size() + formatOpenings(windows), false);
        addLine(document, "Doors: " + doors.size() + formatOpenings(doors), false);
        addLine(document, String.format("Gross wall area: %.3f m2", result.grossWallAreaSquareMeters()), false);
        addLine(document, String.format("Openings area: %.3f m2", result.openingsAreaSquareMeters()), false);
        addLine(document, String.format("Net wall area: %.3f m2", result.netWallAreaSquareMeters()), false);
        addLine(document, String.format("Wall volume: %.3f m3", result.wallVolumeCubicMeters()), false);
        addLine(document, String.format("Gable area: %.3f m2", result.gableAreaSquareMeters()), false);
        addLine(document, String.format("Gable volume: %.3f m3", result.gableVolumeCubicMeters()), false);
        addLine(document, String.format("Openings volume: %.3f m3", result.openingsVolumeCubicMeters()), false);
        addLine(document, String.format("Net volume before cross cuts: %.3f m3", result.netVolumeCubicMeters()), false);
        addLine(document, String.format("Cross-cut volume: %.3f m3", result.crossCutVolumeCubicMeters()), false);
        addLine(document, String.format("Total log volume: %.3f m3", result.totalLogVolumeCubicMeters()), false);
        addLine(document, String.format("Total log volume with waste: %.3f m3", result.totalLogVolumeWithWasteCubicMeters()), false);
        addLine(document, String.format("Timber cost: %.2f", result.timberCost()), false);
        addLine(document, String.format("Ceiling board: %.3f m3, cost %.2f",
                result.ceilingBoardVolumeCubicMeters(), result.ceilingBoardCost()), false);
        addLine(document, String.format("Floor board: %.3f m3, cost %.2f",
                result.floorBoardVolumeCubicMeters(), result.floorBoardCost()), false);
        addLine(document, String.format("Roof cost: %.2f", result.roofCost()), false);
        addLine(document, String.format("Stove cost: %.2f", result.stoveCost()), false);
        addLine(document, String.format("Terrace cost: %.2f", result.terraceCost()), false);
        addLine(document, String.format("TOTAL: %.2f", result.grandTotal()), true);
    }

    private String formatOpenings(List<OpeningItem> openings) {
        if (openings.isEmpty()) {
            return "";
        }
        return " (" + openings.stream()
                .map(opening -> String.format("%.2f x %.2f m", opening.widthMeters(), opening.heightMeters()))
                .collect(Collectors.joining(", ")) + ")";
    }

    private void addLine(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setText(text);
    }

    /**
     * Input + result pair for export.
     */
    public record EstimateBundle(LogHouseInput input, EstimateResult result) {
    }
}
