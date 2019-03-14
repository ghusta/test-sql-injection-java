package fr.husta.test.sqlinjection;

import org.h2.api.ErrorCode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DestructiveSqlInjectionTest {

    private static final Logger log = LoggerFactory.getLogger(DestructiveSqlInjectionTest.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void contextLoads() {
    }

    /**
     * Inspired from XKCD : <a href="https://www.xkcd.com/327/">Exploits of a Mom</a>.
     * <br>
     * See also : <a href="https://stackoverflow.com/questions/332365/how-does-the-sql-injection-from-the-bobby-tables-xkcd-comic-work">ref in StackOverflow</a>.
     */
    @Test
    public void doSqlInjectionWithDropTable() {
        // assert table student exists
        int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "student");
        assertThat(count).isGreaterThan(0);

        String param1 = "99";
        String param2 = "Robert'); DROP TABLE student; --";
        String sqlInsert = "insert into student (id, name) values(" + param1 + ", '" + param2 + "')";

        jdbcTemplate.execute(sqlInsert);

        // assert table student destroyed !
        try {
            JdbcTestUtils.countRowsInTable(jdbcTemplate, "student");
            fail("Should have failed");
        } catch (BadSqlGrammarException e) {
            // OK
            log.debug("Exception : {}", e.getMessage());
            assertThat(e.getSQLException().getErrorCode()).isEqualTo(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1);
        }
    }

}
