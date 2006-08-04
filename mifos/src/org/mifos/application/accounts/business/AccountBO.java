/**

 * AccountBO.java    version: xxx

 

 * Copyright (c) 2005-2006 Grameen Foundation USA

 * 1029 Vermont Avenue, NW, Suite 400, Washington DC 20005

 * All rights reserved.

 

 * Apache License 
 * Copyright (c) 2005-2006 Grameen Foundation USA 
 * 

 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 *

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the 

 * License. 
 * 
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an explanation of the license 

 * and how it is applied. 

 *

 */

package org.mifos.application.accounts.business;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.mifos.application.accounts.exceptions.AccountException;
import org.mifos.application.accounts.exceptions.AccountExceptionConstants;
import org.mifos.application.accounts.exceptions.IDGenerationException;
import org.mifos.application.accounts.financial.business.FinancialTransactionBO;
import org.mifos.application.accounts.financial.business.service.FinancialBusinessService;
import org.mifos.application.accounts.financial.exceptions.FinancialException;
import org.mifos.application.accounts.loan.business.LoanScheduleEntity;
import org.mifos.application.accounts.persistence.service.AccountPersistanceService;
import org.mifos.application.accounts.savings.util.helpers.SavingsConstants;
import org.mifos.application.accounts.util.helpers.AccountConstants;
import org.mifos.application.accounts.util.helpers.AccountState;
import org.mifos.application.accounts.util.helpers.PaymentData;
import org.mifos.application.accounts.util.helpers.PaymentStatus;
import org.mifos.application.accounts.util.helpers.WaiveEnum;
import org.mifos.application.accounts.util.valueobjects.AccountFees;
import org.mifos.application.customer.business.CustomerBO;
import org.mifos.application.customer.center.exception.StateChangeException;
import org.mifos.application.customer.persistence.service.CustomerPersistenceService;
import org.mifos.application.fees.business.FeeBO;
import org.mifos.application.fees.persistence.FeePersistence;
import org.mifos.application.fees.util.helpers.FeeFrequencyType;
import org.mifos.application.fees.util.valueobjects.Fees;
import org.mifos.application.master.persistence.service.MasterPersistenceService;
import org.mifos.application.master.util.valueobjects.AccountType;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.meeting.util.valueobjects.Meeting;
import org.mifos.application.office.business.OfficeBO;
import org.mifos.application.personnel.business.PersonnelBO;
import org.mifos.framework.business.BusinessObject;
import org.mifos.framework.business.service.ServiceFactory;
import org.mifos.framework.components.configuration.business.Configuration;
import org.mifos.framework.components.logger.LoggerConstants;
import org.mifos.framework.components.logger.MifosLogManager;
import org.mifos.framework.components.scheduler.SchedulerException;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.HibernateProcessException;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.exceptions.StatesInitializationException;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.hibernate.helper.HibernateUtil;
import org.mifos.framework.security.util.ActivityMapper;
import org.mifos.framework.security.util.UserContext;
import org.mifos.framework.security.util.resources.SecurityConstants;
import org.mifos.framework.util.helpers.BusinessServiceName;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.PersistenceServiceName;
import org.mifos.framework.util.helpers.StringUtils;

public class AccountBO extends BusinessObject {
	AccountPersistanceService accountPersistanceService = null;

	// TODO: make constructor as protected and remove usage of this constructor
	// from test cases.
	protected AccountBO() {
		accountId = null;
		globalAccountNum=null;
		customer=null;
		accountType = null;
		office =null;
		personnel = null;
		accountFees = new HashSet<AccountFeesEntity>();
		accountPayments = new HashSet<AccountPaymentEntity>();
		accountActionDates = new HashSet<AccountActionDateEntity>();
		accountCustomFields = new HashSet<AccountCustomFieldEntity>();
		accountNotes = new HashSet<AccountNotesEntity>();
		accountStatusChangeHistory = new HashSet<AccountStatusChangeHistoryEntity>();
		accountFlags = new HashSet<AccountFlagMapping>();
	}
	

	protected AccountBO(UserContext userContext, CustomerBO customer,
			AccountType accountType)
			throws Exception {
		super(userContext);
		accountFees = new HashSet<AccountFeesEntity>();
		accountPayments = new HashSet<AccountPaymentEntity>();
		accountActionDates = new HashSet<AccountActionDateEntity>();
		accountCustomFields = new HashSet<AccountCustomFieldEntity>();
		accountNotes = new HashSet<AccountNotesEntity>();
		accountStatusChangeHistory = new HashSet<AccountStatusChangeHistoryEntity>();
		accountFlags = new HashSet<AccountFlagMapping>();
		this.accountId = null;
		this.globalAccountNum = generateId(userContext.getBranchGlobalNum());
		this.customer = customer;
		this.accountType = accountType;
		this.office = customer.getOffice();
		this.personnel = customer.getPersonnel();
	}
	
