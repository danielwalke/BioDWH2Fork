package de.unibi.agbi.biodwh2.cazy.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterConnectionException;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.cazy.CazyDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CazyUpdater extends Updater<CazyDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(CazyUpdater.class);

    private static final String DATA_URL = "https://www.cazy.org/IMG/cazy_data/cazy_data.zip";
    static final String DATA_FILE_NAME = "cazy_data.zip";
    static final String EC_FILE_NAME = "cazy_ec_numbers.tsv";

    private static final String CAZY_BASE_URL = "https://www.cazy.org/";
    private static final Pattern EC_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
    private static final long REQUEST_DELAY_MS = 500;

    public CazyUpdater(final CazyDataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected Version getNewestVersion(final Workspace workspace) throws UpdaterException {
        String versionStr = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://www.cazy.org").openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            final int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                final String source = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                final Pattern pattern = Pattern.compile("Last update: (\\d{4}-\\d{2}-\\d{2})");
                final Matcher matcher = pattern.matcher(source);
                if (matcher.find()) {
                    versionStr = matcher.group(1);
                }
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        if (versionStr == null) {
            return Version.tryParse(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return Version.tryParse(versionStr);
    }

    @Override
    protected boolean tryUpdateFiles(final Workspace workspace) throws UpdaterException {
        downloadCAZyData(workspace);
        scrapeECNumbers(workspace);
        return true;
    }

    private void downloadCAZyData(final Workspace workspace) throws UpdaterException {
        try {
            downloadFileAsBrowser(workspace, DATA_URL, DATA_FILE_NAME);
        } catch (UpdaterException e) {
            throw new UpdaterException("Failed to download CAZy data", e);
        }
    }

    /**
     * Scrape EC numbers from each family page on the CAZy website. Each family page (e.g. GH1.html)
     * contains an activities table with EC numbers in {@code <td id="separateur2">} cells.
     * Family IDs are extracted from the downloaded bulk data ZIP to know which pages to fetch.
     */
    private void scrapeECNumbers(final Workspace workspace) throws UpdaterException {
        final Path filePath = dataSource.resolveSourceFilePath(workspace, EC_FILE_NAME);
        try {
            final Set<String> familyIds = extractFamilyIdsFromData(workspace);
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Found " + familyIds.size() + " enzymatic families to scrape for EC numbers");

            final StringBuilder tsv = new StringBuilder();
            tsv.append("class\tfamily\tec_number\tactivity_name\n");

            int scraped = 0;
            int totalEC = 0;
            for (final String familyId : familyIds) {
                final String classId = extractClassId(familyId);
                if (classId == null)
                    continue;

                try {
                    final List<String[]> ecEntries = scrapeFamilyECNumbers(familyId);
                    for (final String[] entry : ecEntries) {
                        tsv.append(classId).append('\t');
                        tsv.append(familyId).append('\t');
                        tsv.append(entry[0]).append('\t');
                        tsv.append(entry[1]).append('\n');
                        totalEC++;
                    }
                    scraped++;
                    if (scraped % 50 == 0 && LOGGER.isInfoEnabled())
                        LOGGER.info("Scraped EC numbers from " + scraped + "/" + familyIds.size() +
                                    " families (" + totalEC + " EC entries so far)");
                    Thread.sleep(REQUEST_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (LOGGER.isWarnEnabled())
                        LOGGER.warn("Failed to scrape EC numbers for " + familyId + ": " + e.getMessage());
                }
            }

            if (LOGGER.isInfoEnabled())
                LOGGER.info("EC number scraping complete: " + totalEC + " entries from " + scraped + " families");
            Files.writeString(filePath, tsv.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UpdaterException("Failed to write EC numbers file", e);
        }
    }

    /**
     * Extract unique enzymatic family IDs (GH*, GT*, PL*, CE*, AA*) from the downloaded bulk data.
     * CBM families are excluded as they are binding modules without enzymatic activity.
     */
    private Set<String> extractFamilyIdsFromData(final Workspace workspace) throws IOException {
        final Set<String> familyIds = new TreeSet<>();
        final Path zipPath = dataSource.resolveSourceFilePath(workspace, DATA_FILE_NAME);
        try (final ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".txt"))
                    continue;
                // Do NOT wrap in try-with-resources: closing the reader would close the ZipInputStream
                final BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;
                    final String[] cols = line.split("\t", 2);
                    final String family = cols[0].trim();
                    if (family.startsWith("GH") || family.startsWith("GT") || family.startsWith("PL") ||
                        family.startsWith("CE") || family.startsWith("AA")) {
                        familyIds.add(family);
                    }
                }
                zipStream.closeEntry();
            }
        }
        return familyIds;
    }

    /**
     * Scrape EC numbers from a single family page. The activities table on each family page
     * has rows with {@code <td id="separateur2">} cells where the second cell is the EC number
     * and the third cell is the activity name.
     *
     * @return list of [ec_number, activity_name] pairs
     */
    private List<String[]> scrapeFamilyECNumbers(final String familyId) throws IOException {
        final List<String[]> results = new ArrayList<>();
        final String url = CAZY_BASE_URL + familyId + ".html";
        String html;
        try {
            html = getWebsiteSource(url);
        } catch (UpdaterConnectionException e) {
            return results;
        }
        if (html == null || html.isEmpty())
            return results;

        final Document doc = Jsoup.parse(html);
        final Elements rows = doc.select("tr");
        for (final Element row : rows) {
            final Elements cells = row.select("td[id=separateur2]");
            if (cells.size() < 3)
                continue;

            final String ecText = cells.get(1).text().trim();
            final String activityText = cells.get(2).text().trim();

            final Matcher ecMatcher = EC_PATTERN.matcher(ecText);
            if (ecMatcher.matches()) {
                results.add(new String[]{ecText, activityText});
            }
        }
        return results;
    }

    private static String extractClassId(final String familyId) {
        if (familyId.startsWith("GH"))
            return "GH";
        if (familyId.startsWith("GT"))
            return "GT";
        if (familyId.startsWith("PL"))
            return "PL";
        if (familyId.startsWith("CE"))
            return "CE";
        if (familyId.startsWith("AA"))
            return "AA";
        return null;
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{DATA_FILE_NAME, EC_FILE_NAME};
    }
}
