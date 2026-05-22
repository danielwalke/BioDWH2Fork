package de.unibi.agbi.biodwh2.cazy.etl;

import de.unibi.agbi.biodwh2.cazy.CazyDataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterFormatException;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CazyGraphExporter extends GraphExporter<CazyDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(CazyGraphExporter.class);

    private static final Map<String, String> CLASS_NAMES = new HashMap<>();
    static {
        CLASS_NAMES.put("GH", "Glycoside Hydrolases");
        CLASS_NAMES.put("GT", "GlycosylTransferases");
        CLASS_NAMES.put("PL", "Polysaccharide Lyases");
        CLASS_NAMES.put("CE", "Carbohydrate Esterases");
        CLASS_NAMES.put("AA", "Auxiliary Activities");
        CLASS_NAMES.put("CBM", "Carbohydrate-Binding Modules");
    }

    public CazyGraphExporter(final CazyDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 5;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode("CazyClass", "code", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode("CazyFamily", "code", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode("CazyOrganism", "name", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode("CazyProtein", "id", IndexDescription.Type.UNIQUE));

        final Path structuresPath = dataSource.resolveSourceFilePath(workspace, CazyUpdater.STRUCTURES_FILE_NAME);
        if (!Files.exists(structuresPath)) {
            throw new ExporterException("Structures file not found: " + CazyUpdater.STRUCTURES_FILE_NAME);
        }

        final Set<String> genBankIds = new HashSet<>();
        try (final BufferedReader reader = Files.newBufferedReader(structuresPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                final String[] cols = line.split("\t", -1);
                if (cols.length >= 5) {
                    final String genbank = cols[4].trim();
                    if (!genbank.isEmpty()) {
                        for (final String part : genbank.split(";")) {
                            final String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                genBankIds.add(trimmed);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to read structures TSV file to collect GenBank IDs", e);
        }

        final Map<String, Integer> genbankToTaxonId = loadNCBITaxonMappings(workspace, genBankIds);

        try (final BufferedReader reader = Files.newBufferedReader(structuresPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                final String[] cols = line.split("\t", -1);
                if (cols.length < 8)
                    continue;

                final String familyCode = cols[0].trim();
                final String proteinName = cols[1].trim();
                final String ec = cols[2].trim();
                final String organismName = cols[3].trim();
                final String genbank = cols[4].trim();
                final String uniprot = cols[5].trim();

                if (familyCode.isEmpty())
                    continue;

                // Determine Class code
                String classCode = familyCode;
                if (classCode.startsWith("CBM")) {
                    classCode = "CBM";
                } else if (classCode.length() > 2) {
                    classCode = familyCode.substring(0, 2);
                } else {
                    continue;
                }

                // Get or create Class
                Node classNode = graph.findNode("CazyClass", "code", classCode);
                if (classNode == null) {
                    classNode = graph.addNode("CazyClass", "code", classCode, "name", CLASS_NAMES.getOrDefault(classCode, classCode));
                }

                // Get or create Family
                Node familyNode = graph.findNode("CazyFamily", "code", familyCode);
                if (familyNode == null) {
                    familyNode = graph.addNode("CazyFamily", "code", familyCode, "name", getFamilyName(familyCode));
                    graph.addEdge(familyNode, classNode, "BELONGS_TO");
                }

                // Process GenBank accessions
                if (!genbank.isEmpty()) {
                    for (final String part : genbank.split(";")) {
                        final String genbankId = part.trim();
                        if (genbankId.isEmpty())
                            continue;

                        // Get Taxon ID
                        final Integer taxonId = genbankToTaxonId.get(genbankId);

                        // Get or create Organism
                        Node organismNode = null;
                        if (!organismName.isEmpty()) {
                            organismNode = graph.findNode("CazyOrganism", "name", organismName);
                            if (organismNode == null) {
                                if (taxonId != null) {
                                    organismNode = graph.addNode("CazyOrganism", "name", organismName, "ncbi_taxid", taxonId);
                                } else {
                                    organismNode = graph.addNode("CazyOrganism", "name", organismName);
                                }
                            } else if (taxonId != null && organismNode.getProperty("ncbi_taxid") == null) {
                                organismNode.setProperty("ncbi_taxid", taxonId);
                                graph.update(organismNode);
                            }
                        }

                        // Get or create Protein
                        Node proteinNode = graph.findNode("CazyProtein", "id", genbankId);
                        if (proteinNode == null) {
                            final Map<String, Object> props = new HashMap<>();
                            props.put("id", genbankId);
                            props.put("name", proteinName);
                            if (!ec.isEmpty()) {
                                props.put("ec", ec);
                            }
                            if (!uniprot.isEmpty()) {
                                props.put("uniprot_id", uniprot);
                            }
                            proteinNode = graph.addNode("CazyProtein", props);
                        }

                        // Connect Protein -> Family and Class
                        graph.addEdge(proteinNode, familyNode, "BELONGS_TO");
                        graph.addEdge(proteinNode, classNode, "BELONGS_TO");

                        // Connect Organism -> Protein
                        if (organismNode != null) {
                            graph.addEdge(organismNode, proteinNode, "HAS_PROTEIN");
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to parse structures TSV file and build graph", e);
        }

        return true;
    }

    private Map<String, Integer> loadNCBITaxonMappings(final Workspace workspace, final Set<String> genBankIds) {
        final Map<String, Integer> genbankToTaxonId = new HashMap<>();
        final Path taxonMappingPath = dataSource.resolveSourceFilePath(workspace, "prot.accession2taxid");
        if (!Files.exists(taxonMappingPath)) {
            LOGGER.warn("prot.accession2taxid not found; skipping NCBI taxon ID mappings.");
            return genbankToTaxonId;
        }

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Loading NCBI taxon mappings from " + taxonMappingPath.getFileName() + " for " + genBankIds.size() + " GenBank IDs...");

        try (final BufferedReader reader = Files.newBufferedReader(taxonMappingPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                final int tab1 = line.indexOf('\t');
                if (tab1 == -1) continue;
                final int tab2 = line.indexOf('\t', tab1 + 1);
                if (tab2 == -1) continue;
                final int tab3 = line.indexOf('\t', tab2 + 1);

                final String accession = line.substring(0, tab1);
                final String accessionVersion = line.substring(tab1 + 1, tab2);
                final String taxIdStr = tab3 == -1 ? line.substring(tab2 + 1) : line.substring(tab2 + 1, tab3);

                if (genBankIds.contains(accessionVersion)) {
                    try {
                        genbankToTaxonId.put(accessionVersion, Integer.parseInt(taxIdStr));
                        count++;
                    } catch (NumberFormatException ignored) {}
                } else if (genBankIds.contains(accession)) {
                    try {
                        genbankToTaxonId.put(accession, Integer.parseInt(taxIdStr));
                        count++;
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Loaded " + count + " NCBI taxon mappings.");
        } catch (IOException e) {
            LOGGER.error("Failed to load NCBI taxon mappings: " + e.getMessage(), e);
        }

        return genbankToTaxonId;
    }

    private String getFamilyName(final String familyCode) {
        for (final String prefix : CLASS_NAMES.keySet()) {
            if (familyCode.startsWith(prefix)) {
                final String suffix = familyCode.substring(prefix.length());
                return CLASS_NAMES.get(prefix) + " Family " + suffix;
            }
        }
        return familyCode;
    }
}
