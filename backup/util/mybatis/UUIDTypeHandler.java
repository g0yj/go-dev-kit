package com.app.api.common.util.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Created by heedong.kang@lemonhc.com 2/9/24
 */
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
    // UUID를 데이터베이스에 저장할 때 문자열로 변환하여 저장합니다.
    ps.setString(i, parameter.toString());
  }

  @Override
  public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
    // 데이터베이스에서 UUID 값을 가져올 때 문자열로부터 UUID 객체를 생성합니다.
    String uuid = rs.getString(columnName);
    return uuid != null ? UUID.fromString(uuid) : null;
  }

  @Override
  public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String uuid = rs.getString(columnIndex);
    return uuid != null ? UUID.fromString(uuid) : null;
  }

  @Override
  public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String uuid = cs.getString(columnIndex);
    return uuid != null ? UUID.fromString(uuid) : null;
  }
}
