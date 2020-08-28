package example.springdata.cassandra.util.migrations;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import example.springdata.cassandra.config.ServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import uk.sky.cqlmigrate.CassandraLockConfig;
import uk.sky.cqlmigrate.CqlMigrator;
import uk.sky.cqlmigrate.CqlMigratorFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cassandra migration class only used for tests.
 */
@Component
@EnableConfigurationProperties({CassandraProperties.class, ServiceProperties.class})
public class CassandraDBTestMigrator implements HealthIndicator, SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraDBTestMigrator.class);

    private Health status = new Health.Builder().down().build();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Autowired
    protected CassandraProperties cassandraProperties;

    @Autowired
    protected ServiceProperties serviceProperties;

    /**
     * Start cassandra database migration.
     */
    public void migrateCassandra() {
        LOGGER.info("###################  Starting TEST migration of cassandra tables...");
        CqlSession session = buildEmbeddedCluster();
        createCqlMigrateKeyspace(session);
        migrateCassandra(true);
    }

    protected void migrateCassandra(boolean rethrowExceptions) {
        LOGGER.info("Starting migration of cassandra tables...");
        try {
            isRunning.set(true);
            status = new Health.Builder().status("STARTING").withDetail("reason", "Cassandra db migration pending").build();

            // Configure locking for coordination of multiple nodes
            CassandraLockConfig lockConfig = CassandraLockConfig.builder()
                    .withTimeout(Duration.ofSeconds(3))
                    .withPollingInterval(Duration.ofMillis(500))
                    .unlockOnFailure()
                    .build();

            // Create a migrator and run it
            CqlMigrator migrator = CqlMigratorFactory.create(lockConfig);

            String keySpace = cassandraProperties.getKeyspaceName();
            LOGGER.info("Using keyspace {}", keySpace);

            Path basicSchema = getSchema("/database/cql");
//            migrator.migrate(session, keySpace, Arrays.asList(basicSchema));
            String[] localhost = {"localhost"};

            migrator.migrate(localhost, 9142, "cassandra",
                    "cassandra", "cqlmigrate", Arrays.asList(basicSchema));

            LOGGER.info("Migration of cassandra tables was successfull.");
            status = new Health.Builder().up().withDetail("reason", "Cassandra db migration successfull").build();
        } catch (Exception e) {
            LOGGER.error("################################\n\nMigration of cassandra tables was NOT successfull.", e);
            status = new Health.Builder().down().withDetail("reason", "Cassandra db migration error").build();
            if (rethrowExceptions) {
                throw new RuntimeException(e);
            }
        } finally {
            isRunning.set(false);
        }
    }

    protected Path getSchema(String folder) throws URISyntaxException, IOException {
        Resource resource = new ClassPathResource(folder);
        URI uri = resource.getURI();
        if (uri.getScheme().equals("jar")) {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
            return Paths.get(uri);
        } else {
            return Paths.get(this.getClass().getResource(folder).toURI());
        }
    }

    private CqlSession buildEmbeddedCluster() {
        return CqlSession.builder()
                .withLocalDatacenter("datacenter1")
                .addContactPoint(new InetSocketAddress("localhost", 9142))
                .build();
    }

    private void createCqlMigrateKeyspace(CqlSession session) {
        LOGGER.info("################ Creating cqlmigrate lock keyspace for embedded cassandra.");
        session.execute("DROP KEYSPACE IF EXISTS cqlmigrate");
        session.execute("CREATE KEYSPACE IF NOT EXISTS cqlmigrate WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };");
        session.execute("CREATE TABLE IF NOT EXISTS cqlmigrate.dummyTable (first_key text, dummy_data text, PRIMARY KEY (first_key));");
        ResultSet execute = session.execute("CREATE TABLE IF NOT EXISTS cqlmigrate.locks (name text PRIMARY KEY, client text)");
        List<Row> all = execute.all();
        LOGGER.info("Row: {}", !all.isEmpty() ? all : "null");
    }

    @Override
    public Health health() {
        return status;
    }

    @Override
    public boolean isAutoStartup() {
        return serviceProperties.getCassandra().isCqlmigrateAutostart();
    }

    @Override
    public void stop(Runnable callback) {
        callback.run();
    }

    @Override
    public void start() {
        migrateCassandra();
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE-2;
    }
}
