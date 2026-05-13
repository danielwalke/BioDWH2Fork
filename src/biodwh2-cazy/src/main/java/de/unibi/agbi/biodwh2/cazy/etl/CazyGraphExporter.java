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
    static final String ENZYME_ACTIVITY_LABEL = "EnzymeActivity";
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
        return 3;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode(CAZY_FAMILY_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(CAZY_CLASS_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(CBM_CLASS_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PROTEIN_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PROTEIN_LABEL, "uniprot_accession", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(ORGANISM_LABEL, "name", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(DOMAIN_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(EC_LABEL, "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(ENZYME_ACTIVITY_LABEL, "id", IndexDescription.Type.UNIQUE));

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
        try (final ZipInputStream zipStream = FileUtils.openZip(workspace, dataSource, CazyUpdater.DATA_FILE_NAME)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (!entry.getName().endsWith(".txt"))
                    continue;

                try {
                    processCaZyZipEntry(zipStream, graph);
                } catch (IOException e) {
                    throw new ExporterFormatException("Failed to process CAZy data: " + e.getMessage(), e);
                }
                zipStream.closeEntry();
            }
        } catch (IOException e) {
            throw new ExporterFormatException("Failed to read CAZy data zip: " + e.getMessage(), e);
        }
    }

    private void processCaZyZipEntry(final java.io.InputStream zipStream, final Graph graph) throws IOException {
        BoundedCache familyCache = new BoundedCache(CACHE_MAX_SIZE);
        BoundedCache proteinCache = new BoundedCache(CACHE_MAX_SIZE);
        int count = 0;
        try (final BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(zipStream))) {
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

                String genBank = "", uniprot = "", pdb = "", carbLigand = "", ecNumber = "",
                        knownActivities = "", mechanism = "", clan = "", site = "";
                if (cols.length > 4)
                    genBank = cols[4].trim();
                if (cols.length > 5)
                    uniprot = cols[5].trim();
                if (cols.length > 6)
                    pdb = cols[6].trim();
                if (cols.length > 7)
                    carbLigand = cols[7].trim();
                if (cols.length > 8)
                    ecNumber = cols[8].trim();
                if (cols.length > 9)
                    knownActivities = cols[9].trim();
                if (cols.length > 10)
                    mechanism = cols[10].trim();
                if (cols.length > 11)
                    clan = cols[11].trim();
                if (cols.length > 12)
                    site = cols[12].trim();

                if (!family.startsWith("GH") && !family.startsWith("GT") && !family.startsWith("PL") &&
                        !family.startsWith("CE") && !family.startsWith("AA") && !family.startsWith("CBM"))
                    continue;

                String classID = family;
                if (classID.startsWith("CBM")) {
                    classID = "CBM";
                } else if (classID.length() > 2) {
                    classID = family.substring(0, 2);
                } else {
                    continue;
                }

                final String label = CBM_CLASS_LABEL.equals(classID) ? CBM_CLASS_LABEL : CAZY_CLASS_LABEL;
                final Node classNode = graph.findNode(label, "id", classID);
                if (classNode == null)
                    continue;

                final Long famId = ensureFamilyNode(graph, familyCache, family, classID);
                if (famId != null) {
                    graph.addEdge(classNode.getId(), famId, "HAS_FAMILY");
                }

                final Long domainId = ensureDomainNode(graph, domain);
                if (domainId != null && famId != null) {
                    graph.addEdge(famId, domainId, "PRESENT_IN");
                }

                if (!organism.isEmpty()) {
                    final Long orgId = ensureOrganismNode(graph, organism);
                    if (orgId != null && famId != null) {
                        graph.addEdge(orgId, famId, "HAS_FAMILY");
                    }
                }

                if (!proteinId.isEmpty()) {
                    final String proteinLabel = !uniprot.isEmpty() ? uniprot : proteinId;
                    final Long protoId = ensureProteinNode(graph, proteinCache, proteinLabel, genBank, uniprot, pdb,
                            carbLigand, ecNumber, knownActivities, mechanism, clan, site, organism);
                    if (protoId != null && famId != null) {
                        graph.addEdge(famId, protoId, "HAS_PROTEIN");
                    }
                }

                count++;
                if (count % PROGRESS_INTERVAL == 0 && LOGGER.isDebugEnabled())
                    LOGGER.debug("Processed " + count + " CAZy entries");
            }
        }
    }

    private Long ensureFamilyNode(final Graph graph, final BoundedCache cache,
                                   final String family, final String classID) {
        Long id = cache.get(family);
        if (id == null) {
            final Node node = graph.findNode(CAZY_FAMILY_LABEL, "id", family);
            if (node != null) {
                cache.put(family, node.getId());
                return node.getId();
            }
            graph.addNode(CAZY_FAMILY_LABEL, "id", family, "class_id", classID);
            final Node newNode = graph.findNode(CAZY_FAMILY_LABEL, "id", family);
            if (newNode != null) {
                id = newNode.getId();
                cache.put(family, id);
            }
        }
        return id;
    }

    private Long ensureDomainNode(final Graph graph, final String domainId) {
        final Node node = graph.findNode(DOMAIN_LABEL, "id", domainId);
        if (node != null) {
            return node.getId();
        }
        graph.addNode(DOMAIN_LABEL, "id", domainId, "name", domainId);
        final Node newNode = graph.findNode(DOMAIN_LABEL, "id", domainId);
        return newNode != null ? newNode.getId() : null;
    }

    private Long ensureOrganismNode(final Graph graph, final String name) {
        final Node node = graph.findNode(ORGANISM_LABEL, "name", name);
        if (node != null) {
            return node.getId();
        }
        graph.addNode(ORGANISM_LABEL, "name", name);
        final Node newNode = graph.findNode(ORGANISM_LABEL, "name", name);
        return newNode != null ? newNode.getId() : null;
    }

    private Long ensureProteinNode(final Graph graph, final BoundedCache cache, final String proteinLabel,
                                    final String genBank, final String uniprot, final String pdb,
                                    final String carbLigand, final String ecNumber,
                                    final String knownActivities, final String mechanism,
                                    final String clan, final String site, final String organism) {
        Long id = cache.get(proteinLabel);
        if (id == null) {
            final Node existingNode = graph.findNode(PROTEIN_LABEL, "id", proteinLabel);
            if (existingNode != null) {
                id = existingNode.getId();
                cache.put(proteinLabel, id);
            } else {
                final Map<String, Object> props = new HashMap<>();
                props.put("id", proteinLabel);
                if (!genBank.isEmpty())
                    props.put("genbank_id", genBank);
                if (!uniprot.isEmpty())
                    props.put("uniprot_accession", uniprot);
                if (!pdb.isEmpty())
                    props.put("pdb_id", pdb);
                if (!carbLigand.isEmpty())
                    props.put("carbohydrate_ligand", carbLigand);
                if (!ecNumber.isEmpty())
                    props.put("ec_number", ecNumber);
                if (!knownActivities.isEmpty())
                    props.put("known_activities", knownActivities);
                if (!mechanism.isEmpty())
                    props.put("mechanism", mechanism);
                if (!clan.isEmpty())
                    props.put("clan", clan);
                if (!site.isEmpty())
                    props.put("site", site);
                if (!organism.isEmpty())
                    props.put("organism", organism);
                graph.addNode(PROTEIN_LABEL, props);
                final Node newNode = graph.findNode(PROTEIN_LABEL, "id", proteinLabel);
                if (newNode != null) {
                    id = newNode.getId();
                    cache.put(proteinLabel, id);
                }
            }
        }
        return id;
    }

    private static class BoundedCache {
        private final int maxSize;
        private final Map<String, Long> cache;
        private final List<String> insertionOrder;

        BoundedCache(final int maxSize) {
            this.maxSize = maxSize;
            this.cache = new HashMap<>();
            this.insertionOrder = new LinkedList<>();
        }

        public Long get(final String key) {
            return cache.get(key);
        }

        public void put(final String key, final Long value) {
            if (cache.containsKey(key)) {
                return;
            }
            if (cache.size() >= maxSize) {
                String oldest = insertionOrder.remove(0);
                cache.remove(oldest);
            }
            cache.put(key, value);
            insertionOrder.add(key);
        }
    }

    private void exportEnzymeActivities(final Workspace workspace, final Graph graph) throws ExporterException {
        final Map<String, Integer> colMap = new HashMap<>();
        MappingIterator<String[]> iterator;
        try {
            iterator = FileUtils.openTsvWithHeader(
                    workspace, dataSource, CazyUpdater.EC_FILE_NAME, String[].class);
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Failed to open EC numbers file: " + e.getMessage());
            return;
        }

        final MappingIterator<String[]> headerIter;
        try {
            headerIter = FileUtils.openTsvWithHeader(
                    workspace, dataSource, CazyUpdater.EC_FILE_NAME, String[].class);
            final String[] header = headerIter.hasNext() ? headerIter.next() : null;
            if (header != null) {
                for (int i = 0; i < header.length; i++) {
                    colMap.put(header[i].trim().toLowerCase().replace(" ", "_"), i);
                }
            }
        } catch (IOException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Failed to read EC numbers header: " + e.getMessage());
            return;
        }

        String[] row;
        while ((row = iterator.hasNext() ? iterator.next() : null) != null) {
            if (row == null || row.length < 3)
                continue;

            final int clsIdx = colMap.getOrDefault("class", -1);
            final int famIdx = colMap.getOrDefault("family", -1);
            final int ecIdx = colMap.getOrDefault("ec_number", -1);
            final int actIdx = colMap.getOrDefault("activity_name", -1);

            if (clsIdx < 0 || famIdx < 0 || ecIdx < 0)
                continue;

            final String classID = row[clsIdx].trim();
            final String famID = row[famIdx].trim();
            final String ecNumber = row[ecIdx].trim();
            final String activityName = row.length > actIdx ? row[actIdx].trim() : "";

            final Node classNode = graph.findNode(CAZY_CLASS_LABEL, "id", classID);
            final Node famNode = graph.findNode(CAZY_FAMILY_LABEL, "id", famID);

            Node ecNode = graph.findNode(EC_LABEL, "id", ecNumber);
            if (ecNode == null && !ecNumber.isEmpty()) {
                graph.addNode(EC_LABEL, "id", ecNumber, "names", ecNumber, "activity_name", activityName);
                ecNode = graph.findNode(EC_LABEL, "id", ecNumber);
            }

            if (ecNode != null && classNode != null) {
                graph.addEdge(classNode, ecNode, "ASSOCIATED_WITH_EC");
            }
            if (ecNode != null && famNode != null) {
                graph.addEdge(famNode, ecNode, "HAS_EC_NUMBER");
            }
        }
    }
}
