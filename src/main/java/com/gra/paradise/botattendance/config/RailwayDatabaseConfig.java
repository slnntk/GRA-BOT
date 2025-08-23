package com.gra.paradise.botattendance.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuração de banco de dados otimizada para Railway
 * - Usa H2 por padrão (economia de custos)
 * - PostgreSQL apenas quando DATABASE_URL está disponível
 */
@Configuration
public class RailwayDatabaseConfig {

    /**
     * DataSource PostgreSQL para Railway quando DATABASE_URL está configurada
     * Usado apenas se você realmente precisar de recursos específicos do PostgreSQL
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource postgresqlDataSource() throws URISyntaxException {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && !dbUrl.isEmpty()) {
            URI dbUri = new URI(dbUrl);

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String url = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();

            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("org.postgresql.Driver");

            return dataSource;
        }
        return null;
    }

    /**
     * DataSource H2 padrão para economia de custos
     * Será usado quando DATABASE_URL não estiver configurada
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource h2DataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:file:./data/botattendance;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        
        return dataSource;
    }
}