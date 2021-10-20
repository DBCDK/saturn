package dk.dbc.saturn.entity;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;
import java.util.List;

@Converter
public class CustomHttpHeaderToJsonArrayConverter implements AttributeConverter<List<CustomHttpHeader>, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(List entries) {
        PGobject pGobject = new PGobject();
        pGobject.setType("jsonb");

        try {
            if (entries != null) {
                pGobject.setValue(JSONB_CONTEXT.marshall(entries));
            }
            return pGobject;
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<CustomHttpHeader> convertToEntityAttribute(PGobject pGobject) {
        if (pGobject != null) {
            try {
                final CollectionType collectionType = JSONB_CONTEXT
                        .getTypeFactory()
                        .constructCollectionType(List.class, CustomHttpHeader.class);
                return JSONB_CONTEXT.unmarshall(pGobject.getValue(), collectionType);

            } catch (JSONBException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
