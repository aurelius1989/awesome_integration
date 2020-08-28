package example.springdata.cassandra.util.cassandra;

import example.springdata.cassandra.config.ServiceProperties;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.DataCenterReplication;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

import java.util.Collections;
import java.util.List;

/**
 * Manages the configuration of the Cassandra database using the values provided by the Spring <i>application.properties</i> file.
 */
@Configuration
@EnableReactiveCassandraRepositories(basePackages = {"example.springdata.cassandra.persistence",
        "example.springdata.cassandra.model"})
@EnableConfigurationProperties({CassandraProperties.class, ServiceProperties.class})
public class CassandraConfiguration extends AbstractCassandraConfiguration {

    private final CassandraProperties cassandraProperties;
    private final ServiceProperties.Cassandra customProperties;

    CassandraConfiguration(CassandraProperties cassandraProperties, ServiceProperties serviceProperties) {
        this.cassandraProperties = cassandraProperties;
        this.customProperties = serviceProperties.getCassandra();
    }


    @Override
    public String getContactPoints() {
        return String.join(",", cassandraProperties.getContactPoints());
    }

    @Override
    public int getPort() {
        return cassandraProperties.getPort();
    }

    @Override
    protected String getKeyspaceName() {
        return cassandraProperties.getKeyspaceName();
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        final CreateKeyspaceSpecification keyspaceSpecification =
                CreateKeyspaceSpecification.createKeyspace(cassandraProperties.getKeyspaceName()).ifNotExists();

        if (customProperties.isDatacenterAware()) {
            keyspaceSpecification.withNetworkReplication(
                    DataCenterReplication.of(customProperties.getDatacenter(), customProperties.getReplicationFactor())
            );
        }

        return Collections.singletonList(keyspaceSpecification);
    }

    @Override
    protected String getLocalDataCenter() {
        return cassandraProperties.getLocalDatacenter();
    }


}
