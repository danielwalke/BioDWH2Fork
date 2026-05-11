package de.unibi.agbi.biodwh2.reactome.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.MappingDescriber;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;
import de.unibi.agbi.biodwh2.core.model.graph.Edge;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import de.unibi.agbi.biodwh2.core.model.graph.NodeMappingDescription;
import de.unibi.agbi.biodwh2.core.model.graph.PathMapping;
import de.unibi.agbi.biodwh2.core.model.graph.PathMappingDescription;
import org.apache.commons.lang3.StringUtils;

public class ReactomeMappingDescriber extends MappingDescriber {
    public ReactomeMappingDescriber(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public NodeMappingDescription[] describe(final Graph graph, final Node node, final String localMappingLabel) {
        if (ReactomeGraphExporter.PATHWAY_LABEL.equals(localMappingLabel))
            return describePathway(node);
        if (ReactomeGraphExporter.REACTION_LABEL.equals(localMappingLabel))
            return describeReaction(node);
        if (ReactomeGraphExporter.PHYSICAL_ENTITY_LABEL.equals(localMappingLabel))
            return describePhysicalEntity(graph, node);
        if (ReactomeGraphExporter.REFERENCE_ENTITY_LABEL.equals(localMappingLabel))
            return describeReferenceEntity(graph, node);
        return null;
    }

    @Override
    public PathMappingDescription describe(final Graph graph, final Node[] nodes, final Edge[] edges) {
        return null;
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{
                ReactomeGraphExporter.PATHWAY_LABEL,
                ReactomeGraphExporter.REACTION_LABEL,
                ReactomeGraphExporter.PHYSICAL_ENTITY_LABEL,
                ReactomeGraphExporter.REFERENCE_ENTITY_LABEL
        };
    }

    @Override
    protected PathMapping[] getEdgePathMappings() {
        return new PathMapping[0];
    }

    private NodeMappingDescription[] describePathway(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.PATHWAY);
        description.addIdentifier(IdentifierType.REACTOME, node.<String>getProperty("stable_identifier"));
        description.addName(node.getProperty("display_name"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeReaction(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.PATHWAY);
        final String stableId = node.<String>getProperty("stable_identifier");
        if (stableId != null)
            description.addIdentifier(IdentifierType.REACTOME, stableId.replaceAll("-REACT_", "-R-"));
        description.addName(node.getProperty("display_name"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describePhysicalEntity(final Graph graph, final Node node) {
        final NodeMappingDescription proteinDescription = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
        final NodeMappingDescription compoundDescription = new NodeMappingDescription(NodeMappingDescription.NodeType.COMPOUND);
        final String stableId = node.<String>getProperty("stable_identifier");
        final String className = node.<String>getProperty("class_name");
        
        Long referenceEntityNodeId = null;
        if (className != null && className.contains("Protein")) {
            proteinDescription.addIdentifier(IdentifierType.REACTOME, stableId);
            proteinDescription.addName(node.getProperty("display_name"));
            
            referenceEntityNodeId = tryGetReferenceEntityNodeId(graph, node);
            if (referenceEntityNodeId != null) {
                final Node referenceEntityNode = graph.getNode(referenceEntityNodeId);
                final String referenceDatabaseName = referenceEntityNode.<String>getProperty("reference_database_name");
                final String identifier = referenceEntityNode.<String>getProperty("identifier");
                
                if (identifier != null) {
                    if ("UniProt".equals(referenceDatabaseName) || "UniProtKB".equals(referenceDatabaseName)) {
                        proteinDescription.addIdentifier(IdentifierType.UNIPROT_KB, identifier);
                    }
                }
            }
            compoundDescription.addIdentifier(IdentifierType.REACTOME, stableId);
            compoundDescription.addName(node.getProperty("display_name"));
            return new NodeMappingDescription[]{proteinDescription, compoundDescription};
        }
        
        compoundDescription.addIdentifier(IdentifierType.REACTOME, stableId);
        compoundDescription.addName(node.getProperty("display_name"));
        
        if (referenceEntityNodeId == null)
            referenceEntityNodeId = tryGetReferenceEntityNodeId(graph, node);
        
        if (referenceEntityNodeId != null) {
            final Node referenceEntityNode = graph.getNode(referenceEntityNodeId);
            final String referenceDatabaseName = referenceEntityNode.<String>getProperty("reference_database_name");
            final String identifier = referenceEntityNode.<String>getProperty("identifier");
            
            if (identifier != null) {
                if ("ChEBI".equals(referenceDatabaseName)) {
                    compoundDescription.addIdentifier(IdentifierType.CHEBI, identifier);
                } else if ("KEGG Compound".equals(referenceDatabaseName)) {
                    compoundDescription.addIdentifier(IdentifierType.KEGG, identifier);
                } else if ("KEGG Drug".equals(referenceDatabaseName)) {
                    compoundDescription.addIdentifier(IdentifierType.KEGG, identifier);
                } else if ("HMDB".equals(referenceDatabaseName)) {
                    compoundDescription.addIdentifier(IdentifierType.HMDB, identifier);
                } else if ("PubChem".equals(referenceDatabaseName)) {
                    compoundDescription.addIdentifier(IdentifierType.PUB_CHEM_COMPOUND, identifier);
                }
            }
        }
        return new NodeMappingDescription[]{compoundDescription};
    }

    private Long tryGetReferenceEntityNodeId(final Graph graph, final Node node) {
        final Long[] nodeIds = graph.getAdjacentNodeIdsForEdgeLabel(node.getId(), "HAS_REFERENCE_ENTITY");
        if (nodeIds.length > 0)
            return nodeIds[0];
        return null;
    }

    private NodeMappingDescription[] describeReferenceEntity(final Graph graph, final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.COMPOUND);
        final String referenceDatabaseName = node.<String>getProperty("reference_database_name");
        final String identifier = node.<String>getProperty("identifier");
        
        if (identifier != null) {
            if ("UniProt".equals(referenceDatabaseName) || "UniProtKB".equals(referenceDatabaseName)) {
                final NodeMappingDescription proteinDescription = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
                proteinDescription.addIdentifier(IdentifierType.UNIPROT_KB, identifier);
                proteinDescription.addName(node.getProperty("display_name"));
                return new NodeMappingDescription[]{proteinDescription};
            }
            description.addIdentifier(IdentifierType.REACTOME, node.<String>getProperty("stable_identifier"));
        }
        
        if (referenceDatabaseName != null && identifier != null) {
            if ("ChEBI".equals(referenceDatabaseName)) {
                description.addIdentifier(IdentifierType.CHEBI, identifier);
            } else if ("KEGG Compound".equals(referenceDatabaseName)) {
                description.addIdentifier(IdentifierType.KEGG, identifier);
            } else if ("KEGG Drug".equals(referenceDatabaseName)) {
                description.addIdentifier(IdentifierType.KEGG, identifier);
            } else if ("HMDB".equals(referenceDatabaseName)) {
                description.addIdentifier(IdentifierType.HMDB, identifier);
            } else if ("PubChem".equals(referenceDatabaseName)) {
                description.addIdentifier(IdentifierType.PUB_CHEM_COMPOUND, identifier);
            }
        }
        
        description.addName(node.getProperty("display_name"));
        return new NodeMappingDescription[]{description};
    }
}
