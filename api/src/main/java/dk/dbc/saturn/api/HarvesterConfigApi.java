/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.saturn.HarvesterConfigRepository;
import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Stateless
@Path("configs")
public class HarvesterConfigApi {
    private final static String HTTP_LIST_ENDPOINT = "http/list";
    private final static String HTTP_ADD_ENDPOINT = "http/add";
    private final static String HTTP_GET_SINGLE_CONFIG_ENDPOINT = "http/get/{id}";
    private final static String FTP_LIST_ENDPOINT = "ftp/list";
    private final static String FTP_ADD_ENDPOINT = "ftp/add";
    private final static String FTP_GET_SINGLE_CONFIG_ENDPOINT = "ftp/get/{id}";
    private final static JSONBContext jsonbContext = new JSONBContext();

    @EJB HarvesterConfigRepository harvesterConfigRepository;

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
    public Response addHttpHarvesterConfig(String harvesterConfigString) {
        return addHarvesterConfig(HttpHarvesterConfig.class,
            harvesterConfigString);
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
    public Response addFtpHarvesterConfig(String harvesterConfigString) {
        return addHarvesterConfig(FtpHarvesterConfig.class,
            harvesterConfigString);
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

    private <T extends AbstractHarvesterConfigEntity> Response
            addHarvesterConfig(Class<T> type, String harvesterConfigString) {
        try {
            T httpHarvesterConfig = jsonbContext.unmarshall(
                harvesterConfigString, type);
            harvesterConfigRepository.add(httpHarvesterConfig);
            return Response.ok().build();
        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.toString())
                .build();
        }
    }
}
