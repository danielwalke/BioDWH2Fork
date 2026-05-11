package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "DB_ID", "accessUrl", "url", "resourceIdentifier", "identifiersPrefix"
})
public class ReferenceDatabase {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("accessUrl")
    public String accessUrl;
    @JsonProperty("url")
    public String url;
    @JsonProperty("resourceIdentifier")
    public String resourceIdentifier;
    @JsonProperty("identifiersPrefix")
    public String identifiersPrefix;
}