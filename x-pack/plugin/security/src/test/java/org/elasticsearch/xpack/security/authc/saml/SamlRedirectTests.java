/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.authc.saml;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.security.x509.X509Credential;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

public class SamlRedirectTests extends SamlTestCase {

    private static final String IDP_ENTITY_ID = "https://idp.test/";
    private static final String LOGOUT_URL = "https://idp.test/saml/logout";

    private static final SigningConfiguration NO_SIGNING = new SigningConfiguration(emptySet(), null);

    public void testRedirectUrlWithoutRelayStateOrSigning() {
        final SamlRedirect redirect = new SamlRedirect(buildLogoutRequest(LOGOUT_URL), NO_SIGNING);
        final String url = redirect.getRedirectUrl();
        assertThat(url, equalTo(LOGOUT_URL + "?SAMLRequest=nZFBa4QwFIT%2FSnh3Naa2ax%2FqsiAFYdtDu91DLyVo2AY0cX2x9Oc36gpLCz30mAwz3" +
                "wwv2351LftUA2lrcohDDkyZ2jbanHJ4PTwEKWyLjGTXih739mRH96zOoyLHvNMQLlIO42DQStKERnaK0NX4snvcowg59oN1trYtsNIbtZFupn04" +
                "1xNGkW760HkhmrKidoYAq8oc3nUTi5vk9m6T3vsfolFVhpw0LgfB4zTgcRAnByEw2SDnIef8DdhxnePZcCmPs3m4Lv13Z0mkhqknFL96ZtF15kp" +
                "48hlV%2BS%2FCJAbL0sBP5StgiSwuzx8HKL4B"));
    }

    public void testRedirectUrlWithRelayStateAndSigning() throws Exception {
        final SigningConfiguration signing =
                new SigningConfiguration(singleton("*"), (X509Credential) buildOpenSamlCredential(createKeyPair()).get(0));
        final SamlRedirect redirect = new SamlRedirect(buildLogoutRequest(LOGOUT_URL), signing);
        final String url = redirect.getRedirectUrl("hello");
        assertThat(url, startsWith(LOGOUT_URL + "?SAMLRequest=nZFBa4QwFIT%2FSnh3Naa2ax%2FqsiAFYdtDu91DLyVo2AY0cX2x9Oc36gpLC" +
                "z30mAwz3wwv2351LftUA2lrcohDDkyZ2jbanHJ4PTwEKWyLjGTXih739mRH96zOoyLHvNMQLlIO42DQStKERnaK0NX4snvcowg59oN1trY" +
                "tsNIbtZFupn041xNGkW760HkhmrKidoYAq8oc3nUTi5vk9m6T3vsfolFVhpw0LgfB4zTgcRAnByEw2SDnIef8DdhxnePZcCmPs3m4Lv13Z" +
                "0mkhqknFL96ZtF15kp48hlV%2BS%2FCJAbL0sBP5StgiSwuzx8HKL4B" +
                "&RelayState=hello" +
                "&SigAlg=http%3A%2F%2Fwww.w3.org%2F2001%2F04%2Fxmldsig-more%23rsa-sha256" +
                "&Signature="));
    }

    public void testRedirectUrlWithExistingParameters() {
        final SamlRedirect redirect = new SamlRedirect(buildLogoutRequest(LOGOUT_URL + "?a=xyz"), NO_SIGNING);
        final String url = redirect.getRedirectUrl("foo");
        assertThat(url, equalTo(LOGOUT_URL + "?a=xyz" +
                "&SAMLRequest=nZFBS8QwFIT%2FSnn3tmmsbn00LUIRCqsHXT14kdCGNdAmtS%2BV1V9v2u7CouDBYzLMzDe8vDz0XfChRtLWCE" +
                "giBoEyjW212Qt42t2GGZRFTrLv%2BIBbu7eTe1DvkyIXeKchXCUB02jQStKERvaK0DX4eHO3RR4xHEbrbGM7CCpv1Ea6pe3NuYE" +
                "wjnU7RM4L8ZwVd0tJKcXh8wuCuhLwqtuEX6SXV5vs2v8QTao25KRxAjhLspAlYZLuOMd0g4xFjLEXCJ5PozwBHCfgYh7P0f8ml0" +
                "RqnGmh%2BEWbx%2BeZp4Z7n1FX%2F2qYxXBdGvqp7FSwRhbH548zFN8%3D" +
                "&RelayState=foo"));
    }

    public void testRedirectUrlWithTrailingQuestionMark() {
        final SamlRedirect redirect = new SamlRedirect(buildLogoutRequest(LOGOUT_URL + "?"), NO_SIGNING);
        final String url = redirect.getRedirectUrl();
        assertThat(url, equalTo(LOGOUT_URL + "?SAMLRequest=nZFPS8QwFMS%2FSnj3tmmsbn30D0IRCqsHXffgRUIb1kCb1L5U%2FPim7R" +
                "YWBQ8ek2HmN8PLyq%2B%2BY59qJG1NDnHIgSnT2FabUw4vh%2FsghbLISPadGHBvT3ZyT%2BpjUuSYdxrCVcphGg1aSZrQyF4Rug" +
                "af7x72KEKOw2idbWwHrPJGbaRbaO%2FODYRRpNshdF6I5qyoWyAlsLrK4U23sbhKrm926a3%2FIZpUbchJ43IQPE4DHgdxchACkx" +
                "1yHnLOX4Edtz0eDuf2uJjHy9Z%2Fl5ZEapyLQvGraBZdZm6ER59RV%2F8izGKwLg38VL4B1sji%2FPxxgeIb"));
    }

    private LogoutRequest buildLogoutRequest(String logoutUrl) {
        final LogoutRequest logoutRequest = SamlUtils.buildObject(LogoutRequest.class, LogoutRequest.DEFAULT_ELEMENT_NAME);
        logoutRequest.setDestination(logoutUrl);
        logoutRequest.setIssueInstant(new DateTime(2018, 1, 14, 22, 47, DateTimeZone.UTC));
        logoutRequest.setID("_id123456789");
        final Issuer issuer = SamlUtils.buildObject(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(IDP_ENTITY_ID);
        logoutRequest.setIssuer(issuer);
        final NameID nameId = SamlUtils.buildObject(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameId.setValue("name-123456-7890");
        logoutRequest.setNameID(nameId);
        return logoutRequest;
    }

}