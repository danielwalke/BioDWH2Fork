package de.unibi.agbi.biodwh2.cazy.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.MappingDescriber;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;
import de.unibi.agbi.biodwh2.core.model.graph.*;

public class CazyMappingDescriber extends MappingDescriber {
    public CazyMappingDescriber(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public NodeMappingDescription[] describe(final Graph graph, final Node node, final String localMappingLabel) {
        if (CazyGraphExporter.PROTEIN_LABEL.equalsIgnoreCase(localMappingLabel)) {
            return describeProtein(node);
        }
        if (CazyGraphExporter.ORGANISM_LABEL.equalsIgnoreCase(localMappingLabel)) {
            return describeOrganism(node);
        }
        return null;
    }

    private NodeMappingDescription[] describeProtein(final Node node) {
        final NodeMappingDescription proteinDescription = new NodeMappingDescription(
                NodeMappingDescription.NodeType.PROTEIN);
        final String uniprot = node.<String>getProperty("uniprot_accession");
        if (uniprot != null && !uniprot.isEmpty()) {
            proteinDescription.addIdentifier(IdentifierType.UNIPROT_KB, uniprot);
        }
        final String ecNumber = node.<String>getProperty("ec_number");
        if (ecNumber != null && !ecNumber.isEmpty()) {
            proteinDescription.addIdentifier(IdentifierType.EC_NUMBER, ecNumber);
        }
        final String proteinId = node.<String>getProperty("id");
        if (proteinId != null && !proteinId.isEmpty()) {
            proteinDescription.addIdentifier(IdentifierType.GENBANK, proteinId);
        }
        proteinDescription.addIdentifier(IdentifierType.UNII, proteinId);
        final String organism = node.<String>getProperty("organism");
        if (organism != null && !organism.isEmpty()) {
            proteinDescription.addName(organism);
        }
        proteinDescription.addName("CAZy protein: " + proteinId);
        return new NodeMappingDescription[]{proteinDescription};
    }

    private NodeMappingDescription[] describeOrganism(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.TAXON);
        description.addName(node.getProperty("name"));
        return new NodeMappingDescription[]{description};
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{CazyGraphExporter.PROTEIN_LABEL, CazyGraphExporter.ORGANISM_LABEL};
    }

    @Override
    public PathMappingDescription describe(final Graph graph, final Node[] nodes, final Edge[] edges) {
        return null;
    }

    @Override
    protected PathMapping[] getEdgePathMappings() {
        return new PathMapping[0];
    }
}
