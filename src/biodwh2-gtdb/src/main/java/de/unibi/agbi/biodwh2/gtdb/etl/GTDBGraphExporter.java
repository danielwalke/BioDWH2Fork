package de.unibi.agbi.biodwh2.gtdb.etl;

import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.io.FileUtils;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.gtdb.GTDBDataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public class GTDBGraphExporter extends GraphExporter<GTDBDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GTDBGraphExporter.class);

    private static final String TAXON_LABEL = "Taxon";
    private static final String GENOME_LABEL = "Genome";

    // Edge labels
    private static final String EDGE_CHILD_OF = "CHILD_OF";              // Taxon -> parent Taxon
    private static final String EDGE_BELONGS_TO = "BELONGS_TO";          // Genome -> leaf Taxon
    private static final String EDGE_REPRESENTED_BY = "REPRESENTED_BY";  // Genome -> representative Genome

    // High-value genome property column names (written 1:1 onto the Genome node when present).
    private static final String[] STRING_PROPS = {
        "ncbi_genbank_assembly_accession", "ncbi_assembly_name", "ncbi_assembly_level",
        "ncbi_bioproject", "ncbi_biosample", "ncbi_organism_name", "ncbi_refseq_category",
        "ncbi_isolation_source", "ncbi_country", "ncbi_lat_lon", "ncbi_seq_rel_date",
        "ncbi_taxonomy_unfiltered", "gtdb_genome_representative", "gtdb_type_species_of_genus",
        "checkm_marker_lineage", "mimag_high_quality", "mimag_medium_quality", "mimag_low_quality"
    };
    private static final String[] INT_PROPS = {
        "genome_size", "contig_count", "scaffold_count", "protein_count", "n50_contigs",
        "ncbi_species_taxid"
    };
    private static final String[] DOUBLE_PROPS = {
        "checkm_completeness", "checkm_contamination", "gc_percentage", "coding_density"
    };

    // Shared across both metadata files so taxa common to bacteria/archaea trees are not duplicated.
    private final Map<String, Node> taxonomyCache = new HashMap<>();
    // Track which (child,parent) Taxon edges have been created to avoid redundant CHILD_OF edges.
    private final Set<Long> taxonEdgeCache = new HashSet<>();
    // Genome accession -> node id, used to resolve REPRESENTED_BY edges after all genomes are loaded.
    private final Map<String, Long> genomeNodeIds = new HashMap<>();
    // Pending (non-representative) genome accession -> representative accession, resolved at the end.
    private final Map<String, String> pendingRepresentedBy = new HashMap<>();

    public GTDBGraphExporter(GTDBDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 3;
    }

    @Override
    protected boolean exportGraph(Workspace workspace, Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode(TAXON_LABEL, "taxonomy", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(GENOME_LABEL, "accession", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(GENOME_LABEL, "ncbi_genbank_assembly_accession",
            IndexDescription.Type.NON_UNIQUE));

        boolean success = true;
        success &= exportMetadataFile(workspace, graph, GTDBUpdater.BAC120_METADATA_FILE, "Bacteria");
        success &= exportMetadataFile(workspace, graph, GTDBUpdater.AR53_METADATA_FILE, "Archaea");
        resolveRepresentedByEdges(graph);
        return success;
    }

    private void resolveRepresentedByEdges(Graph graph) {
        int created = 0;
        for (Map.Entry<String, String> entry : pendingRepresentedBy.entrySet()) {
            Long fromId = genomeNodeIds.get(entry.getKey());
            Long toId = genomeNodeIds.get(entry.getValue());
            if (fromId == null || toId == null)
                continue;
            graph.addEdge(fromId, toId, EDGE_REPRESENTED_BY);
            created++;
        }
        LOGGER.info("Created " + created + " REPRESENTED_BY edges (of " + pendingRepresentedBy.size() + " pending)");
    }

    private boolean exportMetadataFile(Workspace workspace, Graph graph, String fileName,
                                       String domain) throws ExporterException {
        LOGGER.info("Exporting GTDB metadata from " + fileName + "...");
        String[] header = null;
        int count = 0;

        try (InputStream inputStream = FileUtils.openGzip(workspace, dataSource, fileName);
             Scanner scanner = new Scanner(inputStream)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (header == null) {
                    header = line.split("\t");
                    continue;
                }

                String[] row = line.split("\t", -1);
                if (row.length < header.length)
                    continue;

                Map<String, String> values = new HashMap<>();
                for (int i = 0; i < header.length; i++) {
                    values.put(header[i], row[i]);
                }

                exportGenomeAndTaxonomy(graph, values, domain, fileName);
                count++;
            }
        } catch (Exception e) {
            throw new ExporterException("Failed to process " + fileName, e);
        }

        LOGGER.info("Exported " + count + " genomes from " + fileName);
        return true;
    }

    private void exportGenomeAndTaxonomy(Graph graph, Map<String, String> values, String domain,
                                         String sourceFile) {
        String accession = values.get("accession");
        String gtdbTaxonomy = values.get("gtdb_taxonomy");
        String ncbiTaxonomy = values.get("ncbi_taxonomy");
        String ncbiTaxidStr = values.get("ncbi_taxid");
        String gtdbRepresentative = values.get("gtdb_representative");

        if (StringUtils.isBlank(accession) || StringUtils.isBlank(gtdbTaxonomy))
            return;

        // Genome node — carries genome-specific provenance and identifiers.
        Node genomeNode = graph.addNode(GENOME_LABEL,
            "accession", accession,
            "gtdb_taxonomy", gtdbTaxonomy,
            "domain", domain,
            "source_file", sourceFile);

        if (StringUtils.isNotBlank(ncbiTaxonomy))
            genomeNode.setProperty("ncbi_taxonomy", ncbiTaxonomy);
        if (StringUtils.isNotBlank(gtdbRepresentative))
            genomeNode.setProperty("gtdb_representative", gtdbRepresentative);
        Integer ncbiTaxid = parseIntOrNull(ncbiTaxidStr);
        if (ncbiTaxid != null)
            genomeNode.setProperty("ncbi_taxid", ncbiTaxid);

        // High-value column properties — only set when present and parseable, to keep the schema sparse.
        for (String key : STRING_PROPS) {
            String v = values.get(key);
            if (StringUtils.isNotBlank(v) && !"none".equalsIgnoreCase(v) && !"na".equalsIgnoreCase(v))
                genomeNode.setProperty(key, v);
        }
        for (String key : INT_PROPS) {
            Integer v = parseIntOrNull(values.get(key));
            if (v != null)
                genomeNode.setProperty(key, v);
        }
        for (String key : DOUBLE_PROPS) {
            Double v = parseDoubleOrNull(values.get(key));
            if (v != null)
                genomeNode.setProperty(key, v);
        }
        graph.update(genomeNode);

        // Track for post-pass REPRESENTED_BY edge creation.
        genomeNodeIds.put(accession, genomeNode.getId());
        String repAcc = values.get("gtdb_genome_representative");
        if (StringUtils.isNotBlank(repAcc) && !repAcc.equals(accession))
            pendingRepresentedBy.put(accession, repAcc);

        // Build/extend taxonomy chain. Edge direction: child --CHILD_OF--> parent.
        String[] taxa = gtdbTaxonomy.split(";");
        Node parentNode = null;
        Node leafNode = null;

        for (String taxon : taxa) {
            String taxonTrimmed = taxon.trim();
            if (StringUtils.isBlank(taxonTrimmed))
                continue;

            String[] parts = taxonTrimmed.split("__", 2);
            String rank = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : taxonTrimmed;

            Node taxonNode = taxonomyCache.get(taxonTrimmed);
            if (taxonNode == null) {
                taxonNode = graph.addNode(TAXON_LABEL,
                    "taxonomy", taxonTrimmed,
                    "rank", rank,
                    "name", name);
                taxonomyCache.put(taxonTrimmed, taxonNode);
            }

            if (parentNode != null) {
                // Use a deterministic key built from the two node ids to deduplicate CHILD_OF edges.
                long edgeKey = (taxonNode.getId() << 32) ^ (parentNode.getId() & 0xffffffffL);
                if (taxonEdgeCache.add(edgeKey))
                    graph.addEdge(taxonNode, parentNode, EDGE_CHILD_OF);
            }

            parentNode = taxonNode;
            leafNode = taxonNode;
        }

        // Connect the genome only to its leaf (most specific) taxon. Higher ranks are reachable via CHILD_OF.
        if (leafNode != null)
            graph.addEdge(genomeNode, leafNode, EDGE_BELONGS_TO);
    }

    private static Integer parseIntOrNull(String s) {
        if (StringUtils.isBlank(s) || "none".equalsIgnoreCase(s.trim()) || "na".equalsIgnoreCase(s.trim()))
            return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double parseDoubleOrNull(String s) {
        if (StringUtils.isBlank(s) || "none".equalsIgnoreCase(s.trim()) || "na".equalsIgnoreCase(s.trim()))
            return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}