/*
 * Copyright 2017 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.app.authentication;

import java.util.function.Function;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PasswordBasedAccess implements Authenticated {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordBasedAccess.class);

    static ZonkyApiToken trigger(final ApiProvider apis, final String username, final char... password) {
        return apis.oauth((oauth) -> {
            LOGGER.info("Authenticating as '{}', using password.", username);
            return oauth.login(username, password);
        });
    }

    private final SecretProvider secrets;
    private final ApiProvider apis;

    PasswordBasedAccess(final ApiProvider apis, final SecretProvider secrets) {
        this.secrets = secrets;
        this.apis = apis;
    }

    @Override
    public <T> T call(final Function<Zonky, T> op) {
        final ZonkyApiToken token = PasswordBasedAccess.trigger(apis, secrets.getUsername(), secrets.getPassword());
        return apis.authenticated(token, (zonky) -> {
            try {
                return op.apply(zonky);
            } finally { // attempt to log out no matter what happens
                LOGGER.info("Logging out.");
                zonky.logout();
            }
        });
    }

    @Override
    public SecretProvider getSecretProvider() {
        return secrets;
    }
}
