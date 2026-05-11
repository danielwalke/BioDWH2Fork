package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "DB_ID", "definition", "evidenceType", "goBiologicalProcess", "goCellularComponent",
        "releaseDate", "_doRelease", "releaseStatus", "previousReviewStatus", "reviewStatus"
})
public class Event {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("definition")
    public String definition;
    @JsonProperty("evidenceType")
    public String evidenceType;
    @JsonProperty("goBiologicalProcess")
    public Long goBiologicalProcess;
    @JsonProperty("goCellularComponent")
    public Long goCellularComponent;
    @JsonProperty("releaseDate")
    public String releaseDate;
    @JsonProperty("_doRelease")
    public boolean doRelease;
    @JsonProperty("releaseStatus")
    public Long releaseStatus;
    @JsonProperty("previousReviewStatus")
    public Long previousReviewStatus;
    @JsonProperty("reviewStatus")
    public Long reviewStatus;
}