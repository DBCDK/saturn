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
import javax.ws.rs.core.Response;

@Stateless
@Path("configs")
public class ConfigFieldApi {
    private static final String VALIDATE_CRON_ENDPOINT = "cron/validate";

    @EJB CronParserBean cronParserBean;

    /**
     * validate a cron expression
     * @param cronExpression cron expression
     * @return 200 OK if the expression is a valid cron expression
     *         400 Bad Request if the cron expression is invalid
     */
    @POST
    @Path(VALIDATE_CRON_ENDPOINT)
    public Response validateCron(String cronExpression) {
        if(cronParserBean.validate(cronExpression)) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
