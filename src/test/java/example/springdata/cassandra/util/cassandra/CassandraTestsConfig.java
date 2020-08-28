package example.springdata.cassandra.util.cassandra;

import example.springdata.cassandra.util.migrations.CassandraDBTestMigrator;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveDataAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CassandraConfiguration.class, CassandraDBTestMigrator.class})
@ImportAutoConfiguration({CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class, CassandraReactiveDataAutoConfiguration.class})
@ComponentScan(basePackages = {"example.springdata.cassandra.util.migrations"})
public class CassandraTestsConfig {
}
