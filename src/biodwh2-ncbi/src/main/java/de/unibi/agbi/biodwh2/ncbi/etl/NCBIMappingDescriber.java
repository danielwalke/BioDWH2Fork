package de.unibi.agbi.biodwh2.ncbi.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.MappingDescriber;
import de.unibi.agbi.biodwh2.core.model.graph.*;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;

public class NCBIMappingDescriber extends MappingDescriber {

    public NCBIMappingDescriber(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public NodeMappingDescription[] describe(final Graph graph, final Node node, final String localMappingLabel) {
        if (NCBIGraphExporter.TAXON_LABEL.equals(localMappingLabel))
            return describeTaxon(node);
        return null;
    }

    private NodeMappingDescription[] describeTaxon(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.TAXON);

        // "id" is stored as plain String — set directly
        final String taxId = node.getProperty("id");
        if (taxId != null)
            description.addIdentifier(IdentifierType.NCBI_TAXON, Integer.parseInt(taxId));

        // All other properties are stored as String[] due to setTaxonProperty()
        final String[] nameTxts = node.getProperty("name_txt");
        if (nameTxts != null) {
            for (final String name : nameTxts) {
                if (name != null && !name.isEmpty())
                    description.addName(name);
            }
        }

        // Pick the scientific name as the primary name if available
        final String[] nameClasses = node.getProperty("name_class");
        final String[] uniqueNames = node.getProperty("unique_name");
        if (nameClasses != null && uniqueNames != null) {
            for (int i = 0; i < nameClasses.length; i++) {
                if ("scientific name".equals(nameClasses[i]) && i < uniqueNames.length
                        && uniqueNames[i] != null && !uniqueNames[i].isEmpty()) {
                    description.addName(uniqueNames[i]);
                }
            }
        }

        return new NodeMappingDescription[]{description};
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{NCBIGraphExporter.TAXON_LABEL};
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