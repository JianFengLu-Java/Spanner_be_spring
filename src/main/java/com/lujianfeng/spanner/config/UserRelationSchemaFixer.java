package com.lujianfeng.spanner.config;

import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class UserRelationSchemaFixer implements ApplicationRunner {

    private static final String TABLE_NAME = "user_relation";
    private static final String CONSTRAINT_NAME = "user_relation_relation_type_check";

    private final JdbcTemplate jdbcTemplate;

    public UserRelationSchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists(TABLE_NAME)) {
            return;
        }
        syncRelationTypeCheckConstraint();
    }

    private boolean tableExists(String tableName) {
        String sql = "select to_regclass(?) is not null";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, "public." + tableName);
        return Boolean.TRUE.equals(exists);
    }

    private void syncRelationTypeCheckConstraint() {
        String allowedValues = Arrays.stream(UserRelationEnum.values())
                .map(UserRelationEnum::name)
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(","));

        jdbcTemplate.execute("alter table " + TABLE_NAME + " drop constraint if exists " + CONSTRAINT_NAME);
        jdbcTemplate.execute(
                "alter table " + TABLE_NAME
                        + " add constraint " + CONSTRAINT_NAME
                        + " check (relation_type in (" + allowedValues + "))"
        );
    }
}
