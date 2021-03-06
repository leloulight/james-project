/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mpt.smtp;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.equalTo;

import java.net.InetAddress;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.script.AbstractSimpleScriptedTestProtocol;
import org.apache.james.mpt.smtp.dns.InMemoryDNSService;
import org.apache.james.mpt.smtp.utils.DockerRule;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public class ForwardSmtpTest extends AbstractSimpleScriptedTestProtocol {

    public static final String USER = "bob";
    public static final String DOMAIN = "mydomain.tld";
    public static final String USER_AT_DOMAIN = USER + "@" + DOMAIN;
    public static final String PASSWORD = "secret";

    private final TemporaryFolder folder = new TemporaryFolder();
    private final DockerRule fakeSmtp = new DockerRule("weave/rest-smtp-sink");

    @Rule
    public final RuleChain chain = RuleChain.outerRule(folder).around(fakeSmtp);

    @Inject
    private static SmtpHostSystem hostSystem;

    @Inject
    private static RecipientRewriteTable recipientRewriteTable;

    @Inject
    private static InMemoryDNSService dnsService;

    public ForwardSmtpTest() throws Exception {
        super(hostSystem, USER_AT_DOMAIN, PASSWORD, "/org/apache/james/smtp/scripts/");
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InetAddress containerIp = InetAddresses.forString(fakeSmtp.getContainerIp());
        dnsService.registerRecord("yopmail.com", new InetAddress[]{containerIp}, ImmutableList.of("yopmail.com"), ImmutableList.of());
        recipientRewriteTable.addAddressMapping(USER, DOMAIN, "ray@yopmail.com");

        RestAssured.port = Integer.valueOf("80");
        RestAssured.baseURI = "http://" + containerIp.getHostAddress();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
    }

    @Test
    public void forwardingAnEmailShouldWork() throws Exception {
        scriptTest("helo", Locale.US);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get("/api/email")
        .then()
            .statusCode(200)
            .body("[0].from", equalTo("matthieu@yopmail.com"))
            .body("[0].subject", equalTo("test"));
    }
}
