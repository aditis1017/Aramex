import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-95 | TC_U&RM_FUNC_009 | AC1/AC2/AC3 - View Details UI

def login = new MSLogin()
def roles = new RolesPermission()

String roleName = 'Super Admin'
if (binding.hasVariable('detailsRoleName') && detailsRoleName) {
	roleName = detailsRoleName as String
}

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Click View Details on role card and verify details screen'
roles.clickViewDetailsForRole(roleName)
roles.verifyRoleDetailsBasicInfo(roleName)
roles.verifyPermissionsGroupedByModule()
roles.verifyRoleDetailsPermissionsScrollable()
WebUI.takeScreenshot()
WebUI.closeBrowser()
