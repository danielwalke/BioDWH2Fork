package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "DB_ID", "identifier", "referenceDatabase"
})
public class ReferenceEntity {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("identifier")
    public String identifier;
    @JsonProperty("referenceDatabase")
    public Long referenceDatabase;
}