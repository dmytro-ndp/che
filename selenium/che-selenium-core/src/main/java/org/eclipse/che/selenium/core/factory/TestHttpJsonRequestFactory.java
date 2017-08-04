/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.selenium.core.factory;

import com.google.inject.Inject;

import org.eclipse.che.api.core.rest.DefaultHttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpJsonRequest;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.selenium.core.user.DefaultTestUser;
import org.eclipse.che.selenium.core.user.TestUser;

import javax.validation.constraints.NotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestHttpJsonRequestFactory extends DefaultHttpJsonRequestFactory {

    private final TestUser testUser;

    @Inject
    public TestHttpJsonRequestFactory(DefaultTestUser testUser) {
        this.testUser = testUser;
    }

    @Override
    public HttpJsonRequest fromUrl(@NotNull String url) {
        return super.fromUrl(url)
                    .setAuthorizationHeader(testUser.getAuthToken());
    }

    @Override
    public HttpJsonRequest fromLink(@NotNull Link link) {
        return super.fromLink(link)
                    .setAuthorizationHeader(testUser.getAuthToken());
    }
}
