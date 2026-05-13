import re

with open('/home/daniel.walke/server_data/kegg_data/BioDWH2/src/biodwh2-kegg/src/main/java/de/unibi/agbi/biodwh2/kegg/etl/KeggGraphExporter.java', 'r') as f:
    content = f.read()

# Chunk 1
content = content.replace('public static final String RCLASS_LABEL = "ReactionClass";\n    public static final String GLYCAN_LABEL = "Glycan";', 'public static final String RCLASS_LABEL = "ReactionClass";')
content = content.replace('public static final String GLYCAN_LABEL = "Glycan";\n    public static final String RCLASS_LABEL = "ReactionClass";', 'public static final String RCLASS_LABEL = "ReactionClass";')

# Chunk 2
content = content.replace('exportGlycanList(workspace, graph);\n', '')

# Chunk 3
org_old = """    private void exportOrganismsList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.ORGANISMS_LIST_FILE_NAME)) {
            if (row != null && row.length == 4) {
                String scientificName = row[2].trim();
                if (scientificName.endsWith(")")) {
                    int commonNameStart = scientificName.lastIndexOf('(');
                    String commonName = scientificName.substring(commonNameStart + 1, scientificName.length() - 1);
                    scientificName = scientificName.substring(0, commonNameStart - 1).trim();
                    graph.addNode(ORGANISM_LABEL, "id", row[0], "symbol", row[1], "name", scientificName, "common_name",
                                  commonName, "taxonomy", StringUtils.split(row[3], ';'));
                } else {
                    graph.addNode(ORGANISM_LABEL, "id", row[0], "symbol", row[1], "name", scientificName, "taxonomy",
                                  StringUtils.split(row[3], ';'));
                }
            }
        }
    }"""
org_new = """    private void exportOrganismsList(Workspace workspace, Graph graph) {
        java.util.Map<String, String> keggToTaxid = new java.util.HashMap<>();
        String taxonomyFilePath = dataSource.resolveSourceFilePath(workspace, KeggUpdater.TAXONOMY_GENOME_FILE_NAME).toString();
        if (new java.io.File(taxonomyFilePath).exists()) {
            try {
                for (String line : java.nio.file.Files.readAllLines(java.nio.file.Paths.get(taxonomyFilePath))) {
                    String[] parts = org.apache.commons.lang3.StringUtils.split(line, '\t');
                    if (parts.length == 2) {
                        keggToTaxid.put(parts[0].replace("gn:", ""), parts[1].replace("taxid:", ""));
                    }
                }
            } catch (java.io.IOException e) {}
        }

        for (final String[] row : openTSV(workspace, KeggUpdater.ORGANISMS_LIST_FILE_NAME)) {
            if (row != null && row.length == 4) {
                String scientificName = row[2].trim();
                String organismId = row[0];
                String taxid = keggToTaxid.get(organismId);
                String ncbiTaxonProperty = taxid != null ? "NCBITaxon:" + taxid : null;
                
                if (scientificName.endsWith(")")) {
                    int commonNameStart = scientificName.lastIndexOf('(');
                    String commonName = scientificName.substring(commonNameStart + 1, scientificName.length() - 1);
                    scientificName = scientificName.substring(0, commonNameStart - 1).trim();
                    de.unibi.agbi.biodwh2.core.model.graph.NodeBuilder builder = graph.buildNode().withLabel(ORGANISM_LABEL).withProperty("id", organismId).withProperty("symbol", row[1]).withProperty("name", scientificName).withProperty("common_name", commonName).withProperty("taxonomy", StringUtils.split(row[3], ';'));
                    if (ncbiTaxonProperty != null) builder.withProperty("ncbi_taxid", ncbiTaxonProperty);
                    builder.build();
                } else {
                    de.unibi.agbi.biodwh2.core.model.graph.NodeBuilder builder = graph.buildNode().withLabel(ORGANISM_LABEL).withProperty("id", organismId).withProperty("symbol", row[1]).withProperty("name", scientificName).withProperty("taxonomy", StringUtils.split(row[3], ';'));
                    if (ncbiTaxonProperty != null) builder.withProperty("ncbi_taxid", ncbiTaxonProperty);
                    builder.build();
                }
            }
        }
    }"""
content = content.replace(org_old, org_new)

# Chunk 4
reac_old = """    private void exportReactionsList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.REACTIONS_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(REACTION_LABEL, "id", row[0], "name", row[1]);
    }"""
reac_new = """    private void exportReactionsList(Workspace workspace, Graph graph) {
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
                        String[] parts = org.apache.commons.lang3.StringUtils.split(line, " \t");
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
    }"""
content = content.replace(reac_old, reac_new)

# Chunk 5
mod_old = """    private void exportModulesList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.MODULES_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(MODULE_LABEL, "id", row[0], "name", row[1]);
    }"""
mod_new = """    private void exportModulesList(Workspace workspace, Graph graph) {
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
                        String[] parts = org.apache.commons.lang3.StringUtils.split(line, " \t");
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
    }"""
content = content.replace(mod_old, mod_new)

