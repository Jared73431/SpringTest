package db.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import lombok.Getter;
import lombok.Setter;

public class V1_0_5__ComplexMigration extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(
                new SingleConnectionDataSource(context.getConnection(), true));

        // 复杂的数据迁移示例：根据现有数据进行条件更新

        // 1. 查询所有记录
        List<JavaStackRecord> records = jdbcTemplate.query(
                "SELECT id, title, content, note FROM t_javastack",
                new JavaStackRowMapper()
        );

        System.out.println("找到 " + records.size() + " 条记录需要处理");

        // 2. 根据业务逻辑处理每条记录
        for (JavaStackRecord record : records) {
            String newNote = generateNote(record);

            // 3. 更新特定记录
            jdbcTemplate.update(
                    "UPDATE t_javastack SET note = ? WHERE id = ?",
                    newNote, record.getId()
            );
        }

        System.out.println("复杂数据迁移完成");

        // 4. 验证迁移结果
        validateMigration(jdbcTemplate);
    }

    private String generateNote(JavaStackRecord record) {
        // 根据业务逻辑生成note
        if (record.getTitle().contains("标题1")) {
            return "特殊标题1";
        } else if (record.getContent().contains("内容")) {
            return "包含内容关键字";
        } else {
            return "默认备注";
        }
    }

    private void validateMigration(JdbcTemplate jdbcTemplate) {
        Integer specialCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_javastack WHERE note LIKE '特殊%'",
                Integer.class);
        Integer contentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_javastack WHERE note = '包含内容关键字'",
                Integer.class);

        System.out.println("特殊标题记录数: " + specialCount);
        System.out.println("包含内容关键字记录数: " + contentCount);
    }

    // 内部类用于映射查询结果
    @Getter
    @Setter
    private static class JavaStackRecord {
        private Long id;
        private String title;
        private String content;
        private String note;

        // 构造函数、getter和setter
        public JavaStackRecord() {}
    }

    // RowMapper实现
    private static class JavaStackRowMapper implements RowMapper<JavaStackRecord> {

        @Override
        public JavaStackRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            JavaStackRecord record = new JavaStackRecord();
            record.setId(rs.getLong("id"));
            record.setTitle(rs.getString("title"));
            record.setContent(rs.getString("content"));
            record.setNote(rs.getString("note"));
            return record;
        }
    }
}
