package com.app.api.common.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(value = "com.lms.api.common.mybatis")
@RequiredArgsConstructor
public class MyBatisConfig {

  private final DataSource dataSource;

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
    bean.setDataSource(dataSource);
    bean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource(
        "classpath:mybatis-config.xml"));

    return bean.getObject();
  }

//  @Bean
//  public DataSourceTransactionManager transactionManager() {
//    return new DataSourceTransactionManager(dataSource);
//  }
}
