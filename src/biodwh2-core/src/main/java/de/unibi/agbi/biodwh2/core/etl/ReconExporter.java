package de.unibi.agbi.biodwh2.core.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.exceptions.ReconException;
import de.unibi.agbi.biodwh2.core.model.graph.Edge;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class ReconExporter<D extends DataSource> {
    public static final String IDENTIFIER_LABEL = "Identifier";
    public static final String SYNONYM_LABEL = "Synonym";
    public static final String STRUCTURE_LABEL = "Structure";
    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String INCHI_KEY = "inchi";
    public static final String MOL_KEY = "mol";
    public static final String INCHIKEY_PREFIX = "InChIKey";
    public static final String SMILES_PREFIX = "SMILES";

    protected final D dataSource;

    public ReconExporter(final D dataSource) {
        this.dataSource = dataSource;
    }

    public abstract long getReconVersion();

    public abstract void recon(final Workspace workspace, final Graph graph) throws ReconException;

    protected Long createStructureFromInchi(final Graph graph, String inchi) {
        if (StringUtils.isBlank(inchi))
            return null;
        inchi = StringUtils.strip(inchi);
        if (!inchi.startsWith("InChI="))
            inchi = "InChI=" + inchi;
        var node = graph.findNode(STRUCTURE_LABEL, INCHI_KEY, inchi);
        if (node == null)
            node = graph.addNode(STRUCTURE_LABEL, INCHI_KEY, inchi);
        return node.getId();
    }

    protected Long createStructureFromMol(final Graph graph, final String mol) {
        if (StringUtils.isBlank(mol))
            return null;
        var node = graph.findNode(STRUCTURE_LABEL, MOL_KEY, mol);
        if (node == null)
            node = graph.addNode(STRUCTURE_LABEL, MOL_KEY, mol);
        return node.getId();
    }

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

    protected void createStructureRelationFromInchi(final Graph graph, final String sourceId, final String inchi) {
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(inchi))
            return;
        final var sourceNodeId = createIdentifier(graph, sourceId);
        final var targetNodeId = createStructureFromInchi(graph, inchi);
        createXrefOrNameRelation(graph, sourceNodeId, targetNodeId);
    }

    protected void createStructureRelationFromMol(final Graph graph, final String sourceId, final String mol) {
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(mol))
            return;
        final var sourceNodeId = createIdentifier(graph, sourceId);
        final var targetNodeId = createStructureFromMol(graph, mol);
        createXrefOrNameRelation(graph, sourceNodeId, targetNodeId);
    }

    protected void createXrefRelation(final Graph graph, final String sourceId, final String targetId) {
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(targetId))
            return;
        final var sourceNodeId = createIdentifier(graph, sourceId);
        final var targetNodeId = createIdentifier(graph, targetId);
        createXrefOrNameRelation(graph, sourceNodeId, targetNodeId);
    }

    protected void createNameRelation(final Graph graph, final String sourceId, final String name) {
        createNameRelation(graph, sourceId, name, null);
    }

    protected void createNameRelation(final Graph graph, final String sourceId, final String name,
                                      final String[] emptyPlaceholders) {
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(name))
            return;
        if (emptyPlaceholders != null && ArrayUtils.contains(emptyPlaceholders, name))
            return;
        final var sourceNodeId = createIdentifier(graph, sourceId);
        final var targetNodeId = createSynonym(graph, name);
        createXrefOrNameRelation(graph, sourceNodeId, targetNodeId);
    }

    protected void createXrefOrNameRelation(final Graph graph, final Long sourceNodeId, final Long targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null)
            return;
        var edge = graph.findEdge(dataSource.getId(), Edge.FROM_ID_FIELD, sourceNodeId, Edge.TO_ID_FIELD, targetNodeId);
        if (edge == null)
            graph.addEdge(sourceNodeId, targetNodeId, dataSource.getId());
    }

    protected String prefixIdentifier(final String prefix, final String id) {
        if (StringUtils.isBlank(id))
            return null;
        return prefix + ":" + id;
    }
}
