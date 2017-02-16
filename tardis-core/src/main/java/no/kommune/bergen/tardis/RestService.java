package no.kommune.bergen.tardis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
public class RestService {
    @Autowired
    private Tardis tardis;
    private Logger LOG = LoggerFactory.getLogger(RestService.class);

    @RequestMapping(method = RequestMethod.GET, value = "/{dataSourceName}/{tableName}")
    public void getChanges(HttpServletResponse response,
                           @PathVariable String dataSourceName, @PathVariable String tableName,
                           @RequestParam(required = false) String fromRevision, @RequestParam(required = false) String toRevision,
                           @RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate)
            throws IOException {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            ServletOutputStream out = response.getOutputStream();

            if (isNotBlank(fromRevision) && isNotBlank(toRevision)) {
                tardis.getDiff(dataSourceName, tableName, fromRevision, toRevision, out);
            } else if (isNotBlank(fromDate) && isNotBlank(toDate)) {
                DateFormat df = new UtcDateFormat();
                tardis.getDiff(dataSourceName, tableName, df.parse(fromDate), df.parse(toDate), out);
            } else {
                throw new IllegalArgumentException("You must provide either fromRevision and toRevision or fromDate and toDate query params");
            }
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

}