# Chunk 6
glycan_old = """    private void exportGlycanList(Workspace workspace, Graph graph) {
        for (final String[] row : openTSV(workspace, KeggUpdater.GLYCAN_LIST_FILE_NAME))
            if (row != null && row.length >= 2)
                graph.addNode(GLYCAN_LABEL, "id", row[0], "name", row[1]);
    }"""
content = content.replace(glycan_old, "")

# Chunk 7
links_old = """        exportLinkTSV(workspace, graph, KeggUpdater.PATHWAY_COMPOUND_FILE_NAME, PATHWAY_LABEL, COMPOUND_LABEL,
                      "ASSOCIATED_WITH_COMPOUND");
        exportLinkTSV(workspace, graph, KeggUpdater.DISEASE_DRUG_FILE_NAME, DISEASE_LABEL, DRUG_LABEL,
                      "ASSOCIATED_WITH_DRUG");"""
links_new = """        exportLinkTSV(workspace, graph, KeggUpdater.PATHWAY_COMPOUND_FILE_NAME, PATHWAY_LABEL, COMPOUND_LABEL,
                      "ASSOCIATED_WITH_COMPOUND");
        exportLinkTSV(workspace, graph, KeggUpdater.PATHWAY_REACTION_FILE_NAME, PATHWAY_LABEL, REACTION_LABEL,
                      "ASSOCIATED_WITH_REACTION");
        exportLinkTSV(workspace, graph, KeggUpdater.DISEASE_DRUG_FILE_NAME, DISEASE_LABEL, DRUG_LABEL,
                      "ASSOCIATED_WITH_DRUG");"""
content = content.replace(links_old, links_new)

# Chunk 8
uni_old = """    private void exportUniProtHsaMappings(final Workspace workspace, final Graph graph) {
        final Map<String, List<String>> geneToUniProt = new HashMap<>();
        for (final String[] row : openTSV(workspace, KeggUpdater.UNIPROT_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String geneKeggId = row[0].trim();          // hsa:10
                final String uniprotId = stripPrefix(row[1].trim()); // P31946
                geneToUniProt.computeIfAbsent(geneKeggId, k -> new ArrayList<>()).add(uniprotId);
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
    }"""
uni_new = """    private void exportUniProtHsaMappings(final Workspace workspace, final Graph graph) {
        final java.util.Map<String, java.util.List<String>> geneToUniProt = new java.util.HashMap<>();
        for (final String[] row : openTSV(workspace, KeggUpdater.UNIPROT_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                final String geneKeggId = row[0].trim();          // hsa:10
                final String uniprotId = stripPrefix(row[1].trim()); // P31946
                geneToUniProt.computeIfAbsent(geneKeggId, k -> new java.util.ArrayList<>()).add(uniprotId);
            }
        }
        
        final java.util.Map<String, java.util.List<String>> geneToKo = new java.util.HashMap<>();
        for (final String[] row : openTSV(workspace, KeggUpdater.KO_HSA_FILE_NAME)) {
            if (row != null && row.length == 2) {
                geneToKo.computeIfAbsent(row[0].trim(), k -> new java.util.ArrayList<>()).add(stripPrefix(row[1].trim()));
            }
        }

        for (final java.util.Map.Entry<String, java.util.List<String>> entry : geneToUniProt.entrySet()) {
            java.util.List<String> koIds = geneToKo.get(entry.getKey());
            if (koIds != null) {
                for (String koId : koIds) {
                    final de.unibi.agbi.biodwh2.core.model.graph.Node proteinNode = graph.findNode(PROTEIN_LABEL, "id", koId);
                    if (proteinNode != null) {
                        String[] existing = proteinNode.getProperty("uniprot_ids");
                        java.util.Set<String> allIds = new java.util.HashSet<>(entry.getValue());
                        if (existing != null) allIds.addAll(java.util.Arrays.asList(existing));
                        proteinNode.setProperty("uniprot_ids", allIds.toArray(new String[0]));
                        graph.update(proteinNode);
                    }
                }
            }
        }
    }"""
content = content.replace(uni_old, uni_new)

# Chunk 9
net_old = """        for (final NameIdsPair metabolite : network.metabolites) {
            final Node metaboliteNode = findEntry(graph, metabolite.ids);
            if (metaboliteNode != null)
                graph.addEdge(node, metaboliteNode, "ASSOCIATED_WITH_METABOLITE");
        }"""
net_new = """        int metaboliteOrder = 1;
        for (final NameIdsPair metabolite : network.metabolites) {
            final de.unibi.agbi.biodwh2.core.model.graph.Node metaboliteNode = findEntry(graph, metabolite.ids);
            if (metaboliteNode != null) {
                graph.addEdge(node, metaboliteNode, "ASSOCIATED_WITH_COMPOUND", "order", metaboliteOrder);
            }
            metaboliteOrder++;
        }"""
content = content.replace(net_old, net_new)

with open('/home/daniel.walke/server_data/kegg_data/BioDWH2/src/biodwh2-kegg/src/main/java/de/unibi/agbi/biodwh2/kegg/etl/KeggGraphExporter.java', 'w') as f:
    f.write(content)

