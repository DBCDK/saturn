package dk.dbc.saturn.api;

import dk.dbc.saturn.PasswordRepository;
import dk.dbc.saturn.entity.PasswordEntry;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dk.dbc.saturn.DateTimeUtil.LOCAL_DATE_TIME_FORMATTER;
import static dk.dbc.saturn.DateTimeUtil.parseLocalDateTime;

@Stateless
@Path("passwordrepository")
public class PasswordRepositoryApi {
    private static final String PASSWORDREPO_LIST = "list/{host}/{username}";
    private static final String PASSWORDREPO_ADD = "add";
    private static final String PASSWORDREPO_DELETE_PASSWORD = "{host}/{username}/{date}";
    private static final String PASSWORDREPO_GET_PASSWORD = "{host}/{username}/{date}";
    private static final Base64.Decoder decoder = Base64.getDecoder();
    private static final Base64.Encoder encoder = Base64.getEncoder();

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
        PasswordEntry passwordEntry = passwordRepository.getPasswordForDate(host, username, parseLocalDateTime(date));
        if (passwordEntry != null) {
            passwordEntryFrontEnd.setPassword(new String(encoder.encode(passwordEntry.getPassword().getBytes())));
            passwordEntryFrontEnd.setActiveFrom(passwordEntry.getActiveFrom().format(LOCAL_DATE_TIME_FORMATTER));
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
    public Response addEntry(PasswordEntryFrontEnd entry) {
        PasswordEntry newEntry = new PasswordEntry()
                .withHost(entry.getHost())
                .withUsername(entry.getUsername())
                .withPassword(base64Decode(entry.getPassword()).orElse(entry.getPassword()))
                .withActiveFrom(parseLocalDateTime(entry.getActiveFrom()));
        passwordRepository.save(newEntry);
        return Response.ok().build();
    }

    private PasswordEntryFrontEnd getPasswordEntryFrontEnd(PasswordEntry passwordEntry) {
        return new PasswordEntryFrontEnd()
                .withHost(passwordEntry.getHost())
                .withUsername(passwordEntry.getUsername())
                .withPassword(passwordEntry.getPassword())
                .withActiveFrom(passwordEntry.getActiveFrom().format(LOCAL_DATE_TIME_FORMATTER));
    }

    private Optional<String> base64Decode(String coded) {
        try {
            return Optional.of(new String(decoder.decode(coded.getBytes())));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
