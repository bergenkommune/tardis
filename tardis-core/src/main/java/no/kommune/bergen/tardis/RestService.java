package no.kommune.bergen.tardis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
public class RestService {

    private static final MediaType APPLICATION_X_NDJSON = MediaType.valueOf("application/x-ndjson");
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.TEXT_PLAIN;

    @Autowired
    private Tardis tardis;
    private Logger LOG = LoggerFactory.getLogger(RestService.class);

    @RequestMapping(method = RequestMethod.GET, value = "/{dataSourceName}/{tableName}")
    public void getChanges(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable String dataSourceName, @PathVariable String tableName,
                           @RequestParam(required = false) String fromRevision, @RequestParam(required = false) String toRevision,
                           @RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate)
            throws IOException {
        try {
            OutputStream out = prepareOutputStream(request, response);

            if (isNotBlank(fromRevision) && isNotBlank(toRevision)) {
                tardis.getDiff(dataSourceName, tableName, fromRevision, toRevision, out);
            } else if (isNotBlank(fromDate) && isNotBlank(toDate)) {
                DateFormat df = new UtcDateFormat();
                tardis.getDiff(dataSourceName, tableName, df.parse(fromDate), df.parse(toDate), out);
            } else {
                throw new IllegalArgumentException("You must provide either fromRevision and toRevision or fromDate and toDate query params");
            }
        } catch (HttpServerErrorException e) {
            LOG.error("Couldn't get changes", e);
            response.sendError(e.getStatusCode().value(), e.getMessage());
        } catch (Exception e) {
            LOG.error("Couldn't get changes", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/log", produces = "text/plain; charset=utf-8")
    @ResponseBody
    public String log(HttpServletResponse response) {
        return tardis.log();
    }


    private OutputStream prepareOutputStream(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // look for format parameter first, then at accept header
        MediaType outputContentType = lookForFormatParameter(request);

        if(outputContentType==null) {
            outputContentType = negotiateContentType(request);
        }

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", outputContentType.toString() );

        OutputStream outputStream = response.getOutputStream();

        if (outputContentType == MediaType.APPLICATION_JSON) {
            // default output is ndjson, convert to json
            outputStream = new NdjsonToJsonOutputStream(outputStream);
        }

        return outputStream;
    }

    private MediaType lookForFormatParameter(HttpServletRequest request) {

        String requestedFormat = request.getParameter("format");
        if (requestedFormat == null) {
            return null;
        }

        switch (requestedFormat) {
            case "json":
                return MediaType.APPLICATION_JSON;
            case "ndjson":
                return APPLICATION_X_NDJSON;
            default:
                throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,
                        "Unsupported format parameter, use json og ndjson");
        }
    }

    private MediaType negotiateContentType(HttpServletRequest request) {

        List<MediaType> requestedMediaTypes = MediaType.parseMediaTypes(request.getHeader("accept"));

        if (requestedMediaTypes.isEmpty()) {
            // none specified, use default
            return DEFAULT_MEDIA_TYPE;
        }

        MediaType.sortBySpecificityAndQuality(requestedMediaTypes);

        for(MediaType candidateMediaType : requestedMediaTypes) {

            // text/plain first for backward compatibility in case */* is specified
            if (candidateMediaType.includes(MediaType.TEXT_PLAIN))
                return MediaType.TEXT_PLAIN;

            if (candidateMediaType.includes(MediaType.APPLICATION_JSON))
                return MediaType.APPLICATION_JSON;

            if (candidateMediaType.includes(APPLICATION_X_NDJSON))
                return APPLICATION_X_NDJSON;
        }

        // none of the accepted media types are supported
        throw new HttpServerErrorException(HttpStatus.NOT_ACCEPTABLE,
                "Unsupported accept media type, use application/json or application/x-ndjson");
    }
}