	protected AccountBO(UserContext userContext, CustomerBO customer,
			AccountType accountType, AccountState accountState)
			throws AccountException {
		super(userContext);
		try{
			accountFees = new HashSet<AccountFeesEntity>();
			accountPayments = new HashSet<AccountPaymentEntity>();
			accountActionDates = new HashSet<AccountActionDateEntity>();
			accountCustomFields = new HashSet<AccountCustomFieldEntity>();
			accountNotes = new HashSet<AccountNotesEntity>();
			accountStatusChangeHistory = new HashSet<AccountStatusChangeHistoryEntity>();
			accountFlags = new HashSet<AccountFlagMapping>();
			this.accountId = null;
			this.globalAccountNum = generateId(userContext.getBranchGlobalNum());
			this.customer = customer;
			this.accountType = accountType;
			this.office = customer.getOffice();
			this.personnel = customer.getPersonnel();
			this.setAccountState(new AccountStateEntity(accountState));
			setCreateDetails();
		}catch(IDGenerationException idge){
			throw new AccountException(idge);
		}
		catch(ServiceException se){
			throw new AccountException(se);
		}
	}
	
	protected AccountBO(UserContext userContext) {
		super(userContext);
		accountId = null;
		globalAccountNum = null;
		customer = null;
		office = null;
		personnel = null;
		accountType = null;
		accountFees = new HashSet<AccountFeesEntity>();
		accountPayments = new HashSet<AccountPaymentEntity>();
		accountActionDates = new HashSet<AccountActionDateEntity>();
		accountCustomFields = new HashSet<AccountCustomFieldEntity>();
		accountNotes = new HashSet<AccountNotesEntity>();
		accountStatusChangeHistory = new HashSet<AccountStatusChangeHistoryEntity>();
		accountFlags = new HashSet<AccountFlagMapping>();
	}

	private  Integer accountId;

	protected  String globalAccountNum;

	private Date closedDate;

	protected  CustomerBO customer;

	private AccountStateEntity accountState;

	private Set<AccountFlagMapping> accountFlags;

	protected  AccountType accountType;

	protected  OfficeBO office;

	protected  PersonnelBO personnel;

	private Set<AccountFeesEntity> accountFees;

	private Set<AccountActionDateEntity> accountActionDates;

	private Set<AccountPaymentEntity> accountPayments;

	private Set<AccountCustomFieldEntity> accountCustomFields;

	public Set<AccountNotesEntity> accountNotes;

	public Set<AccountStatusChangeHistoryEntity> accountStatusChangeHistory;
	
	public Integer getAccountId() {
		return accountId;
	}

	public Set<AccountActionDateEntity> getAccountActionDates() {
		return accountActionDates;
	}

	public Set<AccountNotesEntity> getAccountNotes() {
		return accountNotes;
	}

	private void setAccountNotes(Set<AccountNotesEntity> accountNotes) {
		this.accountNotes = accountNotes;
	}

	private void setAccountActionDates(
			Set<AccountActionDateEntity> accountActionDates) {
		this.accountActionDates = accountActionDates;
	}

	public Set<AccountFeesEntity> getAccountFees() {
		return accountFees;
	}

	private void setAccountFees(Set<AccountFeesEntity> accountFees) {
		this.accountFees = accountFees;
	}

	

	public Set<AccountPaymentEntity> getAccountPayments() {
		return accountPayments;
	}

	public void setAccountPayments(Set<AccountPaymentEntity> accountPayments) {
		this.accountPayments = accountPayments;
	}

	public AccountStateEntity getAccountState() {
		return accountState;
	}

