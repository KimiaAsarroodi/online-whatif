package au.org.aurin.wif.impl.suitability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import au.org.aurin.wif.exception.config.InvalidEntityIdException;
import au.org.aurin.wif.exception.config.WifInvalidConfigException;
import au.org.aurin.wif.exception.validate.InvalidLabelException;
import au.org.aurin.wif.exception.validate.WifInvalidInputException;
import au.org.aurin.wif.io.CouchMapper;
import au.org.aurin.wif.io.parsers.SuitabilityCouchParser;
import au.org.aurin.wif.model.WifProject;
import au.org.aurin.wif.model.suitability.Factor;
import au.org.aurin.wif.model.suitability.FactorType;
import au.org.aurin.wif.repo.suitability.FactorTypeDao;
import au.org.aurin.wif.repo.suitability.impl.CouchFactorDao;
import au.org.aurin.wif.svc.ProjectService;
import au.org.aurin.wif.svc.WifKeys;
import au.org.aurin.wif.svc.suitability.FactorService;

/**
 * The Class FactorServiceImpl.
 */
@Service
@Qualifier("factorService")
public class FactorServiceImpl implements FactorService {

  /** The Constant serialVersionUID. */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 35200015343L;

  /** The factor type dao. */
  @Autowired
  private FactorTypeDao factorTypeDao;

  /** The Constant LOGGER. */
  private static final Logger LOGGER = LoggerFactory
      .getLogger(FactorServiceImpl.class);

  /** The factor dao. */
  @Autowired
  private CouchFactorDao factorDao;


  /** The mapper. */
  @Autowired
  private CouchMapper mapper;

  /** The parser. */
  @Autowired
  private SuitabilityCouchParser suitabilityParser;

  /** The project service. */
  @Resource
  private ProjectService projectService;

  /**
   * Inits the.
   */
  @PostConstruct
  public void init() {
    LOGGER.trace("Initializing version: " + WifKeys.WIF_KEY_VERSION);
  }

  /**
   * Cleanup.
   */

  @PreDestroy
  public void cleanup() {
    LOGGER.trace(" Service succesfully cleared! ");
  }

  /*
   * (non-Javadoc)
   * @see
   * au.org.aurin.wif.svc.suitability.FactorService#createFactor(au.org.aurin
   * .wif.model.suitability.Factor, java.lang.String)
   */
  @Override
  public Factor createFactor(final Factor factor, final String projectId)
      throws WifInvalidInputException, WifInvalidConfigException,
      InvalidLabelException {
    validate(factor, projectId);
    LOGGER.debug("persisting the wif factor= {}", factor.getLabel());
    final Set<FactorType> factorTypes = factor.getFactorTypes();
    if (factorTypes != null) {
      LOGGER.debug("it has {} factor types associated with it",
          factorTypes.size());
      final Set<FactorType> savedFactorTypes = new HashSet<FactorType>();
      for (final FactorType factorType : factorTypes) {
        validate(factorType, factor);
        savedFactorTypes.add(factorTypeDao.persistFactorType(factorType));
      }
      factor.setFactorTypes(savedFactorTypes);
    }

    final WifProject project = projectService.getProject(projectId);
    factor.setProjectId(projectId);
    final Factor savedFactor = factorDao.persistFactor(factor);

    LOGGER.debug("returning the factor with id={}", savedFactor.getId());
    project.getFactors().add(savedFactor);
    projectService.updateProject(project);
    return savedFactor;

  }

  /**
   * Validate ffactor type .
   *
   * @param factorType
   *          the factor type
   * @param factor
   *          the factor
   * @throws InvalidLabelException
   *           the invalid label exception
   * @throws WifInvalidInputException
   *           the wif invalid input exception
   */
  private void validate(final FactorType factorType, final Factor factor)
      throws InvalidLabelException, WifInvalidInputException {
    String message = "createfactor failed: factorType is null";
    if (factorType == null) {
      LOGGER.error(message);
      throw new WifInvalidInputException(message);
    } else if (factorType.getLabel() == null) {
      message = "createfactor failed: factorType label is null";
      LOGGER.error(message);
      throw new InvalidLabelException(message);
    } else {
      message = "createfactor failed: factorType label has already been used in this factor configuration";
      final Set<FactorType> factorTypes = factor.getFactorTypes();
      for (final FactorType factorType2 : factorTypes) {
        if (factorType.getLabel().equals(factorType2.getLabel())
            && factorType2.getId() != null) {
          LOGGER.error(message);
          throw new InvalidLabelException(message);
        }
      }
    }

  }

