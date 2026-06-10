package de.unibi.agbi.biodwh2.kegg.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterConnectionException;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.core.net.AnonymousFTPClient;
import de.unibi.agbi.biodwh2.kegg.KeggDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KeggUpdater extends Updater<KeggDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(KeggUpdater.class);
    static final String DGROUP_FILE_NAME = "dgroup";
    static final String DRUG_FILE_NAME = "drug";
    static final String DISEASE_FILE_NAME = "disease";
    static final String NETWORK_FILE_NAME = "network";
    static final String VARIANT_FILE_NAME = "variant";
    private static final String FTP_BASE_PATH = "pub/kegg/medicus/";
    private static final String HUMAN_GENES_LIST_URL = "https://rest.kegg.jp/list/hsa";
    static final String HUMAN_GENES_LIST_FILE_NAME = "human_genes_list.tsv";
    private static final String COMPOUNDS_LIST_URL = "https://rest.kegg.jp/list/compound";
    static final String COMPOUNDS_LIST_FILE_NAME = "compounds_list.tsv";
    private static final String ORGANISMS_LIST_URL = "https://rest.kegg.jp/list/organism";
    static final String ORGANISMS_LIST_FILE_NAME = "organisms_list.tsv";

    private static final String REACTIONS_LIST_URL = "https://rest.kegg.jp/list/reaction";
    static final String REACTIONS_LIST_FILE_NAME = "reactions_list.tsv";
    private static final String MODULES_LIST_URL = "https://rest.kegg.jp/list/module";
    static final String MODULES_LIST_FILE_NAME = "modules_list.tsv";
    private static final String ORTHOLOGY_LIST_URL = "https://rest.kegg.jp/list/ko";
    static final String ORTHOLOGY_LIST_FILE_NAME = "orthology_list.tsv";
    private static final String PATHWAYS_LIST_URL = "https://rest.kegg.jp/list/pathway";
    static final String PATHWAYS_LIST_FILE_NAME = "pathways_list.tsv";
    private static final String ENZYMES_LIST_URL = "https://rest.kegg.jp/list/enzyme";
    static final String ENZYMES_LIST_FILE_NAME = "enzymes_list.tsv";
    private static final String BRITE_LIST_URL = "https://rest.kegg.jp/list/brite";
    static final String BRITE_LIST_FILE_NAME = "brite_list.tsv";
    private static final String GLYCAN_LIST_URL = "https://rest.kegg.jp/list/glycan";
    static final String GLYCAN_LIST_FILE_NAME = "glycan_list.tsv";
    private static final String RCLASS_LIST_URL = "https://rest.kegg.jp/list/rclass";
    static final String RCLASS_LIST_FILE_NAME = "rclass_list.tsv";

    private static final String REACTION_COMPOUND_URL = "https://rest.kegg.jp/link/reaction/compound";
    static final String REACTION_COMPOUND_FILE_NAME = "reaction_compound.tsv";
    private static final String REACTION_KO_URL = "https://rest.kegg.jp/link/reaction/ko";
    static final String REACTION_KO_FILE_NAME = "reaction_ko.tsv";
    private static final String MODULE_REACTION_URL = "https://rest.kegg.jp/link/module/reaction";
    static final String MODULE_REACTION_FILE_NAME = "module_reaction.tsv";
    private static final String MODULE_KO_URL = "https://rest.kegg.jp/link/module/ko";
    static final String MODULE_KO_FILE_NAME = "module_ko.tsv";
    private static final String MODULE_COMPOUND_URL = "https://rest.kegg.jp/link/module/compound";
    static final String MODULE_COMPOUND_FILE_NAME = "module_compound.tsv";
    private static final String PATHWAY_KO_URL = "https://rest.kegg.jp/link/pathway/ko";
    static final String PATHWAY_KO_FILE_NAME = "pathway_ko.tsv";
    private static final String PATHWAY_COMPOUND_URL = "https://rest.kegg.jp/link/pathway/compound";
    static final String PATHWAY_COMPOUND_FILE_NAME = "pathway_compound.tsv";
    private static final String PATHWAY_MODULE_URL = "https://rest.kegg.jp/link/pathway/module";
    static final String PATHWAY_MODULE_FILE_NAME = "pathway_module.tsv";
    private static final String DISEASE_DRUG_URL = "https://rest.kegg.jp/link/disease/drug";
    static final String DISEASE_DRUG_FILE_NAME = "disease_drug.tsv";
    private static final String DISEASE_HSA_URL = "https://rest.kegg.jp/link/disease/hsa";
    static final String DISEASE_HSA_FILE_NAME = "disease_hsa.tsv";
    private static final String REACTION_ENZYME_URL = "https://rest.kegg.jp/link/reaction/enzyme";
    static final String REACTION_ENZYME_FILE_NAME = "reaction_enzyme.tsv";
    private static final String UNIPROT_HSA_URL = "https://rest.kegg.jp/conv/uniprot/hsa";
    static final String UNIPROT_HSA_FILE_NAME = "uniprot_hsa.tsv";
    private static final String NCBI_PROTEINID_HSA_URL = "https://rest.kegg.jp/conv/ncbi-proteinid/hsa";
    static final String NCBI_PROTEINID_HSA_FILE_NAME = "ncbi_proteinid_hsa.tsv";
    private static final String KO_HSA_URL = "https://rest.kegg.jp/link/ko/hsa";
    static final String KO_HSA_FILE_NAME = "ko_hsa.tsv";
    private static final String PATHWAY_REACTION_URL = "https://rest.kegg.jp/link/pathway/reaction";
    static final String PATHWAY_REACTION_FILE_NAME = "pathway_reaction.tsv";
    private static final String TAXONOMY_GENOME_URL = "https://rest.kegg.jp/link/taxonomy/genome";
    static final String TAXONOMY_GENOME_FILE_NAME = "taxonomy_genome.tsv";
    private static final String PATHWAY_GENOME_URL = "https://rest.kegg.jp/link/pathway/genome";
    static final String PATHWAY_GENOME_FILE_NAME = "pathway_genome.tsv";
    private static final String PATHWAY_HSA_URL = "https://rest.kegg.jp/link/pathway/hsa";
    static final String PATHWAY_HSA_FILE_NAME = "pathway_hsa.tsv";
    // Aggregated per-organism downloads (list/<org> and link/pathway/<org>)
    static final String GENES_PER_ORGANISM_FILE_NAME = "genes_per_organism.tsv";
    static final String PATHWAY_PER_ORGANISM_FILE_NAME = "pathway_per_organism.tsv";
    // Persistent skip-list of organism symbols that KEGG's free public REST API refuses (HTTP 400).
    // Populated lazily on the first run; afterwards these organisms are skipped without any HTTP call.
    static final String RESTRICTED_ORGANISMS_FILE_NAME = "restricted_organisms.txt";
    static final String REACTIONS_FILE_NAME = "reactions.txt";
    static final String MODULES_FILE_NAME = "modules.txt";
    static final String PATHWAYS_FILE_NAME = "pathways.txt";
    static final String ORGANISMS_FILE_NAME = "organisms.txt";

    public KeggUpdater(KeggDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Version getNewestVersion(final Workspace workspace) throws UpdaterException {
        AnonymousFTPClient ftpClient = connectToFTP();
        List<LocalDateTime> folderDateTimes = new ArrayList<>();
        folderDateTimes.add(ftpClient.getModificationTimeFromServer(FTP_BASE_PATH + "dgroup/dgroup"));
        folderDateTimes.add(ftpClient.getModificationTimeFromServer(FTP_BASE_PATH + "disease/disease"));
        folderDateTimes.add(ftpClient.getModificationTimeFromServer(FTP_BASE_PATH + "drug/drug"));
        folderDateTimes.add(ftpClient.getModificationTimeFromServer(FTP_BASE_PATH + "network/network"));
        folderDateTimes.add(ftpClient.getModificationTimeFromServer(FTP_BASE_PATH + "network/variant"));
        ftpClient.tryDisconnect();
        Version newestVersion = null;
        for (LocalDateTime dateTime : folderDateTimes) {
            Version dateTimeVersion = dateTime != null ? convertDateTimeToVersion(dateTime) : null;
            if (dateTimeVersion != null && dateTimeVersion.compareTo(newestVersion) >= 0)
                newestVersion = dateTimeVersion;
        }
        return newestVersion;
    }

    private AnonymousFTPClient connectToFTP() throws UpdaterConnectionException {
        AnonymousFTPClient ftpClient = new AnonymousFTPClient();
        boolean isConnected;
        try {
            isConnected = ftpClient.connect("ftp.genome.jp");
        } catch (IOException e) {
            throw new UpdaterConnectionException(e);
        }
        if (!isConnected)
            throw new UpdaterConnectionException();
        return ftpClient;
    }

    @Override
    protected boolean tryUpdateFiles(Workspace workspace) throws UpdaterException {
        AnonymousFTPClient ftpClient = connectToFTP();
        boolean success = updateFile(workspace, dataSource, ftpClient, "dgroup/" + DGROUP_FILE_NAME);
        success = success && updateFile(workspace, dataSource, ftpClient, "disease/" + DISEASE_FILE_NAME);
        success = success && updateFile(workspace, dataSource, ftpClient, "drug/" + DRUG_FILE_NAME);
        success = success && updateFile(workspace, dataSource, ftpClient, "network/" + NETWORK_FILE_NAME);
        success = success && updateFile(workspace, dataSource, ftpClient, "network/" + VARIANT_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, HUMAN_GENES_LIST_URL, HUMAN_GENES_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, COMPOUNDS_LIST_URL, COMPOUNDS_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, ORGANISMS_LIST_URL, ORGANISMS_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, REACTIONS_LIST_URL, REACTIONS_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, MODULES_LIST_URL, MODULES_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, ORTHOLOGY_LIST_URL, ORTHOLOGY_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAYS_LIST_URL, PATHWAYS_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, ENZYMES_LIST_URL, ENZYMES_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, BRITE_LIST_URL, BRITE_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, RCLASS_LIST_URL, RCLASS_LIST_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, REACTION_COMPOUND_URL, REACTION_COMPOUND_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, REACTION_KO_URL, REACTION_KO_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, MODULE_REACTION_URL, MODULE_REACTION_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, MODULE_KO_URL, MODULE_KO_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, MODULE_COMPOUND_URL, MODULE_COMPOUND_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAY_KO_URL, PATHWAY_KO_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAY_COMPOUND_URL, PATHWAY_COMPOUND_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAY_MODULE_URL, PATHWAY_MODULE_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, DISEASE_DRUG_URL, DISEASE_DRUG_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, DISEASE_HSA_URL, DISEASE_HSA_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, REACTION_ENZYME_URL, REACTION_ENZYME_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, UNIPROT_HSA_URL, UNIPROT_HSA_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, NCBI_PROTEINID_HSA_URL, NCBI_PROTEINID_HSA_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, KO_HSA_URL, KO_HSA_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAY_REACTION_URL, PATHWAY_REACTION_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, TAXONOMY_GENOME_URL, TAXONOMY_GENOME_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAY_GENOME_URL, PATHWAY_GENOME_FILE_NAME);
        tryDownloadFileAsBrowser(workspace, PATHWAY_HSA_URL, PATHWAY_HSA_FILE_NAME);
        downloadKeggEntries(workspace, REACTIONS_LIST_FILE_NAME, REACTIONS_FILE_NAME);
        downloadKeggEntries(workspace, MODULES_LIST_FILE_NAME, MODULES_FILE_NAME);
        downloadKeggEntries(workspace, PATHWAYS_LIST_FILE_NAME, PATHWAYS_FILE_NAME);
        // downloadKeggEntries(workspace, ORGANISMS_LIST_FILE_NAME, ORGANISMS_FILE_NAME);
        // downloadPerOrganismLinks(workspace);
        return success;
    }

    private void tryDownloadFileAsBrowser(final Workspace workspace, final String url, final String fileName) {
        try {
            downloadFileAsBrowser(workspace, url, fileName);
        } catch (UpdaterException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Failed to download " + fileName + " from " + url, e);
        }
    }

    /**
     * Iterates the organisms_list.tsv and aggregates the per-organism KEGG REST endpoints
     * `list/<org>` and `link/pathway/<org>` into two TSV files:
     *  - genes_per_organism.tsv: <org>\t<gene_id>\t<name>   (one row per gene)
     *  - pathway_per_organism.tsv: <gene_id>\t<pathway_id>  (e.g. hsa:10327 \t path:hsa00010)
     * Throttled at 200 ms per request to avoid hammering the KEGG REST API.
     * Resumes by skipping already-processed organism prefixes if files exist.
     */
    private void downloadPerOrganismLinks(final Workspace workspace) {
        final String organismsListPath = dataSource.resolveSourceFilePath(workspace, ORGANISMS_LIST_FILE_NAME).toString();
        if (!new java.io.File(organismsListPath).exists())
            return;
        final String genesOutPath = dataSource.resolveSourceFilePath(workspace, GENES_PER_ORGANISM_FILE_NAME).toString();
        final String pathwayOutPath = dataSource.resolveSourceFilePath(workspace, PATHWAY_PER_ORGANISM_FILE_NAME).toString();

        // Collect already-processed organism symbols to allow resuming an interrupted run
        final java.util.Set<String> processedGenes = readProcessedSymbols(genesOutPath, 0);
        final java.util.Set<String> processedPathways = readProcessedPathwaySymbols(pathwayOutPath);

        // Load persistent skip-list of organisms KEGG's free REST API refuses (HTTP 400 on a previous run).
        // These are silently skipped without any HTTP request on this and future runs.
        final String restrictedPath = dataSource.resolveSourceFilePath(workspace, RESTRICTED_ORGANISMS_FILE_NAME).toString();
        final java.util.Set<String> restrictedOrganisms = readProcessedSymbols(restrictedPath, 0);

        final java.util.List<String> organismSymbols = new ArrayList<>();
        try {
            for (final String line : java.nio.file.Files.readAllLines(Paths.get(organismsListPath))) {
                final String[] parts = org.apache.commons.lang3.StringUtils.split(line, '\t');
                if (parts.length >= 2 && parts[1] != null && !parts[1].isEmpty())
                    organismSymbols.add(parts[1].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (java.io.FileWriter genesWriter = new java.io.FileWriter(genesOutPath, true);
             java.io.FileWriter pathwayWriter = new java.io.FileWriter(pathwayOutPath, true);
             java.io.FileWriter restrictedWriter = new java.io.FileWriter(restrictedPath, true)) {
            int idx = 0;
            for (final String symbol : organismSymbols) {
                idx++;
                // Skip organisms previously confirmed as not available in the free KEGG REST tier.
                if (restrictedOrganisms.contains(symbol))
                    continue;
                try {
                    if (!processedGenes.contains(symbol)) {
                        final String genesContent = de.unibi.agbi.biodwh2.core.net.HTTPClient.getWebsiteSource(
                                "https://rest.kegg.jp/list/" + symbol, 3);
                        if (genesContent != null) {
                            for (final String line : genesContent.split("\n")) {
                                if (line.isEmpty()) continue;
                                genesWriter.write(symbol);
                                genesWriter.write('\t');
                                genesWriter.write(line);
                                genesWriter.write('\n');
                            }
                            genesWriter.flush();
                        }
                        try { Thread.sleep(200); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
                    }
                    if (!processedPathways.contains(symbol)) {
                        final String pathwayContent = de.unibi.agbi.biodwh2.core.net.HTTPClient.getWebsiteSource(
                                "https://rest.kegg.jp/link/pathway/" + symbol, 3);
                        if (pathwayContent != null) {
                            pathwayWriter.write(pathwayContent);
                            if (!pathwayContent.endsWith("\n")) pathwayWriter.write('\n');
                            pathwayWriter.flush();
                        }
                        try { Thread.sleep(200); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
                    }
                } catch (Exception e) {
                    final String msg = e.getMessage();
                    // KEGG returns HTTP 400 for organisms that are not part of the free public REST API
                    // (subscription-only / restricted genomes). Record them once so future runs skip
                    // them entirely without firing any HTTP request.
                    if (msg != null && msg.contains("response code: 400")) {
                        if (restrictedOrganisms.add(symbol)) {
                            try {
                                restrictedWriter.write(symbol);
                                restrictedWriter.write('\n');
                                restrictedWriter.flush();
                            } catch (IOException ignore) { /* best-effort */ }
                        }
                    } else {
                        System.err.println("Failed to process organism " + symbol + ": " + msg);
                    }
                }
                if (idx % 500 == 0)
                    System.out.println("[KEGG] per-organism download progress: " + idx + "/" + organismSymbols.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private java.util.Set<String> readProcessedSymbols(final String path, final int column) {
        final java.util.Set<String> result = new java.util.HashSet<>();
        final java.io.File f = new java.io.File(path);
        if (!f.exists()) return result;
        try {
            for (final String line : java.nio.file.Files.readAllLines(Paths.get(path))) {
                final String[] parts = org.apache.commons.lang3.StringUtils.split(line, '\t');
                if (parts.length > column && parts[column] != null)
                    result.add(parts[column].trim());
            }
        } catch (IOException e) { /* ignore */ }
        return result;
    }

    /**
     * In pathway_per_organism.tsv rows have form `hsa:10327\tpath:hsa00010`.
     * The organism symbol is the prefix before ':' of column 0.
     */
    private java.util.Set<String> readProcessedPathwaySymbols(final String path) {
        final java.util.Set<String> result = new java.util.HashSet<>();
        final java.io.File f = new java.io.File(path);
        if (!f.exists()) return result;
        try {
            for (final String line : java.nio.file.Files.readAllLines(Paths.get(path))) {
                final int colon = line.indexOf(':');
                final int tab = line.indexOf('\t');
                if (colon > 0 && (tab == -1 || colon < tab))
                    result.add(line.substring(0, colon).trim());
            }
        } catch (IOException e) { /* ignore */ }
        return result;
    }
    private void downloadKeggEntries(Workspace workspace, String listFileName, String outputFileName) {
        String listFilePath = dataSource.resolveSourceFilePath(workspace, listFileName).toString();
        String outputFilePath = dataSource.resolveSourceFilePath(workspace, outputFileName).toString();
        if (!new java.io.File(listFilePath).exists())
            return;
        try {
            List<String> lines = java.nio.file.Files.readAllLines(Paths.get(listFilePath));
            List<String> ids = new ArrayList<>();
            for (String line : lines) {
                String[] parts = org.apache.commons.lang3.StringUtils.split(line, '\t');
                if (parts.length > 0) {
                    ids.add(parts[0].replace("rn:", "").replace("md:", "").replace("gn:", "").replace("path:", ""));
                }
            }
            int total = ids.size();
            int processed = 0;
            try (java.io.FileWriter writer = new java.io.FileWriter(outputFilePath)) {
                for (int i = 0; i < ids.size(); i += 10) {
                    int end = Math.min(i + 10, ids.size());
                    String batchIds = String.join("+", ids.subList(i, end));
                    try {
                        String content = de.unibi.agbi.biodwh2.core.net.HTTPClient.getWebsiteSource("https://rest.kegg.jp/get/" + batchIds, 3);
                        if (content != null) {
                            writer.write(content);
                        }
                        Thread.sleep(200);
                        processed += (end - i);
                        if (processed % 500 == 0 || processed == total) {
                            LOGGER.info("Processed {}/{} entries for {}", processed, total, listFileName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean updateFile(Workspace workspace, DataSource dataSource, AnonymousFTPClient ftpClient,
                               String filePath) throws UpdaterException {
        final String fileName = Paths.get(filePath).getFileName().toString();
        final var sourceFilePath = dataSource.resolveSourceFilePath(workspace, fileName);
        try {
            return ftpClient.downloadFile(FTP_BASE_PATH + filePath, sourceFilePath);
        } catch (IOException e) {
            throw new UpdaterConnectionException("Failed to download file '" + FTP_BASE_PATH + filePath + "'", e);
        }
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{
                HUMAN_GENES_LIST_FILE_NAME, COMPOUNDS_LIST_FILE_NAME, ORGANISMS_LIST_FILE_NAME, DGROUP_FILE_NAME,
                DRUG_FILE_NAME, DISEASE_FILE_NAME, NETWORK_FILE_NAME, VARIANT_FILE_NAME,
                REACTIONS_LIST_FILE_NAME, MODULES_LIST_FILE_NAME, ORTHOLOGY_LIST_FILE_NAME,
                PATHWAYS_LIST_FILE_NAME, ENZYMES_LIST_FILE_NAME, BRITE_LIST_FILE_NAME,
                RCLASS_LIST_FILE_NAME,
                REACTION_COMPOUND_FILE_NAME, REACTION_KO_FILE_NAME, MODULE_REACTION_FILE_NAME,
                MODULE_KO_FILE_NAME, MODULE_COMPOUND_FILE_NAME,
                PATHWAY_KO_FILE_NAME, PATHWAY_COMPOUND_FILE_NAME, PATHWAY_MODULE_FILE_NAME, PATHWAY_REACTION_FILE_NAME,
                DISEASE_DRUG_FILE_NAME, DISEASE_HSA_FILE_NAME, REACTION_ENZYME_FILE_NAME,
                UNIPROT_HSA_FILE_NAME, NCBI_PROTEINID_HSA_FILE_NAME, KO_HSA_FILE_NAME,
                TAXONOMY_GENOME_FILE_NAME, PATHWAY_GENOME_FILE_NAME, PATHWAY_HSA_FILE_NAME,
                GENES_PER_ORGANISM_FILE_NAME, PATHWAY_PER_ORGANISM_FILE_NAME,
                REACTIONS_FILE_NAME, MODULES_FILE_NAME, PATHWAYS_FILE_NAME,
                ORGANISMS_FILE_NAME
        };
    }
}
