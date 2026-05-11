package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "regulator", "regulator_class", "activity", "activity_class", "goBiologicalProcess", "goBiologicalProcess_class"})
public class Regulation {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("regulator")
    public Long regulator;
    @JsonProperty("regulator_class")
    public String regulatorClass;
    @JsonProperty("activity")
    public Long activity;
    @JsonProperty("activity_class")
    public String activityClass;
    @JsonProperty("goBiologicalProcess")
    public Long goBiologicalProcess;
    @JsonProperty("goBiologicalProcess_class")
    public String goBiologicalProcessClass;
}