	public void setAccountState(AccountStateEntity accountState) {
		this.accountState = accountState;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public String getGlobalAccountNum() {
		return globalAccountNum;
	}

	public Date getClosedDate() {
		return closedDate;
	}

	public void setClosedDate(Date closedDate) {
		this.closedDate = closedDate;
	}

	public CustomerBO getCustomer() {
		return customer;
	}

	

	public OfficeBO getOffice() {
		return office;
	}

	public PersonnelBO getPersonnel() {
		return personnel;
	}

	public Set<AccountStatusChangeHistoryEntity> getAccountStatusChangeHistory() {
		return accountStatusChangeHistory;
	}

	private void setAccountStatusChangeHistory(
			Set<AccountStatusChangeHistoryEntity> accountStatusChangeHistory) {
		this.accountStatusChangeHistory = accountStatusChangeHistory;
	}

	public void addAccountStatusChangeHistory(
			AccountStatusChangeHistoryEntity accountStatusChangeHistoryEntity) {
		accountStatusChangeHistoryEntity.setAccount(this);
		this.accountStatusChangeHistory.add(accountStatusChangeHistoryEntity);
	}

	public Set<AccountCustomFieldEntity> getAccountCustomFields() {
		return accountCustomFields;
	}

	/*
	 * public setter is needed at the time of action form to business object
	 * conversion.
	 */
	public void setAccountCustomFields(
			Set<AccountCustomFieldEntity> accountCustomFields) {
		this.accountCustomFields = accountCustomFields;
	}

	public void setAccountCustomFieldSet(
			Set<AccountCustomFieldEntity> accountCustomFields) {
		if (accountCustomFields != null) {
			for (AccountCustomFieldEntity customField : accountCustomFields) {
				this.addAccountCustomField(customField);
			}
		}
	}

	private void addAccountCustomField(AccountCustomFieldEntity customField) {
		if (customField.getFieldId() != null) {
			AccountCustomFieldEntity accountCustomField = getAccountCustomField(customField
					.getFieldId());
			if (accountCustomField == null) {
				customField.setAccount(this);
				this.accountCustomFields.add(customField);
			} else {
				accountCustomField.setFieldValue(customField.getFieldValue());
			}
		}
	}

	private AccountCustomFieldEntity getAccountCustomField(Short fieldId) {
		if (null != this.accountCustomFields
				&& this.accountCustomFields.size() > 0) {
			for (AccountCustomFieldEntity obj : this.accountCustomFields) {
				if (obj.getFieldId().equals(fieldId))
					return obj;
			}
		}
		return null;
	}

	public void resetAccountActionDates() {
		this.accountActionDates.clear();
	}

	public void addAccountFees(AccountFeesEntity fees) {
		fees.setAccount(this);
		accountFees.add(fees);
		setAccountFees(accountFees);
	}

	public void addAccountActionDate(AccountActionDateEntity accountAction) {
		if (accountAction == null) {
			// TODO generate a new InvalidStateException
			throw new NullPointerException();
		}
		this.getAccountActionDates().add(accountAction);
	}

	public void addAccountPayment(AccountPaymentEntity payment) {
		if (accountPayments == null)
			accountPayments = new HashSet<AccountPaymentEntity>();
		accountPayments.add(payment);
	}

	public void addAccountNotes(AccountNotesEntity notes) {
		notes.setAccount(this);
		accountNotes.add(notes);
	}

	public Set<AccountFlagMapping> getAccountFlags() {
		return accountFlags;
	}

	private void setAccountFlags(Set<AccountFlagMapping> accountFlags) {
		this.accountFlags = accountFlags;
	}

	public void addAccountFlag(AccountStateFlagEntity flagDetail) {
		AccountFlagMapping flagMap = new AccountFlagMapping();
		flagMap.setCreatedBy(this.getUserContext().getId());
		flagMap.setCreatedDate(new Date());
		flagMap.setFlag(flagDetail);
		this.accountFlags.add(flagMap);
	}

	public void applyPayment(PaymentData paymentData) throws AccountException,
			SystemException {
		AccountPaymentEntity accountPayment = makePayment(paymentData);
		addAccountPayment(accountPayment);
		try {
			buildFinancialEntries(accountPayment.getAccountTrxns());
			getAccountPersistenceService().update(this);
		} catch (FinancialException fe) {
			throw new AccountException("errors.update", fe);
		} catch (ServiceException e) {
			throw new AccountException("errors.update", e);
		}

	}

	protected AccountPaymentEntity makePayment(PaymentData accountPaymentData)
			throws AccountException, SystemException {
		return null;
	}

	protected void updateTotalFeeAmount(Money totalFeeAmount) {
	}

	public void updateTotalPenaltyAmount(Money totalPenaltyAmount) {
	}

	public Money updateAccountActionDateEntity(List<Short> intallmentIdList,
			Short feeId) {
		return new Money();
	}

	public void updateAccountFeesEntity(Short feeId) {
		Set<AccountFeesEntity> accountFeesEntitySet = this.getAccountFees();
		for (AccountFeesEntity accountFeesEntity : accountFeesEntitySet) {
			if (accountFeesEntity.getFees().getFeeId().equals(feeId)) {
				accountFeesEntity.changeFeesStatus(
						AccountConstants.INACTIVE_FEES, new Date(System
								.currentTimeMillis()));
				accountFeesEntity.setLastAppliedDate(null);
			}
		}
	}

	public FeeBO getAccountFeesObject(Short feeId) {
		Set<AccountFeesEntity> accountFeesEntitySet = this.getAccountFees();
		for (AccountFeesEntity accountFeesEntity : accountFeesEntitySet) {
			if (accountFeesEntity.getFees().getFeeId().equals(feeId)) {
				return accountFeesEntity.getFees();
			}
		}
		return null;
	}

	public AccountFeesEntity getAccountFees(Short feeId) {
		Set<AccountFeesEntity> accountFeesEntitySet = this.getAccountFees();
		for (AccountFeesEntity accountFeesEntity : accountFeesEntitySet) {
			if (accountFeesEntity.getFees().getFeeId().equals(feeId)) {
				return accountFeesEntity;
			}
		}
		return null;
	}

	public Boolean isFeeActive(Short feeId) {
		Set<AccountFeesEntity> accountFeesEntitySet = this.getAccountFees();
		for (AccountFeesEntity accountFeesEntity : accountFeesEntitySet) {
			if (accountFeesEntity.getFees().getFeeId().equals(feeId)) {
				if (accountFeesEntity.getFeeStatus() == null
						|| accountFeesEntity.getFeeStatus().equals(
								AccountConstants.ACTIVE_FEES)) {
					return true;
				}
			}
		}
		return false;
	}

	protected void buildFinancialEntries(Set<AccountTrxnEntity> accountTrxns)
			throws ServiceException, FinancialException {
		FinancialBusinessService financialBusinessService = (FinancialBusinessService) ServiceFactory
				.getInstance()
				.getBusinessService(BusinessServiceName.Financial);
		for (AccountTrxnEntity accountTrxn : accountTrxns) {
			financialBusinessService.buildAccountingEntries(accountTrxn);
		}
	}

	protected String generateId(String officeGlobalNum)
			throws ServiceException, IDGenerationException {
		StringBuilder systemId = new StringBuilder();
		systemId.append(officeGlobalNum);
		MifosLogManager.getLogger(LoggerConstants.ACCOUNTSLOGGER).debug(
				"After appending the officeGlobalNum to loanAccountSysID  it becomes"
						+ systemId.toString());
		// setting the 11 digits of account running number.
		try {
			systemId.append(StringUtils.lpad(getAccountPersistenceService()
					.getAccountRunningNumber().toString(), '0', 11));
			MifosLogManager.getLogger(LoggerConstants.ACCOUNTSLOGGER).debug(
					"After appending the running number to loanAccountSysID  it becomes"
							+ systemId.toString());
		} catch (PersistenceException se) {
			MifosLogManager.getLogger(LoggerConstants.ACCOUNTSLOGGER).error(
					"There was some error retieving the running number", true,
					null, se);
			throw new IDGenerationException(
					AccountExceptionConstants.IDGenerationException, se);
		}
		return systemId.toString();
	}

	protected AccountPersistanceService getAccountPersistenceService()
			throws ServiceException {
		if (accountPersistanceService == null) {
			accountPersistanceService = (AccountPersistanceService) ServiceFactory
					.getInstance().getPersistenceService(
							PersistenceServiceName.Account);
		}
		return accountPersistanceService;
	}

	public double getLastPmntAmnt() {
		if (null != accountPayments && accountPayments.size() > 0) {
			return getLastPmnt().getAmount().getAmountDoubleValue();
		}
		return 0;
	}

	/**
	 * If there are no accountPayments associated then this method will throw a
	 * NullPointerException.
	 */
	public AccountPaymentEntity getLastPmnt() {
		AccountPaymentEntity accntPmnt = null;
		for (AccountPaymentEntity accntPayment : accountPayments) {

			accntPmnt = accntPayment;
			break;
		}
		return accntPmnt;
	}

	/**
	 * This is just a dummy implementation, actual implementation should be with
	 * LoanBO or SavingsBO.
	 */
	public boolean isAdjustPossibleOnLastTrxn() {
		return false;
	}

	public AccountActionDateEntity getAccountActionDate(Short installmentId) {
		if (null != accountActionDates && accountActionDates.size() > 0) {
			for (AccountActionDateEntity accntActionDate : accountActionDates) {
				if (accntActionDate.getInstallmentId().equals(installmentId)) {
					return accntActionDate;
				}
			}
		}
		return null;
	}

	public AccountActionDateEntity getAccountActionDate(Short installmentId,
			Integer customerId) {
		if (null != accountActionDates && accountActionDates.size() > 0) {
			for (AccountActionDateEntity accntActionDate : accountActionDates) {
				if (accntActionDate.getInstallmentId().equals(installmentId)
						&& accntActionDate.getCustomer().getCustomerId()
								.equals(customerId)) {
					return accntActionDate;
				}
			}
		}
		return null;
	}

	public void removeFees(Short feeId, Short personnelId)
			throws SystemException, ApplicationException {
		List<Short> installmentIdList = getApplicableInstallmentIdsForRemoveFees();
		Money totalFeeAmount = new Money();
		if (installmentIdList != null && installmentIdList.size() != 0
				&& isFeeActive(feeId)) {
			totalFeeAmount = updateAccountActionDateEntity(installmentIdList,
					feeId);
			updateAccountFeesEntity(feeId);
			updateTotalFeeAmount(totalFeeAmount);
			FeeBO feesBO = getAccountFeesObject(feeId);
			String description = feesBO.getFeeName() + " "
					+ AccountConstants.FEES_REMOVED;
			updateAccountActivity(totalFeeAmount, personnelId, description);
			roundInstallments(installmentIdList);
		}

	}
	
	private List<Short> getApplicableInstallmentIdsForRemoveFees() {
		List<Short> installmentIdList = new ArrayList<Short>();
		for(AccountActionDateEntity accountActionDateEntity : getApplicableIdsForFutureInstallments()){
			installmentIdList.add(accountActionDateEntity.getInstallmentId());
		}
		installmentIdList.add(getDetailsOfNextInstallment().getInstallmentId());
		return installmentIdList;
	}

	public void roundInstallments(List<Short> installmentIdList) {
	}

	public void updateAccountActivity(Money totalFeeAmount, Short personnelId,
			String description) {
	}

	public void adjustPmnt(String adjustmentComment)
			throws ApplicationException, SystemException {
		if (isAdjustPossibleOnLastTrxn()) {
			MifosLogManager.getLogger(LoggerConstants.ACCOUNTSLOGGER).debug(
					"Adjustment is possible hence attempting to adjust.");
			List<AccountTrxnEntity> reversedTrxns = getLastPmnt()
					.reversalAdjustment(adjustmentComment);
			updateInstallmentAfterAdjustment(reversedTrxns);
			buildFinancialEntries(new HashSet(reversedTrxns));
			updatePerformanceHistoryOnAdjustment(reversedTrxns.size());
			((AccountPersistanceService) ServiceFactory.getInstance()
					.getPersistenceService(PersistenceServiceName.Account))
					.save(this);
		} else
			throw new ApplicationException(
					AccountExceptionConstants.CANNOTADJUST);
	}

	protected void updatePerformanceHistoryOnAdjustment(Integer noOfTrxnReversed) {
	}

	protected void updateInstallmentAfterAdjustment(
			List<AccountTrxnEntity> reversedTrxns) {
	}

	protected List<AccountActionDateEntity> getApplicableIdsForDueInstallments() {
		List<AccountActionDateEntity> dueActionDateList = new ArrayList<AccountActionDateEntity>();
		if (isCurrentDateEquallToInstallmentDate()) {
			for (AccountActionDateEntity accountActionDateEntity : getAccountActionDates()) {
				if (accountActionDateEntity.getPaymentStatus().equals(
						PaymentStatus.UNPAID.getValue())) {
					if (accountActionDateEntity.compareDate(DateUtils
							.getCurrentDateWithoutTimeStamp()) <= 0) {
						dueActionDateList.add(accountActionDateEntity);
					}
				}
			}
		} else {
			Boolean flag = true;
			for (AccountActionDateEntity accountActionDateEntity : getAccountActionDates()) {
				if (accountActionDateEntity.getPaymentStatus().equals(
						PaymentStatus.UNPAID.getValue())) {
					if (accountActionDateEntity.compareDate(DateUtils
							.getCurrentDateWithoutTimeStamp()) < 0) {
						dueActionDateList.add(accountActionDateEntity);
					} else if (flag == true
							&& accountActionDateEntity
									.getActionDate()
									.compareTo(
											DateUtils
													.getCurrentDateWithoutTimeStamp()) > 0) {
						dueActionDateList.add(accountActionDateEntity);
						flag = false;
					}
				}
			}
		}
		return dueActionDateList;
	}

	public List<AccountActionDateEntity> getApplicableIdsForFutureInstallments() {
		List<AccountActionDateEntity> futureActionDateList = new ArrayList<AccountActionDateEntity>();
		AccountActionDateEntity accountActionDate = null;
		for (AccountActionDateEntity accountActionDateEntity : getAccountActionDates()) {
			if (accountActionDateEntity.getPaymentStatus().equals(
					PaymentStatus.UNPAID.getValue())) {
				if (accountActionDateEntity.compareDate(DateUtils
						.getCurrentDateWithoutTimeStamp()) >= 0) {
					if (accountActionDate == null) {
						accountActionDate = accountActionDateEntity;
					} else if (!accountActionDate.getInstallmentId().equals(
							(accountActionDateEntity.getInstallmentId())))
						futureActionDateList.add(accountActionDateEntity);
				}
			}
		}
		return futureActionDateList;
	}

	public List<AccountActionDateEntity> getPastInstallments() {
		List<AccountActionDateEntity> pastActionDateList = new ArrayList<AccountActionDateEntity>();

		for (AccountActionDateEntity accountActionDateEntity : getAccountActionDates()) {

			if (accountActionDateEntity.compareDate(DateUtils
					.getCurrentDateWithoutTimeStamp()) < 0) {
				pastActionDateList.add(accountActionDateEntity);
			}

		}
		return pastActionDateList;

	}

	protected boolean isCurrentDateEquallToInstallmentDate() {
		for (AccountActionDateEntity accountActionDateEntity : getAccountActionDates()) {
			if (accountActionDateEntity.getPaymentStatus().equals(
					PaymentStatus.UNPAID.getValue())) {
				if (accountActionDateEntity.compareDate(DateUtils
						.getCurrentDateWithoutTimeStamp()) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	public Short getEntityID() {

		return null;
	}

	public List<TransactionHistoryView> getTransactionHistoryView() {

		List<TransactionHistoryView> trxnHistory = new ArrayList<TransactionHistoryView>();
		for (AccountPaymentEntity accountPayment : getAccountPayments()) {
			for (AccountTrxnEntity accountTrxn : accountPayment
					.getAccountTrxns()) {
				for (FinancialTransactionBO financialTrxn : accountTrxn
						.getFinancialTransactions()) {
					TransactionHistoryView transactionHistory = new TransactionHistoryView();
					setFinancialEntries(financialTrxn, transactionHistory);
					setAccountingEntries(accountTrxn, transactionHistory);
					trxnHistory.add(transactionHistory);
				}
			}
		}

		return trxnHistory;
	}

	public Money removeSign(Money amount) {
		if (amount != null && amount.getAmountDoubleValue() < 0)
			return amount.negate();
		else
			return amount;
	}

	private void setFinancialEntries(FinancialTransactionBO financialTrxn,
			TransactionHistoryView transactionHistory) {
		String debit = "-", credit = "-", notes = "-";
		if (financialTrxn.isDebitEntry()) {
			debit = String.valueOf(removeSign(financialTrxn.getPostedAmount()));
		} else if (financialTrxn.isCreditEntry()) {
			credit = String
					.valueOf(removeSign(financialTrxn.getPostedAmount()));
		}
		if (financialTrxn.getNotes() != null
				&& !financialTrxn.getNotes().equals(""))
			notes = financialTrxn.getNotes();
		transactionHistory.setFinancialEnteries(financialTrxn.getActionDate(),
				financialTrxn.getFinancialAction().getName(
						userContext.getLocaleId()), financialTrxn.getGlcode()
						.getGlcode(), debit, credit, financialTrxn
						.getPostedDate(), notes);

	}

	private void setAccountingEntries(AccountTrxnEntity accountTrxn,
			TransactionHistoryView transactionHistory) {

		transactionHistory.setAccountingEnteries(accountTrxn
				.getAccountPayment().getPaymentId(), accountTrxn
				.getAccountTrxnId(), String.valueOf(removeSign(accountTrxn
				.getAmount())), accountTrxn.getCustomer().getDisplayName(),
				accountTrxn.getCustomer().getPersonnel().getDisplayName());
	}

	protected List<AccountTrxnEntity> getAccountTrxnsOrderByTrxnDate() {
		List<AccountTrxnEntity> accountTrxnList = new ArrayList<AccountTrxnEntity>();
		for (AccountPaymentEntity payment : getAccountPayments()) {
			accountTrxnList.addAll(payment.getAccountTrxns());
		}

		Collections.sort(accountTrxnList, new Comparator<AccountTrxnEntity>() {
			public int compare(AccountTrxnEntity trx1, AccountTrxnEntity trx2) {
				if (trx1.getActionDate().equals(trx2.getActionDate()))
					return trx1.getAccountTrxnId().compareTo(
							trx2.getAccountTrxnId());
				else
					return trx1.getActionDate().compareTo(trx2.getActionDate());
			}
		});
		return accountTrxnList;
	}

	public void waiveAmountDue(WaiveEnum waiveType) throws ServiceException,
			AccountException {
	}

	public void waiveAmountOverDue(WaiveEnum waiveType)
			throws ServiceException, AccountException {
	}

	public Date getNextMeetingDate() {
		AccountActionDateEntity nextAccountAction = getDetailsOfNextInstallment();
		Date currentDate = DateUtils.getCurrentDateWithoutTimeStamp();
		return nextAccountAction != null ? nextAccountAction.getActionDate()
				: currentDate;
	}

	public List<AccountActionDateEntity> getDetailsOfInstallmentsInArrears() {
		List<AccountActionDateEntity> installmentsInArrears = new ArrayList<AccountActionDateEntity>();
		Date currentDate = DateUtils.getCurrentDateWithoutTimeStamp();
		if (getAccountActionDates() != null
				&& getAccountActionDates().size() > 0) {
			for (AccountActionDateEntity accountAction : getAccountActionDates()) {
				if (accountAction.getActionDate().compareTo(currentDate) < 0
						&& accountAction.getPaymentStatus().equals(
								PaymentStatus.UNPAID.getValue()))
					installmentsInArrears.add(accountAction);
			}
		}
		return installmentsInArrears;
	}

	public AccountActionDateEntity getDetailsOfNextInstallment() {
		AccountActionDateEntity nextAccountAction = null;
		Date currentDate = DateUtils.getCurrentDateWithoutTimeStamp();
		if (getAccountActionDates() != null
				&& getAccountActionDates().size() > 0) {
			for (AccountActionDateEntity accountAction : getAccountActionDates()) {
				if (accountAction.getActionDate().compareTo(currentDate) >= 0)
					if (null == nextAccountAction)
						nextAccountAction = accountAction;
					else if (nextAccountAction.getInstallmentId() > accountAction
							.getInstallmentId())
						nextAccountAction = accountAction;
			}
		}

		return nextAccountAction;
	}

	protected List<AccountFeesEntity> getPeriodicFeeList() {
		List<AccountFeesEntity> periodicFeeList = new ArrayList<AccountFeesEntity>();
		
		for (AccountFeesEntity accountFee : getAccountFees()) {
			if (accountFee.getFees().isPeriodic()) {
				new FeePersistence().getFee(accountFee.getFees().getFeeId());
				periodicFeeList.add(accountFee);
			}
		}
		return periodicFeeList;
	}

	public AccountFeesEntity getPeriodicAccountFees(Short feeId) {
		for (AccountFeesEntity accountFeesEntity : getAccountFees()) {
			if (feeId.equals(accountFeesEntity.getFees().getFeeId())) {
				return accountFeesEntity;
			}
		}
		return null;
	}

	public Money getTotalAmountDue() {
		Money totalAmt = getTotalAmountInArrears();
		AccountActionDateEntity nextInstallment = getDetailsOfNextInstallment();
		if (nextInstallment != null
				&& nextInstallment.getPaymentStatus().equals(
						PaymentStatus.UNPAID.getValue()))
			totalAmt = totalAmt.add(getDueAmount(nextInstallment));
		return totalAmt;
	}

	public Money getTotalPaymentDue() {
		Money totalAmt = getTotalAmountInArrears();
		AccountActionDateEntity nextInstallment = getDetailsOfNextInstallment();
		if (nextInstallment != null
				&& nextInstallment.getPaymentStatus().equals(
						PaymentStatus.UNPAID.getValue())
				&& DateUtils.getDateWithoutTimeStamp(
						nextInstallment.getActionDate().getTime()).equals(
						DateUtils.getCurrentDateWithoutTimeStamp()))
			totalAmt = totalAmt.add(getDueAmount(nextInstallment));
		return totalAmt;
	}

	public Money getTotalAmountInArrears() {
		List<AccountActionDateEntity> installmentsInArrears = getDetailsOfInstallmentsInArrears();
		Money totalAmount = new Money();
		if (installmentsInArrears != null && installmentsInArrears.size() > 0)
			for (AccountActionDateEntity accountAction : installmentsInArrears)
				totalAmount = totalAmount.add(getDueAmount(accountAction));
		return totalAmount;
	}

	protected Money getDueAmount(AccountActionDateEntity installment) {
		return null;
	}

	public List<AccountActionDateEntity> getTotalInstallmentsDue() {
		List<AccountActionDateEntity> dueInstallments = getDetailsOfInstallmentsInArrears();
		AccountActionDateEntity nextInstallment = getDetailsOfNextInstallment();
		if (nextInstallment != null
				&& nextInstallment.getPaymentStatus().equals(
						PaymentStatus.UNPAID.getValue())
				&& DateUtils.getDateWithoutTimeStamp(
						nextInstallment.getActionDate().getTime()).equals(
						DateUtils.getCurrentDateWithoutTimeStamp()))
			dueInstallments.add(nextInstallment);
		return dueInstallments;
	}

	public boolean isTrxnDateValid(Date trxnDate) throws ApplicationException,
			SystemException {

		if (Configuration.getInstance().getAccountConfig(
				getOffice().getOfficeId()).isBackDatedTxnAllowed()) {
			Date meetingDate = getCustomerDBService()
					.getLastMeetingDateForCustomer(
							getCustomer().getCustomerId());
			Date lastMeetingDate = null;
			if (meetingDate != null) {
				lastMeetingDate = DateUtils.getDateWithoutTimeStamp(meetingDate
						.getTime());
				return trxnDate.compareTo(lastMeetingDate) >= 0 ? true : false;
			} else
				return false;

		}
		return trxnDate.equals(DateUtils.getCurrentDateWithoutTimeStamp());
	}

	private CustomerPersistenceService getCustomerDBService()
			throws ServiceException {
		return (CustomerPersistenceService) ServiceFactory.getInstance()
				.getPersistenceService(PersistenceServiceName.Customer);
	}

	public void handleChangeInMeetingSchedule() throws SchedulerException,
			ServiceException, HibernateException, PersistenceException {
		AccountActionDateEntity accountActionDateEntity = getDetailsOfNextInstallment();
		if (accountActionDateEntity != null) {
			MeetingBO meeting = getCustomer().getCustomerMeeting().getMeeting();
			Calendar meetingStartDate = meeting.getMeetingStartDate();
			meeting.setMeetingStartDate(DateUtils
					.getCalendarDate(accountActionDateEntity.getActionDate()
							.getTime()));
			regenerateFutureInstallments((short) (accountActionDateEntity
					.getInstallmentId().intValue() + 1));
			meeting.setMeetingStartDate(meetingStartDate);
			getAccountPersistenceService().update(this);
		}
	}

	protected void regenerateFutureInstallments(Short nextIntallmentId)
			throws HibernateException, ServiceException, PersistenceException,
			SchedulerException {
	}

	protected void deleteFutureInstallments() throws HibernateException,
			ServiceException {
		List<AccountActionDateEntity> futureInstllments = getApplicableIdsForFutureInstallments();
		for (AccountActionDateEntity accountActionDateEntity : futureInstllments) {
			accountActionDates.remove(accountActionDateEntity);
			getAccountPersistenceService().delete(accountActionDateEntity);
		}
	}

	public Money getTotalPrincipalAmountInArrears() {
		Money amount = new Money();
		List<AccountActionDateEntity> actionDateList = getDetailsOfInstallmentsInArrears();
		for (AccountActionDateEntity accountActionDateEntity : actionDateList) {
			amount = amount.add(((LoanScheduleEntity)accountActionDateEntity).getPrincipal());
		}
		return amount;
	}

	public List<AccountNotesEntity> getRecentAccountNotes() {
		List<AccountNotesEntity> notes = new ArrayList<AccountNotesEntity>();
		int count = 0;
		for (AccountNotesEntity accountNotesEntity : getAccountNotes()) {
			if (count > 2)
				break;
			notes.add(accountNotesEntity);
			count++;
		}
		return notes;
	}

	public void update() throws SystemException {
		this.setUpdatedBy(userContext.getId());
		this.setUpdatedDate(new Date());
		getAccountPersistenceService().update(this);
	}
	
	protected Meeting convertM2StyleToM1(MeetingBO meeting) {

		Meeting meetingM1 = null;
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			meetingM1 = (Meeting) session.get(Meeting.class, meeting
					.getMeetingId());
		} catch (HibernateProcessException e) {
			e.printStackTrace();
		} finally {
			try {
				HibernateUtil.closeSession(session);
			} catch (HibernateProcessException e) {
				e.printStackTrace();
			}
		}
		return meetingM1;
	}
	
	// TODO this method will go once scheduler is moved to m2 style
	protected AccountFees getAccountFees(Integer accountFeeId) {
		AccountFees accountFees = new AccountFees();
		Session session = null;
		try {
			session = HibernateUtil.getSession();
			accountFees = (AccountFees) session.get(AccountFees.class,
					accountFeeId);
			Fees fees = accountFees.getFees();
			initializeMeetings(fees);
			if (null != fees) {
				fees.getFeeFrequency().getFeeFrequencyId();
			}
		} catch (HibernateProcessException e) {
			e.printStackTrace();
		} finally {
			try {
				HibernateUtil.closeSession(session);
			} catch (HibernateProcessException e) {
				e.printStackTrace();
			}
		}
		Hibernate.initialize(accountFees);
		return accountFees;
	}
	


	private void initializeMeetings(Fees fees) {

		if (fees.getFeeFrequency().getFeeFrequencyTypeId().equals(
				FeeFrequencyType.PERIODIC.getValue())) {
			Meeting meeting = fees.getFeeFrequency().getFeeMeetingFrequency();
			meeting.getMeetingType().getMeetingPurpose();
		}

	}
	
	protected  Short getLastInstallmentId(){
		
		Short LastInstallmentId = null;
		for (AccountActionDateEntity date : this.getAccountActionDates()) {
			
			if( LastInstallmentId ==null) LastInstallmentId = date.getInstallmentId();
			else {
				if ( LastInstallmentId < date.getInstallmentId()) LastInstallmentId= date.getInstallmentId();
			}
			
		}
		return LastInstallmentId;
		
	}
	
	public void changeStatus(Short newStatusId, Short flagId, String comment) throws SecurityException, ServiceException, PersistenceException, ApplicationException {
		if (null != getCustomer().getPersonnel().getPersonnelId())
			checkPermissionForStatusChange(newStatusId, this.getUserContext(),
					flagId, getOffice().getOfficeId(), getCustomer()
							.getPersonnel().getPersonnelId());
		else
			checkPermissionForStatusChange(newStatusId, this.getUserContext(),
					flagId, getOffice().getOfficeId(), this.getUserContext()
							.getId());
		MasterPersistenceService masterPersistenceService = (MasterPersistenceService) ServiceFactory
				.getInstance().getPersistenceService(
						PersistenceServiceName.MasterDataService);
		AccountStateEntity accountStateEntity = (AccountStateEntity) masterPersistenceService
				.findById(AccountStateEntity.class, newStatusId);
		//checkStatusChangeAllowed(accountStateEntity);
		accountStateEntity.setLocaleId(this.getUserContext().getLocaleId());
		AccountStateFlagEntity accountStateFlagEntity = null;
		if (flagId != null) {
			accountStateFlagEntity = (AccountStateFlagEntity) masterPersistenceService
					.findById(AccountStateFlagEntity.class, flagId);
		}
		AccountStatusChangeHistoryEntity historyEntity = new AccountStatusChangeHistoryEntity(
				this.getAccountState(), accountStateEntity, this.getPersonnel());
		AccountNotesEntity accountNotesEntity = createAccountNotes(comment);
		this.addAccountStatusChangeHistory(historyEntity);
		this.setAccountState(accountStateEntity);
		this.addAccountNotes(accountNotesEntity);
		if (accountStateFlagEntity != null) {
			accountStateFlagEntity.setLocaleId(this.getUserContext()
					.getLocaleId());
			this.addAccountFlag(accountStateFlagEntity);
		}
		this.setClosedDate(new Date(System.currentTimeMillis()));
	}
	
	private void checkPermissionForStatusChange(Short newState,
			UserContext userContext, Short flagSelected, Short recordOfficeId,
			Short recordLoanOfficerId) throws SecurityException {
		if (!isPermissionAllowed(newState, userContext, flagSelected,
				recordOfficeId, recordLoanOfficerId))
			throw new SecurityException(
					SecurityConstants.KEY_ACTIVITY_NOT_ALLOWED);
	}

	private boolean isPermissionAllowed(Short newState,
			UserContext userContext, Short flagSelected, Short recordOfficeId,
			Short recordLoanOfficerId) {
		return ActivityMapper.getInstance().isStateChangePermittedForAccount(
				newState.shortValue(),
				null != flagSelected ? flagSelected.shortValue() : 0,
				userContext, recordOfficeId, recordLoanOfficerId);
	}
	
	private AccountNotesEntity createAccountNotes(String comment)throws ServiceException {
		AccountNotesEntity accountNotes = new AccountNotesEntity();
		accountNotes.setCommentDate(new java.sql.Date(System
				.currentTimeMillis()));
		accountNotes.setPersonnel(this.getPersonnel());
		accountNotes.setComment(comment);
		return accountNotes;
	}
	
	protected void checkStatusChangeAllowed(AccountStateEntity newState) throws ApplicationException {
		if (!(AccountStateMachines.getInstance().isTransitionAllowed(this,newState))) {
			throw new StateChangeException(	SavingsConstants.STATUS_CHANGE_NOT_ALLOWED);
		}

	}
	
	public void initializeStateMachine(Short localeId) throws StatesInitializationException{
	}
	
	public List<AccountStateEntity> getStatusList() {
		return null;
	}
	
	public String getStatusName(Short localeId, Short accountStateId) throws ApplicationException, SystemException {
		return null;
	}

	public String getFlagName(Short flagId) throws ApplicationException,SystemException {
		return null;
	}
}
