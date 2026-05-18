package de.unibi.agbi.biodwh2.gtdb;

import de.unibi.agbi.biodwh2.core.DataSource;
import de.unibi.agbi.biodwh2.core.DevelopmentState;
import de.unibi.agbi.biodwh2.core.etl.*;
import de.unibi.agbi.biodwh2.gtdb.etl.GTDBGraphExporter;
import de.unibi.agbi.biodwh2.gtdb.etl.GTDBMappingDescriber;
import de.unibi.agbi.biodwh2.gtdb.etl.GTDBUpdater;

public class GTDBDataSource extends DataSource {
    @Override
    public String getId() {
        return "GTDB";
    }

    @Override
    public DevelopmentState getDevelopmentState() {
        return DevelopmentState.InDevelopment;
    }

    @Override
    public Updater<GTDBDataSource> getUpdater() {
        return new GTDBUpdater(this);
    }

    @Override
    protected Parser<GTDBDataSource> getParser() {
        return new PassThroughParser<>(this);
    }

    @Override
    protected GraphExporter<GTDBDataSource> getGraphExporter() {
        return new GTDBGraphExporter(this);
    }

    @Override
    public MappingDescriber getMappingDescriber() {
        return new GTDBMappingDescriber(this);
    }
}