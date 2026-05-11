package de.unibi.agbi.biodwh2.kegg.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterConnectionException;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.core.net.AnonymousFTPClient;
import de.unibi.agbi.biodwh2.kegg.KeggDataSource;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KeggUpdater extends Updater<KeggDataSource> {
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
        downloadFileAsBrowser(workspace, HUMAN_GENES_LIST_URL, HUMAN_GENES_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, COMPOUNDS_LIST_URL, COMPOUNDS_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, ORGANISMS_LIST_URL, ORGANISMS_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, REACTIONS_LIST_URL, REACTIONS_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, MODULES_LIST_URL, MODULES_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, ORTHOLOGY_LIST_URL, ORTHOLOGY_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, PATHWAYS_LIST_URL, PATHWAYS_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, ENZYMES_LIST_URL, ENZYMES_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, BRITE_LIST_URL, BRITE_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, GLYCAN_LIST_URL, GLYCAN_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, RCLASS_LIST_URL, RCLASS_LIST_FILE_NAME);
        downloadFileAsBrowser(workspace, REACTION_COMPOUND_URL, REACTION_COMPOUND_FILE_NAME);
        downloadFileAsBrowser(workspace, REACTION_KO_URL, REACTION_KO_FILE_NAME);
        downloadFileAsBrowser(workspace, MODULE_REACTION_URL, MODULE_REACTION_FILE_NAME);
        downloadFileAsBrowser(workspace, MODULE_KO_URL, MODULE_KO_FILE_NAME);
        downloadFileAsBrowser(workspace, MODULE_COMPOUND_URL, MODULE_COMPOUND_FILE_NAME);
        downloadFileAsBrowser(workspace, PATHWAY_KO_URL, PATHWAY_KO_FILE_NAME);
        downloadFileAsBrowser(workspace, PATHWAY_COMPOUND_URL, PATHWAY_COMPOUND_FILE_NAME);
        downloadFileAsBrowser(workspace, DISEASE_DRUG_URL, DISEASE_DRUG_FILE_NAME);
        downloadFileAsBrowser(workspace, DISEASE_HSA_URL, DISEASE_HSA_FILE_NAME);
        downloadFileAsBrowser(workspace, REACTION_ENZYME_URL, REACTION_ENZYME_FILE_NAME);
        downloadFileAsBrowser(workspace, UNIPROT_HSA_URL, UNIPROT_HSA_FILE_NAME);
        downloadFileAsBrowser(workspace, NCBI_PROTEINID_HSA_URL, NCBI_PROTEINID_HSA_FILE_NAME);
        downloadFileAsBrowser(workspace, KO_HSA_URL, KO_HSA_FILE_NAME);
        return success;
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
                GLYCAN_LIST_FILE_NAME, RCLASS_LIST_FILE_NAME,
                REACTION_COMPOUND_FILE_NAME, REACTION_KO_FILE_NAME, MODULE_REACTION_FILE_NAME,
                MODULE_KO_FILE_NAME, MODULE_COMPOUND_FILE_NAME,
                PATHWAY_KO_FILE_NAME, PATHWAY_COMPOUND_FILE_NAME,
                DISEASE_DRUG_FILE_NAME, DISEASE_HSA_FILE_NAME, REACTION_ENZYME_FILE_NAME,
                UNIPROT_HSA_FILE_NAME, NCBI_PROTEINID_HSA_FILE_NAME, KO_HSA_FILE_NAME
        };
    }
}
