import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-95 | TC_U&RM_FUNC_010 | AC4 - Permissions view only

def login = new MSLogin()
def roles = new RolesPermission()

String roleName = 'Super Admin'
if (binding.hasVariable('detailsRoleName') && detailsRoleName) {
	roleName = detailsRoleName as String
}

//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
roles.clickViewDetailsForRole(roleName)
roles.verifyPermissionsAreViewOnlyOnDetailsScreen()
WebUI.takeScreenshot()
WebUI.closeBrowser()
