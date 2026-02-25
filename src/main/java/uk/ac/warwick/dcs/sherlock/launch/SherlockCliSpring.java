package uk.ac.warwick.dcs.sherlock.launch;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import uk.ac.warwick.dcs.sherlock.api.util.Side;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.*;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.JpaVendorAdapter;
import java.util.Properties;


@Configuration
@ComponentScan("uk.ac.warwick.dcs.sherlock.module")
@EnableJpaRepositories("uk.ac.warwick.dcs.sherlock.module.core")
@EntityScan("uk.ac.warwick.dcs.sherlock.module.core")
public class SherlockCliSpring {

	// @Bean
    // public SherlockEngine sherlockEngine() {
    //     SherlockEngine engine = new SherlockEngine(Side.CLIENT);
    //     if (!engine.isValidInstance()) {
    //         System.err.println("Sherlock is already running, closing....");
    //         System.exit(1);
    //     }
    //     engine.initialise();
    //     return engine;
    // }

	@Bean
	@Primary
	@Profile("cli")
	public DataSource dataSource() {
		return DataSourceBuilder
				.create()
				.username("sa")
				.password("")
				.url("jdbc:h2:file:" + SherlockEngine.configuration.getDataPath() + "/Sherlock-Web")
				.driverClassName("org.h2.Driver")
				.build();
	}

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("uk.ac.warwick.dcs.sherlock.module.core.data.models.db"); // your entities
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);
        
        Properties jpaProps = new Properties();
        jpaProps.put("hibernate.hbm2ddl.auto", "update"); // keep schema in sync
        jpaProps.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        emf.setJpaProperties(jpaProps);
        
        return emf;
    }

}