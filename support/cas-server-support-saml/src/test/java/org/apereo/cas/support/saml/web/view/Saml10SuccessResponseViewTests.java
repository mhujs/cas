package org.apereo.cas.support.saml.web.view;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.support.DefaultCasProtocolAttributeEncoder;
import org.apereo.cas.services.DefaultServicesManager;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.validation.Assertion;
import org.apereo.cas.validation.ImmutableAssertion;
import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.authentication.RememberMeCredential;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.services.InMemoryServiceRegistryDao;
import org.apereo.cas.support.saml.AbstractOpenSamlTests;
import org.apereo.cas.support.saml.authentication.SamlAuthenticationMetaDataPopulator;
import org.apereo.cas.support.saml.util.Saml10ObjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit test for {@link Saml10SuccessResponseView} class.
 *
 * @author Scott Battaglia
 * @author Marvin S. Addison
 * @since 3.1
 *
 */
public class Saml10SuccessResponseViewTests extends AbstractOpenSamlTests {

    private Saml10SuccessResponseView response;

    @Before
    public void setUp() throws Exception {

        final List<RegisteredService> list = new ArrayList<>();
        list.add(RegisteredServiceTestUtils.getRegisteredService("https://.+"));
        final InMemoryServiceRegistryDao dao = new InMemoryServiceRegistryDao();
        dao.setRegisteredServices(list);
        this.response = new Saml10SuccessResponseView();
        final DefaultServicesManager mgmr = new DefaultServicesManager(dao);
        mgmr.load();
        this.response.setServicesManager(mgmr);
        this.response.setProtocolAttributeEncoder(new DefaultCasProtocolAttributeEncoder(this.response.getServicesManager()));
        
        final Saml10ObjectBuilder builder = new Saml10ObjectBuilder(configBean);
        this.response.setSamlObjectBuilder(builder);
        this.response.setIssuer("testIssuer");
        this.response.setIssueLength(1000);
    }

    @Test
    public void verifyResponse() throws Exception {
        final Map<String, Object> model = new HashMap<>();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("testAttribute", "testValue");
        attributes.put("testEmptyCollection", Collections.emptyList());
        attributes.put("testAttributeCollection", Arrays.asList("tac1", "tac2"));
        final Principal principal = new DefaultPrincipalFactory().createPrincipal("testPrincipal", attributes);

        final Map<String, Object> authAttributes = new HashMap<>();
        authAttributes.put(
                SamlAuthenticationMetaDataPopulator.ATTRIBUTE_AUTHENTICATION_METHOD,
                SamlAuthenticationMetaDataPopulator.AUTHN_METHOD_SSL_TLS_CLIENT);
        authAttributes.put("testSamlAttribute", "value");

        final Authentication primary =
                CoreAuthenticationTestUtils.getAuthentication(principal, authAttributes);
        final Assertion assertion = new ImmutableAssertion(
                primary, Collections.singletonList(primary),
                CoreAuthenticationTestUtils.getService(), true);
        model.put("assertion", assertion);

        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        this.response.renderMergedOutputModel(model, new MockHttpServletRequest(), servletResponse);
        final String written = servletResponse.getContentAsString();

        assertTrue(written.contains("testPrincipal"));
        assertTrue(written.contains("testAttribute"));
        assertTrue(written.contains("testValue"));
        assertFalse(written.contains("testEmptyCollection"));
        assertTrue(written.contains("testAttributeCollection"));
        assertTrue(written.contains("tac1"));
        assertTrue(written.contains("tac2"));
        assertTrue(written.contains(SamlAuthenticationMetaDataPopulator.AUTHN_METHOD_SSL_TLS_CLIENT));
        assertTrue(written.contains("AuthenticationMethod"));
        assertTrue(written.contains("AssertionID"));
    }

    @Test
    public void verifyResponseWithNoAttributes() throws Exception {
        final Map<String, Object> model = new HashMap<>();

        final Principal principal = new DefaultPrincipalFactory().createPrincipal("testPrincipal");

        final Map<String, Object> authAttributes = new HashMap<>();
        authAttributes.put(
                SamlAuthenticationMetaDataPopulator.ATTRIBUTE_AUTHENTICATION_METHOD,
                SamlAuthenticationMetaDataPopulator.AUTHN_METHOD_SSL_TLS_CLIENT);
        authAttributes.put("testSamlAttribute", "value");

        final Authentication primary = CoreAuthenticationTestUtils.getAuthentication(principal, authAttributes);

        final Assertion assertion = new ImmutableAssertion(
                primary, Collections.singletonList(primary),
                CoreAuthenticationTestUtils.getService(), true);
        model.put("assertion", assertion);

        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        this.response.renderMergedOutputModel(model, new MockHttpServletRequest(), servletResponse);
        final String written = servletResponse.getContentAsString();

        assertTrue(written.contains("testPrincipal"));
        assertTrue(written.contains(SamlAuthenticationMetaDataPopulator.AUTHN_METHOD_SSL_TLS_CLIENT));
        assertTrue(written.contains("AuthenticationMethod="));
    }

    @Test
    public void verifyResponseWithoutAuthMethod() throws Exception {
        final Map<String, Object> model = new HashMap<>();

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("testAttribute", "testValue");
        final Principal principal = new DefaultPrincipalFactory().createPrincipal("testPrincipal", attributes);

        final Map<String, Object> authnAttributes = new HashMap<>();
        authnAttributes.put("authnAttribute1", "authnAttrbuteV1");
        authnAttributes.put("authnAttribute2", "authnAttrbuteV2");
        authnAttributes.put(RememberMeCredential.AUTHENTICATION_ATTRIBUTE_REMEMBER_ME, Boolean.TRUE);

        final Authentication primary =
                CoreAuthenticationTestUtils.getAuthentication(principal, authnAttributes);

        final Assertion assertion = new ImmutableAssertion(
                primary, Collections.singletonList(primary),
                CoreAuthenticationTestUtils.getService(), true);
        model.put("assertion", assertion);

        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        this.response.renderMergedOutputModel(model, new MockHttpServletRequest(), servletResponse);
        final String written = servletResponse.getContentAsString();

        assertTrue(written.contains("testPrincipal"));
        assertTrue(written.contains("testAttribute"));
        assertTrue(written.contains("testValue"));
        assertTrue(written.contains("authnAttribute1"));
        assertTrue(written.contains("authnAttribute2"));
        assertTrue(written.contains(CasProtocolConstants.VALIDATION_REMEMBER_ME_ATTRIBUTE_NAME));
        assertTrue(written.contains("urn:oasis:names:tc:SAML:1.0:am:unspecified"));
    }
}
