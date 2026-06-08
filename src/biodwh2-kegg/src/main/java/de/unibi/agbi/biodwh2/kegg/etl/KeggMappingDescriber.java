package de.unibi.agbi.biodwh2.kegg.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.MappingDescriber;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;
import de.unibi.agbi.biodwh2.core.model.graph.*;
import de.unibi.agbi.biodwh2.core.model.graph.mapping.PublicationNodeMappingDescription;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class KeggMappingDescriber extends MappingDescriber {
    public KeggMappingDescriber(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public NodeMappingDescription[] describe(final Graph graph, final Node node, final String localMappingLabel) {
        if (KeggGraphExporter.DRUG_LABEL.equals(localMappingLabel))
            return describeDrug(node);
        if (KeggGraphExporter.REFERENCE_LABEL.equals(localMappingLabel))
            return describeReference(node);
        if (KeggGraphExporter.GENE_LABEL.equals(localMappingLabel))
            return describeGene(node);
        if (KeggGraphExporter.DISEASE_LABEL.equals(localMappingLabel))
            return describeDisease(node);
        if (KeggGraphExporter.COMPOUND_LABEL.equals(localMappingLabel))
            return describeCompound(node);
        if (KeggGraphExporter.ORGANISM_LABEL.equals(localMappingLabel))
            return describeOrganism(node);
        if (KeggGraphExporter.REACTION_LABEL.equals(localMappingLabel))
            return describeReaction(node);
        if (KeggGraphExporter.MODULE_LABEL.equals(localMappingLabel))
            return describeModule(node);
        if (KeggGraphExporter.PROTEIN_LABEL.equals(localMappingLabel))
            return describeProtein(node);
        if (KeggGraphExporter.PATHWAY_LABEL.equals(localMappingLabel))
            return describePathway(node);
        if (KeggGraphExporter.ENZYME_LABEL.equals(localMappingLabel))
            return describeEnzyme(node);
        if (KeggGraphExporter.RCLASS_LABEL.equals(localMappingLabel))
            return describeRClass(node);
        return null;
    }

    private NodeMappingDescription[] describeDrug(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.DRUG);
        final String[] names = getEntryNames(node);
        description.addNames(names);
        for (final String name : names) {
            if (name.endsWith(")")) {
                final int startIndex = name.lastIndexOf('(');
                final String nameWithoutTags = name.substring(0, startIndex).trim();
                final String[] nameTags = StringUtils.split(name.substring(startIndex + 1, name.length() - 1), '/');
                for (final String nameTag : nameTags)
                    if (nameTag.equals("INN")) {
                        description.addIdentifier(IdentifierType.INN, nameWithoutTags);
                        break;
                    }
            }
        }
        final String[] externalIdentifier = node.getProperty("external_identifier");
        if (externalIdentifier != null)
            for (final String identifier : externalIdentifier) {
                final String[] idParts = StringUtils.split(identifier, ":", 2);
                if ("DrugBank".equals(idParts[0]))
                    description.addIdentifier(IdentifierType.DRUG_BANK, idParts[1]);
                else if ("CAS".equals(idParts[0]))
                    description.addIdentifier(IdentifierType.CAS, idParts[1]);
            }
        return new NodeMappingDescription[]{description};
    }

    private String[] getEntryNames(final Node node) {
        final String name = node.getProperty("name");
        if (name != null)
            return new String[]{name.trim()};
        final String[] names = node.getProperty("names");
        return names != null ? Arrays.stream(names).map(String::trim).toArray(String[]::new) : new String[0];
    }

    private NodeMappingDescription[] describeReference(final Node node) {
        final PublicationNodeMappingDescription description = new PublicationNodeMappingDescription();
        description.pubmedId = node.getProperty("pmid");
        description.setDOI(node.getProperty("doi"));
        description.addIdentifier(IdentifierType.DOI, description.doi);
        description.addIdentifier(IdentifierType.PUBMED_ID, description.pubmedId);
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeGene(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.GENE);
        description.addNames(node.<String>getProperty("name"));
        description.addNames(node.<String[]>getProperty("symbols"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeDisease(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.DISEASE);
        description.addNames(getEntryNames(node));
        final String[] externalIdentifier = node.getProperty("external_identifier");
        if (externalIdentifier != null)
            for (final String identifier : externalIdentifier) {
                final String[] idParts = StringUtils.split(identifier, ":", 2);
                if ("ICD-10".equals(idParts[0]))
                    description.addIdentifier(IdentifierType.ICD10, idParts[1]);
                else if ("ICD-11".equals(idParts[0]))
                    description.addIdentifier(IdentifierType.ICD11, idParts[1]);
                // OMIM ids clump together a lot of KEGG diseases (for example H02463 has many different OMIM ids)
                //else if ("OMIM".equals(idParts[0]))
                //    description.addIdentifier(IdentifierType.OMIM, idParts[1]);
            }
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeCompound(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.COMPOUND);
        description.addNames(node.<String[]>getProperty("names"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeOrganism(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.TAXON);
        description.addNames(node.<String>getProperty("name"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        final String ncbiTaxid = node.getProperty("ncbi_taxid");
        if (ncbiTaxid != null) {
            String bareTaxid = ncbiTaxid.replace("NCBITaxon:", "");
            description.addIdentifier(IdentifierType.NCBI_TAXON, bareTaxid);
        }
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeReaction(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription("REACTION");
        description.addNames(node.<String>getProperty("name"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeModule(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.PATHWAY);
        description.addNames(node.<String>getProperty("name"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeProtein(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
        description.addNames(node.<String>getProperty("name"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        final String[] uniprotIds = node.getProperty("uniprot_ids");
        if (uniprotIds != null) {
            for (String uniprotId : uniprotIds) {
                description.addIdentifier(IdentifierType.UNIPROT_KB, uniprotId);
            }
        }
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describePathway(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.PATHWAY);
        description.addNames(node.<String>getProperty("name"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeEnzyme(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
        description.addNames(node.<String[]>getProperty("names"));
        // Enzyme id is the bare EC number e.g. "1.1.1.1"
        description.addIdentifier(IdentifierType.EC_NUMBER, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }


    private NodeMappingDescription[] describeRClass(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription("REACTION_CLASS");
        description.addNames(node.<String>getProperty("name"));
        description.addIdentifier(IdentifierType.KEGG, node.<String>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{
                KeggGraphExporter.DRUG_LABEL, KeggGraphExporter.REFERENCE_LABEL, KeggGraphExporter.GENE_LABEL,
                KeggGraphExporter.DISEASE_LABEL, KeggGraphExporter.COMPOUND_LABEL, KeggGraphExporter.ORGANISM_LABEL,
                KeggGraphExporter.REACTION_LABEL, KeggGraphExporter.MODULE_LABEL, KeggGraphExporter.PROTEIN_LABEL,
                KeggGraphExporter.PATHWAY_LABEL, KeggGraphExporter.ENZYME_LABEL,
                KeggGraphExporter.RCLASS_LABEL
        };
    }

    @Override
    public PathMappingDescription describe(final Graph graph, final Node[] nodes, final Edge[] edges) {
        if (edges.length == 1) {
            if (edges[0].getLabel().endsWith(KeggGraphExporter.TARGETS_LABEL))
                return new PathMappingDescription(PathMappingDescription.EdgeType.TARGETS);
            if (edges[0].getLabel().equals("CONTAINS_PATHWAY"))
                return new PathMappingDescription(PathMappingDescription.EdgeType.ASSOCIATED_WITH);
            if (edges[0].getLabel().equals("ASSOCIATED_WITH_PATHWAY"))
                return new PathMappingDescription(PathMappingDescription.EdgeType.ASSOCIATED_WITH);
            if (edges[0].getLabel().equals("CONTAINS_GENE"))
                return new PathMappingDescription(PathMappingDescription.EdgeType.ASSOCIATED_WITH);
        }
        return null;
    }

    @Override
    protected PathMapping[] getEdgePathMappings() {
        return new PathMapping[]{
                new PathMapping().add(KeggGraphExporter.DRUG_LABEL, KeggGraphExporter.TARGETS_LABEL,
                                      KeggGraphExporter.GENE_LABEL, EdgeDirection.FORWARD),
                new PathMapping().add(KeggGraphExporter.GENE_LABEL, "ENCODES",
                                      KeggGraphExporter.PROTEIN_LABEL, EdgeDirection.FORWARD),
                new PathMapping().add(KeggGraphExporter.ORGANISM_LABEL, "CONTAINS_PATHWAY",
                                      KeggGraphExporter.PATHWAY_LABEL, EdgeDirection.FORWARD),
                new PathMapping().add(KeggGraphExporter.GENE_LABEL, "ASSOCIATED_WITH_PATHWAY",
                                      KeggGraphExporter.PATHWAY_LABEL, EdgeDirection.FORWARD)
        };
    }
}
