package de.unibi.agbi.biodwh2.reactome.etl;

import de.unibi.agbi.biodwh2.core.Workspace;
import de.unibi.agbi.biodwh2.core.etl.GraphExporter;
import de.unibi.agbi.biodwh2.core.exceptions.ExporterException;
import de.unibi.agbi.biodwh2.core.io.FileUtils;
import de.unibi.agbi.biodwh2.core.model.graph.Graph;
import de.unibi.agbi.biodwh2.core.model.graph.IndexDescription;
import de.unibi.agbi.biodwh2.core.model.graph.Node;
import de.unibi.agbi.biodwh2.reactome.ReactomeDataSource;
import de.unibi.agbi.biodwh2.reactome.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ReactomeGraphExporter extends GraphExporter<ReactomeDataSource> {
    private static final Logger LOGGER = LogManager.getLogger(ReactomeGraphExporter.class);
    
    static final String DATABASE_OBJECT_LABEL = "DatabaseObject";
    static final String EVENT_LABEL = "Event";
    static final String PATHWAY_LABEL = "Pathway";
    static final String REACTION_LABEL = "ReactionLikeEvent";
    static final String PHYSICAL_ENTITY_LABEL = "PhysicalEntity";
    static final String REFERENCE_ENTITY_LABEL = "ReferenceEntity";
    static final String REFERENCE_DATABASE_LABEL = "ReferenceDatabase";
    static final String STABLE_IDENTIFIER_LABEL = "StableIdentifier";
    static final String COMPLEX_LABEL = "Complex";
    static final String COMPARTMENT_LABEL = "Compartment";
    static final String LITERATURE_REFERENCE_LABEL = "LiteratureReference";
    static final String SPECIES_LABEL = "Species";
    static final String REGULATION_LABEL = "Regulation";
    static final String NEGATIVE_REGULATION_LABEL = "NegativeRegulation";
    static final String POSITIVE_REGULATION_LABEL = "PositiveRegulation";
    static final String GO_BIOLOGICAL_PROCESS_LABEL = "GO_BiologicalProcess";
    static final String GO_CELLLULAR_COMPONENT_LABEL = "GO_CellularComponent";
    static final String GO_MOLECULAR_FUNCTION_LABEL = "GO_MolecularFunction";

    static final String IS_A = "IS_A";
    static final String MAPPED_TO = "MAPPED_TO";
    static final String HAS_COMPONENT = "HAS_COMPONENT";
    static final String REGULATES = "REGULATES";
    static final String PUBLISHED_IN = "PUBLISHED_IN";
    static final String MAPPED_FROM = "MAPPED_FROM";
    static final String IN_COMPARTMENT = "IN_COMPARTMENT";
    static final String RELATES_TO = "RELATES_TO";

    public ReactomeGraphExporter(final ReactomeDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long getExportVersion() {
        return 5;
    }

    @Override
    protected boolean exportGraph(final Workspace workspace, final Graph graph) throws ExporterException {
        graph.addIndex(IndexDescription.forNode(DATABASE_OBJECT_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(EVENT_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PATHWAY_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REACTION_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(PHYSICAL_ENTITY_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REFERENCE_ENTITY_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REFERENCE_DATABASE_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(STABLE_IDENTIFIER_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(COMPLEX_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(COMPARTMENT_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(LITERATURE_REFERENCE_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(SPECIES_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(REGULATION_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(NEGATIVE_REGULATION_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(POSITIVE_REGULATION_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(GO_BIOLOGICAL_PROCESS_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(GO_CELLLULAR_COMPONENT_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        graph.addIndex(IndexDescription.forNode(GO_MOLECULAR_FUNCTION_LABEL, ID_KEY, IndexDescription.Type.UNIQUE));
        
        exportDatabaseObjects(workspace, graph);
        exportStableIdentifiers(workspace, graph);
        exportReferenceDatabases(workspace, graph);
        exportEvents(workspace, graph);
        exportPathways(workspace, graph);
        exportReactions(workspace, graph);
        exportPhysicalEntities(workspace, graph);
        exportReferenceEntities(workspace, graph);
        exportComplexes(workspace, graph);
        exportCompartments(workspace, graph);
        exportLiteratureReferences(workspace, graph);
        exportSpecies(workspace, graph);
        exportRegulations(workspace, graph);
        exportNegativeRegulations(workspace, graph);
        exportPositiveRegulations(workspace, graph);
        exportGoTerms(workspace, graph);
        exportEventLiteratureReferences(workspace, graph);
        exportEventGoTerms(workspace, graph);
        exportComplexHasComponents(workspace, graph);
        exportComplexSpecies(workspace, graph);
        exportComplexCompartments(workspace, graph);
        exportPhysicalEntityInCompartment(workspace, graph);
        return true;
    }

    private void openTsvFile(final Workspace workspace, final String fileName, 
                             final BiConsumer<String[], String[]> consumer) throws IOException {
        final java.io.InputStream stream = FileUtils.openInput(workspace, dataSource, fileName);
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String[] headers = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (headers == null) {
                    headers = parseTsvHeaders(line);
                } else if (StringUtils.isNotBlank(line)) {
                    final String[] values = parseTsvLine(line);
                    if (values.length > 0) {
                        consumer.accept(headers, values);
                    }
                }
            }
        }
    }

    private String[] parseTsvHeaders(final String line) {
        return line.split("\t", -1);
    }

    private String[] parseTsvLine(final String line) {
        final String[] parts = line.split("\t", -1);
        final String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            part = cleanJsonValue(part);
            if ("null".equals(part) || part.isEmpty()) {
                result[i] = null;
            } else {
                result[i] = part;
            }
        }
        return result;
    }

    private String cleanJsonValue(final String value) {
        if (value == null || value.isEmpty())
            return value;
        
        String result = value.trim();
        
        if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
            result = result.substring(1, result.length() - 1);
        }
        
        if (result.startsWith("\"") && !result.endsWith("\"")) {
            final int lastQuote = result.lastIndexOf("\"");
            if (lastQuote > 0) {
                result = result.substring(1, lastQuote);
            } else {
                result = result.substring(1);
            }
        }
        
        result = result.trim();
        
        if (result.isEmpty() || "null".equals(result)) {
            return null;
        }
        
        return result;
    }

    private String getColumnValue(final String[] headers, final String[] row, final String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equals(columnName) && i < row.length) {
                final String value = row[i];
                return value == null || value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private Long parseLong(final String value) {
        if (value == null || value.isEmpty())
            return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getColumnValueTrimmed(final String[] headers, final String[] row, final String columnName) {
        String value = getColumnValue(headers, row, columnName);
        return value == null ? null : value.trim();
    }

    private void exportDatabaseObjects(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting database objects...");
        try {
            openTsvFile(workspace, "DatabaseObject.tsv", (headers, row) -> {
                final DatabaseObject entry = new DatabaseObject();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.className = getColumnValue(headers, row, "_class");
                entry.displayName = getColumnValue(headers, row, "_displayName");
                entry.stableIdentifier = parseLong(getColumnValue(headers, row, "stableIdentifier"));
                exportDatabaseObject(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export database objects: " + e.getMessage());
        }
    }

    private void exportEvents(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting events...");
        try {
            openTsvFile(workspace, "Event.tsv", (headers, row) -> {
                final Event entry = new Event();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.definition = getColumnValue(headers, row, "definition");
                entry.evidenceType = getColumnValue(headers, row, "evidenceType");
                entry.goBiologicalProcess = parseLong(getColumnValue(headers, row, "goBiologicalProcess"));
                entry.goCellularComponent = parseLong(getColumnValue(headers, row, "goCellularComponent"));
                entry.releaseDate = getColumnValue(headers, row, "releaseDate");
                entry.doRelease = Boolean.parseBoolean(getColumnValue(headers, row, "_doRelease"));
                entry.releaseStatus = parseLong(getColumnValue(headers, row, "releaseStatus"));
                entry.previousReviewStatus = parseLong(getColumnValue(headers, row, "previousReviewStatus"));
                entry.reviewStatus = parseLong(getColumnValue(headers, row, "reviewStatus"));
                exportEvent(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export events: " + e.getMessage());
        }
    }

    private void exportStableIdentifiers(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting stable identifiers...");
        try {
            openTsvFile(workspace, "StableIdentifier.tsv", (headers, row) -> {
                final StableIdentifier entry = new StableIdentifier();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.identifier = getColumnValue(headers, row, "identifier");
                entry.identifierVersion = getColumnValue(headers, row, "identifierVersion");
                entry.released = Boolean.parseBoolean(getColumnValue(headers, row, "released"));
                exportStableIdentifier(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export stable identifiers: " + e.getMessage());
        }
    }

    private void exportReferenceDatabases(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting reference databases...");
        try {
            openTsvFile(workspace, "ReferenceDatabase.tsv", (headers, row) -> {
                final ReferenceDatabase entry = new ReferenceDatabase();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.accessUrl = getColumnValue(headers, row, "accessUrl");
                entry.url = getColumnValue(headers, row, "url");
                entry.resourceIdentifier = getColumnValue(headers, row, "resourceIdentifier");
                entry.identifiersPrefix = getColumnValue(headers, row, "identifiersPrefix");
                exportReferenceDatabase(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export reference databases: " + e.getMessage());
        }
    }

    private void exportReferenceEntities(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting reference entities...");
        try {
            openTsvFile(workspace, "ReferenceEntity.tsv", (headers, row) -> {
                final ReferenceEntity entry = new ReferenceEntity();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.identifier = getColumnValue(headers, row, "identifier");
                entry.referenceDatabase = parseLong(getColumnValue(headers, row, "referenceDatabase"));
                exportReferenceEntity(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export reference entities: " + e.getMessage());
        }
    }

    private void exportEventsBulk(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting events from DatabaseObject...");
        try {
            openTsvFile(workspace, "DatabaseObject.tsv", (headers, row) -> {
                final String className = getColumnValue(headers, row, "_class");
                if (className == null)
                    return;
                final Long dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                if ("Event".equals(className)) {
                    final Event entry = new Event();
                    entry.dbId = dbId;
                    entry.definition = getColumnValue(headers, row, "definition");
                    entry.evidenceType = getColumnValue(headers, row, "evidenceType");
                    entry.goBiologicalProcess = parseLong(getColumnValue(headers, row, "goBiologicalProcess"));
                    entry.goCellularComponent = parseLong(getColumnValue(headers, row, "goCellularComponent"));
                    entry.releaseDate = getColumnValue(headers, row, "releaseDate");
                    entry.doRelease = Boolean.parseBoolean(getColumnValue(headers, row, "_doRelease"));
                    entry.releaseStatus = parseLong(getColumnValue(headers, row, "releaseStatus"));
                    entry.previousReviewStatus = parseLong(getColumnValue(headers, row, "previousReviewStatus"));
                    entry.reviewStatus = parseLong(getColumnValue(headers, row, "reviewStatus"));
                    exportEvent(graph, entry);
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export events from DatabaseObject: " + e.getMessage());
        }
    }

    private final Map<Long, String> displayNameCache = new HashMap<>();
    private final Map<Long, String> classNameCache = new HashMap<>();
    private final Map<Long, Long> stableIdentifierCache = new HashMap<>();
    private final Map<Long, String> stableIdCache = new HashMap<>();

    private void exportDatabaseObject(final Graph graph, final DatabaseObject entry) {
        if (entry.dbId != null) {
            displayNameCache.put(entry.dbId, entry.displayName);
            classNameCache.put(entry.dbId, entry.className);
        }
        if (entry.stableIdentifier != null) {
            stableIdentifierCache.put(entry.dbId, entry.stableIdentifier);
        }
    }

    private void exportStableIdentifier(final Graph graph, final StableIdentifier entry) {
        graph.buildNode().withLabel(STABLE_IDENTIFIER_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("identifier", entry.identifier)
            .withPropertyIfNotNull("identifier_version", entry.identifierVersion)
            .withPropertyIfNotNull("released", entry.released)
            .build();
        
        if (StringUtils.isNotEmpty(entry.identifier)) {
            stableIdCache.put(entry.dbId, entry.identifier);
        }
    }

    private void exportReferenceDatabase(final Graph graph, final ReferenceDatabase entry) {
        final Node node = graph.buildNode().withLabel(REFERENCE_DATABASE_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("access_url", entry.accessUrl)
            .withPropertyIfNotNull("url", entry.url)
            .withPropertyIfNotNull("resource_identifier", entry.resourceIdentifier)
            .withPropertyIfNotNull("identifiers_prefix", entry.identifiersPrefix)
            .build();
        
        final String displayName = displayNameCache.get(entry.dbId);
        if (StringUtils.isNotEmpty(displayName)) {
            displayNameCache.put(entry.dbId, displayName);
        }
    }

    private void exportEvent(final Graph graph, final Event entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(EVENT_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("definition", entry.definition)
            .withPropertyIfNotNull("go_biological_process", entry.goBiologicalProcess)
            .withPropertyIfNotNull("go_cellular_component", entry.goCellularComponent)
            .withPropertyIfNotNull("evidence_type", entry.evidenceType)
            .withPropertyIfNotNull("release_date", entry.releaseDate)
            .withPropertyIfNotNull("do_release", entry.doRelease)
            .withPropertyIfNotNull("release_status", entry.releaseStatus)
            .withPropertyIfNotNull("previous_review_status", entry.previousReviewStatus)
            .withPropertyIfNotNull("review_status", entry.reviewStatus)
            .build();
    }

    private void exportPathways(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting pathways...");
        try {
            openTsvFile(workspace, "Pathway.tsv", (headers, row) -> {
                final Pathway entry = new Pathway();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.doi = getColumnValue(headers, row, "doi");
                entry.isCanonical = getColumnValue(headers, row, "isCanonical");
                entry.normalPathway = parseLong(getColumnValue(headers, row, "normalPathway"));
                entry.normalPathwayClass = getColumnValue(headers, row, "normalPathway_class");
                entry.hasEHLD = getColumnValue(headers, row, "hasEHLD");
                entry.lastUpdatedDate = getColumnValue(headers, row, "lastUpdatedDate");
                entry.cell = parseLong(getColumnValue(headers, row, "cell"));
                entry.cellClass = getColumnValue(headers, row, "cell_class");
                exportPathway(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export pathways: " + e.getMessage());
        }
    }

    private void exportPathway(final Graph graph, final Pathway entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node eventNode = graph.findNode(EVENT_LABEL, ID_KEY, entry.dbId);
        if (eventNode == null)
            return;
        
        final Node node = graph.buildNode().withLabel(PATHWAY_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .withPropertyIfNotNull("doi", entry.doi)
            .withPropertyIfNotNull("is_canonical", entry.isCanonical)
            .withPropertyIfNotNull("normal_pathway", entry.normalPathway)
            .withPropertyIfNotNull("normal_pathway_class", entry.normalPathwayClass)
            .withPropertyIfNotNull("has_ehld", entry.hasEHLD)
            .withPropertyIfNotNull("last_updated_date", entry.lastUpdatedDate)
            .withPropertyIfNotNull("cell", entry.cell)
            .withPropertyIfNotNull("cell_class", entry.cellClass)
            .build();
        
        graph.addEdge(node, eventNode, IS_A);
    }

    private void exportReactions(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting reactions...");
        try {
            openTsvFile(workspace, "ReactionlikeEvent.tsv", (headers, row) -> {
                final ReactionlikeEvent entry = new ReactionlikeEvent();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.isChimeric = getColumnValue(headers, row, "isChimeric");
                entry.systematicName = getColumnValue(headers, row, "systematicName");
                entry.normalReaction = parseLong(getColumnValue(headers, row, "normalReaction"));
                entry.catalystActivityReference = parseLong(getColumnValue(headers, row, "catalystActivityReference"));
                entry.reactionType = parseLong(getColumnValue(headers, row, "reactionType"));
                exportReaction(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export reactions: " + e.getMessage());
        }
    }

    private void exportReaction(final Graph graph, final ReactionlikeEvent entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node eventNode = graph.findNode(EVENT_LABEL, ID_KEY, entry.dbId);
        if (eventNode == null)
            return;
        
        final Node node = graph.buildNode().withLabel(REACTION_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .withPropertyIfNotNull("systematic_name", entry.systematicName)
            .withPropertyIfNotNull("is_chimeric", entry.isChimeric)
            .withPropertyIfNotNull("normal_reaction", entry.normalReaction)
            .withPropertyIfNotNull("catalyst_activity_reference", entry.catalystActivityReference)
            .withPropertyIfNotNull("reaction_type", entry.reactionType)
            .build();
        
        graph.addEdge(node, eventNode, IS_A);
    }

    private void exportPhysicalEntities(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting physical entities...");
        try {
            openTsvFile(workspace, "PhysicalEntity.tsv", (headers, row) -> {
                final PhysicalEntity entry = new PhysicalEntity();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.definition = getColumnValue(headers, row, "definition");
                entry.goCellularComponent = parseLong(getColumnValue(headers, row, "goCellularComponent"));
                entry.goCellularComponentClass = getColumnValue(headers, row, "goCellularComponent_class");
                entry.authored = getColumnValue(headers, row, "authored");
                entry.authoredClass = getColumnValue(headers, row, "authored_class");
                entry.systematicName = getColumnValue(headers, row, "systematicName");
                exportPhysicalEntity(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export physical entities: " + e.getMessage());
        }
    }

    private void exportPhysicalEntity(final Graph graph, final PhysicalEntity entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(PHYSICAL_ENTITY_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .withPropertyIfNotNull("definition", entry.definition)
            .withPropertyIfNotNull("go_cellular_component", entry.goCellularComponent)
            .withPropertyIfNotNull("go_cellular_component_class", entry.goCellularComponentClass)
            .withPropertyIfNotNull("authored", entry.authored)
            .withPropertyIfNotNull("authored_class", entry.authoredClass)
            .withPropertyIfNotNull("systematic_name", entry.systematicName)
            .build();
    }

    private void exportReferenceEntity(final Graph graph, final ReferenceEntity entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(REFERENCE_ENTITY_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .withPropertyIfNotNull("identifier", entry.identifier)
            .withPropertyIfNotNull("reference_database_id", entry.referenceDatabase)
            .build();
        
        if (entry.referenceDatabase != null) {
            final Long referenceDatabaseNodeId = graph.findNodeId(REFERENCE_DATABASE_LABEL, ID_KEY, entry.referenceDatabase);
            if (referenceDatabaseNodeId != null)
                graph.addEdge(node, referenceDatabaseNodeId, MAPPED_FROM);
        }
    }

    private void exportComplexes(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting complexes...");
        try {
            openTsvFile(workspace, "Complex.tsv", (headers, row) -> {
                final Complex entry = new Complex();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.isChimeric = getColumnValue(headers, row, "isChimeric");
                entry.stoichiometryKnown = getColumnValue(headers, row, "stoichiometryKnown");
                exportComplex(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export complexes: " + e.getMessage());
        }
    }

    private void exportComplex(final Graph graph, final Complex entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(COMPLEX_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .withPropertyIfNotNull("is_chimeric", entry.isChimeric)
            .withPropertyIfNotNull("stoichiometry_known", entry.stoichiometryKnown)
            .build();
    }

    private void exportCompartments(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting compartments...");
        try {
            openTsvFile(workspace, "Compartment.tsv", (headers, row) -> {
                final Compartment entry = new Compartment();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                exportCompartment(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export compartments: " + e.getMessage());
        }
    }

    private void exportCompartment(final Graph graph, final Compartment entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        
        graph.buildNode().withLabel(COMPARTMENT_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .build();
    }

    private void exportLiteratureReferences(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting literature references...");
        try {
            openTsvFile(workspace, "LiteratureReference.tsv", (headers, row) -> {
                final LiteratureReference entry = new LiteratureReference();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.journal = getColumnValue(headers, row, "journal");
                entry.pages = getColumnValue(headers, row, "pages");
                entry.pubMedIdentifier = parseLong(getColumnValue(headers, row, "pubMedIdentifier"));
                entry.volume = parseLong(getColumnValue(headers, row, "volume"));
                entry.year = parseLong(getColumnValue(headers, row, "year"));
                entry.comment = getColumnValue(headers, row, "comment");
                exportLiteratureReference(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export literature references: " + e.getMessage());
        }
    }

    private void exportLiteratureReference(final Graph graph, final LiteratureReference entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(LITERATURE_REFERENCE_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("journal", entry.journal)
            .withPropertyIfNotNull("pages", entry.pages)
            .withPropertyIfNotNull("pubmed_id", entry.pubMedIdentifier)
            .withPropertyIfNotNull("volume", entry.volume)
            .withPropertyIfNotNull("year", entry.year)
            .withPropertyIfNotNull("comment", entry.comment)
            .build();
    }

    private void exportSpecies(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting species...");
        try {
            openTsvFile(workspace, "Species.tsv", (headers, row) -> {
                final Species entry = new Species();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.abbreviation = getColumnValue(headers, row, "abbreviation");
                exportSpecies(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export species: " + e.getMessage());
        }
    }

    private void exportSpecies(final Graph graph, final Species entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(SPECIES_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("abbreviation", entry.abbreviation)
            .build();
    }

    private void exportRegulations(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting regulations...");
        try {
            openTsvFile(workspace, "Regulation.tsv", (headers, row) -> {
                final Regulation entry = new Regulation();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                entry.regulator = parseLong(getColumnValue(headers, row, "regulator"));
                entry.regulatorClass = getColumnValue(headers, row, "regulator_class");
                entry.activity = parseLong(getColumnValue(headers, row, "activity"));
                entry.activityClass = getColumnValue(headers, row, "activity_class");
                entry.goBiologicalProcess = parseLong(getColumnValue(headers, row, "goBiologicalProcess"));
                entry.goBiologicalProcessClass = getColumnValue(headers, row, "goBiologicalProcess_class");
                exportRegulation(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export regulations: " + e.getMessage());
        }
    }

    private void exportRegulation(final Graph graph, final Regulation entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node node = graph.buildNode().withLabel(REGULATION_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .withPropertyIfNotNull("regulator", entry.regulator)
            .withPropertyIfNotNull("regulator_class", entry.regulatorClass)
            .withPropertyIfNotNull("activity", entry.activity)
            .withPropertyIfNotNull("activity_class", entry.activityClass)
            .withPropertyIfNotNull("go_biological_process", entry.goBiologicalProcess)
            .withPropertyIfNotNull("go_biological_process_class", entry.goBiologicalProcessClass)
            .build();
    }

    private void exportNegativeRegulations(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting negative regulations...");
        try {
            openTsvFile(workspace, "NegativeRegulation.tsv", (headers, row) -> {
                final NegativeRegulation entry = new NegativeRegulation();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                exportNegativeRegulation(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export negative regulations: " + e.getMessage());
        }
    }

    private void exportNegativeRegulation(final Graph graph, final NegativeRegulation entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node regulationNode = graph.findNode(REGULATION_LABEL, ID_KEY, entry.dbId);
        if (regulationNode == null)
            return;
        
        final Node node = graph.buildNode().withLabel(NEGATIVE_REGULATION_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .build();
        
        graph.addEdge(node, regulationNode, IS_A);
    }

    private void exportPositiveRegulations(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting positive regulations...");
        try {
            openTsvFile(workspace, "PositiveRegulation.tsv", (headers, row) -> {
                final PositiveRegulation entry = new PositiveRegulation();
                entry.dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                exportPositiveRegulation(graph, entry);
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export positive regulations: " + e.getMessage());
        }
    }

    private void exportPositiveRegulation(final Graph graph, final PositiveRegulation entry) {
        final String displayName = displayNameCache.get(entry.dbId);
        final String stableId = getStableId(entry.dbId);
        final String className = classNameCache.get(entry.dbId);
        
        final Node regulationNode = graph.findNode(REGULATION_LABEL, ID_KEY, entry.dbId);
        if (regulationNode == null)
            return;
        
        final Node node = graph.buildNode().withLabel(POSITIVE_REGULATION_LABEL).withProperty(ID_KEY, entry.dbId)
            .withPropertyIfNotNull("display_name", displayName)
            .withPropertyIfNotNull("stable_identifier", stableId)
            .withPropertyIfNotNull("class_name", className)
            .build();
        
        graph.addEdge(node, regulationNode, IS_A);
    }

    private void exportGoTerms(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting GO terms...");
        try {
            openTsvFile(workspace, "GO_BiologicalProcess.tsv", (headers, row) -> {
                final Long dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                if (dbId != null) {
                    final String displayName = displayNameCache.get(dbId);
                    graph.buildNode().withLabel(GO_BIOLOGICAL_PROCESS_LABEL).withProperty(ID_KEY, dbId)
                        .withPropertyIfNotNull("display_name", displayName)
                        .build();
                }
            });
            
            openTsvFile(workspace, "GO_CellularComponent.tsv", (headers, row) -> {
                final Long dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                if (dbId != null) {
                    final String displayName = displayNameCache.get(dbId);
                    graph.buildNode().withLabel(GO_CELLLULAR_COMPONENT_LABEL).withProperty(ID_KEY, dbId)
                        .withPropertyIfNotNull("display_name", displayName)
                        .build();
                }
            });
            
            openTsvFile(workspace, "GO_MolecularFunction.tsv", (headers, row) -> {
                final Long dbId = parseLong(getColumnValue(headers, row, "DB_ID"));
                if (dbId != null) {
                    final String displayName = displayNameCache.get(dbId);
                    graph.buildNode().withLabel(GO_MOLECULAR_FUNCTION_LABEL).withProperty(ID_KEY, dbId)
                        .withPropertyIfNotNull("display_name", displayName)
                        .build();
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export GO terms: " + e.getMessage());
        }
    }

    private void exportEventLiteratureReferences(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting event-literature reference relationships...");
        try {
            openTsvFile(workspace, "Event_hasLiteratureReference.tsv", (headers, row) -> {
                final Long eventId = parseLong(getColumnValue(headers, row, "Event"));
                final Long literatureReferenceId = parseLong(getColumnValue(headers, row, "literatureReference"));
                
                if (eventId != null && literatureReferenceId != null) {
                    final Long eventNodeId = graph.findNodeId(EVENT_LABEL, ID_KEY, eventId);
                    final Long literatureReferenceNodeId = graph.findNodeId(LITERATURE_REFERENCE_LABEL, ID_KEY, literatureReferenceId);
                    
                    if (eventNodeId != null && literatureReferenceNodeId != null) {
                        graph.addEdge(eventNodeId, literatureReferenceNodeId, PUBLISHED_IN);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export event-literature reference relationships: " + e.getMessage());
        }
    }

    private void exportEventGoTerms(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting event-GO term relationships...");
        try {
            openTsvFile(workspace, "Event_goBiologicalProcess.tsv", (headers, row) -> {
                final Long eventId = parseLong(getColumnValue(headers, row, "Event"));
                final Long goTermId = parseLong(getColumnValue(headers, row, "goBiologicalProcess"));
                
                if (eventId != null && goTermId != null) {
                    final Long eventNodeId = graph.findNodeId(EVENT_LABEL, ID_KEY, eventId);
                    final Long goTermNodeId = graph.findNodeId(GO_BIOLOGICAL_PROCESS_LABEL, ID_KEY, goTermId);
                    
                    if (eventNodeId != null && goTermNodeId != null) {
                        graph.addEdge(eventNodeId, goTermNodeId, RELATES_TO);
                    }
                }
            });
            
            openTsvFile(workspace, "Event_goCellularComponent.tsv", (headers, row) -> {
                final Long eventId = parseLong(getColumnValue(headers, row, "Event"));
                final Long goTermId = parseLong(getColumnValue(headers, row, "goCellularComponent"));
                
                if (eventId != null && goTermId != null) {
                    final Long eventNodeId = graph.findNodeId(EVENT_LABEL, ID_KEY, eventId);
                    final Long goTermNodeId = graph.findNodeId(GO_CELLLULAR_COMPONENT_LABEL, ID_KEY, goTermId);
                    
                    if (eventNodeId != null && goTermNodeId != null) {
                        graph.addEdge(eventNodeId, goTermNodeId, RELATES_TO);
                    }
                }
            });
            
            openTsvFile(workspace, "Event_goMolecularFunction.tsv", (headers, row) -> {
                final Long eventId = parseLong(getColumnValue(headers, row, "Event"));
                final Long goTermId = parseLong(getColumnValue(headers, row, "goMolecularFunction"));
                
                if (eventId != null && goTermId != null) {
                    final Long eventNodeId = graph.findNodeId(EVENT_LABEL, ID_KEY, eventId);
                    final Long goTermNodeId = graph.findNodeId(GO_MOLECULAR_FUNCTION_LABEL, ID_KEY, goTermId);
                    
                    if (eventNodeId != null && goTermNodeId != null) {
                        graph.addEdge(eventNodeId, goTermNodeId, RELATES_TO);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export event-GO term relationships: " + e.getMessage());
        }
    }

    private void exportComplexHasComponents(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting complex-component relationships...");
        try {
            openTsvFile(workspace, "Complex_2_hasComponent.tsv", (headers, row) -> {
                final Long complexId = parseLong(getColumnValue(headers, row, "DB_ID"));
                final Long componentId = parseLong(getColumnValue(headers, row, "hasComponent"));
                
                if (complexId != null && componentId != null) {
                    final Long complexNodeId = graph.findNodeId(COMPLEX_LABEL, ID_KEY, complexId);
                    final Long componentNodeId = getComponentNodeId(graph, componentId);
                    
                    if (complexNodeId != null && componentNodeId != null) {
                        graph.addEdge(complexNodeId, componentNodeId, HAS_COMPONENT);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export complex-component relationships: " + e.getMessage());
        }
    }

    private void exportComplexSpecies(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting complex-species relationships...");
        try {
            openTsvFile(workspace, "Complex_2_species.tsv", (headers, row) -> {
                final Long complexId = parseLong(getColumnValue(headers, row, "DB_ID"));
                final Long speciesId = parseLong(getColumnValue(headers, row, "species"));
                
                if (complexId != null && speciesId != null) {
                    final Long complexNodeId = graph.findNodeId(COMPLEX_LABEL, ID_KEY, complexId);
                    final Long speciesNodeId = graph.findNodeId(SPECIES_LABEL, ID_KEY, speciesId);
                    
                    if (complexNodeId != null && speciesNodeId != null) {
                        graph.addEdge(complexNodeId, speciesNodeId, RELATES_TO);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export complex-species relationships: " + e.getMessage());
        }
    }

    private void exportComplexCompartments(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting complex-compartment relationships...");
        try {
            openTsvFile(workspace, "Complex_2_compartment.tsv", (headers, row) -> {
                final Long complexId = parseLong(getColumnValue(headers, row, "DB_ID"));
                final Long compartmentId = parseLong(getColumnValue(headers, row, "compartment"));
                
                if (complexId != null && compartmentId != null) {
                    final Long complexNodeId = graph.findNodeId(COMPLEX_LABEL, ID_KEY, complexId);
                    final Long compartmentNodeId = graph.findNodeId(COMPARTMENT_LABEL, ID_KEY, compartmentId);
                    
                    if (complexNodeId != null && compartmentNodeId != null) {
                        graph.addEdge(complexNodeId, compartmentNodeId, IN_COMPARTMENT);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export complex-compartment relationships: " + e.getMessage());
        }
    }

    private void exportPhysicalEntityInCompartment(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting physical entity-compartment relationships...");
        try {
            openTsvFile(workspace, "PhysicalEntity_2_compartment.tsv", (headers, row) -> {
                final Long physicalEntityId = parseLong(getColumnValue(headers, row, "PhysicalEntity"));
                final Long compartmentId = parseLong(getColumnValue(headers, row, "compartment"));
                
                if (physicalEntityId != null && compartmentId != null) {
                    final Long physicalEntityNodeId = graph.findNodeId(PHYSICAL_ENTITY_LABEL, ID_KEY, physicalEntityId);
                    final Long compartmentNodeId = graph.findNodeId(COMPARTMENT_LABEL, ID_KEY, compartmentId);
                    
                    if (physicalEntityNodeId != null && compartmentNodeId != null) {
                        graph.addEdge(physicalEntityNodeId, compartmentNodeId, IN_COMPARTMENT);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export physical entity-compartment relationships: " + e.getMessage());
        }
    }

    private void exportPhysicalEntityMappedToReferenceEntity(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting physical entity-reference entity relationships...");
        try {
            openTsvFile(workspace, "PhysicalEntity_2_referenceEntity.tsv", (headers, row) -> {
                final Long physicalEntityId = parseLong(getColumnValue(headers, row, "PhysicalEntity"));
                final Long referenceEntityId = parseLong(getColumnValue(headers, row, "referenceEntity"));
                
                if (physicalEntityId != null && referenceEntityId != null) {
                    final Long physicalEntityNodeId = graph.findNodeId(PHYSICAL_ENTITY_LABEL, ID_KEY, physicalEntityId);
                    final Long referenceEntityNodeId = graph.findNodeId(REFERENCE_ENTITY_LABEL, ID_KEY, referenceEntityId);
                    
                    if (physicalEntityNodeId != null && referenceEntityNodeId != null) {
                        graph.addEdge(physicalEntityNodeId, referenceEntityNodeId, MAPPED_TO);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export physical entity-reference entity relationships: " + e.getMessage());
        }
    }

    private void exportRegulationRegulatesEvent(final Workspace workspace, final Graph graph) {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Exporting regulation-event relationships...");
        try {
            openTsvFile(workspace, "Regulation_2_regulated.tsv", (headers, row) -> {
                final Long regulationId = parseLong(getColumnValue(headers, row, "regulation"));
                final Long regulatedEventId = parseLong(getColumnValue(headers, row, "regulated"));
                
                if (regulationId != null && regulatedEventId != null) {
                    final Long regulationNodeId = graph.findNodeId(REGULATION_LABEL, ID_KEY, regulationId);
                    final Long eventNodeId = graph.findNodeId(EVENT_LABEL, ID_KEY, regulatedEventId);
                    
                    if (regulationNodeId != null && eventNodeId != null) {
                        graph.addEdge(regulationNodeId, eventNodeId, REGULATES);
                    }
                }
            });
        } catch (final IOException e) {
            LOGGER.warn("Failed to export regulation-event relationships: " + e.getMessage());
        }
    }

    private Long getComponentNodeId(final Graph graph, final Long componentId) {
        final Long[] labels = {
            graph.findNodeId(PHYSICAL_ENTITY_LABEL, ID_KEY, componentId),
            graph.findNodeId(COMPLEX_LABEL, ID_KEY, componentId),
            graph.findNodeId(REFERENCE_ENTITY_LABEL, ID_KEY, componentId)
        };
        for (final Long nodeId : labels) {
            if (nodeId != null)
                return nodeId;
        }
        return null;
    }

    private String getStableId(final Long dbId) {
        final Long stableIdentifierId = stableIdentifierCache.get(dbId);
        if (stableIdentifierId != null) {
            return stableIdCache.get(stableIdentifierId);
        }
        return null;
    }
}
