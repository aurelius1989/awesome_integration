package example.springdata.cassandra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Type-safe representation of 'service.*' application.cassandraProperties.
 */
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties("service")
public class ServiceProperties {

    @NotNull
    private String vertical;
    @NotNull
    private String name;
    @NotNull
    private String basePath;
    @Valid
    private final Cassandra cassandra = new Cassandra();

    /**
     * Custom cassandra properties not covered by <i>spring.data.cassandra.*</i>.
     */
    @Getter
    @Setter
    public static class Cassandra {

        private boolean datacenterAware;
        private String datacenter;
        private Integer replicationFactor;
        private boolean cqlmigrateAutostart = true;

    }
}
