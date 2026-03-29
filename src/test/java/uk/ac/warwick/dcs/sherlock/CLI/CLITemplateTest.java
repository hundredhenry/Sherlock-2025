// package uk.ac.warwick.dcs.sherlock.engine;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Disabled;
// import org.junit.jupiter.api.Test;
// import uk.ac.warwick.dcs.sherlock.api.util.Side;
// import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TDetector;

// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.beans.factory.annotation.Autowired;

// import static org.junit.jupiter.api.Assertions.*;



// @SpringBootTest
// @ActiveProfiles("test")
// @Transactional
// class CLITemplateTest extends AbstractCLITest {


//     SherlockEngine se = new SherlockEngine(Side.CLIENT);
//     @BeforeEach
//     void setUp() {
//         initialise();
        
//     }

//     @AfterEach
//     void tearDown() {

//     }

//     @Disabled("Did not fully understand how loader was implemented")
//     @Test
//     void test() {
//         se.initialise();
//         //AnnotationLoader al = new AnnotationLoader();
//         //al.registerModules();

//     }

//     /*
//     Tests:
//     Testing CLI works
//     Testing CLI shows templates
//      */
// }