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

package com.github.triceo.robozonky.app;

import com.github.triceo.robozonky.api.ReturnCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

public class AppTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void notWellFormedCli() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main();
    }

    @Test
    public void wrongStrategyOnCli() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main("-s", "some.random.file", "-u", "user", "-p", "password", "-t");
    }

    @Test
    public void wrongKeyStore() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_WRONG_PARAMETERS.getCode());
        App.main("-l", "1", "-a", "1000", "-g", "some.random.file", "-p", "password");
    }

    @Test
    public void handleUnexpectedError() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        App.handleUnexpectedError(null);
    }

    @Test
    public void handleMaintenanceError() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        App.handleZonkyMaintenanceError(null, false);
    }

    @Test
    public void handleMaintenanceErrorFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        App.handleZonkyMaintenanceError(null, true);
    }
}
