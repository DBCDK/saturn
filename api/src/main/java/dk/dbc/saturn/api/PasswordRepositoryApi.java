package dk.dbc.saturn.api;

import dk.dbc.saturn.PasswordRepository;
import dk.dbc.saturn.entity.PasswordEntry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("passwordrepository")
public class PasswordRepositoryApi {
    private static final String PASSWORDREPO_LIST = "list/{host}/{username}";
    private static final String PASSWORDREPO_ADD = "add";
    private static final String PASSWORDREPO_DELETE_PASSWORD = "{host}/{username}/{date}";
    private static final String PASSWORDREPO_GET_PASSWORD = "{host}/{username}/{date}";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static {
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Copenhagen"));
    }

    @EJB
    PasswordRepository passwordRepository;

    @GET
    @Path(PASSWORDREPO_LIST)
    @Produces({MediaType.APPLICATION_JSON})
    public Response listPasswordEntries(@PathParam("host") String host, @PathParam("username") String username) {
        List<PasswordEntry> entries = passwordRepository.list(host, username, 100);
        List<PasswordEntryFrontEnd> passwordEntryFrontEndList = entries.stream().map(this::getPasswordEntryFrontEnd).collect(Collectors.toList());
        return Response.ok(passwordEntryFrontEndList).build();
    }

    @GET
    @Path(PASSWORDREPO_GET_PASSWORD)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPassword(@PathParam("host") String host,
                                @PathParam("username") String username,
                                @PathParam("date") String date) throws ParseException {
        PasswordEntryFrontEnd passwordEntryFrontEnd = new PasswordEntryFrontEnd()
                .withHost(host)
                .withUsername(username);
        PasswordEntry passwordEntry = passwordRepository.getPasswordForDate(host, username, sdf.parse(date));
        if (passwordEntry != null) {
            passwordEntryFrontEnd.setPassword(passwordEntry.getPassword());
            passwordEntryFrontEnd.setActiveFrom(sdf.format(passwordEntry.getActiveFrom()));
        }
        return Response.ok(passwordEntryFrontEnd).build();
    }

    @DELETE
    @Path(PASSWORDREPO_DELETE_PASSWORD)
    public Response deletePasswordEntry(@PathParam("host") String host,
                                        @PathParam("username") String username,
                                        @PathParam("date") String date) {
        /* Todo */
        return Response.ok().build();
    }

    @POST
    @Path(PASSWORDREPO_ADD)
    @Consumes({MediaType.APPLICATION_JSON})
    public Response addEntry(PasswordEntryFrontEnd entry) throws ParseException {
        PasswordEntry newEntry = new PasswordEntry()
                .withHost(entry.getHost())
                .withUsername(entry.getUsername())
                .withPassword(entry.getPassword())
                .withActiveFrom(sdf.parse(entry.getActiveFrom()));
        passwordRepository.save(newEntry);
        return Response.ok().build();
    }

    private PasswordEntryFrontEnd getPasswordEntryFrontEnd(PasswordEntry passwordEntry) {
        return new PasswordEntryFrontEnd()
                .withHost(passwordEntry.getHost())
                .withUsername(passwordEntry.getUsername())
                .withPassword(passwordEntry.getPassword())
                .withActiveFrom(sdf.format(passwordEntry.getActiveFrom()));
    }
}
