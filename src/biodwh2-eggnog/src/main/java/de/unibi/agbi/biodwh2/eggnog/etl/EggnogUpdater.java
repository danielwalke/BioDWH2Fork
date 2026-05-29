package de.unibi.agbi.biodwh2.eggnog.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.Updater;
import de.unibi.agbi.biodwh2.core.exceptions.UpdaterException;
import de.unibi.agbi.biodwh2.core.model.Version;
import de.unibi.agbi.biodwh2.eggnog.EggnogDataSource;

public class EggnogUpdater extends Updater<EggnogDataSource> {
    static final String[] MAPPING_FILES = new String[]{
            "latest.Archaea.tsv.gz", "latest.Bacteria.tsv.gz", "latest.Eukaryota.tsv.gz"
    };
    private static final String BASE_URL = "http://eggnog5.embl.de/download/eggnog_5.0/id_mappings/uniprot/";

    public EggnogUpdater(final EggnogDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Version getNewestVersion(final Workspace workspace) throws UpdaterException {
        return new Version(5, 0);
    }

    @Override
    protected boolean tryUpdateFiles(final Workspace workspace) throws UpdaterException {
        for (String file : MAPPING_FILES) {
            downloadFileAsBrowser(workspace, BASE_URL + file, file);
        }
        return true;
    }

    @Override
    protected String[] expectedFileNames() {
        return MAPPING_FILES;
    }
}
