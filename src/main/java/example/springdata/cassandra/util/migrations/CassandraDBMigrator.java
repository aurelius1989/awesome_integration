package example.springdata.cassandra.util.migrations;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@EnableConfigurationProperties({CassandraProperties.class, ServiceProperties.class})
public class CassandraDBMigrator implements HealthIndicator, SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraDBMigrator.class);

    @Autowired
    protected CassandraProperties cassandraProperties;

    @Autowired
    protected ServiceProperties serviceProperties;

    private Health status = new Health.Builder().down().build();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Start cassandra database migration.
     */
    public void migrateCassandra() {
        migrateCassandra(false);
    }

    protected void migrateCassandra(boolean rethrowExceptions) {
        LOG.info("Starting migration of cassandra tables...");
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
            LOG.info("Using keyspace {}", keySpace);

            Path basicSchema = getSchema("/database/cql");
//            migrator.migrate(session, keySpace, Arrays.asList(basicSchema));
            String[] localhost = {"localhost"};

            migrator.migrate(localhost, 9042, "production_username",
                    "production_password", "production_keyspace_name", Arrays.asList(basicSchema));

            LOG.info("Migration of cassandra tables was successfull.");
            status = new Health.Builder().up().withDetail("reason", "Cassandra db migration successfull").build();
        } catch (Exception e) {
            LOG.error("################################\n\nMigration of cassandra tables was NOT successfull.", e);
            status = new Health.Builder().down().withDetail("reason", "Cassandra db migration error").build();
            if (rethrowExceptions) {
                throw new RuntimeException(e);
            }
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Fetch the path of an URI: Has to be handled in different way if folder is in a JAR.
     *
     * See:
     * https://github.com/spring-projects/spring-boot/issues/7161
     * https://github.com/sky-uk/cqlmigrate/issues/62
     * https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
     * https://stackoverflow.com/questions/25032716/getting-filesystemnotfoundexception-from-zipfilesystemprovider-when-creating-a-p
     */
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
