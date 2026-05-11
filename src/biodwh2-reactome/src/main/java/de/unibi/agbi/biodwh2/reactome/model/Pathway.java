package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "doi", "isCanonical", "normalPathway", "normalPathway_class", "hasEHLD", "lastUpdatedDate", "cell", "cell_class"})
public class Pathway {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("doi")
    public String doi;
    @JsonProperty("isCanonical")
    public String isCanonical;
    @JsonProperty("normalPathway")
    public Long normalPathway;
    @JsonProperty("normalPathway_class")
    public String normalPathwayClass;
    @JsonProperty("hasEHLD")
    public String hasEHLD;
    @JsonProperty("lastUpdatedDate")
    public String lastUpdatedDate;
    @JsonProperty("cell")
    public Long cell;
    @JsonProperty("cell_class")
    public String cellClass;
}
