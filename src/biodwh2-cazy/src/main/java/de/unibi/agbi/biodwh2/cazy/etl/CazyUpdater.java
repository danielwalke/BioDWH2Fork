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
import java.io.BufferedWriter;
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
    static final String STRUCTURES_FILE_NAME = "cazy_structures.tsv";

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
                final Pattern pattern = Pattern.compile("Last update:.*?(\\d{4}-\\d{2}-\\d{2})");
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
        scrapeCazyStructures(workspace);
        return true;
    }

    private void downloadCAZyData(final Workspace workspace) throws UpdaterException {
        try {
            downloadFileAsBrowser(workspace, DATA_URL, DATA_FILE_NAME);
        } catch (UpdaterException e) {
            throw new UpdaterException("Failed to download CAZy data", e);
        }
    }

    private void scrapeCazyStructures(final Workspace workspace) throws UpdaterException {
        final Path structuresPath = dataSource.resolveSourceFilePath(workspace, STRUCTURES_FILE_NAME);
        try {
            final Set<String> familyIds = extractFamilyIdsFromData(workspace);
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Found " + familyIds.size() + " CAZy families to scrape for structure information");

            try (final BufferedWriter writer = Files.newBufferedWriter(structuresPath, StandardCharsets.UTF_8)) {
                writer.write("family\tprotein_name\tec\torganism\tgenbank\tuniprot\tpdb\tligands\n");

                int scraped = 0;
                int totalEntries = 0;
                for (final String familyId : familyIds) {
                    try {
                        final List<String[]> entries = scrapeFamilyStructures(familyId);
                        for (final String[] entry : entries) {
                            writer.write(familyId + "\t" +
                                         entry[0] + "\t" +
                                         entry[1] + "\t" +
                                         entry[2] + "\t" +
                                         entry[3] + "\t" +
                                         entry[4] + "\t" +
                                         entry[5] + "\t" +
                                         entry[6] + "\n");
                            totalEntries++;
                        }
                        scraped++;
                        if (scraped % 50 == 0 && LOGGER.isInfoEnabled()) {
                            LOGGER.info("Scraped structures from " + scraped + "/" + familyIds.size() +
                                        " families (" + totalEntries + " entries so far)");
                        }
                        Thread.sleep(REQUEST_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        if (LOGGER.isWarnEnabled())
                            LOGGER.warn("Failed to scrape structures for " + familyId + ": " + e.getMessage());
                    }
                }
                if (LOGGER.isInfoEnabled())
                    LOGGER.info("Structure scraping complete: " + totalEntries + " entries from " + scraped + " families");
            }
        } catch (IOException e) {
            throw new UpdaterException("Failed to write structures TSV file", e);
        }
    }

    private Set<String> extractFamilyIdsFromData(final Workspace workspace) throws IOException {
        final Set<String> familyIds = new TreeSet<>();
        final Path zipPath = dataSource.resolveSourceFilePath(workspace, DATA_FILE_NAME);
        try (final ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".txt"))
                    continue;
                final BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;
                    final String[] cols = line.split("\t", 2);
                    final String family = cols[0].trim();
                    if (family.startsWith("GH") || family.startsWith("GT") || family.startsWith("PL") ||
                        family.startsWith("CE") || family.startsWith("AA") || family.startsWith("CBM")) {
                        familyIds.add(family);
                    }
                }
                zipStream.closeEntry();
            }
        }
        return familyIds;
    }

    private List<String[]> scrapeFamilyStructures(final String familyId) throws IOException {
        final List<String[]> results = new ArrayList<>();
        final String url = CAZY_BASE_URL + familyId + "_structure.html";
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
            final Elements cells = row.children();
            if (cells.size() >= 6 && "separateur2".equals(cells.get(0).id())) {
                final String proteinName = cleanText(cells.get(0).text());
                final String ecNumbers = extractECNumbers(cells.get(1));
                final String organism = cleanText(cells.get(2).text());
                final String genbankIds = extractAccessions(cells.get(3));
                final String uniprotIds = extractAccessions(cells.get(4));
                final List<String[]> pdbLigands = extractPDBAndLigands(cells.get(5));

                final List<String> pdbs = new ArrayList<>();
                final List<String> ligands = new ArrayList<>();
                for (final String[] pair : pdbLigands) {
                    if (!pair[0].isEmpty()) pdbs.add(pair[0]);
                    if (!pair[1].isEmpty()) ligands.add(pair[1]);
                }
                final String pdbJoined = String.join("; ", pdbs);
                final String ligandsJoined = String.join("; ", ligands);

                results.add(new String[]{
                    proteinName, ecNumbers, organism, genbankIds, uniprotIds, pdbJoined, ligandsJoined
                });
            }
        }
        return results;
    }

    private String cleanText(final String text) {
        if (text == null)
            return "";
        // Replace non-breaking spaces and trim
        return text.replace("\u00a0", " ").replace("&nbsp;", " ").trim();
    }

    private String extractECNumbers(final Element cell) {
        final List<String> ecs = new ArrayList<>();
        final Matcher matcher = EC_PATTERN.matcher(cell.text());
        while (matcher.find()) {
            ecs.add(matcher.group());
        }
        return String.join("; ", ecs);
    }

    private String extractAccessions(final Element cell) {
        final List<String> ids = new ArrayList<>();
        final String html = cell.html();
        final String[] parts = html.split("(?i)<br\\s*/?>");
        for (final String part : parts) {
            final String text = Jsoup.parse(part).text().replace("\u00a0", " ").replace("&nbsp;", " ").trim();
            if (!text.isEmpty()) {
                ids.add(text);
            }
        }
        return String.join("; ", ids);
    }

    private List<String[]> extractPDBAndLigands(final Element cell) {
        final List<String[]> list = new ArrayList<>();
        final Elements subRows = cell.select("tr");
        for (final Element subRow : subRows) {
            final Elements subCells = subRow.children();
            if (subCells.size() >= 2) {
                final String pdb = cleanText(subCells.get(0).text());
                final String ligand = cleanText(subCells.get(1).text());
                if (!pdb.isEmpty() || !ligand.isEmpty()) {
                    list.add(new String[]{pdb, ligand});
                }
            }
        }
        // Fallback in case there is no nested table or parsing fails
        if (list.isEmpty()) {
            final String text = cleanText(cell.text());
            if (!text.isEmpty()) {
                list.add(new String[]{text, ""});
            }
        }
        return list;
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{DATA_FILE_NAME, STRUCTURES_FILE_NAME, "prot.accession2taxid"};
    }
}
