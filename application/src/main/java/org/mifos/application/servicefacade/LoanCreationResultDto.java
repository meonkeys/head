/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.application.servicefacade;

import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.customers.business.CustomerBO;

public class LoanCreationResultDto {

    private final boolean isGlimApplicable;
    private final Integer accountId;
    private final String globalAccountNum;
    private final LoanBO loan;
    private final CustomerBO customer;

    public LoanCreationResultDto(boolean isGlimApplicable, Integer accountId, String globalAccountNum, LoanBO loan, CustomerBO customer) {
        this.isGlimApplicable = isGlimApplicable;
        this.accountId = accountId;
        this.globalAccountNum = globalAccountNum;
        this.loan = loan;
        this.customer = customer;
    }

    public boolean isGlimApplicable() {
        return this.isGlimApplicable;
    }

    public Integer getAccountId() {
        return this.accountId;
    }

    public String getGlobalAccountNum() {
        return this.globalAccountNum;
    }

    public LoanBO getLoan() {
        return this.loan;
    }

    public CustomerBO getCustomer() {
        return this.customer;
    }
}