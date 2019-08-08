/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.pageobject.dashboard.stacks;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@Singleton
public class StackDetails {
  private final SeleniumWebDriverHelper seleniumWebDriverHelper;

  @Inject
  public StackDetails(
      SeleniumWebDriver seleniumWebDriver, SeleniumWebDriverHelper seleniumWebDriverHelper) {
    this.seleniumWebDriverHelper = seleniumWebDriverHelper;
    PageFactory.initElements(seleniumWebDriver, this);
  }

  private interface Locators {
    String TOOLBAR_XPATH = "//*[@name='stackForm']";
    String TOOLBAR_WITH_STACK_NAME_XPATH_PATTERN = "//div[@che-title='%s']";
    String NEW_STACK_NAME = "deskname";
    String SAVE_CHANGES_BUTTON_NAME = "saveButton";
    String ALL_STACKS_BUTTON_XPATH = "//a[@title='All stacks']";
  }

  @FindBy(name = Locators.NEW_STACK_NAME)
  WebElement stackNameField;

  public void waitToolbar() {
    seleniumWebDriverHelper.waitVisibility(By.xpath(Locators.TOOLBAR_XPATH));
  }

  public void waitToolbarWithStackName(String stackName) {
    seleniumWebDriverHelper.waitVisibility(
        By.xpath(format(Locators.TOOLBAR_WITH_STACK_NAME_XPATH_PATTERN, stackName)));
  }

  public void setStackName(String stackName) {
    seleniumWebDriverHelper.waitAndClick(stackNameField);
    seleniumWebDriverHelper.setValue(stackNameField, stackName);
  }

  public void clickOnSaveChangesButton() {
    seleniumWebDriverHelper.waitAndClick(By.name(Locators.SAVE_CHANGES_BUTTON_NAME));
  }

  public void clickOnAllStacksButton() {
    seleniumWebDriverHelper.waitAndClick(By.xpath(Locators.ALL_STACKS_BUTTON_XPATH));
  }
}
