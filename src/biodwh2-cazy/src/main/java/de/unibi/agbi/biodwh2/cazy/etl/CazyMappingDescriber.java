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
        if (CazyGraphExporter.EC_LABEL.equalsIgnoreCase(localMappingLabel)) {
            return describeECNumber(node);
        }
        return null;
    }

    private NodeMappingDescription[] describeProtein(final Node node) {
        final NodeMappingDescription proteinDescription = new NodeMappingDescription(
                NodeMappingDescription.NodeType.PROTEIN);
        final String proteinId = node.<String>getProperty("id");
        final String source = node.<String>getProperty("source");

        // Only map as GenBank if the source is NCBI (JGI IDs are not GenBank accessions)
        if (proteinId != null && !proteinId.isEmpty() && "ncbi".equalsIgnoreCase(source)) {
            proteinDescription.addIdentifier(IdentifierType.GENBANK, proteinId);

            final String uniprotId = node.getProperty("uniprot_id");
            if (uniprotId != null && !uniprotId.isEmpty()) {
                proteinDescription.addIdentifier(IdentifierType.UNIPROT_KB, uniprotId);
            }
        }

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
        final Integer ncbiTaxid = node.getProperty("ncbi_taxid");
        if (ncbiTaxid != null) {
            description.addIdentifier(IdentifierType.NCBI_TAXON, ncbiTaxid);
        }
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeECNumber(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(
                NodeMappingDescription.NodeType.PROTEIN);
        final String ecNumber = node.<String>getProperty("id");
        if (ecNumber != null && !ecNumber.isEmpty()) {
            description.addIdentifier(IdentifierType.EC_NUMBER, ecNumber);
        }
        final String activityName = node.<String>getProperty("activity_name");
        if (activityName != null && !activityName.isEmpty()) {
            description.addName(activityName);
        }
        return new NodeMappingDescription[]{description};
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{
                CazyGraphExporter.PROTEIN_LABEL,
                CazyGraphExporter.ORGANISM_LABEL,
                CazyGraphExporter.EC_LABEL
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
