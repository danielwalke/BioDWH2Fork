package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "DB_ID", "identifier", "identifierVersion", "released"
})
public class StableIdentifier {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("identifier")
    public String identifier;
    @JsonProperty("identifierVersion")
    public String identifierVersion;
    @JsonProperty("released")
    public boolean released;
}