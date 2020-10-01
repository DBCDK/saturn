/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.rest;

import dk.dbc.saturn.ScheduledHarvesterBean;
import dk.dbc.serviceutils.HowRU;
import dk.dbc.serviceutils.ServiceStatus;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@LocalBean
@Path("")
public class StatusBean implements ServiceStatus {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            StatusBean.class);

    @Inject
    ScheduledHarvesterBean scheduledHarvesterBean;

    @GET
    @Path("status")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getStatus() {
        if (scheduledHarvesterBean.isScheduleThreadHealthy()) {
            LOGGER.info("Service is healthy");
            return Response.ok().entity(OK_ENTITY).build();
        }
        else {
            HowRU.Error e = new HowRU.Error().withMessage(scheduledHarvesterBean.getLatestException().getMessage());
            LOGGER.info("Error in service: {}", scheduledHarvesterBean.getLatestException().getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new HowRU().withError(e)
                            .withStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                            .toJson()).build();
        }
    }
}
