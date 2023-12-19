package com.exadel.etoolbox.backpack.core.services.util.impl;

import com.exadel.etoolbox.backpack.core.services.util.SessionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

@Component(service = SessionService.class)
public class SessionServiceImpl implements SessionService {

    private static final String SERVICE_NAME = "backpack-service";

    @Reference
    @SuppressWarnings("UnusedDeclaration") // value injected by Sling
    private SlingRepository slingRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getUserImpersonatedSession(final String userId) throws RepositoryException {
        return slingRepository.impersonateFromService(SERVICE_NAME,
                new SimpleCredentials(userId, StringUtils.EMPTY.toCharArray()),
                null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSession(final Session userSession) {
        if (userSession != null && userSession.isLive()) {
            userSession.logout();
        }
    }
}
