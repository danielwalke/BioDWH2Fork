package de.unibi.agbi.biodwh2.core.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.exceptions.ReconException;
import de.unibi.agbi.biodwh2.core.model.graph.Edge;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import org.apache.commons.lang3.StringUtils;

public abstract class ReconExporter<D extends DataSource> {
    public static final String IDENTIFIER_LABEL = "Identifier";
    public static final String SYNONYM_LABEL = "Synonym";
    public static final String STRUCTURE_LABEL = "Structure";
    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";

    protected final D dataSource;

    public ReconExporter(final D dataSource) {
        this.dataSource = dataSource;
    }

    public abstract long getReconVersion();

    public abstract void recon(final Workspace workspace, final Graph graph) throws ReconException;

    protected Long createIdentifier(final Graph graph, String id) {
        if (StringUtils.isBlank(id))
            return null;
        id = StringUtils.strip(id);
        var node = graph.findNode(IDENTIFIER_LABEL, ID_KEY, id);
        if (node == null)
            node = graph.addNode(IDENTIFIER_LABEL, ID_KEY, id);
        return node.getId();
    }

    protected Long createSynonym(final Graph graph, String name) {
        if (StringUtils.isBlank(name))
            return null;
        name = StringUtils.strip(name);
        var node = graph.findNode(SYNONYM_LABEL, NAME_KEY, name);
        if (node == null)
            node = graph.addNode(SYNONYM_LABEL, NAME_KEY, name);
        return node.getId();
    }

    protected void createXrefOrNameRelation(final Graph graph, String sourceId, String targetId) {
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(targetId))
            return;
        sourceId = StringUtils.strip(sourceId);
        targetId = StringUtils.strip(targetId);
        final var sourceNodeId = createIdentifier(graph, sourceId);
        final var targetNodeId = createIdentifier(graph, targetId);
        createXrefOrNameRelation(graph, sourceNodeId, targetNodeId);
    }

    protected void createXrefOrNameRelation(final Graph graph, final Long sourceNodeId, final Long targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null)
            return;
        var edge = graph.findEdge(dataSource.getId(), Edge.FROM_ID_FIELD, sourceNodeId, Edge.TO_ID_FIELD, targetNodeId);
        if (edge == null)
            graph.addEdge(sourceNodeId, targetNodeId, dataSource.getId());
    }
}
