package de.unibi.agbi.biodwh2.kegg.model;

import java.util.ArrayList;
import java.util.List;

public class Pathway extends KeggEntry {
    public final List<String> names = new ArrayList<>();
    public String description;
    public final List<String> classes = new ArrayList<>();
    public String pathwayMap;
    public final List<NameIdsPair> modules = new ArrayList<>();
    public final List<NameIdsPair> diseases = new ArrayList<>();
    public final List<NameIdsPair> drugs = new ArrayList<>();
    public final List<NameIdsPair> genes = new ArrayList<>();
    public final List<NameIdsPair> compounds = new ArrayList<>();
}
