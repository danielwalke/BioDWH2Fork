package de.unibi.agbi.biodwh2.npass.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.ReconExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ReconException;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.npass.NPASSDataSource;

public class NPASSReconExporter extends ReconExporter<NPASSDataSource> {
    public NPASSReconExporter(final NPASSDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getReconVersion() {
        return 1;
    }

    @Override
    public void recon(final Workspace workspace, final Graph graph) throws ReconException {

    }
}
