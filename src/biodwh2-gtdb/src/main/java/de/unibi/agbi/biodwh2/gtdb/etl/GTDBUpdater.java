package de.unibi.agbi.biodwh2.gtdb.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.gtdb.GTDBDataSource;

public class GTDBUpdater extends Updater<GTDBDataSource> {
    private static final String GTDB_BASE_URL = "https://data.gtdb.aau.ecogenomic.org/releases/latest/";

    public static final String BAC120_METADATA_FILE = "bac120_metadata.tsv.gz";
    public static final String AR53_METADATA_FILE = "ar53_metadata.tsv.gz";

    public GTDBUpdater(GTDBDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Version getNewestVersion(Workspace workspace) throws UpdaterException {
        return new Version(2026, 5);
    }

    @Override
    protected boolean tryUpdateFiles(Workspace workspace) throws UpdaterException {
        downloadFileAsBrowser(workspace, GTDB_BASE_URL + BAC120_METADATA_FILE, BAC120_METADATA_FILE);
        downloadFileAsBrowser(workspace, GTDB_BASE_URL + AR53_METADATA_FILE, AR53_METADATA_FILE);
        return true;
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{BAC120_METADATA_FILE, AR53_METADATA_FILE};
    }
}