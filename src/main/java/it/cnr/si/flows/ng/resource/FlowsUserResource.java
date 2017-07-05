package it.cnr.si.flows.ng.resource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.security.AuthoritiesConstants;

@RestController
@RequestMapping("api/users")
public class FlowsUserResource {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Resource(name = "aceDataSource")
    private DataSource aceDataSource;

    @RequestMapping(value= "/ace", method = RequestMethod.GET)
    public String getAce() throws SQLException {

        ResultSet executeQuery = aceDataSource.getConnection().prepareStatement("select * from view").executeQuery();
        executeQuery.next();

        return "" + executeQuery.getString(1);
    }


    @RequestMapping(value = "/{username:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getMyTasks(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        // TODO se cercare con due wildcard e' un overkill (ma non credo visto che sono un diecimila voci)
        // TODO modificare la serach con un parametro piu' scaltro
        List<Object> search = ldapTemplate.search("", "(uid=*"+ username +"*)", new AttributesMapper<Object>() {
            public Object mapFromAttributes(Attributes attrs) throws NamingException {
                return attrs.get("uid").get();
            }
        });

        response.put("more", search.size() > 10);
        response.put("results", search.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

}
