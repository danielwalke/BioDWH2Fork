package de.unibi.agbi.biodwh2.gtdb.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.MappingDescriber;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;
import de.unibi.agbi.biodwh2.core.model.graph.*;
import de.unibi.agbi.biodwh2.core.model.graph.Node;

import java.util.ArrayList;
import java.util.List;

public class GTDBMappingDescriber extends MappingDescriber {
    public GTDBMappingDescriber(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public NodeMappingDescription[] describe(final Graph graph, final Node node, final String localMappingLabel) {
        if ("Taxon".equals(localMappingLabel))
            return describeTaxon(node);
        if ("Genome".equals(localMappingLabel))
            return describeGenome(node);
        return null;
    }

    private NodeMappingDescription[] describeTaxon(final Node node) {
        List<NodeMappingDescription> descriptions = new ArrayList<>();

        String taxonomy = node.getProperty("taxonomy");
        if (taxonomy != null && taxonomy.contains("__")) {
            NodeMappingDescription desc = new NodeMappingDescription(
                NodeMappingDescription.NodeType.TAXON);
            desc.addName(node.getProperty("name"));

            if (node.hasProperty("ncbi_taxid")) {
                Integer ncbiTaxid = node.getProperty("ncbi_taxid");
                if (ncbiTaxid != null) {
                    desc.addIdentifier(IdentifierType.NCBI_TAXON, ncbiTaxid);
                }
            }

            descriptions.add(desc);
        }

        return descriptions.toArray(new NodeMappingDescription[0]);
    }

    private NodeMappingDescription[] describeGenome(final Node node) {
        List<NodeMappingDescription> descriptions = new ArrayList<>();

        if (node.hasProperty("ncbi_taxid")) {
            Integer ncbiTaxid = node.getProperty("ncbi_taxid");
            if (ncbiTaxid != null) {
                NodeMappingDescription desc = new NodeMappingDescription(
                    NodeMappingDescription.NodeType.TAXON);
                desc.addIdentifier(IdentifierType.NCBI_TAXON, ncbiTaxid);
                descriptions.add(desc);
            }
        }

        return descriptions.toArray(new NodeMappingDescription[0]);
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{"Taxon", "Genome"};
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