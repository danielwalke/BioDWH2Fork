package de.unibi.agbi.biodwh2.kegg.etl;

import com.fasterxml.jackson.databind.MappingIterator;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterFormatException;
import de.unibi.agbi.biodwh2.core.io.FileUtils;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import de.unibi.agbi.biodwh2.core.model.graph.NodeBuilder;
import de.unibi.agbi.biodwh2.kegg.KeggDataSource;
import de.unibi.agbi.biodwh2.kegg.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class KeggGraphExporter extends GraphExporter<KeggDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(KeggGraphExporter.class);
    static final String DRUG_LABEL = "Drug";
    static final String VARIANT_LABEL = "Variant";
    static final String REFERENCE_LABEL = "Reference";
    static final String DISEASE_LABEL = "Disease";
    static final String NETWORK_LABEL = "Network";
    static final String DRUG_GROUP_LABEL = "DrugGroup";
    static final String GENE_LABEL = "Gene";
    static final String COMPOUND_LABEL = "Compound";
    static final String ORGANISM_LABEL = "Organism";
    static final String REACTION_LABEL = "Reaction";
    static final String MODULE_LABEL = "Module";
    static final String PROTEIN_LABEL = "Protein";
    static final String PATHWAY_LABEL = "Pathway";
    static final String ENZYME_LABEL = "Enzyme";
    static final String BRITE_LABEL = "Brite";
    static final String GLYCAN_LABEL = "Glycan";
    static final String RCLASS_LABEL = "RClass";
    static final String TARGETS_LABEL = "TARGETS";

    public KeggGraphExporter(final KeggDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 8;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) {
        graph.addIndex(IndexDescription.forNode(GENE_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(DRUG_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(VARIANT_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(DISEASE_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(NETWORK_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(DRUG_GROUP_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(COMPOUND_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(ORGANISM_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REACTION_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(MODULE_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PROTEIN_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PATHWAY_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(ENZYME_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(BRITE_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(GLYCAN_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(RCLASS_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REFERENCE_LABEL, "pmid", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REFERENCE_LABEL, "doi", IndexDescription.Type.UNIQUE));
        exportHumanGenesList(workspace, graph);
        exportCompoundsList(workspace, graph);
        exportOrganismsList(workspace, graph);
        exportReactionsList(workspace, graph);
        exportModulesList(workspace, graph);
        exportProteinsList(workspace, graph);
        exportPathwaysList(workspace, graph);
        exportEnzymesList(workspace, graph);
        exportBriteList(workspace, graph);
                exportRClassList(workspace, graph);
        exportDrugs(graph);
        exportVariants(graph);
        exportDiseases(graph);
        exportNetworks(graph);
        exportDrugGroups(graph);
        exportLinks(workspace, graph);
        return true;
    }

    private void exportHumanGenesList(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.HUMAN_GENES_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                exportHumanGene(graph, row);
    }

    private Iterable<String[]> openTSV(final Workspace workspace, final String fileName) {
        try {
            final MappingIterator<String[]> iterator = FileUtils.openTsv(workspace, dataSource, fileName,
                                                                         String[].class);
            return () -> iterator;
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Skipping missing or unreadable file '" + fileName + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void exportHumanGene(final Graph graph, final String[] row) {
        final String id = row[0].trim();
        // list/hsa returns 4 columns: id, type, chromosome, name
        // Fall back to column 1 if only 2 columns present (legacy format)
        final String nameField = row.length >= 4 ? row[3].trim() : row[1].trim();
        if (nameField.contains(";")) {
            final String[] symbolsAndName = StringUtils.split(nameField, ";", 2);
            final String[] symbols = Arrays.stream(StringUtils.split(symbolsAndName[0], ",")).map(String::trim).toArray(
                    String[]::new);
            if (row.length >= 4)
                graph.addNode(GENE_LABEL, "id", id, "name", symbolsAndName[1].trim(), "symbols", symbols,
                              "gene_type", row[1].trim(), "chromosome", row[2].trim());
            else
                graph.addNode(GENE_LABEL, "id", id, "name", symbolsAndName[1].trim(), "symbols", symbols);
        } else {
            if (row.length >= 4)
                graph.addNode(GENE_LABEL, "id", id, "name", nameField, "gene_type", row[1].trim(),
                              "chromosome", row[2].trim());
            else
                graph.addNode(GENE_LABEL, "id", id, "name", nameField);
        }
    }

    private void exportCompoundsList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.COMPOUNDS_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(COMPOUND_LABEL, "id", row[0], "names",
                              StringUtils.splitByWholeSeparator(row[1], "; "));
    }

    private void exportOrganismsList(Workspace workspace, Graph graph) {
        java.util.Map<String, String> keggToTaxid = new java.util.HashMap<>();
        String organismsFilePath = dataSource.resolveSourceFilePath(workspace, KeggUpdater.ORGANISMS_FILE_NAME).toString();
        if (new java.io.File(organismsFilePath).exists()) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(organismsFilePath));
                String currentOrganism = null;
                for (String line : lines) {
                    if (line.startsWith("ENTRY ")) {
                        String[] parts = org.apache.commons.lang3.StringUtils.split(line, " 	");
                        if (parts.length > 1) {
                            currentOrganism = parts[1].trim();
                            if (currentOrganism.startsWith("T")) {
                                keggToTaxid.put(currentOrganism, "");
                            }
                        }
                    } else if (line.startsWith("TAXONOMY    TAX:") && currentOrganism != null) {
                        String taxid = line.substring("TAXONOMY    TAX:".length()).trim();
                        int semiColon = taxid.indexOf('\t');
                        if (semiColon != -1) taxid = taxid.substring(0, semiColon).trim();
                        keggToTaxid.put(currentOrganism, "NCBITaxon:" + taxid);
                        currentOrganism = null;
                    } else if (line.startsWith("///")) {
                        currentOrganism = null;
                    }
                }
            } catch (java.io.IOException e) {}
        }

        for (final String[] row : openTSV(workspace, KeggUpdater.ORGANISMS_LIST_FILE_NAME)) {
            if (row != null && row.length == 4) {
                String scientificName = row[2].trim();
                String organismId = row[0];
                String ncbiTaxonProperty = keggToTaxid.get(organismId);
                
                if (scientificName.endsWith(")")) {
                    int commonNameStart = scientificName.lastIndexOf('(');
                    String commonName = scientificName.substring(commonNameStart + 1, scientificName.length() - 1);
                    scientificName = scientificName.substring(0, commonNameStart - 1).trim();
                    de.unibi.agbi.biodwh2.core.model.graph.NodeBuilder builder = graph.buildNode().withLabel(ORGANISM_LABEL).withProperty("id", organismId).withProperty("symbol", row[1]).withProperty("name", scientificName).withProperty("common_name", commonName).withProperty("taxonomy", StringUtils.split(row[3], ';'));
                    if (ncbiTaxonProperty != null && !ncbiTaxonProperty.isEmpty()) builder.withProperty("ncbi_taxid", ncbiTaxonProperty);
                    builder.build();
                } else {
                    de.unibi.agbi.biodwh2.core.model.graph.NodeBuilder builder = graph.buildNode().withLabel(ORGANISM_LABEL).withProperty("id", organismId).withProperty("symbol", row[1]).withProperty("name", scientificName).withProperty("taxonomy", StringUtils.split(row[3], ';'));
                    if (ncbiTaxonProperty != null && !ncbiTaxonProperty.isEmpty()) builder.withProperty("ncbi_taxid", ncbiTaxonProperty);
                    builder.build();
                }
            }
        }
    }

    private void exportReactionsList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.REACTIONS_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(REACTION_LABEL, "id", row[0], "name", row[1]);

        final String filePath = dataSource.resolveSourceFilePath(workspace, KeggUpdater.REACTIONS_FILE_NAME).toString();
        if (new java.io.File(filePath).exists()) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));
                String currentReaction = null;
                for (String line : lines) {
                    if (line.startsWith("ENTRY ")) {
                        String[] parts = org.apache.commons.lang3.StringUtils.split(line, " 	");
                        if (parts.length > 1) {
                            currentReaction = parts[1];
                        }
                    } else if (line.startsWith("EQUATION ") && currentReaction != null) {
                        String equation = line.substring("EQUATION ".length()).trim();
                        parseAndAddEquationEdges(graph, currentReaction, equation);
                    } else if (line.startsWith("///")) {
                        currentReaction = null;
                    }
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseAndAddEquationEdges(Graph graph, String reactionId, String equation) {
        de.unibi.agbi.biodwh2.core.model.graph.Node reactionNode = graph.findNode(REACTION_LABEL, "id", reactionId);
        if (reactionNode == null) return;
        String[] sides = org.apache.commons.lang3.StringUtils.splitByWholeSeparator(equation, "<=>");
        if (sides.length > 0) {
            String[] reactants = org.apache.commons.lang3.StringUtils.splitByWholeSeparator(sides[0].trim(), " + ");
            for (String reactant : reactants) {
                addStoichiometryEdge(graph, reactionNode, reactant.trim(), "HAS_REACTANT");
            }
        }
        if (sides.length > 1) {
            String[] products = org.apache.commons.lang3.StringUtils.splitByWholeSeparator(sides[1].trim(), " + ");
            for (String product : products) {
                addStoichiometryEdge(graph, reactionNode, product.trim(), "HAS_PRODUCT");
            }
        }
    }

    private void addStoichiometryEdge(Graph graph, de.unibi.agbi.biodwh2.core.model.graph.Node reactionNode, String compoundPart, String edgeLabel) {
        if (compoundPart.isEmpty()) return;
        String stoichiometry = "1";
        String compoundId = compoundPart;
        int lastSpaceIndex = compoundPart.lastIndexOf(' ');
        if (lastSpaceIndex != -1) {
            stoichiometry = compoundPart.substring(0, lastSpaceIndex).trim();
            compoundId = compoundPart.substring(lastSpaceIndex + 1).trim();
        }
        de.unibi.agbi.biodwh2.core.model.graph.Node compoundNode = graph.findNode(COMPOUND_LABEL, "id", compoundId);
        if (compoundNode != null) {
            graph.addEdge(reactionNode, compoundNode, edgeLabel, "stoichiometry", stoichiometry);
        }
    }

    private void exportModulesList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.MODULES_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(MODULE_LABEL, "id", row[0], "name", row[1]);

        final String filePath = dataSource.resolveSourceFilePath(workspace, KeggUpdater.MODULES_FILE_NAME).toString();
        if (new java.io.File(filePath).exists()) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(filePath));
                String currentModule = null;
                int currentOrder = 1;
                boolean inReaction = false;
                for (String line : lines) {
                    if (line.startsWith("ENTRY ")) {
                        String[] parts = org.apache.commons.lang3.StringUtils.split(line, " 	");
                        if (parts.length > 1) {
                            currentModule = parts[1];
                        }
                        currentOrder = 1;
                        inReaction = false;
                    } else if (line.startsWith("REACTION ") || (inReaction && line.startsWith("            "))) {
                        inReaction = true;
                        String reactionPart = line.startsWith("REACTION ") ? line.substring("REACTION ".length()).trim() : line.trim();
                        if (!reactionPart.isEmpty() && reactionPart.contains("->")) {
                            String[] parts = org.apache.commons.lang3.StringUtils.splitByWholeSeparator(reactionPart, "  ");
                            de.unibi.agbi.biodwh2.core.model.graph.Node moduleNode = graph.findNode(MODULE_LABEL, "id", currentModule);
                            if (moduleNode != null && parts.length > 0) {
                                String[] reactions = org.apache.commons.lang3.StringUtils.split(parts[0].trim(), ",");
                                for (String reactionId : reactions) {
                                    de.unibi.agbi.biodwh2.core.model.graph.Node reactionNode = graph.findNode(REACTION_LABEL, "id", reactionId.trim());
                                    if (reactionNode != null) {
                                        graph.addEdge(moduleNode, reactionNode, "ASSOCIATED_WITH_REACTION", "order", currentOrder);
                                    }
                                }
                                if (parts.length > 1) {
                                    String[] compounds = org.apache.commons.lang3.StringUtils.splitByWholeSeparator(parts[1].trim(), " -> ");
                                    int compOrder = currentOrder;
                                    for (String compoundId : compounds) {
                                        for (String singleCompoundId : org.apache.commons.lang3.StringUtils.splitByWholeSeparator(compoundId, " + ")) {
                                            de.unibi.agbi.biodwh2.core.model.graph.Node compoundNode = graph.findNode(COMPOUND_LABEL, "id", singleCompoundId.trim());
                                            if (compoundNode != null) {
                                                graph.addEdge(moduleNode, compoundNode, "ASSOCIATED_WITH_COMPOUND", "order", compOrder);
                                            }
                                        }
                                        compOrder++;
                                    }
                                }
                            }
                            currentOrder++;
                        }
                    } else if (line.startsWith("///")) {
                        currentModule = null;
                        currentOrder = 1;
                        inReaction = false;
                    } else if (!line.startsWith(" ")) {
                        inReaction = false;
                    }
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void exportProteinsList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.ORTHOLOGY_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(PROTEIN_LABEL, "id", row[0], "name", row[1]);
    }

    private void exportPathwaysList(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.PATHWAYS_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(PATHWAY_LABEL, "id", row[0].trim(), "name", row[1].trim());
    }

    private void exportEnzymesList(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.ENZYMES_LIST_FILE_NAME)) {
            if (row != null && row.length >= 2) {
                // row[0] is bare EC number like "1.1.1.1" (no prefix in this endpoint)
                final String[] names = StringUtils.splitByWholeSeparator(row[1], "; ");
                graph.addNode(ENZYME_LABEL, "id", row[0].trim(), "names", names);
            }
        }
    }

    private void exportBriteList(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.BRITE_LIST_FILE_NAME)) {
            if (row != null && row.length == 2) {
                // row[0] is like "br:br08001" – strip the "br:" prefix for the stored id
                final String rawId = row[0].trim();
                final String id = rawId.contains(":") ? StringUtils.split(rawId, ":", 2)[1] : rawId;
                graph.addNode(BRITE_LABEL, "id", id, "name", row[1].trim());
            }
        }
    }

    private void exportGlycanList(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.GLYCAN_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(GLYCAN_LABEL, "id", row[0].trim(), "name", row[1].trim());
    }

    private void exportRClassList(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.RCLASS_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(RCLASS_LABEL, "id", row[0].trim(), "name", row[1].trim());
    }

    private void exportLinks(Workspace workspace, Graph graph) {
        exportLinkTSV(workspace, graph, KeggUpdater.REACTION_COMPOUND_FILE_NAME, REACTION_LABEL, COMPOUND_LABEL, "ASSOCIATED_WITH_COMPOUND");
        exportLinkTSV(workspace, graph, KeggUpdater.REACTION_KO_FILE_NAME, REACTION_LABEL, PROTEIN_LABEL, "ASSOCIATED_WITH_PROTEIN");
        exportLinkTSV(workspace, graph, KeggUpdater.MODULE_REACTION_FILE_NAME, MODULE_LABEL, REACTION_LABEL, "ASSOCIATED_WITH_REACTION");
        exportLinkTSV(workspace, graph, KeggUpdater.MODULE_KO_FILE_NAME, MODULE_LABEL, PROTEIN_LABEL, "ASSOCIATED_WITH_PROTEIN");
        exportLinkTSV(workspace, graph, KeggUpdater.MODULE_COMPOUND_FILE_NAME, MODULE_LABEL, COMPOUND_LABEL, "ASSOCIATED_WITH_COMPOUND");
        exportLinkTSV(workspace, graph, KeggUpdater.PATHWAY_KO_FILE_NAME, PROTEIN_LABEL, PATHWAY_LABEL, "ASSOCIATED_WITH_PATHWAY");
        exportLinkTSV(workspace, graph, KeggUpdater.PATHWAY_COMPOUND_FILE_NAME, COMPOUND_LABEL, PATHWAY_LABEL, "ASSOCIATED_WITH_PATHWAY");
        exportDiseaseDrugLinks(workspace, graph);
        exportDiseaseHsaLinks(workspace, graph);
        exportReactionEnzymeLinks(workspace, graph);
        exportUniProtHsaMappings(workspace, graph);
        exportNcbiProteinIdMappings(workspace, graph);
        exportGeneProteinLinks(workspace, graph);
    }

    private void exportLinkTSV(Workspace workspace, Graph graph, String fileName, String label1, String label2, String edgeLabel) {
        for (final String[] row : openTSV(workspace, fileName)) {
            if (row != null && row.length == 2) {
                String id1 = null;
                String id2 = null;
                for (String id : row) {
                    if (id.startsWith(getPrefix(label1))) id1 = stripPrefix(id);
                    else if (id.startsWith(getPrefix(label2))) id2 = stripPrefix(id);
                }
                if (id1 != null && id2 != null) {
                    Node node1 = graph.findNode(label1, "id", id1);
                    Node node2 = graph.findNode(label2, "id", id2);
                    if (node1 != null && node2 != null) {
                        graph.addEdge(node1, node2, edgeLabel);
                    }
                }
            }
        }
    }

    private String getPrefix(String label) {
        switch (label) {
            case REACTION_LABEL: return "rn:";
            case MODULE_LABEL: return "md:";
            case PROTEIN_LABEL: return "ko:";
            case COMPOUND_LABEL: return "cpd:";
            case PATHWAY_LABEL: return "path:";
            case ENZYME_LABEL: return "ec:";
            case GLYCAN_LABEL: return "gl:";
            case RCLASS_LABEL: return "rc:";
            default: return "";
        }
    }

    private String stripPrefix(String id) {
        if (id.contains(":")) {
            return StringUtils.split(id, ":", 2)[1];
        }
        return id;
    }

    /**
     * Exports disease←drug links from link/disease/drug.
     * Format: dr:D00162  ds:H00342
     * Disease is stored with id like "H00342" (flat-file ENTRY field, no prefix).
     * Drug is stored with id like "D00162" (flat-file ENTRY field, no prefix).
     */
    private void exportDiseaseDrugLinks(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.DISEASE_DRUG_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String drugId = stripPrefix(row[0].trim());   // D00162
                final String diseaseId = stripPrefix(row[1].trim()); // H00342
                final Node drugNode = graph.findNode(DRUG_LABEL, "id", drugId);
                final Node diseaseNode = graph.findNode(DISEASE_LABEL, "id", diseaseId);
                if (drugNode != null && diseaseNode != null)
                    graph.addEdge(diseaseNode, drugNode, "ASSOCIATED_WITH_DRUG");
                else if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Skipping disease-drug link: drug=" + drugId + " disease=" + diseaseId);
            }
        }
    }

    /**
     * Exports disease←gene (hsa) links from link/disease/hsa.
     * Format: hsa:5546  ds:H00021
     * Gene node id is stored as "hsa:5546" (full prefixed form from /list/hsa).
     */
    private void exportDiseaseHsaLinks(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.DISEASE_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String geneKeggId = row[0].trim();            // hsa:5546
                final String diseaseId = stripPrefix(row[1].trim()); // H00021
                // Gene nodes are indexed by id = "hsa:5546"
                final Node geneNode = graph.findNode(GENE_LABEL, "id", geneKeggId);
                final Node diseaseNode = graph.findNode(DISEASE_LABEL, "id", diseaseId);
                if (geneNode != null && diseaseNode != null)
                    graph.addEdge(diseaseNode, geneNode, "ASSOCIATED_WITH_GENE");
                else if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Skipping disease-hsa link: gene=" + geneKeggId + " disease=" + diseaseId);
            }
        }
    }

    /**
     * Exports reaction←enzyme links from link/reaction/enzyme.
     * Format: ec:1.1.1.1  rn:R02246
     * Enzyme node id is stored as bare EC number "1.1.1.1" (strip "ec:" prefix).
     * Reaction node id is stored as "R02246" (strip "rn:" prefix).
     */
    private void exportReactionEnzymeLinks(final Workspace workspace, final Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.REACTION_ENZYME_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String enzymeId = stripPrefix(row[0].trim()); // 1.1.1.1
                final String reactionId = stripPrefix(row[1].trim()); // R02246
                final Node enzymeNode = graph.findNode(ENZYME_LABEL, "id", enzymeId);
                final Node reactionNode = graph.findNode(REACTION_LABEL, "id", reactionId);
                if (enzymeNode != null && reactionNode != null)
                    graph.addEdge(reactionNode, enzymeNode, "CATALYZED_BY");
                else if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Skipping reaction-enzyme link: enzyme=" + enzymeId + " reaction=" + reactionId);
            }
        }
    }

    /**
     * Exports UniProt accession mappings from conv/uniprot/hsa.
     * Format: hsa:10458  up:A0A024RBG1
     * Stores as a property array "uniprot_ids" on the Gene node.
     */
    private void exportUniProtHsaMappings(final Workspace workspace, final Graph graph) {
        final Map<String, List<String>> geneToUniProt = new HashMap<>();
        for (final String[] row : openTSV(workspace, KeggUpdater.UNIPROT_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String geneKeggId = row[0].trim();      // hsa:10458
                final String uniprotAcc = stripPrefix(row[1].trim()); // A0A024RBG1
                geneToUniProt.computeIfAbsent(geneKeggId, k -> new ArrayList<>()).add(uniprotAcc);
            }
        }
        for (final Map.Entry<String, List<String>> entry : geneToUniProt.entrySet()) {
            final Node geneNode = graph.findNode(GENE_LABEL, "id", entry.getKey());
            if (geneNode != null) {
                geneNode.setProperty("uniprot_ids", entry.getValue().toArray(new String[0]));
                graph.update(geneNode);
            } else if (LOGGER.isDebugEnabled())
                LOGGER.debug("Skipping UniProt mapping for unknown gene: " + entry.getKey());
        }
    }

    /**
     * Exports NCBI protein accessions from conv/ncbi-proteinid/hsa.
     * Format: hsa:1  ncbi-proteinid:NP_570602
     * Stores as a property array "ncbi_protein_ids" on the Gene node.
     */
    private void exportNcbiProteinIdMappings(final Workspace workspace, final Graph graph) {
        final Map<String, List<String>> geneToNcbi = new HashMap<>();
        for (final String[] row : openTSV(workspace, KeggUpdater.NCBI_PROTEINID_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String geneKeggId = row[0].trim();       // hsa:1
                final String ncbiId = stripPrefix(row[1].trim()); // NP_570602
                geneToNcbi.computeIfAbsent(geneKeggId, k -> new ArrayList<>()).add(ncbiId);
            }
        }
        for (final Map.Entry<String, List<String>> entry : geneToNcbi.entrySet()) {
            final Node geneNode = graph.findNode(GENE_LABEL, "id", entry.getKey());
            if (geneNode != null) {
                geneNode.setProperty("ncbi_protein_ids", entry.getValue().toArray(new String[0]));
                graph.update(geneNode);
            } else if (LOGGER.isDebugEnabled())
                LOGGER.debug("Skipping NCBI protein ID mapping for unknown gene: " + entry.getKey());
        }
    }

    /**
     * Exports Gene→Protein (KO) links from link/ko/hsa.
     * Format: hsa:10  ko:K00622
     * Gene node id is stored as "hsa:10", Protein node id is stored as "ko:K00622".
     */
    private void exportGeneProteinLinks(final Workspace workspace, final Graph graph) {
        Map<String, Set<String>> proteinToUniProt = new LinkedHashMap<>();
        Map<String, String> geneToProtein = new HashMap<>();
        for (final String[] row : openTSV(workspace, KeggUpdater.KO_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String geneKeggId = row[0].trim();             // hsa:10
                final String proteinId = stripPrefix(row[1].trim()); // K00622 (strip "ko:")
                final Node geneNode = graph.findNode(GENE_LABEL, "id", geneKeggId);
                final Node proteinNode = graph.findNode(PROTEIN_LABEL, "id", proteinId);
                if (geneNode != null && proteinNode != null) {
                    graph.addEdge(geneNode, proteinNode, "ENCODES");
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Skipping gene-protein link: gene=" + geneKeggId + " protein=" + proteinId);
                }
                if (geneNode != null) {
                    geneToProtein.put(geneKeggId, proteinId);
                }
            }
        }
        for (final String geneKeggId : geneToProtein.keySet()) {
            final Node geneNode = graph.findNode(GENE_LABEL, "id", geneKeggId);
            if (geneNode != null) {
                final String[] uniprotIds = geneNode.getProperty("uniprot_ids");
                if (uniprotIds != null) {
                    final String proteinId = geneToProtein.get(geneKeggId);
                    if (proteinId != null) {
                        proteinToUniProt.computeIfAbsent(proteinId, k -> ConcurrentHashMap.newKeySet()).addAll(Arrays.asList(uniprotIds));
                    }
                }
            }
        }
        for (final Map.Entry<String, Set<String>> entry : proteinToUniProt.entrySet()) {
            final Node proteinNode = graph.findNode(PROTEIN_LABEL, "id", entry.getKey());
            if (proteinNode != null) {
                proteinNode.setProperty("uniprot_ids", entry.getValue().toArray(new String[0]));
                graph.update(proteinNode);
            }
        }
    }

    private void exportDrugs(final Graph graph) {
        for (final Drug drug : dataSource.drugs)
            exportDrug(graph, drug);
    }

    private void exportDrug(final Graph graph, final Drug drug) {
        final NodeBuilder builder = getNodeBuilderForKeggEntry(graph, drug, DRUG_LABEL);
        builder.withPropertyIfNotNull("formula", drug.formula);
        builder.withPropertyIfNotNull("exact_mass", drug.exactMass);
        builder.withPropertyIfNotNull("molecular_weight", drug.molecularWeight);
        builder.withPropertyIfNotNull("atoms", drug.atoms);
        builder.withPropertyIfNotNull("bonds", drug.bonds);
        builder.withPropertyIfNotNull("bracket", drug.bracket);
        builder.withPropertyIfNotNull("name_abbreviation", drug.nameAbbreviation);
        builder.withPropertyIfNotNull("efficacy", drug.efficacy);
        final Node node = builder.build();
        addAllReferencesForEntry(graph, drug, node);
        for (final Sequence sequence : drug.sequences)
            graph.addEdge(node, graph.addNodeFromModel(sequence), "HAS_SEQUENCE");
        exportDrugGeneRelations(graph, drug, node);
        for (final NameIdsPair disease : drug.efficacyDiseases) {
            final Node diseaseNode = findEntry(graph, disease.ids);
            if (diseaseNode != null)
                graph.addEdge(node, diseaseNode, "EFFICACY_DISEASE");
        }
        for (final ParentChildRelation classRelation : drug.classes) {
            final Node classNode = findEntry(graph, classRelation.child.ids);
            if (classNode != null)
                graph.addEdge(node, classNode, "BELONGS_TO_CLASS");
        }
        for (final NameIdsPair target : drug.networkTargets) {
            final Node targetNode = findEntry(graph, target.ids);
            if (targetNode != null)
                graph.addEdge(node, targetNode, "NETWORK_TARGET");
        }
        for (final NameIdsPair source : drug.sources) {
            final Node sourceNode = findEntry(graph, source.ids);
            if (sourceNode != null)
                graph.addEdge(node, sourceNode, "HAS_SOURCE");
        }
        for (final List<NameIdsPair> mixture : drug.mixtures) {
            for (final NameIdsPair component : mixture) {
                final Node componentNode = findEntry(graph, component.ids);
                if (componentNode != null)
                    graph.addEdge(node, componentNode, "HAS_MIXTURE_COMPONENT");
            }
        }
    }

    private NodeBuilder getNodeBuilderForKeggEntry(final Graph graph, final KeggEntry entry, final String label) {
        final NodeBuilder builder = graph.buildNode().withLabel(label);
        builder.withProperty("id", entry.id);
        if (entry.tags.size() > 1)
            builder.withProperty("tags", entry.tags.toArray(new String[0]));
        if (entry.names.size() == 1)
            builder.withProperty("name", entry.names.get(0));
        if (entry.names.size() > 1)
            builder.withProperty("names", entry.names.toArray(new String[0]));
        if (entry.externalIds.size() > 0)
            builder.withProperty("external_identifier", entry.externalIds.toArray(new String[0]));
        if (entry.remarks.size() > 0)
            builder.withProperty("remarks", entry.remarks.toArray(new String[0]));
        if (entry.comments.size() > 0)
            builder.withProperty("comments", entry.comments.toArray(new String[0]));
        return builder;
    }

    private void addAllReferencesForEntry(final Graph graph, final KeggEntry entry, final Node node) {
        for (final Reference reference : entry.references) {
            final Node referenceNode = getOrCreateReference(graph, reference);
            if (reference.remarks != null)
                graph.addEdge(node, referenceNode, "HAS_REFERENCE", "remarks", reference.remarks);
            else
                graph.addEdge(node, referenceNode, "HAS_REFERENCE");
        }
    }

    private Node getOrCreateReference(final Graph graph, final Reference reference) {
        Node node = null;
        if (StringUtils.isNotEmpty(reference.doi))
            node = graph.findNode("Reference", "doi", reference.doi);
        if (node == null)
            node = graph.findNode("Reference", "pmid", reference.pmid);
        if (node == null)
            node = graph.addNodeFromModel(reference);
        return node;
    }

    private void exportDrugGeneRelations(final Graph graph, final Drug drug, final Node node) {
        for (final NameIdsPair target : drug.targets) {
            final Node geneNode = findEntry(graph, target.ids);
            if (geneNode != null)
                graph.addEdge(node, geneNode, TARGETS_LABEL);
            else
                LOGGER.warn("Failed to add targets relation for drug " + drug.id + " and target " + target);
        }
        for (final Metabolism metabolism : drug.metabolisms) {
            final Node geneNode = findEntry(graph, metabolism.target.ids);
            if (geneNode != null)
                graph.addEdge(node, geneNode, TARGETS_LABEL, "type", "substrate", "target_type",
                              metabolism.type.toLowerCase(Locale.ROOT));
            else
                LOGGER.warn(
                        "Failed to add metabolism relation for drug " + drug.id + " and target " + metabolism.target);
        }
        for (final Interaction interaction : drug.interactions) {
            final Node geneNode = findEntry(graph, interaction.target.ids);
            if (geneNode == null) {
                LOGGER.warn(
                        "Failed to add interaction relation for drug " + drug.id + " and target " + interaction.target);
                continue;
            }
            switch (interaction.type.toLowerCase(Locale.ROOT)) {
                case "cyp inhibition":
                    graph.addEdge(node, geneNode, TARGETS_LABEL, "type", "inhibitor", "target_type", "cyp");
                    break;
                case "cyp induction":
                    graph.addEdge(node, geneNode, TARGETS_LABEL, "type", "inducer", "target_type", "cyp");
                    break;
                case "transporter inhibition":
                    graph.addEdge(node, geneNode, TARGETS_LABEL, "type", "inhibitor", "target_type", "transporter");
                    break;
                case "transporter induction":
                    graph.addEdge(node, geneNode, TARGETS_LABEL, "type", "inducer", "target_type", "transporter");
                    break;
                case "enzyme inhibition":
                    graph.addEdge(node, geneNode, TARGETS_LABEL, "type", "inhibitor", "target_type", "enzyme");
                    break;
                default:
                    if (LOGGER.isWarnEnabled())
                        LOGGER.warn("Unhandled interaction type " + interaction.type);
                    graph.addEdge(node, geneNode, TARGETS_LABEL, "type", interaction.type);
                    break;
            }
        }
    }

    private Node findEntry(final Graph graph, final String id) {
        if (id.startsWith("HSA"))
            return graph.findNode(GENE_LABEL, "id", id.toLowerCase(Locale.ROOT));
        if (org.apache.commons.lang3.StringUtils.isNumeric(id))
            return graph.findNode(GENE_LABEL, "id", "hsa:" + id);
        if (id.startsWith("DG"))
            return graph.findNode(DRUG_GROUP_LABEL, "id", id);
        if (id.startsWith("D"))
            return graph.findNode(DRUG_LABEL, "id", id);
        if (id.startsWith("C"))
            return graph.findNode(COMPOUND_LABEL, "id", id);
        return null;
    }

    private void exportVariants(final Graph graph) {
        for (final Variant variant : dataSource.variants)
            exportVariant(graph, variant);
    }

    private void exportVariant(final Graph graph, final Variant variant) {
        final NodeBuilder builder = getNodeBuilderForKeggEntry(graph, variant, VARIANT_LABEL);
        builder.withPropertyIfNotNull("organism", variant.organism);
        final List<String> variationStrings = new ArrayList<>();
        for (final NameIdsPair variation : variant.variations) {
            variationStrings.add(variation.name + " (" + String.join(", ", variation.ids) + ")");
        }
        if (!variationStrings.isEmpty())
            builder.withProperty("variations", variationStrings.toArray(new String[0]));

        final Node node = builder.build();
        addAllReferencesForEntry(graph, variant, node);

        for (final NameIdsPair gene : variant.genes.values()) {
            final Node geneNode = findEntry(graph, gene.ids);
            if (geneNode != null)
                graph.addEdge(node, geneNode, "ASSOCIATED_WITH_GENE");
        }
        for (final NetworkLink network : variant.networks) {
            final Node networkNode = findEntry(graph, network.network.ids);
            if (networkNode != null)
                graph.addEdge(node, networkNode, "ASSOCIATED_WITH_NETWORK");
        }
    }

    private void exportDiseases(final Graph graph) {
        for (final Disease disease : dataSource.diseases)
            exportDisease(graph, disease);
        final Map<Long, Set<Long>> addedHierarchyRelationsCache = new HashMap<>();
        for (final Disease disease : dataSource.diseases)
            exportDiseaseHierarchy(graph, addedHierarchyRelationsCache, disease);
    }

    private void exportDisease(final Graph graph, final Disease disease) {
        final NodeBuilder builder = getNodeBuilderForKeggEntry(graph, disease, DISEASE_LABEL);
        builder.withPropertyIfNotNull("description", disease.description);
        if (disease.categories.size() > 0)
            builder.withProperty("categories", disease.categories.toArray(new String[0]));
        if (disease.envFactors.size() > 0)
            builder.withProperty("env_factors",
                                 disease.envFactors.stream().map(Object::toString).toArray(String[]::new));
        if (disease.carcinogens.size() > 0)
            builder.withProperty("carcinogens",
                                 disease.carcinogens.stream().map(Object::toString).toArray(String[]::new));
        if (disease.pathogens.size() > 0)
            builder.withProperty("pathogens", disease.pathogens.stream().map(Object::toString).toArray(String[]::new));
        if (disease.pathogenModules.size() > 0)
            builder.withProperty("pathogen_modules",
                                 disease.pathogenModules.stream().map(Object::toString).toArray(String[]::new));
        final Node node = builder.build();
        addAllReferencesForEntry(graph, disease, node);

        for (final NetworkLink network : disease.networks) {
            final Node networkNode = findEntry(graph, network.network.ids);
            if (networkNode != null)
                graph.addEdge(node, networkNode, "ASSOCIATED_WITH_NETWORK");
        }
        for (final NameIdsPair drug : disease.drugs) {
            final Node drugNode = findEntry(graph, drug.ids);
            if (drugNode != null)
                graph.addEdge(node, drugNode, "ASSOCIATED_WITH_DRUG");
        }
        for (final NameIdsPair gene : disease.genes) {
            final Node geneNode = findEntry(graph, gene.ids);
            if (geneNode != null)
                graph.addEdge(node, geneNode, "ASSOCIATED_WITH_GENE");
        }
    }

    private void exportDiseaseHierarchy(final Graph graph, final Map<Long, Set<Long>> addedHierarchyRelationsCache,
                                        final Disease disease) {
        final Node node = graph.findNode(DISEASE_LABEL, "id", disease.id);
        if (!addedHierarchyRelationsCache.containsKey(node.getId()))
            addedHierarchyRelationsCache.put(node.getId(), new HashSet<>());
        for (final NameIdsPair group : disease.subGroups) {
            if (group.ids.size() == 0) {
                LOGGER.warn(
                        "Failed to add disease hierarchy relation with parent " + disease.id + " and child " + group);
                continue;
            }
            final String childId = StringUtils.split(group.ids.get(0), ":", 2)[1];
            final Node child = graph.findNode(DISEASE_LABEL, "id", childId);
            if (child == null) {
                LOGGER.warn(
                        "Failed to add disease hierarchy relation with parent " + disease.id + " and child " + group);
                continue;
            }
            final Set<Long> childIds = addedHierarchyRelationsCache.get(node.getId());
            if (!childIds.contains(child.getId())) {
                graph.addEdge(node, child, "HAS_MEMBER");
                childIds.add(child.getId());
            }
        }
        for (final NameIdsPair group : disease.superGroups) {
            if (group.ids.size() == 0) {
                LOGGER.warn(
                        "Failed to add disease hierarchy relation with parent " + group + " and child " + disease.id);
                continue;
            }
            final String parentId = StringUtils.split(group.ids.get(0), ":", 2)[1];
            final Node parent = graph.findNode(DISEASE_LABEL, "id", parentId);
            if (parent == null) {
                LOGGER.warn(
                        "Failed to add disease hierarchy relation with parent " + group + " and child " + disease.id);
                continue;
            }
            if (!addedHierarchyRelationsCache.containsKey(parent.getId()))
                addedHierarchyRelationsCache.put(parent.getId(), new HashSet<>());
            final Set<Long> childIds = addedHierarchyRelationsCache.get(parent.getId());
            if (!childIds.contains(node.getId())) {
                graph.addEdge(parent, node, "HAS_MEMBER");
                childIds.add(node.getId());
            }
        }
    }

    private void exportNetworks(final Graph graph) {
        for (final Network network : dataSource.networks)
            exportNetwork(graph, network);
    }

    private void exportNetwork(final Graph graph, final Network network) {
        final NodeBuilder builder = getNodeBuilderForKeggEntry(graph, network, NETWORK_LABEL);
        builder.withPropertyIfNotNull("type", network.type);
        builder.withPropertyIfNotNull("definition", network.definition);
        builder.withPropertyIfNotNull("expanded_definition", network.expandedDefinition);
        final Node node = builder.build();
        addAllReferencesForEntry(graph, network, node);

        Map<String, Integer> geneOrderMap = new HashMap<>();
        if (network.expandedDefinition != null) {
            int order = 1;
            String[] steps = org.apache.commons.lang3.StringUtils.splitByWholeSeparator(network.expandedDefinition, " -> ");
            for (String step : steps) {
                String cleanStep = step.replace("(", "").replace(")", "").trim();
                for (String id : org.apache.commons.lang3.StringUtils.split(cleanStep, ',')) {
                    geneOrderMap.put(id.trim(), order);
                }
                order++;
            }
        }

        for (final NameIdsPair gene : network.genes) {
            final Node geneNode = findEntry(graph, gene.ids);
            if (geneNode != null) {
                Integer order = null;
                for (String id : gene.ids) {
                    if (geneOrderMap.containsKey(id)) {
                        order = geneOrderMap.get(id);
                        break;
                    }
                }
                if (order != null) {
                    graph.addEdge(node, geneNode, "ASSOCIATED_WITH_GENE", "order", order);
                } else {
                    graph.addEdge(node, geneNode, "ASSOCIATED_WITH_GENE");
                }
            }
        }
        for (final NameIdsPair variant : network.variants) {
            final Node variantNode = findEntry(graph, variant.ids);
            if (variantNode != null)
                graph.addEdge(node, variantNode, "ASSOCIATED_WITH_VARIANT");
        }
        for (final NameIdsPair disease : network.diseases) {
            final Node diseaseNode = findEntry(graph, disease.ids);
            if (diseaseNode != null)
                graph.addEdge(node, diseaseNode, "ASSOCIATED_WITH_DISEASE");
        }
        for (final NameIdsPair member : network.members) {
            final Node memberNode = findEntry(graph, member.ids);
            if (memberNode != null)
                graph.addEdge(node, memberNode, "HAS_MEMBER");
        }
        for (final NameIdsPair perturbant : network.perturbants) {
            final Node perturbantNode = findEntry(graph, perturbant.ids);
            if (perturbantNode != null)
                graph.addEdge(node, perturbantNode, "HAS_PERTURBANT");
        }
        for (final NameIdsPair classPair : network.classes) {
            final Node classNode = findEntry(graph, classPair.ids);
            if (classNode != null)
                graph.addEdge(node, classNode, "BELONGS_TO_CLASS");
        }
        int metaboliteOrder = 1;
        for (final NameIdsPair metabolite : network.metabolites) {
            final de.unibi.agbi.biodwh2.core.model.graph.Node metaboliteNode = findEntry(graph, metabolite.ids);
            if (metaboliteNode != null) {
                graph.addEdge(node, metaboliteNode, "ASSOCIATED_WITH_COMPOUND", "order", metaboliteOrder);
            }
            metaboliteOrder++;
        }
    }

    private void exportDrugGroups(final Graph graph) {
        for (final DrugGroup drugGroup : dataSource.drugGroups)
            exportDrugGroup(graph, drugGroup);
        final Map<Long, Set<Long>> addedHierarchyRelationsCache = new HashMap<>();
        for (final DrugGroup drugGroup : dataSource.drugGroups)
            exportDrugGroupHierarchy(graph, addedHierarchyRelationsCache, drugGroup);
    }

    private void exportDrugGroup(final Graph graph, final DrugGroup drugGroup) {
        final NodeBuilder builder = getNodeBuilderForKeggEntry(graph, drugGroup, DRUG_GROUP_LABEL);
        if (drugGroup.nameStems.size() > 0)
            builder.withProperty("name_stems", drugGroup.nameStems.toArray(new String[0]));
        builder.withPropertyIfNotNull("name_abbreviation", drugGroup.nameAbbreviation);
        final Node node = builder.build();
        addAllReferencesForEntry(graph, drugGroup, node);
    }

    private void exportDrugGroupHierarchy(final Graph graph, final Map<Long, Set<Long>> addedHierarchyRelationsCache,
                                          final DrugGroup drugGroup) {
        for (final ParentChildRelation classRelation : drugGroup.classes) {
            final Node parent = classRelation.parent == null ? null : findEntry(graph, classRelation.parent.ids);
            final Node child = findEntry(graph, classRelation.child.ids);
            if (parent != null && child != null) {
                if (!addedHierarchyRelationsCache.containsKey(parent.getId()))
                    addedHierarchyRelationsCache.put(parent.getId(), new HashSet<>());
                final Set<Long> childIds = addedHierarchyRelationsCache.get(parent.getId());
                if (!childIds.contains(child.getId())) {
                    graph.addEdge(parent, child, "HAS_CLASS");
                    childIds.add(child.getId());
                }
            }
        }
        for (final ParentChildRelation relation : drugGroup.members) {
            final Node parent = relation.parent == null ? graph.findNode(DRUG_GROUP_LABEL, "id", drugGroup.id) :
                                findEntry(graph, relation.parent.ids);
            final Node child = findEntry(graph, relation.child.ids);
            if (parent != null && child != null) {
                if (!addedHierarchyRelationsCache.containsKey(parent.getId()))
                    addedHierarchyRelationsCache.put(parent.getId(), new HashSet<>());
                final Set<Long> childIds = addedHierarchyRelationsCache.get(parent.getId());
                if (!childIds.contains(child.getId())) {
                    graph.addEdge(parent, child, "HAS_MEMBER");
                    childIds.add(child.getId());
                }
            } else {
                final String parentInfo = relation.parent == null ? drugGroup.id : relation.parent.toString();
                LOGGER.warn("Failed to add drug hierarchy relation with parent " + parentInfo + " and child " +
                            relation.child);
            }
        }
    }

    private Node findEntry(final Graph graph, final Iterable<String> ids) {
        for (final String id : ids) {
            final Node node = findEntry(graph, id);
            if (node != null)
                return node;
        }
        return null;
    }
}
