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
        if ("CazyProtein".equalsIgnoreCase(localMappingLabel)) {
            return describeProtein(node);
        }
        if ("CazyOrganism".equalsIgnoreCase(localMappingLabel)) {
            return describeOrganism(node);
        }
        return null;
    }

    private NodeMappingDescription[] describeProtein(final Node node) {
        final NodeMappingDescription proteinDescription = new NodeMappingDescription(
                NodeMappingDescription.NodeType.PROTEIN);
        final String proteinId = node.<String>getProperty("id");

        if (proteinId != null && !proteinId.isEmpty()) {
            proteinDescription.addIdentifier(IdentifierType.GENBANK, proteinId);

            final String uniprotId = node.getProperty("uniprot_id");
            if (uniprotId != null && !uniprotId.isEmpty()) {
                for (final String part : uniprotId.split(";")) {
                    final String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        proteinDescription.addIdentifier(IdentifierType.UNIPROT_KB, trimmed);
                    }
                }
            }
        }

        final String name = node.getProperty("name");
        if (name != null && !name.isEmpty()) {
            proteinDescription.addName(name);
        }
        return new NodeMappingDescription[]{proteinDescription};
    }

    private NodeMappingDescription[] describeOrganism(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.TAXON);
        description.addName(node.getProperty("name"));
        final Integer ncbiTaxid = node.getProperty("ncbi_taxid");
        if (ncbiTaxid != null) {
            description.addIdentifier(IdentifierType.NCBI_TAXON, ncbiTaxid);
        }
        return new NodeMappingDescription[]{description};
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{
                "CazyProtein",
                "CazyOrganism"
        };
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
