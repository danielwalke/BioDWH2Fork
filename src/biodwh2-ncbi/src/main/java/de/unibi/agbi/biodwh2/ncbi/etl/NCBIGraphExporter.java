package de.unibi.agbi.biodwh2.ncbi.etl;

import com.fasterxml.jackson.databind.MappingIterator;
import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.io.FileUtils;
import de.unibi.agbi.biodwh2.core.io.mvstore.MVStoreModel;
import de.unibi.agbi.biodwh2.core.io.sdf.SdfEntry;
import de.unibi.agbi.biodwh2.core.io.sdf.SdfReader;
import de.unibi.agbi.biodwh2.core.model.graph.Edge;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import de.unibi.agbi.biodwh2.ncbi.NCBIDataSource;
import de.unibi.agbi.biodwh2.ncbi.model.GeneAccession;
import de.unibi.agbi.biodwh2.ncbi.model.GeneGo;
import de.unibi.agbi.biodwh2.ncbi.model.GeneInfo;
import de.unibi.agbi.biodwh2.ncbi.model.GeneRelationship;
import de.unibi.agbi.biodwh2.ncbi.model.GeneEnsembl;
import de.unibi.agbi.biodwh2.ncbi.model.Mim2GeneMedgen;
import de.unibi.agbi.biodwh2.ncbi.model.GeneHistory;
import de.unibi.agbi.biodwh2.ncbi.model.GeneNeighbor;
import de.unibi.agbi.biodwh2.ncbi.model.GeneRefseqUniprotkbCollab;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NCBIGraphExporter extends GraphExporter<NCBIDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(NCBIGraphExporter.class);
    private Map<Long, Long> geneIdNodeIdMap;

    public NCBIGraphExporter(final NCBIDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 1;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode("Gene", "id", IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode("Compound", "id", IndexDescription.Type.UNIQUE));
        geneIdNodeIdMap = new HashMap<>();
        try {
            exportGeneDatabase(workspace, dataSource, graph);
        } catch (IOException e) {
            throw new ExporterException("Failed to export NCBI Gene database", e);
        }
        try {
            exportPubChemDatabase(workspace, dataSource, graph);
        } catch (IOException e) {
            throw new ExporterException("Failed to export NCBI PubChem database", e);
        }
        return true;
    }

    private void exportGeneDatabase(final Workspace workspace, final DataSource dataSource,
                                    final Graph graph) throws IOException {
        LOGGER.info("Exporting gene_info.gz...");
        MappingIterator<GeneInfo> geneInfos = FileUtils.openGzipTsv(workspace, dataSource, "gene_info.gz",
                                                                    GeneInfo.class);
        while (geneInfos.hasNext()) {
            GeneInfo geneInfo = geneInfos.next();
            if (!geneInfo.taxonomyId.equals("9606"))
                continue;
            long geneId = Long.parseLong(geneInfo.geneId);
            Node geneNode = graph.addNode("Gene");
            geneNode.setProperty("id", geneId);
            setPropertyIfNotDash(geneNode, "symbol", geneInfo.symbol);
            setPropertyIfNotDash(geneNode, "chromosome", geneInfo.chromosome);
            setPropertyIfNotDash(geneNode, "locus_tag", geneInfo.locusTag);
            setPropertyIfNotDash(geneNode, "type", geneInfo.typeOfGene);
            setPropertyIfNotDash(geneNode, "description", geneInfo.description);
            setArrayPropertyIfNotDash(geneNode, "synonyms", geneInfo.synonyms);
            setArrayPropertyIfNotDash(geneNode, "xrefs", geneInfo.dbXrefs);
            setArrayPropertyIfNotDash(geneNode, "feature_types", geneInfo.featureType);
            setPropertyIfNotDash(geneNode, "map_location", geneInfo.mapLocation);
            setPropertyIfNotDash(geneNode, "symbol_from_nomenclature_authority", geneInfo.symbolFromNomenclatureAuthority);
            setPropertyIfNotDash(geneNode, "full_name_from_nomenclature_authority", geneInfo.fullNameFromNomenclatureAuthority);
            setPropertyIfNotDash(geneNode, "nomenclature_status", geneInfo.nomenclatureStatus);
            setArrayPropertyIfNotDash(geneNode, "other_designations", geneInfo.otherDesignations);
            geneIdNodeIdMap.put(geneId, geneNode.getId());
            graph.update(geneNode);
        }
        LOGGER.info("Exporting gene2accession.gz...");
        MappingIterator<GeneAccession> accessions = FileUtils.openGzipTsv(workspace, dataSource, "gene2accession.gz",
                                                                          GeneAccession.class);
        while (accessions.hasNext()) {
            GeneAccession accession = accessions.next();
            if (!accession.taxonomyId.equals("9606"))
                continue;
            long geneId = Long.parseLong(accession.geneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            if (internalGeneId != null) {
                Node accessionNode = createAccessionNode(graph, accession);
                graph.addEdge(internalGeneId, accessionNode, "HAS_ACCESSION");
            }
        }
        LOGGER.info("Exporting gene2go.gz...");
        MappingIterator<GeneGo> goAnnotations = FileUtils.openGzipTsv(workspace, dataSource, "gene2go.gz",
                                                                      GeneGo.class);
        while (goAnnotations.hasNext()) {
            GeneGo go = goAnnotations.next();
            if (!go.taxonomyId.equals("9606"))
                continue;
            long geneId = Long.parseLong(go.geneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            if (internalGeneId != null) {
                Node goTermNode = graph.findNode("GoTerm", "id", go.goId);
                if (goTermNode == null) {
                    goTermNode = graph.addNode("GoTerm");
                    goTermNode.setProperty("id", go.goId);
                    goTermNode.setProperty("category", go.category);
                    goTermNode.setProperty("term", go.goTerm);
                    graph.update(goTermNode);
                }
                Edge edge = graph.addEdge(internalGeneId, goTermNode, "HAS_GO_TERM");
                setPropertyIfNotDash(edge, "evidence", go.evidence);
                setPropertyIfNotDash(edge, "qualifier", go.qualifier);
                setArrayPropertyIfNotDash(edge, "pubmed_ids", go.pubMedIds);
                graph.update(edge);
            }
        }
        LOGGER.info("Exporting gene_group.gz...");
        MappingIterator<GeneRelationship> groups = FileUtils.openGzipTsv(workspace, dataSource, "gene_group.gz",
                                                                         GeneRelationship.class);
        while (groups.hasNext()) {
            GeneRelationship group = groups.next();
            if (!group.taxonomyId.equals("9606") || !group.otherTaxonomyId.equals("9606"))
                continue;
            long geneId = Long.parseLong(group.geneId);
            long otherGeneId = Long.parseLong(group.otherGeneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            Long internalOtherGeneId = geneIdNodeIdMap.get(otherGeneId);
            if (internalGeneId != null && internalOtherGeneId != null) {
                Edge edge = graph.addEdge(internalGeneId, internalOtherGeneId, "RELATED_TO");
                edge.setProperty("type", group.relationship);
                graph.update(edge);
            }
        }
        LOGGER.info("Exporting gene_orthologs.gz...");
        MappingIterator<GeneRelationship> orthologs = FileUtils.openGzipTsv(workspace, dataSource, "gene_orthologs.gz",
                                                                            GeneRelationship.class);
        while (orthologs.hasNext()) {
            GeneRelationship ortholog = orthologs.next();
            if (!ortholog.taxonomyId.equals("9606") || !ortholog.otherTaxonomyId.equals("9606"))
                continue;
            long geneId = Long.parseLong(ortholog.geneId);
            long otherGeneId = Long.parseLong(ortholog.otherGeneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            Long internalOtherGeneId = geneIdNodeIdMap.get(otherGeneId);
            if (internalGeneId != null && internalOtherGeneId != null) {
                Edge edge = graph.addEdge(internalGeneId, internalOtherGeneId, "RELATED_TO");
                edge.setProperty("type", ortholog.relationship);
                graph.update(edge);
            }
        }
        LOGGER.info("Exporting gene2pubmed.gz...");
        MappingIterator<String[]> genePubMed = FileUtils.openGzipTsv(workspace, dataSource, "gene2pubmed.gz",
                                                                     String[].class);
        long lastGeneId = -1;
        Set<Long> currentPubMedIds = new HashSet<>();
        while (genePubMed.hasNext()) {
            String[] pubMedAnnotation = genePubMed.next();
            if (!pubMedAnnotation[0].equals("9606"))
                continue;
            long geneId = Long.parseLong(pubMedAnnotation[1]);
            if (geneId != lastGeneId) {
                if (lastGeneId != -1) {
                    Long internalLastGeneId = geneIdNodeIdMap.get(lastGeneId);
                    if (internalLastGeneId != null) {
                        Node geneNode = graph.getNode(internalLastGeneId);
                        if (geneNode != null) {
                            geneNode.setProperty("pubmed_ids", currentPubMedIds.toArray(new Long[0]));
                            graph.update(geneNode);
                        }
                    }
                    currentPubMedIds.clear();
                }
                lastGeneId = geneId;
            }
            currentPubMedIds.add(Long.parseLong(pubMedAnnotation[2]));
        }
        if (currentPubMedIds.size() > 0 && lastGeneId != -1) {
            Long internalLastGeneId = geneIdNodeIdMap.get(lastGeneId);
            if (internalLastGeneId != null) {
                Node geneNode = graph.getNode(internalLastGeneId);
                if (geneNode != null) {
                    geneNode.setProperty("pubmed_ids", currentPubMedIds.toArray(new Long[0]));
                    graph.update(geneNode);
                }
            }
        }
        
        LOGGER.info("Exporting gene2ensembl.gz...");
        MappingIterator<GeneEnsembl> ensembls = FileUtils.openGzipTsv(workspace, dataSource, "gene2ensembl.gz", GeneEnsembl.class);
        while (ensembls.hasNext()) {
            GeneEnsembl ensembl = ensembls.next();
            if (!"9606".equals(ensembl.taxonomyId)) continue;
            long geneId = Long.parseLong(ensembl.geneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            if (internalGeneId != null) {
                Node ensemblNode = graph.addNode("Ensembl");
                setPropertyIfNotDash(ensemblNode, "ensembl_gene_identifier", ensembl.ensemblGeneIdentifier);
                setPropertyIfNotDash(ensemblNode, "rna_nucleotide_accession.version", ensembl.rnaNucleotideAccessionVersion);
                setPropertyIfNotDash(ensemblNode, "ensembl_rna_identifier", ensembl.ensemblRnaIdentifier);
                setPropertyIfNotDash(ensemblNode, "protein_accession.version", ensembl.proteinAccessionVersion);
                setPropertyIfNotDash(ensemblNode, "ensembl_protein_identifier", ensembl.ensemblProteinIdentifier);
                graph.update(ensemblNode);
                graph.addEdge(internalGeneId, ensemblNode, "HAS_ENSEMBL");
            }
        }

        LOGGER.info("Exporting mim2gene_medgen...");
        MappingIterator<Mim2GeneMedgen> mim2genes = FileUtils.openTsv(workspace, dataSource, "mim2gene_medgen", Mim2GeneMedgen.class);
        while (mim2genes.hasNext()) {
            Mim2GeneMedgen mim2gene = mim2genes.next();
            if ("-".equals(mim2gene.geneId)) continue;
            long geneId = Long.parseLong(mim2gene.geneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            if (internalGeneId != null) {
                Node medgenNode = graph.addNode("MedGen");
                setPropertyIfNotDash(medgenNode, "mim_number", mim2gene.mimNumber);
                setPropertyIfNotDash(medgenNode, "type", mim2gene.type);
                setPropertyIfNotDash(medgenNode, "source", mim2gene.source);
                setPropertyIfNotDash(medgenNode, "medgen_cui", mim2gene.medgenCui);
                setPropertyIfNotDash(medgenNode, "comment", mim2gene.comment);
                graph.update(medgenNode);
                graph.addEdge(internalGeneId, medgenNode, "HAS_MEDGEN");
            }
        }

        LOGGER.info("Exporting gene_history.gz...");
        MappingIterator<GeneHistory> histories = FileUtils.openGzipTsv(workspace, dataSource, "gene_history.gz", GeneHistory.class);
        while (histories.hasNext()) {
            GeneHistory history = histories.next();
            if (!"9606".equals(history.taxonomyId) || "-".equals(history.geneId)) continue;
            long geneId = Long.parseLong(history.geneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            if (internalGeneId != null) {
                Node historyNode = graph.addNode("GeneHistory");
                setPropertyIfNotDash(historyNode, "discontinued_gene_id", history.discontinuedGeneId);
                setPropertyIfNotDash(historyNode, "discontinued_symbol", history.discontinuedSymbol);
                setPropertyIfNotDash(historyNode, "discontinue_date", history.discontinueDate);
                graph.update(historyNode);
                graph.addEdge(internalGeneId, historyNode, "HAS_HISTORY");
            }
        }

        LOGGER.info("Exporting gene_neighbors.gz...");
        MappingIterator<GeneNeighbor> neighbors = FileUtils.openGzipTsv(workspace, dataSource, "gene_neighbors.gz", GeneNeighbor.class);
        while (neighbors.hasNext()) {
            GeneNeighbor neighbor = neighbors.next();
            if (!"9606".equals(neighbor.taxonomyId)) continue;
            long geneId = Long.parseLong(neighbor.geneId);
            Long internalGeneId = geneIdNodeIdMap.get(geneId);
            if (internalGeneId != null) {
                Node neighborNode = graph.addNode("GeneNeighbor");
                setPropertyIfNotDash(neighborNode, "genomic_accession.version", neighbor.genomicAccessionVersion);
                setPropertyIfNotDash(neighborNode, "genomic_gi", neighbor.genomicGi);
                setPropertyIfNotDash(neighborNode, "start_position", neighbor.startPosition);
                setPropertyIfNotDash(neighborNode, "end_position", neighbor.endPosition);
                setPropertyIfNotDash(neighborNode, "orientation", neighbor.orientation);
                setPropertyIfNotDash(neighborNode, "chromosome", neighbor.chromosome);
                setArrayPropertyIfNotDash(neighborNode, "gene_ids_on_left", neighbor.geneIdsOnLeft);
                setPropertyIfNotDash(neighborNode, "distance_to_left", neighbor.distanceToLeft);
                setArrayPropertyIfNotDash(neighborNode, "gene_ids_on_right", neighbor.geneIdsOnRight);
                setPropertyIfNotDash(neighborNode, "distance_to_right", neighbor.distanceToRight);
                setArrayPropertyIfNotDash(neighborNode, "overlapping_gene_ids", neighbor.overlappingGeneIds);
                setPropertyIfNotDash(neighborNode, "assembly", neighbor.assembly);
                graph.update(neighborNode);
                graph.addEdge(internalGeneId, neighborNode, "HAS_NEIGHBOR_INFO");
            }
        }

        LOGGER.info("Exporting gene_refseq_uniprotkb_collab.gz...");
        MappingIterator<GeneRefseqUniprotkbCollab> collabs = FileUtils.openGzipTsv(workspace, dataSource, "gene_refseq_uniprotkb_collab.gz", GeneRefseqUniprotkbCollab.class);
        while (collabs.hasNext()) {
            GeneRefseqUniprotkbCollab collab = collabs.next();
            if (!"9606".equals(collab.ncbiTaxId)) continue;
            Node collabNode = graph.addNode("RefseqUniprotkbCollab");
            setPropertyIfNotDash(collabNode, "ncbi_protein_accession", collab.ncbiProteinAccession);
            setPropertyIfNotDash(collabNode, "uniprotkb_protein_accession", collab.uniProtKbProteinAccession);
            setPropertyIfNotDash(collabNode, "uniprotkb_tax_id", collab.uniProtKbTaxId);
            setPropertyIfNotDash(collabNode, "method", collab.method);
            graph.update(collabNode);
        }
    }

    private Node createAccessionNode(final Graph graph, final GeneAccession accession) {
        Node accessionNode = graph.addNode("Accession");
        setPropertyIfNotDash(accessionNode, "status", accession.status);
        setLongPropertyIfNotDash(accessionNode, "rna_nucleotide_gi", accession.rnaNucleotideGi);
        setPropertyIfNotDash(accessionNode, "rna_nucleotide_accession.version",
                             accession.rnaNucleotideAccessionVersion);
        setLongPropertyIfNotDash(accessionNode, "protein_gi", accession.proteinGi);
        setPropertyIfNotDash(accessionNode, "protein_accession.version", accession.proteinAccessionVersion);
        setLongPropertyIfNotDash(accessionNode, "genomic_nucleotide_gi", accession.genomicNucleotideGi);
        setPropertyIfNotDash(accessionNode, "genomic_nucleotide_accession.version",
                             accession.genomicNucleotideAccessionVersion);
        setLongPropertyIfNotDash(accessionNode, "mature_peptide_gi", accession.maturePeptideGi);
        setPropertyIfNotDash(accessionNode, "mature_peptide_accession.version",
                             accession.maturePeptideAccessionVersion);
        setLongPropertyIfNotDash(accessionNode, "start_position_on_the_genomic_accession",
                                 accession.startPositionOnTheGenomicAccession);
        setLongPropertyIfNotDash(accessionNode, "end_position_on_the_genomic_accession",
                                 accession.endPositionOnTheGenomicAccession);
        setPropertyIfNotDash(accessionNode, "assembly", accession.assembly);
        setPropertyIfNotDash(accessionNode, "orientation", accession.orientation);
        graph.update(accessionNode);
        return accessionNode;
    }

    private void setPropertyIfNotDash(final MVStoreModel container, final String propertyKey, final String value) {
        if (value != null && !"-".equals(value))
            container.setProperty(propertyKey, value);
    }

    private void setLongPropertyIfNotDash(final MVStoreModel container, final String propertyKey, final String value) {
        if (value != null && !"-".equals(value))
            container.setProperty(propertyKey, Long.parseLong(value));
    }

    private void setArrayPropertyIfNotDash(final MVStoreModel container, final String propertyKey, final String value) {
        if (value != null && !"-".equals(value) && value.trim().length() > 0)
            container.setProperty(propertyKey, StringUtils.split(value, "|"));
    }

    private void exportPubChemDatabase(final Workspace workspace, final DataSource dataSource,
                                       final Graph graph) throws IOException {
        final String[] fileNames = dataSource.listSourceFiles(workspace);
        for (final String fileName : fileNames)
            if (fileName.startsWith("Compound_") && fileName.endsWith(".sdf.gz")) {
                final SdfReader reader = new SdfReader(FileUtils.openGzip(workspace, dataSource, fileName),
                                                       StandardCharsets.UTF_8);
                for (final SdfEntry entry : reader)
                    createPubChemCompoundNode(graph, entry);
            }
    }

    private void createPubChemCompoundNode(final Graph graph, final SdfEntry entry) {
        Node node = graph.addNode("Compound");
        node.setProperty("id", Long.parseLong(entry.properties.get("PUBCHEM_COMPOUND_CID")));
        node.setProperty("IUPAC_openeye_name", entry.properties.get("PUBCHEM_IUPAC_OPENEYE_NAME"));
        node.setProperty("IUPAC_cas_name", entry.properties.get("PUBCHEM_IUPAC_CAS_NAME"));
        node.setProperty("IUPAC_name_markup", entry.properties.get("PUBCHEM_IUPAC_NAME_MARKUP"));
        node.setProperty("IUPAC_name", entry.properties.get("PUBCHEM_IUPAC_NAME"));
        node.setProperty("IUPAC_systematic_name", entry.properties.get("PUBCHEM_IUPAC_SYSTEMATIC_NAME"));
        node.setProperty("IUPAC_traditional_name", entry.properties.get("PUBCHEM_IUPAC_TRADITIONAL_NAME"));
        node.setProperty("IUPAC_inchi", entry.properties.get("PUBCHEM_IUPAC_INCHI"));
        node.setProperty("IUPAC_inchi_key", entry.properties.get("PUBCHEM_IUPAC_INCHIKEY"));
        node.setProperty("IUPAC_openeye_canonical_smiles", entry.properties.get("PUBCHEM_OPENEYE_CAN_SMILES"));
        node.setProperty("IUPAC_openeye_iso_smiles", entry.properties.get("PUBCHEM_OPENEYE_ISO_SMILES"));
        graph.update(node);
    }
}
