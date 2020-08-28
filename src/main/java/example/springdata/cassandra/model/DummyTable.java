package example.springdata.cassandra.model;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.annotation.ParametersAreNonnullByDefault;

import static example.springdata.cassandra.model.DummyTable.DBName.*;


/**
 * Country-specific configurations.
 */
@Table(TABLE_NAME)
@ParametersAreNonnullByDefault
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class DummyTable {

    public interface DBName {
        String TABLE_NAME = "dummytable";
        String FIRST_KEY = "first_key";
        String DUMMY_DATA = "dummy_data";
    }

    @PrimaryKeyColumn(name = FIRST_KEY, type = PrimaryKeyType.PARTITIONED)
    private String firstKey;

    @Column(DUMMY_DATA)
    private String dummyData;
}
