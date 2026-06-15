import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import customsmanagement.MicrosoftLoginKeywords as MSLogin
import customsmanagement.ProfileGlobals
import emailnotification.EmailNotificationKeywords as EmailNoti
import globalsettings.BusinessActions as GS

// CSV: Global Settings - Sheet1.csv | CCIPH-39 | TC_GS_FUNC_044 — Email_notification


	def login = new MSLogin()
	def globalSettings = new GS()
	def emailNoti = new EmailNoti()

	def ruleValue = String.valueOf(System.currentTimeMillis() % 100000)
	String activeTab = (tab ?: ruleTypeTab ?: 'DDU').trim()

	'Login as Super Admin'
	login.loginToCustomsManagementUsingProfile()

	'Navigate to Global Settings'
	globalSettings.navigateToGlobalSettings()
	globalSettings.openTab(activeTab)

	'Record timestamp before Save — used to find the notification email'
	def pollStart = emailNoti.recordPollStartMillis()

	'Create rule and save'
	globalSettings.clickAddNewRule()
	globalSettings.verifyRuleModalVisible('Add New Rule modal')
//	if ((conditionField ?: '').trim() && (conditionOperator ?: '').trim()) {
//		globalSettings.selectConditionFieldOperatorAndValueByLabel(conditionField, conditionOperator, ruleValue)
//	} else {
		globalSettings.addCostOfGoodsFieldAndValue(ruleValue)
	//}
//	if ((actionSummary ?: '').trim()) {
//		globalSettings.selectTargetValue(actionSummary)
//	}
	globalSettings.clickModalSave()

	'Verify on-screen success message'
	emailNoti.verifyRuleCreatedSuccessfullyOnScreen('Rule Created Successfully')
	emailNoti.dismissRuleCreatedSuccessModalIfPresent()

	'Verify IMAP profile before polling mailbox'
	emailNoti.assertImapProfileConfiguredForEmailNotification()

	'Verify notification email via IMAP'
	String emailRuleType = (ruleTypeTab ?: activeTab ?: 'DDU').trim()
	emailNoti.verifyGlobalSettingCreatedNotificationEmail(
		pollStart,
		expectedEmailSubject,
		expectedActionPerformed,
		emailRuleType,
		actionSummary,
		ProfileGlobals.get('MS_LOGIN_EMAIL')
	)

	WebUI.takeScreenshot()
//} catch (Exception e) {
	KeywordUtil.markFailed("Email_notification failed: ${e.getMessage()}")
//} finally {
//	WebUI.closeBrowser()
//}
