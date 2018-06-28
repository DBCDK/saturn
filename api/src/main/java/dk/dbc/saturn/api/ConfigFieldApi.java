/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.saturn.CronParserBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("fields")
public class ConfigFieldApi {
    private static final String VALIDATE_CRON_ENDPOINT = "cron/validate";
    private static final String DESCRIBE_CRON_ENDPOINT = "cron/describe";

    @EJB CronParserBean cronParserBean;

    /**
     * validate a cron expression
     * @param cronExpression cron expression
     * @return 200 OK if the expression is a valid cron expression
     *         400 Bad Request if the cron expression is invalid
     */
    @POST
    @Path(VALIDATE_CRON_ENDPOINT)
    @Produces(MediaType.TEXT_PLAIN)
    public Response validateCron(String cronExpression) {
        if(cronParserBean.validate(cronExpression)) {
            return Response.ok("OK").build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * describe a cron expression in human words
     * @param cronExpression cron expression
     * @return human language description of cron expression
     */
    @POST
    @Path(DESCRIBE_CRON_ENDPOINT)
    @Produces(MediaType.TEXT_PLAIN)
    public Response describeCron(String cronExpression) {
        if(!cronParserBean.validate(cronExpression)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String description = cronParserBean.describe(cronExpression);
        return Response.ok(description).build();
    }
}
