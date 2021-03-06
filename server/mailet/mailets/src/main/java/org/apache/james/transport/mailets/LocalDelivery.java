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

package org.apache.james.transport.mailets;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.apache.mailet.base.GenericMailet;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Receives a Mail from the Queue and takes care of delivery of the
 * message to local inboxes.
 * 
 * This mailet is a composition of RecipientRewriteTable, SieveMailet 
 * and MailboxManager configured to mimic the old "LocalDelivery"
 * James 2.3 behavior.
 */
public class LocalDelivery extends GenericMailet {
    
    private org.apache.james.rrt.api.RecipientRewriteTable rrt;
    private UsersRepository usersRepository;
    private MailboxManager mailboxManager;
    private DomainList domainList;
    private SieveRepository sieveRepository;

    @Inject
    public void setSieveRepository(SieveRepository sieveRepository) {
        this.sieveRepository = sieveRepository;
    }

    @Inject
    public void setRrt(org.apache.james.rrt.api.RecipientRewriteTable rrt) {
        this.rrt = rrt;
    }

    @Inject
    public void setUsersRepository(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }
    
    @Inject
    public void setMailboxManager(@Named("mailboxmanager") MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    @Inject
    public void setDomainList(DomainList domainList) {
        this.domainList = domainList;
    }

    private SieveMailet sieveMailet;  // Mailet that actually stores the message
    private RecipientRewriteTable recipientRewriteTable;  // Mailet that applies RecipientRewriteTable

    /**
     * Delivers a mail to a local mailbox.
     * 
     * @param mail the mail being processed
     * 
     * @throws MessagingException if an error occurs while storing the mail
     */
    public void service(Mail mail) throws MessagingException {
        recipientRewriteTable.service(mail);
        if (!mail.getState().equals(Mail.GHOST)) {
            sieveMailet.service(mail);
        }
    }

    /**
     * Return a string describing this mailet.
     * 
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Local Delivery Mailet";
    }

    /**
     * @see org.apache.mailet.base.GenericMailet#init()
     */
    public void init() throws MessagingException {
        
        super.init();

        recipientRewriteTable = new RecipientRewriteTable();
        recipientRewriteTable.setDomainList(domainList);
        recipientRewriteTable.setRecipientRewriteTable(rrt);
        recipientRewriteTable.init(getMailetConfig());
 
        sieveMailet = new SieveMailet(usersRepository, mailboxManager, sieveRepository, "INBOX");
        sieveMailet.init(new MailetConfig() {
            public String getInitParameter(String name) {
                if ("addDeliveryHeader".equals(name)) {
                    return "Delivered-To";
                } else if ("resetReturnPath".equals(name)) {
                    return "true";
                } else {
                    return getMailetConfig().getInitParameter(name);
                }
            }

            public Iterator<String> getInitParameterNames() {
                return Iterators.concat(
                        getMailetConfig().getInitParameterNames(), 
                        Lists.newArrayList("addDeliveryHeader", "resetReturnPath").iterator());
            }
            
            public MailetContext getMailetContext() {
                return getMailetConfig().getMailetContext();
            }

            public String getMailetName() {
                return getMailetConfig().getMailetName();
            }

        });
        // Override the default value of "quiet"
        sieveMailet.setQuiet(getInitParameter("quiet", true));
    }

}
