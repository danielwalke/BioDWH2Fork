package de.unibi.agbi.biodwh2.ncbi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "tax_id", "GeneID", "genomic_accession.version", "genomic_gi", "start_position", "end_position", "orientation",
        "chromosome", "GeneIDs_on_left", "distance_to_left", "GeneIDs_on_right", "distance_to_right",
        "overlapping_GeneIDs", "assembly"
})
public class GeneNeighbor {
    @JsonProperty("tax_id")
    public String taxonomyId;
    @JsonProperty("GeneID")
    public String geneId;
    @JsonProperty("genomic_accession.version")
    public String genomicAccessionVersion;
    @JsonProperty("genomic_gi")
    public String genomicGi;
    @JsonProperty("start_position")
    public String startPosition;
    @JsonProperty("end_position")
    public String endPosition;
    @JsonProperty("orientation")
    public String orientation;
    @JsonProperty("chromosome")
    public String chromosome;
    @JsonProperty("GeneIDs_on_left")
    public String geneIdsOnLeft;
    @JsonProperty("distance_to_left")
    public String distanceToLeft;
    @JsonProperty("GeneIDs_on_right")
    public String geneIdsOnRight;
    @JsonProperty("distance_to_right")
    public String distanceToRight;
    @JsonProperty("overlapping_GeneIDs")
    public String overlappingGeneIds;
    @JsonProperty("assembly")
    public String assembly;
}
