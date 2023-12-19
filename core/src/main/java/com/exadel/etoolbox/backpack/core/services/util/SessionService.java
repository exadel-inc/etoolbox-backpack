package com.exadel.etoolbox.backpack.core.services.util;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public interface SessionService {

    /**
     * Gets the {@code Session} instance with required rights for package manipulation
     *
     * @param userId User ID per the effective {@code ResourceResolver}
     * @return {@code Session} object
     * @throws RepositoryException in case of a Sling repository failure
     */
    Session getUserImpersonatedSession(final String userId) throws RepositoryException;

    /**
     * Closing the {@code Session} instance
     *
     * @param userSession {@code Session} object used for package installation
     */
    void closeSession(final Session userSession);
}
