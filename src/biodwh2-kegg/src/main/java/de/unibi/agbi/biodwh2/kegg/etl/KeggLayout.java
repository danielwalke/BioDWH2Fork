package de.unibi.agbi.biodwh2.kegg.etl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Lookup for the positions of modules'/pathways' elements in the KEGG diagrams, loaded from the layout files written
 * by {@link KeggUpdater}. An element can be drawn several times in one diagram, therefore edge properties are parallel
 * arrays with one entry per occurrence:
 * <ul>
 * <li>{@code layout}: "module_ko", "module_reaction" or "pathway" (which image the coordinates refer to)</li>
 * <li>{@code x}, {@code y}: element center in image pixels (origin top left)</li>
 * <li>{@code width}, {@code height}: element size in pixels</li>
 * </ul>
 */
final class KeggLayout {
    private final Map<String, List<double[]>> moduleElements = new HashMap<>();
    private final Map<String, List<double[]>> pathwayElements = new HashMap<>();

    static KeggLayout empty() {
        return new KeggLayout();
    }

    static KeggLayout load(final Path moduleLayoutPath, final Path pathwayLayoutPath) throws IOException {
        final KeggLayout layout = new KeggLayout();
        if (Files.exists(moduleLayoutPath)) {
            for (final String line : Files.readAllLines(moduleLayoutPath, StandardCharsets.UTF_8)) {
                // module_id, image, element_id, x, y, width, height
                final String[] row = line.split("\t");
                if (row.length == 7)
                    layout.add(layout.moduleElements, row[0] + "|" + row[1] + "|" + row[2], row, 3);
            }
        }
        if (Files.exists(pathwayLayoutPath)) {
            for (final String line : Files.readAllLines(pathwayLayoutPath, StandardCharsets.UTF_8)) {
                // pathway_id, entry_type, names, reactions, x, y, width, height
                final String[] row = line.split("\t", -1);
                if (row.length != 8 || "group".equals(row[1]))
                    continue;
                for (final String id : splitIds(row[2]))
                    layout.add(layout.pathwayElements, row[0] + "|" + id, row, 4);
                // the box of an enzyme entry is the position of its reactions as well
                for (final String id : splitIds(row[3]))
                    layout.add(layout.pathwayElements, row[0] + "|" + id, row, 4);
            }
        }
        return layout;
    }

    private static List<String> splitIds(final String names) {
        final List<String> ids = new ArrayList<>();
        for (final String name : names.trim().split("\\s+")) {
            if (name.isEmpty())
                continue;
            final int colon = name.indexOf(':');
            ids.add(colon >= 0 ? name.substring(colon + 1) : name);
        }
        return ids;
    }

    private void add(final Map<String, List<double[]>> target, final String key, final String[] row,
                     final int offset) {
        final double[] box = new double[4];
        for (int i = 0; i < 4; i++) {
            if (row[offset + i].isEmpty())
                return;
            try {
                box[i] = Double.parseDouble(row[offset + i]);
            } catch (NumberFormatException e) {
                return;
            }
        }
        final List<double[]> boxes = target.computeIfAbsent(key, k -> new ArrayList<>());
        for (final double[] existing : boxes)
            if (Arrays.equals(existing, box))
                return;
        boxes.add(box);
    }

    /**
     * @param image {@link KeggLayoutParser#IMAGE_KO} or {@link KeggLayoutParser#IMAGE_REACTION}
     */
    Map<String, Object> getModuleProperties(final String moduleId, final String image, final String elementId) {
        return toProperties("module_" + image, moduleElements.get(moduleId + "|" + image + "|" + elementId));
    }

    Map<String, Object> getPathwayProperties(final String pathwayId, final String elementId) {
        return toProperties("pathway", pathwayElements.get(pathwayId + "|" + elementId));
    }

    private static Map<String, Object> toProperties(final String layout, final List<double[]> boxes) {
        if (boxes == null || boxes.isEmpty())
            return Collections.emptyMap();
        final double[] x = new double[boxes.size()];
        final double[] y = new double[boxes.size()];
        final double[] width = new double[boxes.size()];
        final double[] height = new double[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            x[i] = boxes.get(i)[0];
            y[i] = boxes.get(i)[1];
            width[i] = boxes.get(i)[2];
            height[i] = boxes.get(i)[3];
        }
        final Map<String, Object> properties = new HashMap<>();
        properties.put("layout", layout);
        properties.put("x", x);
        properties.put("y", y);
        properties.put("width", width);
        properties.put("height", height);
        return properties;
    }
}
