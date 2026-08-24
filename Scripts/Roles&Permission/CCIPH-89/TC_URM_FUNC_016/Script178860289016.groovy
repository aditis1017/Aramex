import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-89 | TC_U&RM_FUNC_016 | AC6 - Trigger search after minimum characters

def login = new MSLogin()
def roles = new RolesPermission()

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Enter at least 1 character and verify search is triggered'
roles.verifySearchTriggeredWithMinimumCharacters('S')
WebUI.takeScreenshot()
WebUI.closeBrowser()