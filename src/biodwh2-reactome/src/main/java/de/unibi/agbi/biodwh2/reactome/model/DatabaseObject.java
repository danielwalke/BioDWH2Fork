package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "DB_ID", "class", "displayName", "timestamp", "created", "stableIdentifier"
})
public class DatabaseObject {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("_class")
    public String className;
    @JsonProperty("_displayName")
    public String displayName;
    @JsonProperty("stableIdentifier")
    public Long stableIdentifier;
}