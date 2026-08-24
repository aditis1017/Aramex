import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-89 | TC_U&RM_FUNC_014 | AC3 - Search roles

def login = new MSLogin()
def roles = new RolesPermission()

String searchRole = 'Super Admin'

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Search for an existing role name and verify filtered result'
roles.searchRoles(searchRole)
roles.verifyOnlyRolesVisible([searchRole])
WebUI.takeScreenshot()
WebUI.closeBrowser()