package de.unibi.agbi.biodwh2.cazy.etl;

import com.fasterxml.jackson.databind.MappingIterator;
import de.unibi.agbi.biodwh2.cazy.CazyDataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterFormatException;
import de.unibi.agbi.biodwh2.core.io.FileUtils;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CazyGraphExporter extends GraphExporter<CazyDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(CazyGraphExporter.class);

    static final String CAZY_FAMILY_LABEL = "CAZyFamily";
    static final String PROTEIN_LABEL = "Protein";
    static final String ORGANISM_LABEL = "Organism";
    static final String DOMAIN_LABEL = "Domain";
    static final String EC_LABEL = "ECNumber";
    static final String CAZY_CLASS_LABEL = "CAZyClass";
    static final String CBM_CLASS_LABEL = "CBM";

    static final String ARCHAEA = "Archaea";
    static final String BACTERIA = "Bacteria";
    static final String EUKARYOTA = "Eukaryota";
    static final String UNCLASSIFIED = "unclassified";
    static final String VIRUSES = "Viruses";

    private static final int PROGRESS_INTERVAL = 500000;
    private static final int CACHE_MAX_SIZE = 50_000;

    public CazyGraphExporter(final CazyDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 4;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode(CAZY_FAMILY_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(CAZY_CLASS_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(CBM_CLASS_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PROTEIN_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(ORGANISM_LABEL, "name", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(DOMAIN_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(EC_LABEL, "id", IndexDescription.Type.UNIQUE));

        createCAZyClassNodes(graph);
        createDomainNodes(graph);
        exportCAZyData(workspace, graph);
        exportEnzymeActivities(workspace, graph);
        return true;
    }

    private void createCAZyClassNodes(final Graph graph) {
        createClassNode(graph, "GH", "Glycoside Hydrolases");
        createClassNode(graph, "GT", "GlycosylTransferases");
        createClassNode(graph, "PL", "Polysaccharide Lyases");
        createClassNode(graph, "CE", "Carbohydrate Esterases");
        createClassNode(graph, "AA", "Auxiliary Activities");
        graph.addNode(CBM_CLASS_LABEL, "id", "CBM", "name", "Carbohydrate Binding Modules");
    }

    private void createClassNode(final Graph graph, final String id, final String name) {
        graph.addNode(CAZY_CLASS_LABEL, "id", id, "name", name);
    }

    private void createDomainNodes(final Graph graph) {
        for (final String domainId : new String[]{ARCHAEA, BACTERIA, EUKARYOTA, UNCLASSIFIED, VIRUSES}) {
            graph.addNode(DOMAIN_LABEL, "id", domainId, "name", domainId);
        }
    }

    private void exportCAZyData(final Workspace workspace, final Graph graph) throws ExporterException {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Scanning CAZy bulk data for NCBI protein IDs...");
        final java.util.Set<String> genBankIds = collectNCBIProteinIds(workspace);
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Found " + genBankIds.size() + " unique NCBI GenBank protein IDs");

        final Map<String, Integer> genbankToTaxonId = new HashMap<>();
        final Map<String, String> genbankToUniprotId = new HashMap<>();

        loadNCBITaxonMappings(workspace, genBankIds, genbankToTaxonId);
        loadUniProtMappings(workspace, genBankIds, genbankToUniprotId);

        try (final ZipInputStream zipStream = FileUtils.openZip(workspace, dataSource, CazyUpdater.DATA_FILE_NAME)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".txt"))
                    continue;

                try {
                    processCaZyZipEntry(zipStream, graph, genbankToTaxonId, genbankToUniprotId);
                } catch (IOException e) {
                    throw new ExporterFormatException("Failed to process CAZy data: " + e.getMessage(), e);
                }
                zipStream.closeEntry();
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to read CAZy data zip: " + e.getMessage(), e);
        }
    }

    private java.util.Set<String> collectNCBIProteinIds(final Workspace workspace) throws ExporterException {
        final java.util.Set<String> genBankIds = new java.util.HashSet<>();
        try (final ZipInputStream zipStream = FileUtils.openZip(workspace, dataSource, CazyUpdater.DATA_FILE_NAME)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".txt"))
                    continue;
                try (final BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(new NonClosingInputStream(zipStream)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty())
                            continue;
                        final String[] cols = line.split("\t", -1);
                        if (cols.length >= 5) {
                            final String source = cols[4].trim();
                            if ("jgi".equalsIgnoreCase(source)) {
                                // TODO: handle jgi rows
                            } else if ("ncbi".equalsIgnoreCase(source)) {
                                final String proteinId = cols[3].trim();
                                if (!proteinId.isEmpty()) {
                                    genBankIds.add(proteinId);
                                }
                            }
                        }
                    }
                }
                zipStream.closeEntry();
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to scan CAZy data zip: " + e.getMessage(), e);
        }
        return genBankIds;
    }

    private void loadNCBITaxonMappings(final Workspace workspace, final java.util.Set<String> genBankIds,
                                       final Map<String, Integer> genbankToTaxonId) {
        final java.nio.file.Path path = dataSource.resolveSourceFilePath(workspace, "prot.accession2taxid.gz");
        if (!path.toFile().exists()) {
            LOGGER.warn("prot.accession2taxid.gz not found; skipping NCBI taxon ID mappings.");
            return;
        }
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Loading NCBI taxon mappings from " + path.getFileName() + "...");
        try (final BufferedReader reader = FileUtils.createBufferedReaderFromStream(
                FileUtils.openGzip(workspace, dataSource, "prot.accession2taxid.gz"))) {
            String line = reader.readLine(); // Header
            int count = 0;
            while ((line = reader.readLine()) != null) {
                final String[] cols = line.split("\t", -1);
                if (cols.length >= 3) {
                    final String genbankId = cols[1];
                    if (genBankIds.contains(genbankId)) {
                        final String taxIdStr = cols[2];
                        try {
                            genbankToTaxonId.put(genbankId, Integer.parseInt(taxIdStr));
                            count++;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Loaded " + count + " NCBI taxon mappings.");
        } catch (IOException e) {
            LOGGER.error("Failed to load NCBI taxon mappings: " + e.getMessage(), e);
        }
    }

    private void loadUniProtMappings(final Workspace workspace, final java.util.Set<String> genBankIds,
                                     final Map<String, String> genbankToUniprotId) {
        final java.nio.file.Path path = dataSource.resolveSourceFilePath(workspace, "idmapping.dat.gz");
        if (!path.toFile().exists()) {
            LOGGER.warn("idmapping.dat.gz not found; skipping UniProt ID mappings.");
            return;
        }
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Loading UniProt mappings from " + path.getFileName() + "...");
        try (final BufferedReader reader = FileUtils.createBufferedReaderFromStream(
                FileUtils.openGzip(workspace, dataSource, "idmapping.dat.gz"))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                final String[] cols = line.split("\t", -1);
                if (cols.length >= 3) {
                    if ("EMBL-GenBank-DDBJ_CDS".equals(cols[1])) {
                        final String genbankId = cols[2];
                        if (genBankIds.contains(genbankId)) {
                            final String uniprotId = cols[0];
                            genbankToUniprotId.put(genbankId, uniprotId);
                            count++;
                        }
                    }
                }
            }
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Loaded " + count + " UniProt mappings.");
        } catch (IOException e) {
            LOGGER.error("Failed to load UniProt mappings: " + e.getMessage(), e);
        }
    }

    /**
     * Parse the CAZy bulk data file. Each row has exactly 5 tab-separated columns:
     * <ol>
     *   <li>family (e.g. GH1, GT2, CBM4)</li>
     *   <li>domain (e.g. Bacteria, Eukaryota, Archaea, Viruses)</li>
     *   <li>organism name</li>
     *   <li>protein accession ID (NCBI GenBank or JGI ID)</li>
     *   <li>source database (ncbi or jgi)</li>
     * </ol>
     */
    private void processCaZyZipEntry(final java.io.InputStream zipStream, final Graph graph,
                                     final Map<String, Integer> genbankToTaxonId,
                                     final Map<String, String> genbankToUniprotId) throws IOException {
        final BoundedCache<String, Long> familyCache = new BoundedCache<>(CACHE_MAX_SIZE);
        final BoundedCache<String, Long> proteinCache = new BoundedCache<>(CACHE_MAX_SIZE);
        final BoundedCache<String, Long> organismCache = new BoundedCache<>(CACHE_MAX_SIZE);
        final Map<String, Long> classCache = new HashMap<>();
        final Map<String, Long> domainCache = new HashMap<>();
        final Map<Long, java.util.Set<Long>> addedEdges = new HashMap<>();
        final Map<String, Integer> organismToTaxonIdMap = new HashMap<>();

        int count = 0;
        try (final BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(new NonClosingInputStream(zipStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                final String[] cols = line.split("\t", -1);
                if (cols.length < 4)
                    continue;

                final String family = cols[0].trim();
                final String domain = cols.length > 1 ? cols[1].trim() : UNCLASSIFIED;
                final String organism = cols.length > 2 ? cols[2].trim() : "";
                final String proteinId = cols.length > 3 ? cols[3].trim() : "";
                final String source = cols.length > 4 ? cols[4].trim() : "";

                if (!family.startsWith("GH") && !family.startsWith("GT") && !family.startsWith("PL") &&
                    !family.startsWith("CE") && !family.startsWith("AA") && !family.startsWith("CBM"))
                    continue;

                if ("jgi".equalsIgnoreCase(source)) {
                    // TODO: handle jgi rows
                    continue;
                }

                if (!"ncbi".equalsIgnoreCase(source))
                    continue;

                String classID = family;
                if (classID.startsWith("CBM")) {
                    classID = "CBM";
                } else if (classID.length() > 2) {
                    classID = family.substring(0, 2);
                } else {
                    continue;
                }

                Long classNodeId = classCache.get(classID);
                if (classNodeId == null) {
                    final String label = "CBM".equals(classID) ? CBM_CLASS_LABEL : CAZY_CLASS_LABEL;
                    final Node classNode = graph.findNode(label, "id", classID);
                    if (classNode == null)
                        continue;
                    classNodeId = classNode.getId();
                    classCache.put(classID, classNodeId);
                }

                final Long famId = ensureFamilyNode(graph, familyCache, family, classID);
                if (famId != null && addEdgeIfNotExists(addedEdges, classNodeId, famId)) {
                    graph.addEdge(classNodeId, famId, "HAS_FAMILY");
                }

                final Long domainId = ensureDomainNode(graph, domainCache, domain);
                if (domainId != null && famId != null && addEdgeIfNotExists(addedEdges, famId, domainId)) {
                    graph.addEdge(famId, domainId, "PRESENT_IN");
                }

                if (!organism.isEmpty()) {
                    final Long orgId = ensureOrganismNode(graph, organismCache, organism);
                    if (orgId != null && famId != null && addEdgeIfNotExists(addedEdges, orgId, famId)) {
                        graph.addEdge(orgId, famId, "HAS_FAMILY");
                    }
                }

                if (!proteinId.isEmpty()) {
                    final Long protoId = ensureProteinNode(graph, proteinCache, proteinId, source, organism, genbankToTaxonId, genbankToUniprotId);
                    if (protoId != null && famId != null) {
                        graph.addEdge(famId, protoId, "HAS_PROTEIN");
                    }
                    if ("ncbi".equalsIgnoreCase(source) && !organism.isEmpty()) {
                        final Integer taxonId = genbankToTaxonId.get(proteinId);
                        if (taxonId != null) {
                            organismToTaxonIdMap.put(organism, taxonId);
                        }
                    }
                }

                count++;
                if (count % PROGRESS_INTERVAL == 0 && LOGGER.isDebugEnabled())
                    LOGGER.debug("Processed " + count + " CAZy entries");
            }
        }

        // Update Organism nodes with their NCBI taxon ID properties
        for (final Map.Entry<String, Integer> entry : organismToTaxonIdMap.entrySet()) {
            Long orgId = organismCache.get(entry.getKey());
            Node orgNode = null;
            if (orgId != null) {
                orgNode = graph.getNode(orgId);
            }
            if (orgNode == null) {
                orgNode = graph.findNode(ORGANISM_LABEL, "name", entry.getKey());
            }
            if (orgNode != null) {
                orgNode.setProperty("ncbi_taxid", entry.getValue());
                graph.update(orgNode);
            }
        }
    }

    private boolean addEdgeIfNotExists(final Map<Long, java.util.Set<Long>> edgeCache, final Long sourceId, final Long targetId) {
        return edgeCache.computeIfAbsent(sourceId, k -> new java.util.HashSet<>()).add(targetId);
    }

    private Long ensureFamilyNode(final Graph graph, final BoundedCache<String, Long> cache,
                                   final String family, final String classID) {
        Long id = cache.get(family);
        if (id == null) {
            Node node = graph.findNode(CAZY_FAMILY_LABEL, "id", family);
            if (node == null) {
                node = graph.addNode(CAZY_FAMILY_LABEL, "id", family, "class_id", classID);
            }
            id = node.getId();
            cache.put(family, id);
        }
        return id;
    }

    private Long ensureDomainNode(final Graph graph, final Map<String, Long> cache, final String domainId) {
        Long id = cache.get(domainId);
        if (id == null) {
            Node node = graph.findNode(DOMAIN_LABEL, "id", domainId);
            if (node == null) {
                node = graph.addNode(DOMAIN_LABEL, "id", domainId, "name", domainId);
            }
            id = node.getId();
            cache.put(domainId, id);
        }
        return id;
    }

    private Long ensureOrganismNode(final Graph graph, final BoundedCache<String, Long> cache, final String name) {
        Long id = cache.get(name);
        if (id == null) {
            Node node = graph.findNode(ORGANISM_LABEL, "name", name);
            if (node == null) {
                node = graph.addNode(ORGANISM_LABEL, "name", name);
            }
            id = node.getId();
            cache.put(name, id);
        }
        return id;
    }

    /**
     * Create or find a Protein node with the correct properties based on the actual 5-column CAZy data format.
     *
     * @param proteinId the protein accession (e.g. "ABN51453.1" for NCBI, "344991" for JGI)
     * @param source    the source database ("ncbi" or "jgi")
     * @param organism  the organism name for annotation
     */
    private Long ensureProteinNode(final Graph graph, final BoundedCache<String, Long> cache, final String proteinId,
                                    final String source, final String organism,
                                    final Map<String, Integer> genbankToTaxonId,
                                    final Map<String, String> genbankToUniprotId) {
        Long id = cache.get(proteinId);
        if (id == null) {
            Node node = graph.findNode(PROTEIN_LABEL, "id", proteinId);
            if (node == null) {
                final Map<String, Object> props = new HashMap<>();
                props.put("id", proteinId);
                props.put("source", source);
                if (!organism.isEmpty())
                    props.put("organism", organism);

                if ("ncbi".equalsIgnoreCase(source)) {
                    final Integer taxonId = genbankToTaxonId.get(proteinId);
                    if (taxonId != null)
                        props.put("ncbi_taxid", taxonId);
                    final String uniprotId = genbankToUniprotId.get(proteinId);
                    if (uniprotId != null)
                        props.put("uniprot_id", uniprotId);
                }

                node = graph.addNode(PROTEIN_LABEL, props);
            }
            id = node.getId();
            cache.put(proteinId, id);
        }
        return id;
    }

    private static class BoundedCache<K, V> extends java.util.LinkedHashMap<K, V> {
        private final int maxSize;

        BoundedCache(final int maxSize) {
            super(maxSize, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    /**
     * Export EC numbers scraped from CAZy family pages. The TSV file has columns:
     * class, family, ec_number, activity_name
     */
    private void exportEnzymeActivities(final Workspace workspace, final Graph graph) throws ExporterException {
        try (final BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(
                java.nio.file.Files.newInputStream(
                        dataSource.resolveSourceFilePath(workspace, CazyUpdater.EC_FILE_NAME))))) {
            String headerLine = reader.readLine();
            if (headerLine == null)
                return;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                final String[] cols = line.split("\t", -1);
                if (cols.length < 3)
                    continue;

                final String classID = cols[0].trim();
                final String famID = cols[1].trim();
                final String ecNumber = cols[2].trim();
                final String activityName = cols.length > 3 ? cols[3].trim() : "";

                if (ecNumber.isEmpty())
                    continue;

                final Node classNode = graph.findNode(CAZY_CLASS_LABEL, "id", classID);
                final Node famNode = graph.findNode(CAZY_FAMILY_LABEL, "id", famID);

                Node ecNode = graph.findNode(EC_LABEL, "id", ecNumber);
                if (ecNode == null) {
                    graph.addNode(EC_LABEL, "id", ecNumber, "activity_name", activityName);
                    ecNode = graph.findNode(EC_LABEL, "id", ecNumber);
                }

                if (ecNode != null && classNode != null) {
                    graph.addEdge(classNode, ecNode, "ASSOCIATED_WITH_EC");
                }
                if (ecNode != null && famNode != null) {
                    graph.addEdge(famNode, ecNode, "HAS_EC_NUMBER");
                }
            }
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Failed to read EC numbers file: " + e.getMessage());
        }
    }

    private static class NonClosingInputStream extends java.io.FilterInputStream {
        protected NonClosingInputStream(final java.io.InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            // Do not close the underlying stream
        }
    }
}
