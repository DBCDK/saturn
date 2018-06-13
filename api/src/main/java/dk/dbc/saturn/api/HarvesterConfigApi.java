/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.saturn.HarvesterConfigRepository;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("harvester")
public class HarvesterConfigApi {
    private final static String HTTP_LIST_ENDPOINT = "http/list";
    private final static String FTP_LIST_ENDPOINT = "ftp/list";
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
}
