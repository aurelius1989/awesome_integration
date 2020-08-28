package example.springdata.cassandra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"example.springdata.cassandra.resources"})
@SpringBootApplication
public class SpringDataCassandraApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringDataCassandraApplication.class, args);
	}
}