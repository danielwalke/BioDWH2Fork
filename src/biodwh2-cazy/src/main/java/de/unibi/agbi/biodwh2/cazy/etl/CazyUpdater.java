package de.unibi.agbi.biodwh2.cazy.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.cazy.CazyDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CazyUpdater extends Updater<CazyDataSource> {
    private static final String DATA_URL = "https://www.cazy.org/IMG/cazy_data/cazy_data.zip";
    static final String DATA_FILE_NAME = "cazy_data.zip";
    static final String EC_FILE_NAME = "cazy_ec_numbers.tsv";

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
        downloadECNumbers(workspace);
        return true;
    }

    private void downloadCAZyData(final Workspace workspace) throws UpdaterException {
        try {
            final var filePath = dataSource.resolveSourceFilePath(workspace, DATA_FILE_NAME);
            downloadFileAsBrowser(workspace, DATA_URL, DATA_FILE_NAME);
        } catch (UpdaterException e) {
            throw new UpdaterException("Failed to download CAZy data", e);
        }
    }

    private void downloadECNumbers(final Workspace workspace) throws UpdaterException {
        final var filePath = dataSource.resolveSourceFilePath(workspace, EC_FILE_NAME);
        try {
            final var ecContent = fetchECNumbers();
            if (ecContent != null && !ecContent.isEmpty()) {
                Files.writeString(filePath, ecContent, StandardCharsets.UTF_8);
            } else {
                Files.writeString(filePath, "class\tfamily\tactivity_id\tec_number\tactivity_name\n",
                                  StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UpdaterException("Failed to download EC numbers", e);
        }
    }

    private String fetchECNumbers() throws IOException {
        for (final String url : new String[]{
                "https://raw.githubusercontent.com/carvajal-cazy/cazy_ec_numbers/main/cazy_ec_numbers.tsv",
                "https://www.cazy.org/IMG/cazy_data/cazy_data_20260414.txt"
        }) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                final int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    conn.disconnect();
                    continue;
                }
                final byte[] data = conn.getInputStream().readAllBytes();
                if (data != null && data.length > 0) {
                    final String content = new String(data, StandardCharsets.UTF_8);
                    if (!content.trim().isEmpty()) {
                        conn.disconnect();
                        return content;
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{DATA_FILE_NAME, EC_FILE_NAME};
    }
}
