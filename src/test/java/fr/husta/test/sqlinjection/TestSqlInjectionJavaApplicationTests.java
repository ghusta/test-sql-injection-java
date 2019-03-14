package fr.husta.test.sqlinjection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestSqlInjectionJavaApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(TestSqlInjectionJavaApplicationTests.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void openJavaSqlConnection() {
        try (Connection cnx = dataSource.getConnection()) {
            assertThat(cnx).isNotNull();
            assertThat(cnx.isClosed()).isFalse();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Test
    public void selectQuerySpring() {
        String sql = "select id, name, password from user";
        List<String> results =
                jdbcTemplate.query(sql,
                        createSingleStringRowMapper());
        assertThat(results).isNotEmpty();

        results.forEach(log::debug);
    }

    @Test
    public void selectQuerySpringWithRestriction() {
        String param = "Donald";
        String sql = "select id, name, password from user where name = '" + param + "'";
        List<String> results =
                jdbcTemplate.query(sql,
                        createSingleStringRowMapper());
        assertThat(results).hasSize(1);

        results.forEach(s -> log.debug(s));
    }

    @Test(expected = BadSqlGrammarException.class)
    public void selectQuerySpringWithRestrictionFailingBecauseNotSanitized() {
        String param = "Dona'ld";
        String sql = "select id, name, password from user where name = '" + param + "'";
        List<String> results =
                jdbcTemplate.query(sql,
                        createSingleStringRowMapper());

        fail("Should have failed");
    }

    @Test
    public void selectQuerySpringWithRestrictionHackedBySqlInjection() {
        String param = "xxx' or '1'='1";
        String sql = "select id, name, password from user where name = '" + param + "'";
        List<String> results =
                jdbcTemplate.query(sql,
                        createSingleStringRowMapper());
        assertThat(results.size()).isGreaterThanOrEqualTo(4);

        results.forEach(s -> log.debug(s));
    }

    @Test
    public void selectQuerySpringWithRestrictionUsingPreparedStatement() {
        String param = "Donald";
        String sql = "select id, name, password from user where name = ?";
        List<String> results =
                jdbcTemplate.query(sql, new Object[]{param},
                        createSingleStringRowMapper());
        assertThat(results).hasSize(1);

        results.forEach(s -> log.debug(s));
    }

    @Test
    public void selectQuerySpringWithRestrictionUsingPreparedStatementSanitized() {
        String param = "Dona'ld"; // transformed to "Dona''ld"
        String sql = "select id, name, password from user where name = ?";
        List<String> results =
                jdbcTemplate.query(sql, new Object[]{param},
                        createSingleStringRowMapper());
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).startsWith("5 - Dona'ld -");

        results.forEach(s -> log.debug(s));
    }

    @Test
    public void selectQuerySpringWithRestrictionUsingPreparedStatementTryingSqlInjection() {
        String param = "xxx' or '1'='1";
        String sql = "select id, name, password from user where name = ?";
        List<String> results =
                jdbcTemplate.query(sql, new Object[]{param},
                        createSingleStringRowMapper());
        assertThat(results).isEmpty();

        results.forEach(s -> log.debug(s));
    }

    private static RowMapper<String> createSingleStringRowMapper() {
        return (rs, rowNum) -> String.format("%d - %s - %s",
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3));
    }

}
