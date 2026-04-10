package uk.ac.warwick.dcs.sherlock.launch;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.*;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.JpaVendorAdapter;
import java.util.Properties;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Spring configuration for Sherlock CLI. Activated when Spring is enabled.
 * Sets up the components, repositories, and entities
 *  needed to run the Sherlock client in CLI mode.
 */
@Configuration
@ComponentScan("uk.ac.warwick.dcs.sherlock.module")
@EnableJpaRepositories("uk.ac.warwick.dcs.sherlock.module.core")
@EntityScan("uk.ac.warwick.dcs.sherlock.module.core")
public class SherlockCliSpring {

    /**
     * Creates the database under the CLI profile.
     * 
     * @return the data source
     */
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

    /**
     * Configures the transaction manager for the JPA database operations.
     * 
     * @param emf the entity manager factory needed to create entity managers
     * @return the JPA transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * Configures the JPA for the Sherlock CLI.
     * Sets up the entity manager factory, scans packages, and configures hibernate
     *  as the JPA provider.
     * 
     * @param dataSource the database acting as the data source
     * @return the local container entity manager factory bean
     */
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

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

}