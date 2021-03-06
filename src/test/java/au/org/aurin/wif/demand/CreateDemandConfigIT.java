/**
 *

 * marcosnr
 * 30/03/2012
 */
package au.org.aurin.wif.demand;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import au.org.aurin.wif.exception.config.ParsingException;
import au.org.aurin.wif.exception.config.WifInvalidConfigException;
import au.org.aurin.wif.exception.validate.IncompleteDemandConfigException;
import au.org.aurin.wif.exception.validate.WifInvalidInputException;
import au.org.aurin.wif.impl.demand.DemandConfigurator;
import au.org.aurin.wif.model.WifProject;
import au.org.aurin.wif.model.demand.DemandConfig;
import au.org.aurin.wif.repo.WifProjectDao;
import au.org.aurin.wif.svc.ProjectService;
import au.org.aurin.wif.svc.WifKeys;
import au.org.aurin.wif.svc.demand.DemandScenarioService;
import au.org.aurin.wif.svc.demand.DemandOutcomeService;
import au.org.aurin.wif.svc.suitability.DemandConfigService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class CreateCurrentDemographicIT.
 */
@ContextConfiguration(locations = { "/test-integration-context.xml" })
public class CreateDemandConfigIT extends AbstractTestNGSpringContextTests {

  /** The Constant LOGGER. */
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateDemandConfigIT.class);

  /** The demand configurator. */
  @Autowired
  private DemandConfigurator demandConfigurator;

  /** The wif project dao. */
  @Autowired
  private WifProjectDao wifProjectDao;

  /** The wif project. */
  private WifProject project;

  /** The demand config service. */
  @Resource
  private DemandConfigService demandConfigService;

  /** The demand config. */
  private DemandConfig demandConfig;

  /** The project service. */
  @Resource
  private ProjectService projectService;
  /** The demand scenario service. */
  @Autowired
  private DemandScenarioService demandScenarioService;
  @Resource
  private DemandOutcomeService manualDemandScenarioService;
  /**
   * Load wif project.
   * 
   * @throws WifInvalidConfigException
   *           the wif invalid config exception
   * @throws WifInvalidInputException
   *           the wif invalid input exception
   * @throws ParsingException
   *           the parsing exception
   */
  @BeforeClass(enabled = true)
  public void setup() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();

    demandConfig = mapper.readValue(
        (new ClassPathResource("/JSONs/NewResidentialDemandConfig.json").getInputStream()), DemandConfig.class);
    Assert.assertNotNull(demandConfig.getBaseYear());
  }

  /**
   * Setup demand test. PRE: The user specifies the UAZ columns, the projections and the sector's information
   * 
   * @throws WifInvalidInputException
   *           the wif invalid input exception
   * @throws WifInvalidConfigException
   *           the wif invalid config exception
   * @throws IncompleteDemandConfigException
   */
  @Test(enabled = true, groups = { "demand" })
  public void createDemandConfigTest() throws Exception {
    LOGGER.debug("Creating demand config information");
    LOGGER.debug("using base year {}", demandConfig.getBaseYear());
    demandConfig = demandConfigService.createDemandConfig(demandConfig, WifKeys.TEST_PROJECT_ID);

    Assert.assertNotNull(demandConfig.getDemographicTrends());
  }
}
