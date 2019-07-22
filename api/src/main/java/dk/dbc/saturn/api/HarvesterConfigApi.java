/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.saturn.FileHarvest;
import dk.dbc.saturn.FtpHarvesterBean;
import dk.dbc.saturn.HTTPHarvesterBean;
import dk.dbc.saturn.HarvestException;
import dk.dbc.saturn.HarvesterConfigRepository;
import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

@Stateless
@Path("configs")
public class HarvesterConfigApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigApi.class);

    private static final String HTTP_LIST_ENDPOINT = "http/list";
    private static final String HTTP_ADD_ENDPOINT = "http/add";
    private static final String HTTP_GET_SINGLE_CONFIG_ENDPOINT = "http/get/{id}";
    private static final String HTTP_TEST_SINGLE_CONFIG_ENDPOINT = "http/test/{id}";
    private static final String FTP_LIST_ENDPOINT = "ftp/list";
    private static final String FTP_ADD_ENDPOINT = "ftp/add";
    private static final String FTP_GET_SINGLE_CONFIG_ENDPOINT = "ftp/get/{id}";
    private static final String FTP_TEST_SINGLE_CONFIG_ENDPOINT = "ftp/test/{id}";
    private static final String HTTP_DELETE_ENDPOINT = "http/delete/{id}";
    private static final String FTP_DELETE_ENDPOINT = "ftp/delete/{id}";
    private static final JSONBContext jsonbContext = new JSONBContext();

    @EJB HarvesterConfigRepository harvesterConfigRepository;
    @EJB FtpHarvesterBean ftpHarvesterBean;
    @EJB HTTPHarvesterBean httpHarvesterBean;

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
    public Response listHttpHarvesterConfigs(@QueryParam("start") int start,
            @QueryParam("limit") int limit) throws JSONBException {
        final List<HttpHarvesterConfig> configs = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, start, limit);
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
    public Response listFtpHarvesterConfigs(@QueryParam("start") int start,
            @QueryParam("limit") int limit) throws JSONBException {
        final List<FtpHarvesterConfig> configs = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, start, limit);
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
        return addHarvesterConfig(HttpHarvesterConfig.class,
            harvesterConfigString, uriInfo);
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
        return addHarvesterConfig(FtpHarvesterConfig.class,
            harvesterConfigString, uriInfo);
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
        Optional<HttpHarvesterConfig> config = harvesterConfigRepository
            .getHarvesterConfig(HttpHarvesterConfig.class, id);
        if(config.isPresent()) {
            return Response.ok(jsonbContext.marshall(config.get())).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
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
    public Response testHttpHarvesterConfig(@PathParam("id") int id)
            throws JSONBException, HarvestException,
                   ExecutionException, InterruptedException {
        final Optional<HttpHarvesterConfig> config = harvesterConfigRepository
                .getHarvesterConfig(HttpHarvesterConfig.class, id);
        if (config.isPresent()) {
            // sort files using TreeSet
            final Set<FileHarvest> fileHarvests = new TreeSet<>(
                    httpHarvesterBean.harvest(config.get()).get());
            fileHarvests.forEach(fileHarvest -> {
                try {
                    fileHarvest.getContent().close();
                } catch (IOException | HarvestException e) {
                    LOGGER.warn("Unable to close content stream for {}<{}>",
                            config.get().getName(), fileHarvest.getFilename());
                }
            });
            return Response.ok(jsonbContext.marshall(fileHarvests)).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
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
    public Response getFtpHarvesterConfig(@PathParam("id") int id)
            throws JSONBException {
        Optional<FtpHarvesterConfig> config = harvesterConfigRepository
            .getHarvesterConfig(FtpHarvesterConfig.class, id);
        if(config.isPresent()) {
            return Response.ok(jsonbContext.marshall(config.get())).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Tests a single ftp harvester config
     * @param id harvester config id
     * @return 200 OK with list of files that would be harvested if run for real
     *         404 Not Found if no config with the given id is found
     */
    @GET
    @Path(FTP_TEST_SINGLE_CONFIG_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testFtpHarvesterConfig(@PathParam("id") int id)
            throws JSONBException, ExecutionException, InterruptedException {
        final Optional<FtpHarvesterConfig> config = harvesterConfigRepository
                .getHarvesterConfig(FtpHarvesterConfig.class, id);
        if (config.isPresent()) {
            // sort files using TreeSet
            final Set<FileHarvest> fileHarvests = new TreeSet<>(
                    ftpHarvesterBean.harvest(config.get()).get());
            fileHarvests.forEach(fileHarvest -> {
               LOGGER.info("Found: {}",fileHarvest.getFilename());
            });
            return Response.ok(jsonbContext.marshall(fileHarvests)).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
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

    private <T extends AbstractHarvesterConfigEntity> Response
            addHarvesterConfig(Class<T> type, String harvesterConfigString,
            UriInfo uriInfo) {
        try {
            T httpHarvesterConfig = jsonbContext.unmarshall(
                harvesterConfigString, type);
            URI uri = harvesterConfigRepository.add(type, httpHarvesterConfig,
                uriInfo.getAbsolutePathBuilder());
            return Response.created(uri).build();
        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.toString())
                .build();
        }
    }
}
