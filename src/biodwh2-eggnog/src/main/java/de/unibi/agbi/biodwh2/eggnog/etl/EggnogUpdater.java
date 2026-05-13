package de.unibi.agbi.biodwh2.eggnog.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.eggnog.EggnogDataSource;

public class EggnogUpdater extends Updater<EggnogDataSource> {
    static final String TAXID_INFO_FILE = "e5.taxid_info.tsv";
    static final String OG_ANNOTATIONS_FILE = "e5.og_annotations.tsv";
    private static final String BASE_URL = "http://eggnog5.embl.de/download/eggnog_5.0/";

    public EggnogUpdater(final EggnogDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Version getNewestVersion(final Workspace workspace) throws UpdaterException {
        return new Version(5, 0);
    }

    @Override
    protected boolean tryUpdateFiles(final Workspace workspace) throws UpdaterException {
        downloadFileAsBrowser(workspace, BASE_URL + TAXID_INFO_FILE, TAXID_INFO_FILE);
        downloadFileAsBrowser(workspace, BASE_URL + OG_ANNOTATIONS_FILE, OG_ANNOTATIONS_FILE);
        return true;
    }

    @Override
    protected String[] expectedFileNames() {
        return new String[]{TAXID_INFO_FILE, OG_ANNOTATIONS_FILE};
    }
}
