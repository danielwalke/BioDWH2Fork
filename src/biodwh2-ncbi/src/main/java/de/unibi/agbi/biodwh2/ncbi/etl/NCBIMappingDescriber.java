package de.unibi.agbi.biodwh2.ncbi.etl;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.etl.MappingDescriber;
import de.unibi.agbi.biodwh2.core.model.IdentifierType;
import de.unibi.agbi.biodwh2.core.model.graph.*;

public class NCBIMappingDescriber extends MappingDescriber {
    public NCBIMappingDescriber(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public NodeMappingDescription[] describe(final Graph graph, final Node node, final String localMappingLabel) {
        if ("Gene".equals(localMappingLabel))
            return describeGene(node);
        if ("Compound".equals(localMappingLabel))
            return describeCompound(node);
        if ("Accession".equals(localMappingLabel))
            return describeAccession(node);
        if ("Ensembl".equals(localMappingLabel))
            return describeEnsembl(node);
        if ("MedGen".equals(localMappingLabel))
            return describeMedGen(node);
        if ("RefseqUniprotkbCollab".equals(localMappingLabel))
            return describeRefseqUniprotkbCollab(node);
        return null;
    }

    private NodeMappingDescription[] describeGene(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.GENE);
        description.addName(node.getProperty("symbol"));
        description.addNames(node.<String[]>getProperty("synonyms"));
        description.addIdentifier(IdentifierType.NCBI_GENE, node.<Long>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeCompound(final Node node) {
        final NodeMappingDescription description = new NodeMappingDescription(NodeMappingDescription.NodeType.COMPOUND);
        description.addName(node.getProperty("IUPAC_name"));
        description.addIdentifier(IdentifierType.PUB_CHEM_COMPOUND, node.<Long>getProperty("id"));
        return new NodeMappingDescription[]{description};
    }

    private NodeMappingDescription[] describeAccession(final Node node) {
        java.util.List<NodeMappingDescription> descriptions = new java.util.ArrayList<>();
        if (node.hasProperty("protein_accession.version")) {
            NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
            desc.addIdentifier(IdentifierType.REFSEQ, node.<String>getProperty("protein_accession.version"));
            descriptions.add(desc);
        }
        if (node.hasProperty("rna_nucleotide_accession.version")) {
            NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.RNA);
            desc.addIdentifier(IdentifierType.REFSEQ, node.<String>getProperty("rna_nucleotide_accession.version"));
            descriptions.add(desc);
        }
        return descriptions.toArray(new NodeMappingDescription[0]);
    }

    private NodeMappingDescription[] describeEnsembl(final Node node) {
        final java.util.List<NodeMappingDescription> descriptions = new java.util.ArrayList<>();
        if (node.hasProperty("ensembl_gene_identifier")) {
            final NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.GENE);
            desc.addIdentifier(IdentifierType.ENSEMBL, node.<String>getProperty("ensembl_gene_identifier"));
            descriptions.add(desc);
        }
        if (node.hasProperty("ensembl_protein_identifier")) {
            final NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
            desc.addIdentifier(IdentifierType.ENSEMBL, node.<String>getProperty("ensembl_protein_identifier"));
            descriptions.add(desc);
        }
        if (node.hasProperty("ensembl_rna_identifier")) {
            final NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.RNA);
            desc.addIdentifier(IdentifierType.ENSEMBL, node.<String>getProperty("ensembl_rna_identifier"));
            descriptions.add(desc);
        }
        return descriptions.toArray(new NodeMappingDescription[0]);
    }

    private NodeMappingDescription[] describeMedGen(final Node node) {
        final java.util.List<NodeMappingDescription> descriptions = new java.util.ArrayList<>();
        String type = node.getProperty("type");
        if ("phenotype".equals(type)) {
            final NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.PHENOTYPE);
            if (node.hasProperty("medgen_cui"))
                desc.addIdentifier(IdentifierType.UMLS_CUI, node.<String>getProperty("medgen_cui"));
            if (node.hasProperty("mim_number")) {
                try {
                    desc.addIdentifier(IdentifierType.OMIM, Integer.parseInt(node.<String>getProperty("mim_number")));
                } catch (NumberFormatException ignored) {}
            }
            descriptions.add(desc);
        }
        return descriptions.toArray(new NodeMappingDescription[0]);
    }

    private NodeMappingDescription[] describeRefseqUniprotkbCollab(final Node node) {
        final NodeMappingDescription desc = new NodeMappingDescription(NodeMappingDescription.NodeType.PROTEIN);
        if (node.hasProperty("ncbi_protein_accession"))
            desc.addIdentifier(IdentifierType.REFSEQ, node.<String>getProperty("ncbi_protein_accession"));
        if (node.hasProperty("uniprotkb_protein_accession"))
            desc.addIdentifier(IdentifierType.UNIPROT_KB, node.<String>getProperty("uniprotkb_protein_accession"));
        return new NodeMappingDescription[]{desc};
    }

    @Override
    protected String[] getNodeMappingLabels() {
        return new String[]{"Gene", "Compound", "Accession", "Ensembl", "MedGen", "RefseqUniprotkbCollab"};
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