  /**
   * Validate ffactor business logic.
   *
   * @param factor
   *          the factor
   * @param projectId
   *          the project id
   * @throws InvalidLabelException
   *           the invalid label exception
   * @throws WifInvalidInputException
   *           the wif invalid input exception
   */
  private void validate(final Factor factor, final String projectId)
      throws InvalidLabelException, WifInvalidInputException {
    String message = "createfactor failed: factor is null";
    if (factor == null) {
      LOGGER.error(message);
      throw new WifInvalidInputException(message);
    } else if (factor.getLabel() == null) {
      message = "createfactor failed: factor label is null";
      LOGGER.error(message);
      throw new InvalidLabelException(message);
    } else {

      message = "createfactor failed: factor label has already been used in this project configuration";
      final List<Factor> factors = factorDao.getFactors(projectId);
      for (final Factor factor2 : factors) {
        if (factor.getLabel().equals(factor2.getLabel())) {
          LOGGER.error(message);
          throw new InvalidLabelException(message);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * au.org.aurin.wif.svc.suitability.FactorService#getFactor(java.lang.String)
   */
  @Override
  public Factor getFactor(final String id) throws WifInvalidInputException,
  WifInvalidConfigException {

    LOGGER.debug("getting the factor with ID={}", id);
    try {
      final Factor factor = factorDao.findFactorById(id);
      if (factor == null) {
        LOGGER.error("illegal argument, the factor with the ID " + id
            + " supplied was not found ");
        throw new InvalidEntityIdException(
            "illegal argument, the factor with the ID " + id
            + " supplied was not found ");
      }
      return factor;

    } catch (final IllegalArgumentException e) {

      LOGGER.error("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factor ");
      throw new WifInvalidInputException("illegal argument, the ID  " + id
          + "supplied doesn't identify a valid factor ");
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * au.org.aurin.wif.svc.suitability.FactorService#getFactor(java.lang.String,
   * java.lang.String)
   */
  @Override
  public Factor getFactor(final String id, final String projectId)
      throws WifInvalidInputException, WifInvalidConfigException {
    final Factor factor = getFactor(id);
    if (factor.getProjectId().equals(projectId)) {
      final WifProject project = projectService.getProject(projectId);
      factor.setWifProject(project);

      return factor;
    } else {
      LOGGER.error("illegal argument, the factor has project id "
          + factor.getWifProject().getId() + " the supplied is project: "
          + projectId);
      throw new WifInvalidInputException(
          "illegal argument, the factor has project id "
              + factor.getWifProject().getId() + " the supplied is project: "
              + projectId);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * au.org.aurin.wif.svc.suitability.FactorService#updateFactor(au.org.aurin
   * .wif.model.suitability.Factor, java.lang.String)
   */
  @Override
  public void updateFactor(final Factor factor, final String projectId)
      throws WifInvalidInputException, WifInvalidConfigException {
    LOGGER.info("updating factor: {}, with id: {}", factor.getLabel(),
        factor.getId());
    try {
      final WifProject project = projectService.getProject(projectId);
      factor.setWifProject(project);
      final Set<FactorType> factorTypes = factor.getFactorTypes();
      if (factorTypes != null) {
        LOGGER.debug("it has {} factor types associated with it",
            factorTypes.size());
        for (final FactorType factorType : factorTypes) {
          if (factorType.getId() != null) {
            factorType.setRevision(factorTypeDao.findFactorTypeById(
                factorType.getId()).getRevision());
            factorTypeDao.updateFactorType(factorType);
          } else {
            factor.getFactorTypes().add(
                factorTypeDao.persistFactorType(factorType));
          }
        }
      }
      factor
      .setRevision(factorDao.findFactorById(factor.getId()).getRevision());
      factorDao.updateFactor(factor);
      final Object oldFactor = project.getFactorById(factor.getId());
      project.getFactors().remove(oldFactor);
      project.getFactors().add(factor);
      projectService.updateProject(project);

    } catch (final IllegalArgumentException e) {

      LOGGER
      .error("illegal argument, the ID supplied doesn't identify a valid factor ");
      throw new WifInvalidInputException(
          "illegal argument, the ID supplied doesn't identify a valid factor ");
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * au.org.aurin.wif.svc.suitability.FactorService#deleteFactor(java.lang.String
   * , java.lang.String)
   */
  @Override
  public void deleteFactor(final String id, final String projectId)
      throws WifInvalidInputException, WifInvalidConfigException {
    LOGGER.info("deleting the factor with ID={}", id);
    try {
      final Factor factor = factorDao.findFactorById(id);
      if (factor == null) {
        LOGGER.error("illegal argument, the factor with the ID " + id
            + " supplied was not found ");
        throw new InvalidEntityIdException(
            "illegal argument, the factor with the ID " + id
            + " supplied was not found ");
      }
      if (factor.getProjectId().equals(projectId)) {
        final Set<FactorType> factorTypes = factor.getFactorTypes();
        if (factorTypes != null) {
          LOGGER.debug("deleting {} factor types associated with it",
              factorTypes.size());
          for (final FactorType factorType : factorTypes) {
            factorTypeDao.deleteFactorType(factorType);
          }
        }

        factorDao.deleteFactor(factor);
        final WifProject project = projectService.getProject(projectId);

        final Object oldFactor = project.getFactorById(factor.getId());
        project.getFactors().remove(oldFactor);
        projectService.updateProject(project);
      } else {
        LOGGER
        .error("illegal argument, the factor supplied doesn't belong to project: "
            + projectId);
        throw new WifInvalidInputException(
            "illegal argument, the factor supplied doesn't belong to project: "
                + projectId);
      }
    } catch (final IllegalArgumentException e) {

      LOGGER.error("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factor ");
      throw new InvalidEntityIdException("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factor ");
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * au.org.aurin.wif.svc.suitability.FactorService#getFactors(java.lang.String)
   */
  @Override
  public List<Factor> getFactors(final String projectID)
      throws WifInvalidInputException {
    LOGGER.info("getting allfactors for projectID: {} ", projectID);

    return factorDao.getFactors(projectID);
  }

  @Override
  public List<FactorType> getFactorTypes(final String factorID)
      throws WifInvalidInputException {
    LOGGER.info("getting allfactorTypes for factorID: {} ",factorID);
    return factorTypeDao.getFactorTypes(factorID);
  }

  @Override
  public void deleteFactorType(final String projectId, final String factorId, final String id)
      throws WifInvalidInputException, WifInvalidConfigException {
    LOGGER.info("deleting the factorType with ID={}", id);
    try {
      final FactorType factorType = factorTypeDao.findFactorTypeById(id);
      if (factorType == null) {
        final WifProject project = projectService.getProject(projectId);

        final Object oldFactorType = project.getFactorById(factorId).getFactorTypeById(id);

        project.getFactorById(factorId).getFactorTypes().remove(oldFactorType);
        LOGGER.error("illegal argument, the factorType with the ID " + id
            + " supplied was not found ");
        throw new InvalidEntityIdException(
            "illegal argument, the factorType with the ID " + id
            + " supplied was not found ");
      }
      if (factorType.getFactorId().equals(factorId)) {

        factorTypeDao.deleteFactorType(factorType);

        final WifProject project = projectService.getProject(projectId);

        final Object oldFactorType = project.getFactorById(factorId).getFactorTypeById(id);

        project.getFactorById(factorId).getFactorTypes().remove(oldFactorType);
        projectService.updateProject(project);


      } else {
        LOGGER
        .error("illegal argument, the factortype supplied doesn't belong to factor: "
            + factorId);
        throw new WifInvalidInputException(
            "illegal argument, the factor supplied doesn't belong to factor: "
                + factorId);
      }
    } catch (final IllegalArgumentException e) {

      LOGGER.error("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factorType ");
      throw new InvalidEntityIdException("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factorType ");
    }
  }

  @Override
  public FactorType getFactorType(final String projectId, final String factorId, final String id)
      throws WifInvalidInputException, WifInvalidConfigException {

    LOGGER.debug("getting the factorType with ID={}", id);
    try {
      final FactorType factorType = factorTypeDao.findFactorTypeById(id);
      if (factorType == null) {
        LOGGER.error("illegal argument, the factorType with the ID " + id
            + " supplied was not found ");
        throw new InvalidEntityIdException(
            "illegal argument, the factorType with the ID " + id
            + " supplied was not found ");
      }
      return factorType;

    } catch (final IllegalArgumentException e) {

      LOGGER.error("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factorType ");
      throw new WifInvalidInputException("illegal argument, the ID  " + id
          + "supplied doesn't identify a valid factorType ");
    }
  }


  @Override
  public List<FactorType> getFactorTypeByLable(final String projectId, final String factorId, final String lable)
      throws WifInvalidInputException, WifInvalidConfigException {


    final List<FactorType> outList=new ArrayList<FactorType>();
    LOGGER.info("getting allfactorTypes for factorID: {} and Label: {} ",factorId, lable);
    for (final FactorType ft :factorTypeDao.getFactorTypes(factorId))
    {
      if (ft.getLabel().equals(lable))
      {
        outList.add(ft);
      }
    }

    return outList;

  }

  @Override
  public void deleteFactorTypesExtra(final String projectId)
      throws WifInvalidInputException, WifInvalidConfigException {
    LOGGER.info("deleting the FactorTypesExtra with ID={}", projectId);
    try {

      final List<Factor> listFactor=getFactors(projectId);

      for (final Factor prjFactor :listFactor )
      {
        final String factorId = prjFactor.getId();
        final List<FactorType> listFactorTypesExtra= getFactorTypes(factorId);
        final Set<FactorType> listFactorTypes  = getFactor(factorId, projectId).getFactorTypes();


        for (final FactorType fct: listFactorTypesExtra)
        {
          final String st = fct.getId();
          Boolean lsw = false;
          for (final FactorType infct: listFactorTypes)
          {
            if (infct.getId().equals(st))
            {
              lsw = true;
            }
          }
          if (lsw == false) //extra
          {
            deleteFactorTypeNew(projectId, factorId, st);
            LOGGER.info("*******>> deleteFactorTypeExtra for factorType with ID ={} for factor = {}", fct.getLabel(), prjFactor.getLabel());
            //LOGGER.info("*******>> deleteFactorTypeExtra delete for factorType with ID ={} for factor = {}", fct.getLabel(), prjFactor.getLabel());
          }
        }
      }


    } catch (final Exception e) {
      LOGGER.info("error in delete FactorTypesExtra for  the project ID " + projectId);

    }
  }

  public void deleteFactorTypeNew(final String projectId, final String factorId, final String id)
      throws WifInvalidInputException, WifInvalidConfigException {
    LOGGER.info("deleting the factorType with ID={}", id);
    try {
      final FactorType factorType = factorTypeDao.findFactorTypeById(id);
      if (factorType == null) {
        final WifProject project = projectService.getProject(projectId);

        final Object oldFactorType = project.getFactorById(factorId).getFactorTypeById(id);

        project.getFactorById(factorId).getFactorTypes().remove(oldFactorType);
        LOGGER.error("illegal argument, the factorType with the ID " + id
            + " supplied was not found ");
        throw new InvalidEntityIdException(
            "illegal argument, the factorType with the ID " + id
            + " supplied was not found ");
      }
      if (factorType.getFactorId().equals(factorId)) {

        factorTypeDao.deleteFactorType(factorType);

        final WifProject project = projectService.getProject(projectId);

        final Object oldFactorType = project.getFactorById(factorId).getFactorTypeById(id);

        project.getFactorById(factorId).getFactorTypes().remove(oldFactorType);
        projectService.updateProject(project);


      } else {
        LOGGER
        .error("illegal argument, the factortype supplied doesn't belong to factor: "
            + factorId);
        throw new WifInvalidInputException(
            "illegal argument, the factor supplied doesn't belong to factor: "
                + factorId);
      }
    } catch (final IllegalArgumentException e) {

      LOGGER.error("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factorType ");
      throw new InvalidEntityIdException("illegal argument, the ID " + id
          + " supplied doesn't identify a valid factorType ");
    }
  }





}
