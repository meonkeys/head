/*
 * Copyright (c) 2005-2008 Grameen Foundation USA
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
package org.mifos.test.acceptance.framework;


import org.testng.Assert;

import com.thoughtworks.selenium.Selenium;

/**
 * Encapsulates the GUI based actions that can
 * be done from the Home page and the page 
 * that will be navigated to.
 *
 */
public class AdminPage extends MifosPage {

	public AdminPage() {
		super();
	}

	public AdminPage(Selenium selenium) {
		super(selenium);
	}
		
	public CreateOfficeEnterDataPage navigateToCreateOfficeEnterDataPage() {
        selenium.click("admin.link.defineNewOffice");
        waitForPageToLoad();
        return new CreateOfficeEnterDataPage(selenium);	    
	}
	
    public ChooseOfficePage navigateToCreateUserPage() {
        selenium.click("admin.link.defineNewUsers");
        waitForPageToLoad();
        return new ChooseOfficePage(selenium);       
    }
    
    public String getWelcome() {
        return selenium.getText("id=admin.text.welcome");
    }    
    public DefineNewLoanProductPage navigateToDefineLoanProduct() {
        selenium.click("link=Define new Loan product");
        waitForPageToLoad();
        return new DefineNewLoanProductPage(selenium);
    }

    public AdminPage verifyPage() {
        Assert.assertTrue(selenium.isElementPresent("admin.label.admintasks"),"Didn't reach Admin home page");
        return this;
    }
    
}
