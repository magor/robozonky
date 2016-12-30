/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import com.github.triceo.robozonky.api.notifications.Event;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;

public abstract class AbstractEmailingListenerTest extends AbstractListenerTest {

    @Before
    public void expectEmailingListener() { // otherwise all tests here make no sense
        Assume.assumeTrue(AbstractEmailingListener.class.isAssignableFrom(this.listener.getClass()));
    }

    @After
    public void resetBalanceTracker() { // to make sure the tests always return consistent results
        BalanceTracker.INSTANCE.reset();
    }

    protected AbstractEmailingListener<Event> getEmailingListener() {
        return (AbstractEmailingListener<Event>) this.listener;
    }

}
