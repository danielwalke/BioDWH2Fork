package de.unibi.agbi.biodwh2.refseq.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterConnectionException;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.refseq.RefSeqDataSource;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RefSeqUpdater extends Updater<RefSeqDataSource> {
    private static final String ASSEMBLY_SUMMARY_URL = "https://ftp.ncbi.nlm.nih.gov/genomes/refseq/assembly_summary_refseq.txt";
    private static final String ID_MAPPING_URL = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/idmapping_selected.tab.gz";

    public RefSeqUpdater(final RefSeqDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Version getNewestVersion(final Workspace workspace) throws UpdaterException {
        // Force update by returning a version slightly newer than what's normally expected if needed,
        // or just let it update if version is missing. For simplicity, we return current date components.
        java.time.LocalDate now = java.time.LocalDate.now();
        return new Version(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    }

    @Override
    protected boolean tryUpdateFiles(final Workspace workspace) throws UpdaterException {
        try {
            downloadFileAsBrowser(workspace, ASSEMBLY_SUMMARY_URL, "assembly_summary_refseq.txt");
            downloadFileAsBrowser(workspace, ID_MAPPING_URL, "idmapping_selected.tab.gz");

            Path summaryPath = dataSource.resolveSourceFilePath(workspace, "assembly_summary_refseq.txt");
            String catProp = dataSource.getStringProperty(workspace, "categories");
            if (catProp == null) catProp = "bacteria,archaea,fungi,viral,protozoa,vertebrate_mammalian";
            Set<String> targetCategories = new HashSet<>(Arrays.asList(StringUtils.split(catProp, ',')));

            try (BufferedReader reader = Files.newBufferedReader(summaryPath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(line, "\t");
                    if (parts.length > 20) {
                        String refseqCategory = parts[4];
                        String group = parts[24];
                        String ftpPath = parts[19];
                        if ("reference genome".equals(refseqCategory) && targetCategories.contains(group)) {
                            if (!"na".equals(ftpPath)) {
                                if (ftpPath.endsWith("/")) {
                                    ftpPath = ftpPath.substring(0, ftpPath.length() - 1);
                                }
                                String folderName = ftpPath.substring(ftpPath.lastIndexOf('/') + 1);
                                String fileUrl = ftpPath + "/" + folderName + "_genomic.gff.gz";
                                String fileName = folderName + "_genomic.gff.gz";
                                Path targetPath = dataSource.resolveSourceFilePath(workspace, fileName);
                                if (!Files.exists(targetPath)) {
                                    System.out.println("Downloading " + fileName + "...");
                                    downloadFileAsBrowser(workspace, fileUrl, fileName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new UpdaterConnectionException(e);
        }
        return true;
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{"assembly_summary_refseq.txt", "idmapping_selected.tab.gz"};
    }
}
