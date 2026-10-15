package de.unibi.agbi.biodwh2.kegg.etl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the position of drawn elements (reactions, compounds, KOs) from KEGG diagrams. All coordinates are pixel
 * positions in the respective KEGG image, with the origin in the top left corner. {@code x, y} is the element center.
 * <ul>
 * <li>Modules: HTML image maps on {@code https://www.kegg.jp/module/Mxxxxx}. Map {@code module1} belongs to the
 * KO-level image, map {@code reaction1} to the reaction/compound-level image.</li>
 * <li>Pathways: KGML ({@code https://rest.kegg.jp/get/ko00010/kgml}).</li>
 * </ul>
 */
final class KeggLayoutParser {
    static final String IMAGE_KO = "ko";
    static final String IMAGE_REACTION = "reaction";
    private static final Pattern MAP_PATTERN = Pattern.compile("<map name=\"([^\"]+)\">(.*?)</map>", Pattern.DOTALL);
    private static final Pattern AREA_PATTERN = Pattern.compile("<area\\s+([^>]*?)/?>", Pattern.DOTALL);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private KeggLayoutParser() {
    }

    /**
     * @return rows of {image, elementId, x, y, width, height}
     */
    static List<String[]> parseModuleImageMaps(final String html) {
        final List<String[]> rows = new ArrayList<>();
        if (html == null)
            return rows;
        final Matcher maps = MAP_PATTERN.matcher(html);
        while (maps.find()) {
            final String image;
            if ("module1".equals(maps.group(1)))
                image = IMAGE_KO;
            else if ("reaction1".equals(maps.group(1)))
                image = IMAGE_REACTION;
            else
                continue;
            final Matcher areas = AREA_PATTERN.matcher(maps.group(2));
            while (areas.find()) {
                final String[] row = parseArea(image, areas.group(1));
                if (row != null)
                    rows.add(row);
            }
        }
        return rows;
    }

    private static String[] parseArea(final String image, final String attributes) {
        String href = null;
        String coords = null;
        final Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            if ("href".equals(matcher.group(1)))
                href = matcher.group(2);
            else if ("coords".equals(matcher.group(1)))
                coords = matcher.group(2);
        }
        if (href == null || coords == null)
            return null;
        final String[] values = coords.split(",");
        if (values.length < 4)
            return null;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        try {
            for (int i = 0; i + 1 < values.length; i += 2) {
                final double x = Double.parseDouble(values[i].trim());
                final double y = Double.parseDouble(values[i + 1].trim());
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        final String id = href.substring(href.lastIndexOf('/') + 1);
        return new String[]{
                image, id, format((minX + maxX) / 2), format((minY + maxY) / 2), format(maxX - minX),
                format(maxY - minY)
        };
    }

    /**
     * @return rows of {entryType, names (space separated, KEGG prefixes kept), reactions (space separated), x, y,
     * width, height}
     */
    static List<String[]> parseKgml(final String kgml) throws Exception {
        final List<String[]> rows = new ArrayList<>();
        if (kgml == null || kgml.isBlank())
            return rows;
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // the KGML DOCTYPE references an external DTD that must not be fetched
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        final Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(kgml)));
        final NodeList entries = document.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            final Element entry = (Element) entries.item(i);
            final NodeList graphics = entry.getElementsByTagName("graphics");
            if (graphics.getLength() == 0)
                continue;
            final Element graphic = (Element) graphics.item(0);
            if (graphic.getAttribute("x").isEmpty() || graphic.getAttribute("y").isEmpty())
                continue;
            rows.add(new String[]{
                    entry.getAttribute("type"), entry.getAttribute("name"), entry.getAttribute("reaction"),
                    graphic.getAttribute("x"), graphic.getAttribute("y"), graphic.getAttribute("width"),
                    graphic.getAttribute("height")
            });
        }
        return rows;
    }

    private static String format(final double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
