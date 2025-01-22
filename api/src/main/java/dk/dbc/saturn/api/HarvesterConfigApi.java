/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.saturn.FileHarvest;
import dk.dbc.saturn.FtpHarvesterBean;
import dk.dbc.saturn.HTTPHarvesterBean;
import dk.dbc.saturn.HarvestException;
import dk.dbc.saturn.HarvesterConfigRepository;
import dk.dbc.saturn.ProgressTrackerBean;
import dk.dbc.saturn.SFtpHarvesterBean;
import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Stateless
@Path("configs")
public class HarvesterConfigApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigApi.class);

    private static final String ABORT_ENDPOINT = "abort/{id}";
    private static final String JOB_STATUS = "status/{id}";
    private static final String HTTP_LIST_ENDPOINT = "http/list";
    private static final String HTTP_ADD_ENDPOINT = "http/add";
    private static final String HTTP_GET_SINGLE_CONFIG_ENDPOINT = "http/get/{id}";
    private static final String HTTP_TEST_SINGLE_CONFIG_ENDPOINT = "http/test/{id}";
    private static final String FTP_LIST_ENDPOINT = "ftp/list";
    private static final String SFTP_LIST_ENDPOINT = "sftp/list";
    private static final String FTP_ADD_ENDPOINT = "ftp/add";
    private static final String SFTP_ADD_ENDPOINT = "sftp/add";
    private static final String FTP_GET_SINGLE_CONFIG_ENDPOINT = "ftp/get/{id}";
    private static final String SFTP_GET_SINGLE_CONFIG_ENDPOINT = "sftp/get/{id}";
    private static final String FTP_TEST_SINGLE_CONFIG_ENDPOINT = "ftp/test/{id}";
    private static final String SFTP_TEST_SINGLE_CONFIG_ENDPOINT = "sftp/test/{id}";
    private static final String HTTP_DELETE_ENDPOINT = "http/delete/{id}";
    private static final String FTP_DELETE_ENDPOINT = "ftp/delete/{id}";
    private static final String SFTP_DELETE_ENDPOINT = "sftp/delete/{id}";
    private static final JSONBContext jsonbContext = new JSONBContext();

    @EJB HarvesterConfigRepository harvesterConfigRepository;
    @EJB FtpHarvesterBean ftpHarvesterBean;
    @EJB SFtpHarvesterBean sFtpHarvesterBean;
    @EJB HTTPHarvesterBean httpHarvesterBean;
    @EJB ProgressTrackerBean progressTrackerBean;

    /**
     * list http harvester configs
     * @param start harvester config id to start from
     * @param limit limit of returned list
     * @return 200 OK response with json list of harvester configs
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(HTTP_LIST_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listHttpHarvesterConfigs(@QueryParam("start") int start, @QueryParam("limit") int limit) throws JSONBException {
        final List<HttpHarvesterConfig> configs = harvesterConfigRepository.list(HttpHarvesterConfig.class, start, limit);
        configs.forEach(this::setProgress);
        return Response.ok(jsonbContext.marshall(configs)).build();
    }

    /**
     * list sftp harvester configs
     * @param start harvester config id to start from
     * @param limit limit of returned list
     * @return 200 OK response with json list of harvester configs
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(SFTP_LIST_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSFtpHarvesterConfigs(@QueryParam("start") int start, @QueryParam("limit") int limit) throws JSONBException {
        final List<SFtpHarvesterConfig> configs = harvesterConfigRepository.list(SFtpHarvesterConfig.class, start, limit);
        configs.forEach(this::setProgress);
        return Response.ok(jsonbContext.marshall(configs)).build();
    }

    /**
     * list ftp harvester configs
     * @param start harvester config id to start from
     * @param limit limit of returned list
     * @return 200 OK response with json list of harvester configs
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(FTP_LIST_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFtpHarvesterConfigs(@QueryParam("start") int start, @QueryParam("limit") int limit) throws JSONBException {
        final List<FtpHarvesterConfig> configs = harvesterConfigRepository.list(FtpHarvesterConfig.class, start, limit);
        configs.forEach(this::setProgress);
        return Response.ok(jsonbContext.marshall(configs)).build();
    }

    /**
     * add http harvester config entity to database
     * @param harvesterConfigString http harvester config as json data
     * @return 200 OK on successful creation of the entity
     *         400 Bad Request on invalid json content
     */
    @POST
    @Path(HTTP_ADD_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addHttpHarvesterConfig(@Context UriInfo uriInfo,
            String harvesterConfigString) {
        return addHarvesterConfig(HttpHarvesterConfig.class, harvesterConfigString, uriInfo);
    }

    /**
     * add ftp harvester config entity to database
     * @param harvesterConfigString ftp harvester config as json data
     * @return 200 OK on successful creation of the entity
     *         400 Bad Request on invalid json content
     */
    @POST
    @Path(FTP_ADD_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFtpHarvesterConfig(@Context UriInfo uriInfo,
            String harvesterConfigString) {
        return addHarvesterConfig(FtpHarvesterConfig.class, harvesterConfigString, uriInfo);
    }

    /**
     * add sftp harvester config entity to database
     * @param harvesterConfigString sftp harvester config as json data
     * @return 200 OK on successful creation of the entity
     *         400 Bad Request on invalid json content
     */
    @POST
    @Path(SFTP_ADD_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSFtpHarvesterConfig(@Context UriInfo uriInfo,
                                          String harvesterConfigString) {
        return addHarvesterConfig(SFtpHarvesterConfig.class, harvesterConfigString, uriInfo);
    }

    @POST
    @Path(ABORT_ENDPOINT)
    public Response abort(@PathParam("id") int id) {
        ProgressTrackerBean.Progress progress = progressTrackerBean.get(id);
        if(progress == null) return Response.status(Response.Status.NOT_FOUND).build();
        progress.abort();
        return Response.ok(progress).build();
    }

    @GET
    @Path(JOB_STATUS)
    public Response getJobStatus(@PathParam("id") int id) {
        ProgressTrackerBean.Progress progress = progressTrackerBean.get(id);
        if(progress == null) return Response.ok().build();
        return Response.ok(progress).build();
    }

    /**
     * get a single http harvester config
     * @param id harvester config id
     * @return 200 OK with http harvester config json
     *         404 Not Found if no config with the given id is found
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(HTTP_GET_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHttpHarvesterConfig(@PathParam("id") int id)
            throws JSONBException {
        HttpHarvesterConfig config = harvesterConfigRepository.getHarvesterConfig(HttpHarvesterConfig.class, id);
        if(config != null) return Response.ok(jsonbContext.marshall(config)).build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Tests a single http harvester config
     * @param id harvester config id
     * @return 200 OK with list of files that would be harvested if run for real
     *         404 Not Found if no config with the given id is found
     */
    @GET
    @Path(HTTP_TEST_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testHttpHarvesterConfig(@PathParam("id") int id) throws JSONBException, HarvestException {
        HttpHarvesterConfig config = harvesterConfigRepository.getHarvesterConfig(HttpHarvesterConfig.class, id);
        // sort files using TreeSet
        final Set<FileHarvest> fileHarvests = new TreeSet<>(httpHarvesterBean.listFiles(config));
        fileHarvests.forEach(fileHarvest -> {
            try {
                fileHarvest.getContent().close();
            } catch (IOException | HarvestException e) {
                LOGGER.warn("Unable to close content stream for {}<{}>", config.getName(), fileHarvest.getFilename());
            }
        });
        return Response.ok(jsonbContext.marshall(fileHarvests)).build();
    }

    /**
     * get a single sftp harvester config
     * @param id harvester config id
     * @return 200 OK with sftp harvester config json
     *         404 Not Found if no config with the given id is found
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(SFTP_GET_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSFtpHarvesterConfig(@PathParam("id") int id) throws JSONBException {
        SFtpHarvesterConfig config = harvesterConfigRepository.getHarvesterConfig(SFtpHarvesterConfig.class, id);
        return Response.ok(jsonbContext.marshall(config)).build();
    }

    /**
     * get a single ftp harvester config
     * @param id harvester config id
     * @return 200 OK with ftp harvester config json
     *         404 Not Found if no config with the given id is found
     * @throws JSONBException on marshalling failure
     */
    @GET
    @Path(FTP_GET_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFtpHarvesterConfig(@PathParam("id") int id) throws JSONBException {
        FtpHarvesterConfig config = harvesterConfigRepository.getHarvesterConfig(FtpHarvesterConfig.class, id);
        return Response.ok(jsonbContext.marshall(config)).build();
    }

    /**
     * Tests a single ftp harvester config
     * @param id harvester config id
     * @return 200 OK with list of files found on the FTP server
     *         404 Not Found if no config with the given id is found
     */
    @GET
    @Path(FTP_TEST_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testFtpHarvesterConfig(@PathParam("id") int id) throws JSONBException {
        FtpHarvesterConfig config = harvesterConfigRepository.getHarvesterConfig(FtpHarvesterConfig.class, id);
        // sort files in reverse
        SortedSet<FileHarvest> sortedSet = new TreeSet<>(Comparator.comparing(FileHarvest::getFilename).reversed());
        sortedSet.addAll(ftpHarvesterBean.listAllFiles(config));
        return Response.ok(jsonbContext.marshall(sortedSet)).build();
    }

    /**
     * Tests a single ftp harvester config
     * @param id harvester config id
     * @return 200 OK with list of files found on the FTP server
     *         404 Not Found if no config with the given id is found
     */
    @GET
    @Path(SFTP_TEST_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testSFtpHarvesterConfig(@PathParam("id") int id) throws JSONBException {
        SFtpHarvesterConfig config = harvesterConfigRepository.getHarvesterConfig(SFtpHarvesterConfig.class, id);
        // sort files in reverse
        SortedSet<FileHarvest> sortedSet = new TreeSet<>(Comparator.comparing(FileHarvest::getFilename).reversed());
        sortedSet.addAll(sFtpHarvesterBean.listAllFiles(config));
        return Response.ok(jsonbContext.marshall(sortedSet)).build();
    }

    @DELETE
    @Path(HTTP_DELETE_ENDPOINT)
    public Response deleteHttpHarvesterConfig(@PathParam("id") int id) {
        harvesterConfigRepository.delete(HttpHarvesterConfig.class, id);
        return Response.noContent().build();
    }

    @DELETE
    @Path(FTP_DELETE_ENDPOINT)
    public Response deleteFtpHarvesterConfig(@PathParam("id") int id) {
        harvesterConfigRepository.delete(FtpHarvesterConfig.class, id);
        return Response.noContent().build();
    }

    @DELETE
    @Path(SFTP_DELETE_ENDPOINT)
    public Response deleteSFtpHarvesterConfig(@PathParam("id") int id) {
        harvesterConfigRepository.delete(SFtpHarvesterConfig.class, id);
        return Response.noContent().build();
    }

    private <T extends AbstractHarvesterConfigEntity> Response addHarvesterConfig(Class<T> type, String harvesterConfigString, UriInfo uriInfo) {
        try {
            T harvesterConfig = jsonbContext.unmarshall(harvesterConfigString, type);
            URI uri = harvesterConfigRepository.add(type, harvesterConfig, uriInfo.getAbsolutePathBuilder());
            return Response.created(uri).build();
        } catch (JSONBException e) {
            LOGGER.error("Error while unmarshalling: {}", harvesterConfigString, e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.toString())
                .build();
        }
    }

    private void setProgress(AbstractHarvesterConfigEntity harvesterConfig) {
        if(progressTrackerBean != null) {
            ProgressTrackerBean.Progress progress = progressTrackerBean.get(harvesterConfig.getId());
            harvesterConfig.withProgress(progress);
        }
    }
}
