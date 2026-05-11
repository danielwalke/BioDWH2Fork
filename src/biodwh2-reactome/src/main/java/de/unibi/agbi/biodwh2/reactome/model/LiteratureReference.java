package de.unibi.agbi.biodwh2.reactome.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"DB_ID", "journal", "pages", "pubMedIdentifier", "volume", "year", "comment"})
public class LiteratureReference {
    @JsonProperty("DB_ID")
    public Long dbId;
    @JsonProperty("journal")
    public String journal;
    @JsonProperty("pages")
    public String pages;
    @JsonProperty("pubMedIdentifier")
    public Long pubMedIdentifier;
    @JsonProperty("volume")
    public Long volume;
    @JsonProperty("year")
    public Long year;
    @JsonProperty("comment")
    public String comment;
}
